package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.Neollager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

// 征兵官：与学徒一样是工作仆从，不直接战斗
public class RecruiterEntity extends WorkServantEntity {
    public RecruiterEntity(EntityType<? extends RecruiterEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Neollager.setCustomAttributes();
    }
}
