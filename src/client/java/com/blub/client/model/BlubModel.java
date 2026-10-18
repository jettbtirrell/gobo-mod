package com.blub.client.model;

import com.blub.BlubMod;
import com.blub.client.render.BlubRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class BlubModel extends EntityModel<BlubRenderState> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(BlubMod.id("blub"), "main");
	private final ModelPart body;
	private final ModelPart legs;
	private final ModelPart back_left_leg;
	private final ModelPart back_right_leg;
	private final ModelPart front_left_leg;
	private final ModelPart front_right_leg;
	private final ModelPart left_ear;
	private final ModelPart right_ear;
	

	public BlubModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
        this.legs = root.getChild("legs");
		this.back_left_leg = legs.getChild("back_left_leg");
		this.back_right_leg = legs.getChild("back_right_leg");
		this.front_left_leg = legs.getChild("front_left_leg");
		this.front_right_leg = legs.getChild("front_right_leg");
		this.left_ear = body.getChild("left_ear");
		this.right_ear = body.getChild("right_ear");

	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition legs = partdefinition.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(0.0F, 22.0F, 0.0F));

		PartDefinition back_left_leg = legs.addOrReplaceChild("back_left_leg", CubeListBuilder.create().texOffs(40, 10).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 0.0F, 3.5F));

		PartDefinition back_right_leg = legs.addOrReplaceChild("back_right_leg", CubeListBuilder.create().texOffs(40, 10).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 0.0F, 3.5F));

		PartDefinition front_left_leg = legs.addOrReplaceChild("front_left_leg", CubeListBuilder.create().texOffs(40, 10).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 0.0F, -3.0F));

		PartDefinition front_right_leg = legs.addOrReplaceChild("front_right_leg", CubeListBuilder.create().texOffs(40, 10).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 0.0F, -3.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -7.0F, -4.0F, 10.0F, 7.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.0F, 0.0F));

		PartDefinition left_ear = body.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(41, 2).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -7.0F, -2.5F));

		PartDefinition right_ear = body.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(41, 2).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -7.0F, -2.5F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
    public void setupAnim(BlubRenderState state) {
        super.setupAnim(state);

		if (state.isSitting) {
			body.y += 2.0F;
			back_left_leg.xRot = back_right_leg.xRot = Mth.HALF_PI;
			front_left_leg.xRot = front_right_leg.xRot = -Mth.HALF_PI;
			back_left_leg.z += 1.0F;  back_right_leg.z += 1.0F;
			front_left_leg.z -= 1.0F; front_right_leg.z -= 1.0F;
			legs.y += 1.0F;
			return;
		}

		float phase = state.walkAnimationPos * 0.6662F;
        float strength = state.walkAnimationSpeed;

		float swing = Mth.cos(phase) * 0.7F * strength;
        float swingBack = Mth.cos(phase + Mth.PI) * 0.7F * strength;
        back_right_leg.xRot  = swing;
        back_left_leg.xRot   = swingBack;
        front_right_leg.xRot = swingBack;
        front_left_leg.xRot  = swing;

		// Lift each leg by the sine of its tilt (in pixels, y is down). The legs are 2 px deep, so this keeps the top of a
		// tilted leg from dropping below the body and the foot from sinking into the floor.
		for (ModelPart leg : new ModelPart[] {back_left_leg, back_right_leg, front_left_leg, front_right_leg}) {
			leg.y -= Mth.abs(Mth.sin(leg.xRot));
		}
		//squish
		float squish = 0.5F * (1.0F - Mth.cos(2.0F * phase)) * strength;
		body.xScale = 1.0F + 0.05F * squish; //body 5% wider if 0.05F * squish
        legs.xScale = body.xScale; //keeps the legs aligned with body that grows on x axis
        body.yScale = 1.0F - 0.03F * squish; //body 3% less height if 0.03F * squish

		//waddle
		body.zRot = Mth.cos(phase) * 0.0355F * strength;

		//body not coming off feet when waddling
		body.y += 5.0F * body.xScale * Mth.abs(Mth.sin(body.zRot));

		//ears waddle in addition to body waddle
		left_ear.zRot = right_ear.zRot = body.zRot * 1.5F;

		//ears dont come off body when waddling
        left_ear.y  += 1.5F * Mth.abs(Mth.sin(left_ear.zRot));
        right_ear.y += 1.5F * Mth.abs(Mth.sin(right_ear.zRot));
	}


}