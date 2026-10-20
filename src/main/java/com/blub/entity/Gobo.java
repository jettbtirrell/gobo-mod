package com.blub.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.blub.BlubMod;
import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * A spear-wielding goblin. Barters iron ingots for forage like a piglin barters gold, swaps its
 * weapon for better ones dropped nearby, stores food in a pouch and eats it when hurt.
 */
public class Gobo extends Blub implements InventoryCarrier {
    public static final ResourceKey<LootTable> BARTERING_LOOT = ResourceKey.create(Registries.LOOT_TABLE, BlubMod.id("gameplay/gobo_bartering"));

    private static final Codec<Map<UUID, Integer>> TRADE_COUNTS_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT);
    private static final int INVENTORY_SLOT_OFFSET = 300;
    private static final double UNTAMED_MAX_HEALTH = 16.0;
    private static final byte EVENT_UNHAPPY = 120;
    private static final int UNHAPPY_DURATION = 40;
    private static final float FOLLOW_START_DISTANCE = 10.0F;
    private static final int STATION_RADIUS = 8;
    private static final int ADMIRE_DURATION = 119;
    private static final int WEAPON_ADMIRE_DURATION = 20;
    private static final int ADMIRE_DISABLED_DURATION = 400;

    private final SimpleContainer inventory = new SimpleContainer(8);
    private final Map<UUID, Integer> tradeCounts = new HashMap<>();
    private int admireTicks;
    private int unhappyTicks;
    private int admireDisabledTicks;
    private @Nullable UUID barterer;

    public Gobo(EntityType<? extends Gobo> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.MAX_HEALTH, UNTAMED_MAX_HEALTH).add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Gobos never sit by order, and don't leap.
        this.goalSelector.removeAllGoals(goal -> goal instanceof LeapAtTargetGoal || goal instanceof SitWhenOrderedToGoal);
        // Only follow the owner when not stationed; return to the origin if pushed outside the wander radius.
        this.goalSelector.removeAllGoals(goal -> goal instanceof FollowOwnerGoal);
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, FOLLOW_START_DISTANCE, 2.0F) {
            @Override
            public boolean canUse() {
                return !Gobo.this.isStationed() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Gobo.this.isStationed() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(7, new ReturnToOriginGoal(this));
        this.goalSelector.addGoal(7, new SitOnCushionGoal(this));
        this.goalSelector.addGoal(3, new AdmireItemGoal(this));
        this.goalSelector.addGoal(3, new EatStoredFoodGoal(this));
        this.goalSelector.addGoal(4, new SpearAttackGoal(this, 1.0, 1.0, 10.0F, 2.0F));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
        return data;
    }

    @Override
    public int getMaxHeadYRot() {
        return 75;
    }

    // Gobos don't reflect projectiles like Blubs do.
    @Override
    public ProjectileDeflection deflection(Projectile projectile) {
        return ProjectileDeflection.NONE;
    }

    // Food goes into the pouch instead of healing directly, so the Blub feeding path never applies.
    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
    }

    @Override
    protected void applyTamingSideEffects() {
        if (this.isTame()) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(UNTAMED_MAX_HEALTH * 5.0);
            this.setHealth(this.getMaxHealth());
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(UNTAMED_MAX_HEALTH);
        }
    }

    // ---- Persistence / inventory ----

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.writeInventoryToTag(output);
        output.store("TradeCounts", TRADE_COUNTS_CODEC, this.tradeCounts);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setCanPickUpLoot(input.getBooleanOr("CanPickUpLoot", true));
        // Saves from before stationing stored a real sit; convert it to wandering around where it sat.
        if (input.getBooleanOr("Sitting", false)) {
            super.setOrderedToSit(false);
            this.setInSittingPose(false);
            if (!this.hasHome()) {
                this.setHomeTo(this.blockPosition(), STATION_RADIUS);
            }
        }
        this.readInventoryFromTag(input);
        this.tradeCounts.clear();
        input.read("TradeCounts", TRADE_COUNTS_CODEC).ifPresent(this.tradeCounts::putAll);
    }

    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        int inventorySlot = slot - INVENTORY_SLOT_OFFSET;
        return inventorySlot >= 0 && inventorySlot < this.inventory.getContainerSize() ? this.inventory.getSlot(inventorySlot) : super.getSlot(slot);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        this.inventory.removeAllItems().forEach(stack -> this.spawnAtLocation(level, stack));
    }

    // ---- Trade counter ----

    public int getTradeCount(UUID player) {
        return this.tradeCounts.getOrDefault(player, 0);
    }

    private void recordTrade(@Nullable UUID player) {
        if (player != null) {
            this.tradeCounts.merge(player, 1, Integer::sum);
        }
    }

    // ---- Interaction ----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (this.canAdmire(held)) {
            if (this.level() instanceof ServerLevel) {
                this.holdAndAdmire(held.consumeAndReturn(1, player), player.getUUID(), ADMIRE_DURATION);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.canTakeWeapon(held)) {
            if (this.level() instanceof ServerLevel) {
                this.holdAndAdmire(held.consumeAndReturn(1, player), player.getUUID(), WEAPON_ADMIRE_DURATION);
            }
            return InteractionResult.SUCCESS;
        }
        if (!held.isEmpty() && isEdible(held) && this.inventory.canAddItem(held)) {
            if (this.level() instanceof ServerLevel) {
                ItemStack leftover = this.inventory.addItem(held.copy());
                held.consume(held.getCount() - leftover.getCount(), player);
            }
            return InteractionResult.SUCCESS;
        }
        // A weapon it won't swap for is considered and handed back, same as a dropped item.
        if (GoboWeapons.isWeapon(held) && this.isIdle()) {
            if (this.level() instanceof ServerLevel) {
                this.holdAndAdmire(held.consumeAndReturn(1, player), player.getUUID(), WEAPON_ADMIRE_DURATION);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.isTame() && this.isOwnedBy(player)) {
            if (this.level() instanceof ServerLevel level) {
                this.toggleStationed(level);
            }
            return InteractionResult.SUCCESS.withoutItem();
        }
        return super.mobInteract(player, hand);
    }

    // ---- Stationing (replaces Blub's sitting) ----

    // A Gobo never sits; "sitting" means wandering around a saved origin instead of following.
    @Override
    public void setOrderedToSit(boolean orderedToSit) {
    }

    @Override
    public void tame(Player player) {
        super.tame(player);
        this.setHomeTo(this.blockPosition(), STATION_RADIUS);
    }

    /** Gets up from a cushion (if sitting on one). */
    public void stopSitting() {
        if (this.getVehicle() instanceof Cushion) {
            this.stopRiding();
        }
        this.setInSittingPose(false);
    }

    /** Whether the owner is far enough away that this Gobo (in follow mode) would start following. */
    public boolean wantsToFollowOwner() {
        return this.isTame() && !this.isStationed() && this.getOwner() != null
            && this.distanceToSqr(this.getOwner()) > FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE;
    }

    /** Whether the eat goal would start right now, so sitting doesn't fight with it. */
    public boolean wantsToEat() {
        return this.getHealth() < this.getMaxHealth() && this.inventory.hasAnyMatching(Gobo::isEdible);
    }

    private boolean isStationed() {
        return this.hasHome();
    }

    private void toggleStationed(ServerLevel level) {
        this.navigation.stop();
        this.setTarget(null);
        this.jumping = false;
        if (this.isStationed()) {
            this.clearHome();
            level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 4, 0.3, 0.2, 0.3, 1.0);
        } else {
            this.setHomeTo(this.blockPosition(), STATION_RADIUS);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        }
    }

    // ---- Bartering (mirrors PiglinAi) ----

    private static boolean isBarterCurrency(ItemStack stack) {
        return stack.is(Items.IRON_INGOT);
    }

    private boolean canAdmire(ItemStack stack) {
        return this.admireDisabledTicks <= 0 && !this.isAdmiring() && this.getOffhandItem().isEmpty() && isBarterCurrency(stack);
    }

    private boolean canTakeWeapon(ItemStack stack) {
        return this.admireDisabledTicks <= 0 && !this.isAdmiring() && this.getOffhandItem().isEmpty()
            && GoboWeapons.isWeapon(stack) && GoboWeapons.wantsSwap(stack, this.getMainHandItem());
    }

    public boolean isAdmiring() {
        return this.admireTicks > 0;
    }

    /** Client-visible: the Gobo is holding an ingot or new weapon it is looking over. */
    public boolean isAdmiringPose() {
        ItemStack offhand = this.getOffhandItem();
        return !offhand.isEmpty() && !this.isUsingItem();
    }

    private void holdAndAdmire(ItemStack item, @Nullable UUID giver, int duration) {
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.OFFHAND, item);
        this.barterer = giver;
        this.admireTicks = duration;
        this.getNavigation().stop();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // The sit pose is only ever "riding a cushion".
        boolean onCushion = this.getVehicle() instanceof Cushion;
        if (onCushion != this.isInSittingPose()) {
            this.setInSittingPose(onCushion);
        }
        if (this.admireDisabledTicks > 0) {
            this.admireDisabledTicks--;
        }
        // An item loaded from disk has no timer; resume admiring it.
        if (this.admireTicks == 0 && this.isAdmiringPose()) {
            this.admireTicks = isBarterCurrency(this.getOffhandItem()) ? ADMIRE_DURATION : WEAPON_ADMIRE_DURATION;
        }
        if (this.admireTicks > 0 && --this.admireTicks == 0) {
            ItemStack offhand = this.getOffhandItem();
            if (isBarterCurrency(offhand)) {
                this.finishBarter(level);
            } else if (GoboWeapons.isWeapon(offhand) && GoboWeapons.wantsSwap(offhand, this.getMainHandItem())) {
                this.finishWeaponSwap(level);
            } else {
                this.finishReject(level);
            }
        }
    }

    private void finishBarter(ServerLevel level) {
        this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(BARTERING_LOOT);
        List<ItemStack> loot = table.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, this).create(LootContextParamSets.PIGLIN_BARTER));
        this.throwItems(level, loot, this.findRecipient(level, this.barterer));
        this.recordTrade(this.barterer);
        this.barterer = null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean wasHurt = super.hurtServer(level, source, damage);
        if (wasHurt) {
            this.stopSitting();
        }
        if (wasHurt && this.admireTicks > 0) {
            // Like a piglin: a hit while admiring ends the trade. The ingot is lost; a weapon is dropped.
            ItemStack offhand = this.getOffhandItem();
            if (!isBarterCurrency(offhand)) {
                this.spawnAtLocation(level, offhand);
            }
            this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            this.admireTicks = 0;
            this.barterer = null;
            if (source.getEntity() instanceof Player) {
                this.admireDisabledTicks = ADMIRE_DISABLED_DURATION;
            }
        }
        return wasHurt;
    }

    // ---- Head shake (timer modelled on Villager's unhappy counter) ----

    public boolean isUnhappy() {
        return this.unhappyTicks > 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.unhappyTicks > 0) {
            this.unhappyTicks--;
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_UNHAPPY) {
            this.unhappyTicks = UNHAPPY_DURATION;
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ---- Picking up dropped items ----

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING) || !this.canPickUpLoot()) {
            return false;
        }
        // Weapons it won't swap for are picked up too (when idle) so they can be handed back; see pickUpItem.
        // Everything else is ignored, like a piglin ignoring items it has no use for.
        return this.isWanted(stack) || GoboWeapons.isWeapon(stack) && this.isIdle();
    }

    private boolean isIdle() {
        return this.admireDisabledTicks <= 0 && !this.isAdmiring() && this.getOffhandItem().isEmpty();
    }

    private boolean isWanted(ItemStack stack) {
        if (isBarterCurrency(stack)) {
            return this.canAdmire(stack);
        }
        if (GoboWeapons.isWeapon(stack) && this.canTakeWeapon(stack)) {
            return true;
        }
        return isEdible(stack) && this.inventory.canAddItem(stack);
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        boolean wanted = this.isWanted(stack);
        // Only things a player dropped get considered; this also ignores items this Gobo threw back.
        if (!wanted && !(entity.getOwner() instanceof Player)) {
            return;
        }
        UUID giver = entity.getOwner() instanceof Player player ? player.getUUID() : null;
        if (!wanted) {
            this.onItemPickup(entity);
            this.take(entity, stack.getCount());
            this.holdAndAdmire(stack.copy(), giver, WEAPON_ADMIRE_DURATION);
            entity.discard();
        } else if (isBarterCurrency(stack)) {
            this.onItemPickup(entity);
            this.take(entity, 1);
            this.holdAndAdmire(splitOne(entity), giver, ADMIRE_DURATION);
        } else if (isEdible(stack) && !(GoboWeapons.isWeapon(stack))) {
            this.onItemPickup(entity);
            ItemStack toStore = stack.copy();
            ItemStack leftover = this.inventory.addItem(toStore);
            int taken = stack.getCount() - leftover.getCount();
            this.take(entity, taken);
            stack.shrink(taken);
            if (stack.isEmpty()) {
                entity.discard();
            }
        } else {
            this.onItemPickup(entity);
            this.take(entity, 1);
            this.holdAndAdmire(splitOne(entity), giver, WEAPON_ADMIRE_DURATION);
        }
    }

    private static ItemStack splitOne(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        ItemStack one = stack.split(1);
        if (stack.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(stack);
        }
        return one;
    }

    /** Hands an unwanted item back to whoever dropped it, shaking its head. */
    private void finishReject(ServerLevel level) {
        ItemStack rejected = this.getOffhandItem();
        this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        UUID giver = this.barterer;
        this.barterer = null;
        this.throwItems(level, List.of(rejected), this.findRecipient(level, giver));
        this.unhappyTicks = UNHAPPY_DURATION;
        level.broadcastEntityEvent(this, EVENT_UNHAPPY);
    }

    private void finishWeaponSwap(ServerLevel level) {
        ItemStack offered = this.getOffhandItem();
        this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        UUID giver = this.barterer;
        this.barterer = null;
        ItemStack old = this.getMainHandItem();
        int iron = GoboWeapons.ironFor(offered, old);
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.MAINHAND, offered);
        this.setPersistenceRequired();
        List<ItemStack> payout = new java.util.ArrayList<>();
        if (!old.isEmpty()) {
            payout.add(old);
        }
        if (iron > 0) {
            payout.add(new ItemStack(Items.IRON_INGOT, iron));
        }
        this.throwItems(level, payout, this.findRecipient(level, giver));
        this.recordTrade(giver);
    }

    // ---- Food ----

    public static boolean isEdible(ItemStack stack) {
        return stack.has(DataComponents.FOOD) && stack.has(DataComponents.CONSUMABLE);
    }

    @Override
    protected void completeUsingItem() {
        FoodProperties food = this.getUseItem().get(DataComponents.FOOD);
        super.completeUsingItem();
        if (food != null && !this.level().isClientSide()) {
            this.heal(food.nutrition() * 2.0F);
        }
    }

    // ---- Throwing items (mirrors PiglinAi.throwItems) ----

    private Vec3 findRecipient(ServerLevel level, @Nullable UUID preferred) {
        Player player = preferred == null ? null : level.getPlayerByUUID(preferred);
        if (player == null || player.distanceToSqr(this) > 256.0) {
            player = level.getNearestPlayer(this, 16.0);
        }
        if (player != null) {
            return player.position();
        }
        Vec3 random = LandRandomPos.getPos(this, 4, 2);
        return random == null ? this.position() : random;
    }

    private void throwItems(ServerLevel level, List<ItemStack> stacks, Vec3 target) {
        if (stacks.isEmpty()) {
            return;
        }
        this.swing(InteractionHand.OFF_HAND, SwingAnimation.DEFAULT);
        for (ItemStack stack : stacks) {
            BehaviorUtils.throwItem(this, stack, target.add(0.0, 1.0, 0.0));
        }
    }
}
