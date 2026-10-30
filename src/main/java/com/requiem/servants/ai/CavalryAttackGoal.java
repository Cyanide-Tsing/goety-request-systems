package com.requiem.servants.ai;

import com.requiem.servants.entity.CavalryCaptainEntity;
import com.requiem.servants.entity.LightCavalryEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.AABB;

/**
 * 轻骑兵的自定义攻击目标：骑乘时用弓远程射击（边行进边射、保持距离），下马时近战。
 *
 * <p>骑乘态是三段式：太远追、太近退、射程内绕圈侧移，全程不停开弓。
 * 挂载时改用坐骑（马）的导航——马宽 1.4 格，寻路时自然避开狭小空间。
 */
public class CavalryAttackGoal extends Goal {
    private static final double MELEE_RANGE_SQ = 2.25D;
    /** 超出这个距离就追击。 */
    private static final double SHOOT_RANGE_SQ = 16.0D * 16.0D;
    /** 小于这个距离就后撤，保持骑射距离。 */
    private static final double MIN_RANGE_SQ = 6.0D * 6.0D;
    /** 开弓间隔（刻）：比原版更快。 */
    private static final int SHOOT_INTERVAL = 8;
    /** 侧移/后撤的偏移距离。 */
    private static final double MOVE_OFFSET = 8.0D;
    /** 冲锋速度倍率：高速穿过敌军，不因近战而降速。 */
    private static final double CHARGE_SPEED = 2.0D;
    /** 冲锋时穿过目标后方多远再掉头。 */
    private static final double CHARGE_PASS_THROUGH = 5.0D;
    /** 冲锋经过时挥砍的半径。 */
    private static final double SLASH_REACH = 2.5D;

    private final LightCavalryEntity cavalry;
    private int attackTick;
    private int strafeDir = 1;

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
            this.attackMounted(target);
        } else {
            this.attackMelee(target);
        }
    }

    /** 骑乘态：按骑兵队长的冲锋/收拢号令切换机动。 */
    private void attackMounted(LivingEntity target) {
        if (this.isCharging()) {
            this.attackCharge(target);
        } else {
            this.attackRally(target);
        }
    }

    /** 骑兵队长是否下达了冲锋号令；没有队长时默认收拢。 */
    private boolean isCharging() {
        if (this.cavalry.getLeader() instanceof CavalryCaptainEntity captain) {
            return captain.getChargeOrder();
        }
        return false;
    }

    /** 冲锋：高速穿过目标，冲过时挥砍临近敌军，而不是停下来近战。 */
    private void attackCharge(LivingEntity target) {
        this.cavalry.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 始终朝目标后方的穿透点全速冲，绝不因进入近战范围而降速
        double dx = target.getX() - this.cavalry.getX();
        double dz = target.getZ() - this.cavalry.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01D) {
            len = 1.0D;
        }
        double px = target.getX() + dx / len * CHARGE_PASS_THROUGH;
        double pz = target.getZ() + dz / len * CHARGE_PASS_THROUGH;
        // 用直接位移而非寻路，尽可能突破密集阵型
        this.moveDirect(px, target.getY(), pz, CHARGE_SPEED);

        // 边冲边挥砍，不停止
        if (this.attackTick <= 0) {
            this.attackTick = SHOOT_INTERVAL;
            this.slashCharge(target);
        }
    }

    /** 直线位移：忽略寻路，直接朝目标点推进（用于冲锋突破）。 */
    private void moveDirect(double x, double y, double z, double speed) {
        if (this.cavalry.isPassenger() && this.cavalry.getVehicle() instanceof Mob vehicle) {
            vehicle.getMoveControl().setWantedPosition(x, y, z, speed);
        } else {
            this.cavalry.getMoveControl().setWantedPosition(x, y, z, speed);
        }
    }

    /** 冲过时挥砍主目标与所有临近敌军。 */
    private void slashCharge(LivingEntity primary) {
        this.cavalry.doHurtTarget(primary);
        AABB box = this.cavalry.getBoundingBox().inflate(SLASH_REACH);
        for (LivingEntity enemy : this.cavalry.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this.cavalry && e != primary && e.isAlive() && e instanceof Enemy)) {
            this.cavalry.doHurtTarget(enemy);
        }
    }

    /** 收拢：行进间射击，三段式机动（追/退/绕）。 */
    private void attackRally(LivingEntity target) {
        double distSq = this.cavalry.distanceToSqr(target);
        this.cavalry.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (distSq > SHOOT_RANGE_SQ) {
            // 太远：追
            this.nav().moveTo(target, 1.2D);
        } else if (distSq < MIN_RANGE_SQ) {
            // 太近：边打边退
            this.moveAway(target, 1.2D);
        } else {
            // 射程内：边打边绕，保持横向机动
            this.strafe(target, 1.0D);
        }

        // 不论追/退/绕都继续开弓
        if (this.attackTick <= 0) {
            this.shoot(target);
            this.attackTick = SHOOT_INTERVAL;
        }
    }

    /** 朝远离目标的方向撤一段，保持骑射距离。 */
    private void moveAway(LivingEntity target, double speed) {
        double dx = this.cavalry.getX() - target.getX();
        double dz = this.cavalry.getZ() - target.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01D) {
            return;
        }
        double mx = this.cavalry.getX() + dx / len * MOVE_OFFSET;
        double mz = this.cavalry.getZ() + dz / len * MOVE_OFFSET;
        this.nav().moveTo(mx, this.cavalry.getY(), mz, speed);
    }

    /** 朝目标侧向绕行，周期换向。 */
    private void strafe(LivingEntity target, double speed) {
        double dx = this.cavalry.getX() - target.getX();
        double dz = this.cavalry.getZ() - target.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01D) {
            return;
        }
        double px = -dz / len;
        double pz = dx / len;
        this.nav().moveTo(
                this.cavalry.getX() + px * MOVE_OFFSET * this.strafeDir,
                this.cavalry.getY(),
                this.cavalry.getZ() + pz * MOVE_OFFSET * this.strafeDir,
                speed);
        if (this.cavalry.tickCount % 40 == 0) {
            this.strafeDir = -this.strafeDir;
        }
    }

    /** 挂载时用坐骑的导航（马宽 1.4 格），避免钻进狭小空间。 */
    private PathNavigation nav() {
        if (this.cavalry.isPassenger() && this.cavalry.getVehicle() instanceof Mob vehicle) {
            return vehicle.getNavigation();
        }
        return this.cavalry.getNavigation();
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

    /** 下马近战。 */
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
