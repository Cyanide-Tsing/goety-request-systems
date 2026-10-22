package com.requiem.servants.ai;

import com.requiem.servants.entity.LightCavalryEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Arrow;

/**
 * 轻骑兵的自定义攻击目标：骑乘时用弓远程射击，下马时近战。
 *
 * <p>不用 goety 卫道士仆从的近战 AI，避免骑马时还抵近肉搏。
 */
public class CavalryAttackGoal extends Goal {
    private static final double MELEE_RANGE_SQ = 2.25D;
    private static final double SHOOT_RANGE_SQ = 16.0D * 16.0D;
    private static final double MIN_RANGE_SQ = 4.0D * 4.0D;

    private final LightCavalryEntity cavalry;
    private int attackTick;

    public CavalryAttackGoal(LightCavalryEntity cavalry) {
        this.cavalry = cavalry;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.cavalry.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.cavalry.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        this.attackTick--;
        if (this.cavalry.isPassenger()) {
            this.attackRanged(target);
        } else {
            this.attackMelee(target);
        }
    }

    private void attackRanged(LivingEntity target) {
        double distSq = this.cavalry.distanceToSqr(target);
        this.cavalry.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (distSq > SHOOT_RANGE_SQ) {
            this.cavalry.getNavigation().moveTo(target, 1.0D);
            return;
        }
        if (this.attackTick <= 0) {
            this.shoot(target);
            this.attackTick = 20;
        }
    }

    private void shoot(LivingEntity target) {
        Arrow arrow = new Arrow(this.cavalry.level(), this.cavalry);
        double dx = target.getX() - this.cavalry.getX();
        double dy = target.getEyeY() - arrow.getY();
        double dz = target.getZ() - this.cavalry.getZ();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.1D) {
            return;
        }
        // 轻微仰角补偿重力下坠
        dy += len * 0.05D;
        arrow.shoot(dx, dy, dz, 3.0F, 1.0F);
        this.cavalry.level().addFreshEntity(arrow);
    }

    private void attackMelee(LivingEntity target) {
        double distSq = this.cavalry.distanceToSqr(target);
        this.cavalry.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (distSq > MELEE_RANGE_SQ) {
            this.cavalry.getNavigation().moveTo(target, 1.0D);
            return;
        }
        if (this.attackTick <= 0) {
            this.cavalry.doHurtTarget(target);
            this.attackTick = 20;
        }
    }
}
