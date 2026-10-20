package com.blub.entity;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/** Eats food from the Gobo's pouch while it is hurt and out of combat. */
public class EatStoredFoodGoal extends Goal {
    private static final int COMBAT_COOLDOWN = 100;

    private final Gobo gobo;

    public EatStoredFoodGoal(Gobo gobo) {
        this.gobo = gobo;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.outOfCombat()
            && this.gobo.getHealth() < this.gobo.getMaxHealth()
            && !this.gobo.isUsingItem()
            && !this.gobo.isAdmiring()
            && this.gobo.getOffhandItem().isEmpty()
            && this.findFoodSlot() >= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.gobo.isUsingItem() && this.outOfCombat();
    }

    @Override
    public void start() {
        this.gobo.getNavigation().stop();
        SimpleContainer inventory = this.gobo.getInventory();
        ItemStack food = inventory.removeItem(this.findFoodSlot(), 1);
        this.gobo.setItemInHand(InteractionHand.OFF_HAND, food);
        this.gobo.startUsingItem(InteractionHand.OFF_HAND);
    }

    @Override
    public void stop() {
        if (this.gobo.isUsingItem()) {
            this.gobo.stopUsingItem();
        }
        // Eating was interrupted: put the food back instead of letting it sit in the offhand.
        ItemStack leftover = this.gobo.getOffhandItem();
        if (Gobo.isEdible(leftover)) {
            this.gobo.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            ItemStack overflow = this.gobo.getInventory().addItem(leftover);
            if (!overflow.isEmpty()) {
                this.gobo.spawnAtLocation((ServerLevel) this.gobo.level(), overflow);
            }
        }
    }

    private boolean outOfCombat() {
        return this.gobo.getTarget() == null && this.gobo.tickCount - this.gobo.getLastHurtByMobTimestamp() > COMBAT_COOLDOWN;
    }

    private int findFoodSlot() {
        SimpleContainer inventory = this.gobo.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (Gobo.isEdible(inventory.getItem(i))) {
                return i;
            }
        }
        return -1;
    }
}
