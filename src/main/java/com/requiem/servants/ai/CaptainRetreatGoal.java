package com.requiem.servants.ai;

import com.Polarice3.Goety.api.entities.IOwned;
import com.requiem.servants.entity.CaptainEntity;
import java.util.EnumSet;
import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 连队长的保命目标：有敌人靠到 {@value #DANGER_RANGE} 格以内就脱离接触。
 *
 * <p>连队长站在阵后指挥，可它毕竟还是个灾厄村民，被摸到就会掉血，死了整个连队就散了。
 * 所以给它一条明确的后退逻辑：朝远离威胁的方向找一个落点撤过去，
 * 并且在撤退期间压住 goety 的近战与跟随目标（优先级 2，比它们都高）。
 *
 * <p>配合 {@link CaptainEntity#targetSelectGoal()}（已覆盖为空实现，不主动索敌），
 * 连队长就不会自己冲进战线了。
 */
public class CaptainRetreatGoal extends Goal {
    /** 敌人进到这个距离就开始后退。 */
    private static final double DANGER_RANGE = 9.0D;
    /** 退到这个距离之外就算脱离接触。 */
    private static final double SAFE_RANGE = 13.0D;
    /** 每次后退寻路的目标距离。 */
    private static final int RETREAT_DISTANCE = 12;
    /** 后退时的速度倍率：指挥官跑得比行军快一点。 */
    private static final double RETREAT_SPEED = 1.25D;
    /** 重新寻路的间隔。 */
    private static final int RECALC_INTERVAL = 10;

    private static final double DANGER_RANGE_SQR = DANGER_RANGE * DANGER_RANGE;
    private static final double SAFE_RANGE_SQR = SAFE_RANGE * SAFE_RANGE;

    private final CaptainEntity captain;
    private LivingEntity threat;
    private int recalcCooldown;

    public CaptainRetreatGoal(CaptainEntity captain) {
        this.captain = captain;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        this.threat = this.findClosestThreat(DANGER_RANGE);
        return this.threat != null;
    }

    @Override
    public boolean canContinueToUse() {
        // 迟滞：退到 SAFE_RANGE 之外才松手，避免在危险边界上反复起停
        return this.threat != null
                && this.threat.isAlive()
                && this.captain.distanceToSqr(this.threat) < SAFE_RANGE_SQR;
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
        this.threat = null;
        this.captain.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.threat == null) {
            return;
        }
        // 边退边盯着威胁，撤退过程中保持面向敌人方向的视线
        this.captain.getLookControl().setLookAt(this.threat, 30.0F, 30.0F);

        if (--this.recalcCooldown > 0) {
            return;
        }
        this.recalcCooldown = RECALC_INTERVAL;

        Vec3 away = DefaultRandomPos.getPosAway(this.captain, RETREAT_DISTANCE, 7, this.threat.position());
        if (away != null) {
            this.captain.getNavigation().moveTo(away.x, away.y, away.z, RETREAT_SPEED);
        }
    }

    private LivingEntity findClosestThreat(double radius) {
        AABB box = this.captain.getBoundingBox().inflate(radius);
        LivingEntity best = null;
        double bestSqr = DANGER_RANGE_SQR;
        for (LivingEntity candidate : this.captain.level()
                .getEntitiesOfClass(LivingEntity.class, box, this::isThreat)) {
            double distSqr = this.captain.distanceToSqr(candidate);
            if (distSqr < bestSqr) {
                bestSqr = distSqr;
                best = candidate;
            }
        }
        return best;
    }

    private boolean isThreat(LivingEntity entity) {
        if (!entity.isAlive() || entity == this.captain) {
            return false;
        }
        // 自己人不算威胁
        if (entity instanceof IOwned owned
                && Objects.equals(owned.getOwnerId(), this.captain.getOwnerId())) {
            return false;
        }
        LivingEntity owner = this.captain.getTrueOwner();
        if (owner != null && entity == owner) {
            return false;
        }
        if (this.captain.isHostile()) {
            return entity instanceof Player player && !player.isCreative() && !player.isSpectator();
        }
        return entity instanceof Enemy;
    }
}
