package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.Neollager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

// 征兵官：与学徒一样是仆从，不直接战斗（遇敌逃跑由 goety 的 AI 目标处理）
public class RecruiterEntity extends Neollager {
    public RecruiterEntity(EntityType<? extends RecruiterEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Neollager.setCustomAttributes();
    }

    // 两手分开（NEUTRAL）：配合传令兵仆从贴图（双手在身体两侧），避免抱胸导致双手不渲染
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
