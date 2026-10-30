package com.requiem.servants.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * 让亡灵行军单位主动朝玩家行进。
 *
 * <p>单位生成在玩家 8 区块以外，原版 {@code MeleeAttackGoal}/{@code RangedAttackGoal}
 * 只在能创建出到目标的路径时才行动，远距离往往找不到路径，导致原地发呆。
 * 这个目标只要玩家还活着且离得远，就持续朝玩家当前位置寻路，直到足够近为止。
 */
public class MarchTowardsPlayerGoal extends Goal {
    private final PathfinderMob mob;
    private final Player target;
    private final double speed;
    private int pathTimer;

    public MarchTowardsPlayerGoal(PathfinderMob mob, Player target, double speed) {
        this.mob = mob;
        this.target = target;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return target != null && target.isAlive() && mob.distanceToSqr(target) > 64.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && mob.distanceToSqr(target) > 36.0D
                && !mob.getNavigation().isDone();
    }

    @Override
    public void tick() {
        if (--pathTimer <= 0) {
            pathTimer = 20;
            mob.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), speed);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
