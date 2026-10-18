package com.blub.client.model;

import com.blub.BlubMod;
import com.blub.client.render.GoboRenderState;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class GoboModel extends HumanoidModel<GoboRenderState> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(BlubMod.id("gobo"), "main");
	private static final float DEG = Mth.PI / 180.0F;

	public GoboModel(ModelPart root) {
		super(root, RenderTypes::entityCutoutCull);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 2).addBox(-4.5F, -7.0F, -3.0F, 9.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, -1.5F));

		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

		PartDefinition right_ear = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.5F, -7.0F, 1.0F, -2.5F * DEG, 0.0F, 0.0F));

		right_ear.addOrReplaceChild("right_ear_flap", CubeListBuilder.create().texOffs(45, 58).addBox(-8.5F, -3.0F, 0.0F, 9.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, -1.0F, 0.0F, 0.0F, 2.5F * DEG));

		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(45, 58).mirror().addBox(-0.1F, 0.0F, -1.0F, 9.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.1F, -8.0F, 1.0F, -2.5F * DEG, 0.0F, -2.5F * DEG));

		partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 17).addBox(-3.0F, -5.0F, -2.0F, 6.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 14.0F, 0.0F, 12.5F * DEG, 0.0F, 0.0F));

		partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 18).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 10.0F, -1.0F));

		partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(44, 18).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 10.0F, -1.0F));

		partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(2, 18).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.4F, 16.0F, 0.0F));

		partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(2, 18).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.4F, 16.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(GoboRenderState state) {
		super.setupAnim(state);

		if (state.isSitting) {
			float drop = 7.0F;
			body.y += drop;
			head.y += drop;
			rightArm.y += drop;
			leftArm.y += drop;
			rightLeg.y += drop;
			leftLeg.y += drop;
			rightLeg.xRot = leftLeg.xRot = -Mth.HALF_PI;
			rightLeg.yRot = -0.2F;
			leftLeg.yRot = 0.2F;
			rightArm.xRot = leftArm.xRot = -0.3F;
			return;
		}

		float phase = state.walkAnimationPos * 0.6662F;
		float strength = state.walkAnimationSpeed;
		float swing = Mth.cos(phase) * 1.2F * strength;

		rightLeg.xRot = swing;
		leftLeg.xRot = -swing;

		float bounce = Mth.abs(Mth.sin(phase)) * strength;
		body.y -= bounce;
		head.y -= bounce;
		rightArm.y -= bounce;
		leftArm.y -= bounce;
		body.zRot = Mth.cos(phase) * 0.05F * strength;
	}

	@Override
	protected void setupAttackAnimation(GoboRenderState state) {
		super.setupAttackAnimation(state);
		AnimationUtils.animateZombieArms(leftArm, rightArm, state.isAggressive, state);
	}

	@Override
	public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack poseStack) {
		super.translateToHand(state, arm, poseStack);
		poseStack.translate((arm == HumanoidArm.RIGHT ? 1.0F : -1.0F) / 16.0F, -2.0F / 16.0F, 0.0F);
	}
}
