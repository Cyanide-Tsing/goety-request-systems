package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import com.Polarice3.Goety.utils.MobUtil;
import com.requiem.servants.ai.CleanseNegativeEffectsGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 药剂师仆从：支援单位，清除己方仆从身上的负面效果，可编入连队。
 */
public class BoticarioEntity extends VindicatorServant {
    public BoticarioEntity(EntityType<? extends BoticarioEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes().add(Attributes.MAX_HEALTH, 26.0D);
    }

    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 清除己方仆从的负面效果
        this.goalSelector.addGoal(1, new CleanseNegativeEffectsGoal(this,
                target -> target instanceof Mob && MobUtil.areAllies(this, target)));
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }
}
