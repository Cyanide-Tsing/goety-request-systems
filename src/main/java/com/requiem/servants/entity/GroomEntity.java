package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * 马倌仆从：管理马场登记站的功能性仆从。
 *
 * <p>工作方块是马场登记站，会需求小麦与胡萝卜，并自动繁殖马匹；
 * 实际工作逻辑由 KubeJS 脚本（{@code goety_storyline/80_groom.js}）驱动。
 */
public class GroomEntity extends VindicatorServant {
    public GroomEntity(EntityType<? extends GroomEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes();
    }

    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    @Override
    public void targetSelectGoal() {
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }
}
