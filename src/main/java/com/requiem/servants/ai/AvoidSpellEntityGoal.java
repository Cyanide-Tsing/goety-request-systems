package com.requiem.servants.ai;

import com.Polarice3.Goety.api.entities.ISpellEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

/**
 * 仆从感知并避让法术实体：附近出现 goety 法术（投射物/地面效果）时，
 * 朝远离它的方向躲开，避免被友军/敌方法术波及。
 */
public class AvoidSpellEntityGoal extends Goal {
    /** 法术实体进入这个距离就开始避让。 */
    private static final double DANGER_RANGE = 6.0D;
    /** 躲开时的位移距离。 */
    private static final double FLEE_DISTANCE = 8.0D;

    private final Mob mob;

    public AvoidSpellEntityGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.findDanger() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.findDanger() != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Entity danger = this.findDanger();
        if (danger == null) {
            return;
        }
        double dx = this.mob.getX() - danger.getX();
        double dz = this.mob.getZ() - danger.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01D) {
            len = 1.0D;
            dx = 1.0D;
            dz = 0.0D;
        }
        double mx = this.mob.getX() + dx / len * FLEE_DISTANCE;
        double mz = this.mob.getZ() + dz / len * FLEE_DISTANCE;
        this.mob.getNavigation().moveTo(mx, this.mob.getY(), mz, 1.2D);
    }

    private Entity findDanger() {
        AABB box = this.mob.getBoundingBox().inflate(DANGER_RANGE);
        Entity best = null;
        double bestSq = DANGER_RANGE * DANGER_RANGE;
        for (Entity e : this.mob.level().getEntitiesOfClass(Entity.class, box, e -> e instanceof ISpellEntity)) {
            double distSq = this.mob.distanceToSqr(e);
            if (distSq < bestSq) {
                bestSq = distSq;
                best = e;
            }
        }
        return best;
    }
}
