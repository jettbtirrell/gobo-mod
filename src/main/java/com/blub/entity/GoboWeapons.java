package com.blub.entity;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Tier ladder for the melee weapons a Gobo will swap between. */
public final class GoboWeapons {
    private static final List<List<Item>> TIERS = List.of(
        List.of(Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.WOODEN_SPEAR),
        List.of(Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_SPEAR),
        List.of(Items.COPPER_SWORD, Items.COPPER_AXE, Items.COPPER_SPEAR),
        List.of(Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_SPEAR),
        List.of(Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_SPEAR),
        List.of(Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_SPEAR),
        List.of(Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_SPEAR)
    );

    private GoboWeapons() {
    }

    /** Index into the tier ladder, or -1 if the stack is not a swappable melee weapon. */
    public static int tier(ItemStack stack) {
        for (int i = 0; i < TIERS.size(); i++) {
            if (TIERS.get(i).contains(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isWeapon(ItemStack stack) {
        return tier(stack) >= 0;
    }

    /**
     * Whether the Gobo takes {@code offered} in exchange for {@code current}: the same tier or higher,
     * and a plain weapon never replaces an enchanted one.
     */
    public static boolean wantsSwap(ItemStack offered, ItemStack current) {
        int offeredTier = tier(offered);
        if (offeredTier < 0 || offeredTier < tier(current)) {
            return false;
        }
        return offered.isEnchanted() || !current.isEnchanted();
    }

    /** One iron ingot per tier step gained, plus one if a plain weapon is traded for an enchanted one. */
    public static int ironFor(ItemStack offered, ItemStack current) {
        int currentTier = tier(current);
        if (currentTier < 0) {
            return 0;
        }
        int iron = Math.max(0, tier(offered) - currentTier);
        if (offered.isEnchanted() && !current.isEnchanted()) {
            iron++;
        }
        return iron;
    }
}
