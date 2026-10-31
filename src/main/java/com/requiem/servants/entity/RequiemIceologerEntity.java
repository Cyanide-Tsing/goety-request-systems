package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.projectiles.IceChunk;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;

/**
 * 怪物型冰术师：默认敌对玩家的灾厄村民，与掠夺者/卫道士等灾厄村民同阵营，不会互相攻击。
 * 技能与 goety 冰术师仆从一致：向目标投掷冰凌（IceChunk）。外观复用 goety 的冰术师模型与贴图。
 */
public class RequiemIceologerEntity extends Monster {
    public RequiemIceologerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ThrowIceChunkGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    /** 与灾厄村民（Raider）视为同盟，避免冰凌范围伤害打到自己人。 */
    @Override
    public boolean isAlliedTo(Entity entity) {
        return super.isAlliedTo(entity) || entity instanceof Raider;
    }

    /** 向目标投掷冰凌，技能与 goety 冰术师仆从相同。 */
    private static final class ThrowIceChunkGoal extends Goal {
        private final RequiemIceologerEntity mob;
        private int cooldown;

        ThrowIceChunkGoal(RequiemIceologerEntity mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }
            double distSqr = this.mob.distanceToSqr(target);
            if (distSqr > 16.0D && distSqr < 256.0D) {
                IceChunk chunk = new IceChunk(this.mob.level(), this.mob, target);
                chunk.setPos(this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
                this.mob.level().addFreshEntity(chunk);
                this.cooldown = 40;
            }
        }
    }
}
