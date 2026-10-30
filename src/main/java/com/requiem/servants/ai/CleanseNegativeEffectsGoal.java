package com.requiem.servants.ai;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

/**
 * 清除负面效果：选定范围内"带有负面效果"的目标，并清除其身上的所有负面效果。
 *
 * <p>目标由 {@code targetPredicate} 决定：药剂师仆从选己方仆从，敌对药剂师选灾厄村民。
 */
public class CleanseNegativeEffectsGoal extends Goal {
    /** 搜索范围。 */
    private static final double RANGE = 16.0D;
    /** 再次清毒/清负面效果的冷却（刻）。 */
    private static final int CLEANSE_INTERVAL = 20;

    private final Mob mob;
    private final Predicate<LivingEntity> targetPredicate;
    private int cooldown;

    public CleanseNegativeEffectsGoal(Mob mob, Predicate<LivingEntity> targetPredicate) {
        this.mob = mob;
        this.targetPredicate = targetPredicate;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.findTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.findTarget() != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.findTarget();
        if (target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--this.cooldown > 0) {
            return;
        }
        this.cooldown = CLEANSE_INTERVAL;

        List<MobEffectInstance> effects = new ArrayList<>(target.getActiveEffects());
        for (MobEffectInstance effect : effects) {
            if (!effect.getEffect().isBeneficial()) {
                target.removeEffect(effect.getEffect());
            }
        }
    }

    private LivingEntity findTarget() {
        AABB box = this.mob.getBoundingBox().inflate(RANGE);
        LivingEntity best = null;
        double bestSq = RANGE * RANGE;
        for (LivingEntity e : this.mob.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && this.targetPredicate.test(e) && this.hasNegativeEffect(e))) {
            double distSq = this.mob.distanceToSqr(e);
            if (distSq < bestSq) {
                bestSq = distSq;
                best = e;
            }
        }
        return best;
    }

    private boolean hasNegativeEffect(LivingEntity e) {
        for (MobEffectInstance effect : e.getActiveEffects()) {
            if (!effect.getEffect().isBeneficial()) {
                return true;
            }
        }
        return false;
    }
}
