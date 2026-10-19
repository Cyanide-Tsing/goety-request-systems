package com.requiem.servants.ai;

import com.requiem.servants.entity.CaptainEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * 让连队长在自主作战时自己往前走。
 *
 * <p>平时连队长靠 goety 的跟随主人 AI 移动，锚点再由它的位置推导出来。
 * 但突击作战要求在玩家<b>不在场</b>的情况下也能推进，所以作战期间
 * {@link com.requiem.servants.command.CompanyController} 会给它设一个指挥位
 * （{@link CaptainEntity#setCommandPost}），由这个目标负责走过去。
 *
 * <p>作战期间锚点来自<b>作战目标</b>而不是连队长位置，所以这里驱动连队长移动
 * 不会和锚点形成正反馈。优先级 4：低于撤退(2)，高于跟随主人(5)——
 * 也就是遇敌先保命，没事才继续推进。
 */
public class CaptainMarchGoal extends Goal {
    /** 走到这个距离内就算到位。 */
    private static final double ARRIVE_TOLERANCE = 3.0D;
    /** 行军速度倍率。 */
    private static final double MARCH_SPEED = 1.0D;
    /** 重新寻路的间隔。 */
    private static final int RECALC_INTERVAL = 20;

    private static final double ARRIVE_TOLERANCE_SQR = ARRIVE_TOLERANCE * ARRIVE_TOLERANCE;

    private final CaptainEntity captain;
    private int recalcCooldown;

    public CaptainMarchGoal(CaptainEntity captain) {
        this.captain = captain;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        Vec3 post = this.captain.getCommandPost();
        return post != null && this.captain.distanceToSqr(post) > ARRIVE_TOLERANCE_SQR;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.recalcCooldown = 0;
    }

    @Override
    public void stop() {
        this.captain.getNavigation().stop();
    }

    @Override
    public void tick() {
        Vec3 post = this.captain.getCommandPost();
        if (post == null) {
            return;
        }
        if (--this.recalcCooldown > 0) {
            return;
        }
        this.recalcCooldown = RECALC_INTERVAL;
        this.captain.getNavigation().moveTo(post.x, post.y, post.z, MARCH_SPEED);
    }
}
