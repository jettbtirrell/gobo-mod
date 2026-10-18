package com.blub;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class BlubItemTags {
    public static final TagKey<Item> BLUB_FOOD = TagKey.create(Registries.ITEM, BlubMod.id("blub_food"));
}
