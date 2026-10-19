package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

public class GardenerEntity extends VindicatorServant {
    public GardenerEntity(EntityType<? extends GardenerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes();
    }

    // 两手分开：用 NEUTRAL 姿势替代默认的 CROSSED（抱胸）
    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }
}
