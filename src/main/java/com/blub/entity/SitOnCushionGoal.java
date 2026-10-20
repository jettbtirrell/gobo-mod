package com.blub.entity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.phys.AABB;

/**
 * Walks an idle Gobo to a free cushion and sits on it. Anything else (combat, being hurt, eating,
 * trading, being clicked, a higher priority goal) ends it.
 */
public class SitOnCushionGoal extends Goal {
    private static final double SEARCH_RANGE = 10.0;
    private static final double SIT_DISTANCE_SQ = 1.5 * 1.5;
    private static final int MAX_WALK_TICKS = 200;
    // Minimum time between switching in either direction (sit <-> stroll), before the random chance applies.
    private static final int MIN_TICKS_BETWEEN_SWITCHES = 20 * 20;
    // Once the minimum has passed: odds per check of sitting down, and per tick of getting up (~10s on average).
    private static final int SIT_CHANCE = 15;
    private static final int STAND_CHANCE = 200;

    private final Gobo gobo;
    private @Nullable Cushion cushion;
    private int walkTicks;
    private int seatedTicks;
    private int canSitAfterTick;

    public SitOnCushionGoal(Gobo gobo) {
        this.gobo = gobo;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.gobo.getVehicle() instanceof Cushion current) {
            // Already seated (e.g. after a reload): keep sitting as long as nothing else is going on.
            this.cushion = current;
            return this.isIdle();
        }
        if (this.gobo.tickCount < this.canSitAfterTick || this.gobo.getRandom().nextInt(SIT_CHANCE) != 0) {
            return false;
        }
        if (!this.isIdle() || this.gobo.isPassenger()) {
            return false;
        }
        this.cushion = this.findCushion();
        return this.cushion != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.cushion == null || this.cushion.isRemoved() || !this.isIdle()) {
            return false;
        }
        if (this.gobo.getVehicle() == this.cushion) {
            return this.seatedTicks < MIN_TICKS_BETWEEN_SWITCHES || this.gobo.getRandom().nextInt(STAND_CHANCE) != 0;
        }
        return !this.gobo.isPassenger() && !this.cushion.isVehicle() && this.walkTicks < MAX_WALK_TICKS;
    }

    @Override
    public void start() {
        this.walkTicks = 0;
        this.seatedTicks = 0;
    }

    @Override
    public void tick() {
        if (this.cushion == null) {
            return;
        }
        if (this.gobo.getVehicle() == this.cushion) {
            this.seatedTicks++;
            return;
        }
        this.walkTicks++;
        this.gobo.getNavigation().moveTo(this.cushion, 1.0);
        if (this.gobo.distanceToSqr(this.cushion) <= SIT_DISTANCE_SQ) {
            this.gobo.getNavigation().stop();
            if (this.gobo.startRiding(this.cushion)) {
                this.gobo.setInSittingPose(true);
            } else {
                this.walkTicks = MAX_WALK_TICKS;
            }
        }
    }

    @Override
    public void stop() {
        this.gobo.getNavigation().stop();
        this.gobo.stopSitting();
        this.cushion = null;
        this.canSitAfterTick = this.gobo.tickCount + MIN_TICKS_BETWEEN_SWITCHES;
    }

    private boolean isIdle() {
        return this.gobo.getTarget() == null
            && !this.gobo.isAdmiring()
            && !this.gobo.isUsingItem()
            && this.gobo.getOffhandItem().isEmpty()
            && this.gobo.hurtTime == 0
            && this.gobo.tickCount - this.gobo.getLastHurtByMobTimestamp() > 100
            && !this.gobo.wantsToEat()
            && !this.gobo.wantsToFollowOwner();
    }

    private @Nullable Cushion findCushion() {
        AABB area = this.gobo.getBoundingBox().inflate(SEARCH_RANGE, 4.0, SEARCH_RANGE);
        List<Cushion> cushions = this.gobo.level().getEntitiesOfClass(Cushion.class, area,
            c -> !c.isVehicle() && (!this.gobo.hasHome() || this.gobo.isWithinHome(c.position())));
        return cushions.stream().min(Comparator.comparingDouble(this.gobo::distanceToSqr)).orElse(null);
    }
}
