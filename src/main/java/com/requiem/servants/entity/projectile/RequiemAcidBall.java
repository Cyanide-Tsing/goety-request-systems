package com.requiem.servants.entity.projectile;

import com.Polarice3.Goety.common.entities.ModEntityType;
import com.Polarice3.Goety.common.entities.projectiles.AcidPool;
import com.Polarice3.Goety.common.entities.projectiles.SpellHurtingProjectile;
import com.Polarice3.Goety.utils.MobUtil;
import com.Polarice3.Goety.utils.ModDamageSource;
import com.requiem.servants.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RequiemAcidBall extends SpellHurtingProjectile {
    private float baseDamage;
    private MobEffect poisonEffect;
    private int poisonDuration;
    private int poisonAmplifier;
    private float poolRadius;
    private MobEffect secondaryEffect;
    private int secondaryDuration;
    private int secondaryAmplifier;

    public RequiemAcidBall(EntityType<? extends RequiemAcidBall> type, Level level) {
        super(type, level);
    }

    public RequiemAcidBall(Level level, double x, double y, double z, double xPower, double yPower, double zPower) {
        super(ModEntities.REQUIEM_ACID_BALL.get(), x, y, z, xPower, yPower, zPower, level);
    }

    public void configure(float damage, MobEffect poison, int duration, int amplifier, float radius) {
        this.baseDamage = damage;
        this.poisonEffect = poison;
        this.poisonDuration = duration;
        this.poisonAmplifier = amplifier;
        this.poolRadius = radius;
        this.xPower = 0;
        this.yPower = 0;
        this.zPower = 0;
    }

    public void setSecondaryEffect(MobEffect effect, int duration, int amplifier) {
        this.secondaryEffect = effect;
        this.secondaryDuration = duration;
        this.secondaryAmplifier = amplifier;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.ITEM_SLIME;
    }

    @Override
    protected float getInertia() {
        return 0.99F;
    }

    @Override
    public float getGravity() {
        return 0.06F;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.ITEM_SLIME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide()) {
            if (result.getType() == HitResult.Type.ENTITY) {
                Entity target = ((EntityHitResult) result).getEntity();
                Entity owner = this.getOwner();
                if (target.hurt(ModDamageSource.acid(this, owner), this.baseDamage)
                        && target instanceof LivingEntity living) {
                    if (this.poisonEffect != null) {
                        living.addEffect(new MobEffectInstance(this.poisonEffect, this.poisonDuration, this.poisonAmplifier));
                    }
                    if (this.secondaryEffect != null) {
                        living.addEffect(new MobEffectInstance(this.secondaryEffect, this.secondaryDuration, this.secondaryAmplifier));
                    }
                }
            }
            this.spawnPool(result.getLocation());
        }
        this.discard();
    }

    private void spawnPool(Vec3 pos) {
        AcidPool pool = new AcidPool(ModEntityType.ACID_POOL.get(), this.level());
        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity living) {
            pool.setOwner(living);
        }
        pool.setPos(pos.x, pos.y, pos.z);
        pool.setRadius(this.poolRadius);
        pool.setDamage(2.0F);
        pool.setDuration(200);
        MobUtil.moveDownToGround(pool);
        this.level().addFreshEntity(pool);
    }
}
