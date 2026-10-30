package com.requiem.servants.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * 绕障目标：当目标被墙挡住（隔墙，视线受阻）时，围绕目标侧向绕圈，
 * 并且在绕圈过程中周期性尝试"直冲目标"——一旦绕到门口，直冲路径就会
 * 穿过门（门是可通行的 WALKABLE_DOOR），从而触发开门并进屋。
 */
public class OrbitBlockedTargetGoal extends Goal {
    /** 离目标这个距离以内才考虑绕圈。 */
    private static final double MAX_DISTANCE = 16.0D;
    /** 绕圈落点相对目标的正交偏移距离。 */
    private static final double ORBIT_RADIUS = 5.0D;
    /** 到达落点后重新寻路的冷却。 */
    private static final int REPATH_INTERVAL = 12;

    private final Mob mob;
    private LivingEntity target;
    private int direction;
    private boolean directProbe;
    private int repathCooldown;

    public OrbitBlockedTargetGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = this.mob.getTarget();
        if (t == null || !t.isAlive()) {
            return false;
        }
        if (this.mob.distanceToSqr(t) > MAX_DISTANCE * MAX_DISTANCE) {
            return false;
        }
        // 真实视线检测，与 SensingMixin（索敌用）无关。
        if (this.mob.hasLineOfSight(t)) {
            return false;
        }
        this.target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && this.target.isAlive()
                && !this.mob.hasLineOfSight(this.target)
                && this.mob.distanceToSqr(this.target) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.direction = this.mob.getRandom().nextBoolean() ? 1 : -1;
        this.directProbe = false;
        this.repathCooldown = 0;
        this.repath();
    }

    @Override
    public void stop() {
        this.target = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        if (--this.repathCooldown > 0) {
            return;
        }
        this.repathCooldown = REPATH_INTERVAL;

        if (this.mob.getNavigation().isDone()) {
            this.repath();
        }
    }

    private void repath() {
        // 交替：绕圈落点 / 直冲目标。直冲目标在绕到门口时会穿过门并触发开门。
        this.directProbe = !this.directProbe;
        if (this.directProbe) {
            this.mob.getNavigation().moveTo(this.target, 1.0D);
            return;
        }

        Vec3 toMob = this.mob.position().subtract(this.target.position());
        Vec3 dir = new Vec3(toMob.x, 0.0D, toMob.z).normalize();
        Vec3 perpendicular = new Vec3(-dir.z, 0.0D, dir.x).scale(this.direction * ORBIT_RADIUS);
        Vec3 orbitPos = this.target.position().add(perpendicular);
        this.mob.getNavigation().moveTo(orbitPos.x, orbitPos.y, orbitPos.z, 1.0D);
    }
}
