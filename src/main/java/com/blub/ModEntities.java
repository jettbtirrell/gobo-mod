package com.blub;

import com.blub.entity.Blub;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final ResourceKey<EntityType<?>> BLUB_KEY = ResourceKey.create(Registries.ENTITY_TYPE, BlubMod.id("blub"));
    public static final EntityType<Blub> BLUB = Registry.register(BuiltInRegistries.ENTITY_TYPE, BLUB_KEY, EntityType.Builder.of(Blub::new, MobCategory.CREATURE).sized(0.7F, 0.75F).build(BLUB_KEY));
    
    public static void register() {
        FabricDefaultAttributeRegistry.register(BLUB, Blub.createAttributes());
    }
}
