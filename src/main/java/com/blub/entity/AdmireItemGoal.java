package com.blub.entity;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;

/** Holds the Gobo still while it admires a bartered ingot; the timer itself lives in {@link Gobo}. */
public class AdmireItemGoal extends Goal {
    private final Gobo gobo;

    public AdmireItemGoal(Gobo gobo) {
        this.gobo = gobo;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.gobo.isAdmiring();
    }

    @Override
    public boolean canContinueToUse() {
        return this.gobo.isAdmiring();
    }

    @Override
    public void start() {
        this.gobo.getNavigation().stop();
    }
}
