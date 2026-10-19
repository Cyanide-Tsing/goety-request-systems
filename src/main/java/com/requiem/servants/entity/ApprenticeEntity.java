package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.Neollager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

public class ApprenticeEntity extends Neollager {
    public ApprenticeEntity(EntityType<? extends ApprenticeEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Neollager.setCustomAttributes();
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }
}
