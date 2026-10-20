package com.blub.entity;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/** Walks a stationed Gobo back to its origin if it ends up outside the wander radius. */
public class ReturnToOriginGoal extends Goal {
    private final Gobo gobo;

    public ReturnToOriginGoal(Gobo gobo) {
        this.gobo = gobo;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.gobo.hasHome() && !this.gobo.isWithinHome() && this.gobo.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() && !this.gobo.getNavigation().isDone();
    }

    @Override
    public void start() {
        BlockPos home = this.gobo.getHomePosition();
        this.gobo.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
    }
}
