package com.blub.entity;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.Vec3;

public class SpearAttackGoal extends Goal {
    private static final double MAX_FLEEING_TIME = reducedTickDelay(100);

    private final PathfinderMob mob;
    private final double chargeSpeed;
    private final double repositionSpeed;
    private final float approachDistanceSq;
    private final float targetInRangeRadiusSq;
    private @Nullable State state;

    public SpearAttackGoal(PathfinderMob mob, double chargeSpeed, double repositionSpeed, float approachDistance, float targetInRangeRadius) {
        this.mob = mob;
        this.chargeSpeed = chargeSpeed;
        this.repositionSpeed = repositionSpeed;
        this.approachDistanceSq = approachDistance * approachDistance;
        this.targetInRangeRadiusSq = targetInRangeRadius * targetInRangeRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.ableToAttack() && !this.mob.isUsingItem();
    }

    private boolean ableToAttack() {
        return this.mob.getTarget() != null && this.mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
    }

    private int useDuration() {
        KineticWeapon weapon = this.mob.getMainHandItem().get(DataComponents.KINETIC_WEAPON);
        return weapon == null ? 0 : reducedTickDelay(weapon.computeDamageUseDuration());
    }

    @Override
    public boolean canContinueToUse() {
        return this.state != null && !this.state.done && this.ableToAttack();
    }

    @Override
    public void start() {
        super.start();
        this.mob.setAggressive(true);
        this.state = new State();
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.getNavigation().stop();
        this.mob.setAggressive(false);
        this.state = null;
        this.mob.stopUsingItem();
    }

    @Override
    public void tick() {
        if (this.state == null) {
            return;
        }

        LivingEntity target = this.mob.getTarget();
        double distanceSq = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        float speedModifier = this.mob.getRootVehicle() instanceof Mob rootMob ? rootMob.chargeSpeedModifier() : 1.0F;
        int mountedBonus = this.mob.isPassenger() ? 2 : 0;
        this.mob.lookAt(target, 30.0F, 30.0F);
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.state.notEngagedYet()) {
            if (distanceSq > this.approachDistanceSq) {
                this.mob.getNavigation().moveTo(target, speedModifier * this.repositionSpeed);
                return;
            }
            this.state.engageTime = this.useDuration();
            this.mob.startUsingItem(InteractionHand.MAIN_HAND);
        }

        if (this.state.tickAndCheckEngagement()) {
            this.mob.stopUsingItem();
            double distance = Math.sqrt(distanceSq);
            this.state.awayPos = LandRandomPos.getPosAway(this.mob, Math.max(0.0, 9 + mountedBonus - distance), Math.max(1.0, 11 + mountedBonus - distance), 7, target.position());
            this.state.fleeingTime = 1;
        }

        if (this.state.tickAndCheckFleeing()) {
            return;
        }

        if (this.state.awayPos != null) {
            this.mob.getNavigation().moveTo(this.state.awayPos.x, this.state.awayPos.y, this.state.awayPos.z, speedModifier * this.repositionSpeed);
            if (this.mob.getNavigation().isDone()) {
                if (this.state.fleeingTime > 0) {
                    this.state.done = true;
                    return;
                }
                this.state.awayPos = null;
            }
        } else {
            this.mob.getNavigation().moveTo(target, speedModifier * this.chargeSpeed);
            if (distanceSq < this.targetInRangeRadiusSq || this.mob.getNavigation().isDone()) {
                double distance = Math.sqrt(distanceSq);
                this.state.awayPos = LandRandomPos.getPosAway(this.mob, 6 + mountedBonus - distance, 7 + mountedBonus - distance, 7, target.position());
            }
        }
    }

    private static class State {
        int engageTime = -1;
        int fleeingTime = -1;
        @Nullable Vec3 awayPos;
        boolean done;

        boolean notEngagedYet() {
            return this.engageTime < 0;
        }

        boolean tickAndCheckEngagement() {
            if (this.engageTime > 0) {
                this.engageTime--;
                return this.engageTime == 0;
            }
            return false;
        }

        boolean tickAndCheckFleeing() {
            if (this.fleeingTime > 0) {
                this.fleeingTime++;
                if (this.fleeingTime > MAX_FLEEING_TIME) {
                    this.done = true;
                    return true;
                }
            }
            return false;
        }
    }
}
