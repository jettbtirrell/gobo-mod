package com.blub.client.render;

import com.blub.BlubMod;
import com.blub.client.model.GoboModel;
import com.blub.entity.Gobo;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

public class GoboRenderer extends MobRenderer<Gobo, GoboRenderState, GoboModel> {
    private final ItemModelResolver itemModelResolver;

    public GoboRenderer(Context context) {
        super(context, new GoboModel(context.bakeLayer(GoboModel.LAYER_LOCATION)), 0.4F);
        this.itemModelResolver = context.getItemModelResolver();
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public Identifier getTextureLocation(GoboRenderState state) {
        return BlubMod.id("textures/entity/gobo.png");
    }

    @Override
    public GoboRenderState createRenderState() {
        return new GoboRenderState();
    }

    @Override
    public void extractRenderState(Gobo entity, GoboRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
        state.leftArmPose = armPose(entity, HumanoidArm.LEFT);
        state.rightArmPose = armPose(entity, HumanoidArm.RIGHT);
        state.isAggressive = entity.isAggressive();
        state.isSitting = entity.isInSittingPose();
    }

    private static ArmPose armPose(Gobo entity, HumanoidArm arm) {
        return HumanoidMobRenderer.usesSpearPose(entity.getItemHeldByArm(arm), arm, entity) ? ArmPose.SPEAR : ArmPose.EMPTY;
    }
}
