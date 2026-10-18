package com.blub.client.render;

import com.blub.BlubMod;
import com.blub.client.model.BlubModel;
import com.blub.entity.Blub;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class BlubRenderer extends MobRenderer<Blub, BlubRenderState, BlubModel> {

    public BlubRenderer(Context context) {
        super(context, new BlubModel(context.bakeLayer(BlubModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public Identifier getTextureLocation(BlubRenderState state) {
        return BlubMod.id("textures/entity/blub.png");
    }

    @Override
    public BlubRenderState createRenderState() {
        return new BlubRenderState();
    }

    @Override
    public void extractRenderState(Blub entity, BlubRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isSitting = entity.isInSittingPose();
    }
    
}
