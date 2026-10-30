package com.requiem.servants.entity.projectile;

import com.Polarice3.Goety.client.particles.ModParticleTypes;
import com.Polarice3.Goety.common.effects.GoetyEffects;
import com.Polarice3.Goety.common.entities.projectiles.SpellHurtingProjectile;
import com.Polarice3.Goety.utils.ModDamageSource;
import com.requiem.servants.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
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

public class RequiemBolt extends SpellHurtingProjectile {
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(RequiemBolt.class, EntityDataSerializers.INT);

    private float baseDamage;
    private float knockback;
    private MobEffect hitEffect;
    private int effectDuration;
    private int effectAmplifier;
    private boolean voidThreshold;

    public RequiemBolt(EntityType<? extends RequiemBolt> type, Level level) {
        super(type, level);
    }

    public RequiemBolt(Level level, double x, double y, double z, double xPower, double yPower, double zPower) {
        super(ModEntities.REQUIEM_BOLT.get(), x, y, z, xPower, yPower, zPower, level);
    }

    public void configure(int variant, float damage, float knockback, MobEffect effect, int duration, int amplifier,
            boolean voidThreshold) {
        this.setVariant(variant);
        this.baseDamage = damage;
        this.knockback = knockback;
        this.hitEffect = effect;
        this.effectDuration = duration;
        this.effectAmplifier = amplifier;
        this.voidThreshold = voidThreshold;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, 0);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, variant);
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return this.getVariant() == 1 ? ModParticleTypes.END_FIRE.get() : ModParticleTypes.SPELL_ELECTRIC.get();
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
            ParticleOptions particle = this.getTrailParticle();
            for (int i = 0; i < 10; i++) {
                double dx = (this.random.nextDouble() - 0.5) * 0.5;
                double dy = (this.random.nextDouble() - 0.5) * 0.5;
                double dz = (this.random.nextDouble() - 0.5) * 0.5;
                this.level().addParticle(particle, this.getX() + dx, this.getY() + dy, this.getZ() + dz, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) {
            this.setVariant(tag.getInt("Variant"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide() && result.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) result).getEntity();
            Entity owner = this.getOwner();
            float damage = this.baseDamage;
            if (this.voidThreshold && target instanceof LivingEntity living
                    && living.getHealth() < living.getMaxHealth() * 0.3F) {
                damage += 2.0F;
            }
            DamageSource source;
            if (owner instanceof LivingEntity ownerLiving) {
                source = this.getVariant() == 1
                        ? ModDamageSource.magicBolt(this, ownerLiving)
                        : ModDamageSource.directShock(ownerLiving);
            } else {
                source = this.damageSources().magic();
            }
            if (target.hurt(source, damage)) {
                if (this.knockback > 0.0F && target instanceof LivingEntity living) {
                    Vec3 dir = this.getDeltaMovement();
                    double len = dir.length();
                    if (len > 1.0E-4) {
                        dir = dir.scale(1.0 / len);
                    }
                    living.knockback(this.knockback, -dir.x, -dir.z);
                }
                if (target instanceof LivingEntity living) {
                    if (this.hitEffect != null) {
                        living.addEffect(new MobEffectInstance(this.hitEffect, this.effectDuration, this.effectAmplifier));
                    }
                    if (this.getVariant() == 1) {
                        living.addEffect(new MobEffectInstance(GoetyEffects.ENDER_GROUND.get(), 100, 0));
                    }
                }
            }
        }
        this.discard();
    }
}
