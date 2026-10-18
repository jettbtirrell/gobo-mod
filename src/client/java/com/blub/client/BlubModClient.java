package com.blub.client;

import com.blub.ModEntities;
import com.blub.client.model.BlubModel;
import com.blub.client.model.GoboModel;
import com.blub.client.render.BlubRenderer;
import com.blub.client.render.GoboRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class BlubModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(BlubModel.LAYER_LOCATION, BlubModel::createBodyLayer);
		EntityRenderers.register(ModEntities.BLUB, BlubRenderer::new);
		ModelLayerRegistry.registerModelLayer(GoboModel.LAYER_LOCATION, GoboModel::createBodyLayer);
		EntityRenderers.register(ModEntities.GOBO, GoboRenderer::new);
	}
}
