package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * 马倌仆从：管理马场登记站，需求小麦/胡萝卜自动繁殖马匹。
 *
 * <p>工作逻辑由 KubeJS 脚本（{@code goety_storyline/80_groom.js}）驱动。
 */
public class GroomEntity extends WorkServantEntity {
    public GroomEntity(EntityType<? extends GroomEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes();
    }
}
