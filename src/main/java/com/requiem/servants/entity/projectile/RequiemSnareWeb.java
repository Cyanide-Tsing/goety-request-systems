package com.requiem.servants.entity.projectile;

import com.Polarice3.Goety.common.entities.ModEntityType;
import com.Polarice3.Goety.common.entities.projectiles.SnarelingGoop;
import com.Polarice3.Goety.common.entities.projectiles.SpellHurtingProjectile;
import com.Polarice3.Goety.utils.MobUtil;
import com.requiem.servants.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RequiemSnareWeb extends SpellHurtingProjectile {
    private int lifeSpanTicks;
    private float inflation;

    public RequiemSnareWeb(EntityType<? extends RequiemSnareWeb> type, Level level) {
        super(type, level);
    }

    public RequiemSnareWeb(Level level, double x, double y, double z, double xPower, double yPower, double zPower) {
        super(ModEntities.REQUIEM_SNARE_WEB.get(), x, y, z, xPower, yPower, zPower, level);
    }

    public void configure(int lifeSpanTicks, float inflation) {
        this.lifeSpanTicks = lifeSpanTicks;
        this.inflation = inflation;
        this.xPower = 0;
        this.yPower = 0;
        this.zPower = 0;
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
            Vec3 pos = result.getLocation();
            SnarelingGoop goop = new SnarelingGoop(ModEntityType.SNARELING_GOOP.get(), this.level());
            Entity owner = this.getOwner();
            if (owner instanceof LivingEntity living) {
                goop.setOwner(living);
            }
            goop.setPos(pos.x, pos.y, pos.z);
            goop.setLifeSpan(this.lifeSpanTicks);
            MobUtil.moveDownToGround(goop);
            if (this.inflation > 0.0F) {
                goop.setBoundingBox(goop.getBoundingBox().inflate(this.inflation));
            }
            this.level().addFreshEntity(goop);
        }
        this.discard();
    }
}
