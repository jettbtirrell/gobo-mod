package com.blub;

import com.blub.entity.Blub;
import com.blub.entity.Gobo;
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
    public static final ResourceKey<EntityType<?>> GOBO_KEY = ResourceKey.create(Registries.ENTITY_TYPE, BlubMod.id("gobo"));
    public static final EntityType<Gobo> GOBO = Registry.register(BuiltInRegistries.ENTITY_TYPE, GOBO_KEY, EntityType.Builder.of(Gobo::new, MobCategory.CREATURE).sized(0.6F, 1.3F).build(GOBO_KEY));

    public static void register() {
        FabricDefaultAttributeRegistry.register(BLUB, Blub.createAttributes());
        FabricDefaultAttributeRegistry.register(GOBO, Gobo.createAttributes());
    }
}
