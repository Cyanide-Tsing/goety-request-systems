package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

public class GardenerEntity extends WorkServantEntity {
    public GardenerEntity(EntityType<? extends GardenerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes();
    }
}
