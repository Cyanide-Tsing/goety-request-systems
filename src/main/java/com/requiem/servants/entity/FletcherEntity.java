package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * 制箭师仆从：在制箭台工作，按需求制作箭/弓/弩相关物品。
 *
 * <p>工作逻辑由 KubeJS 脚本（{@code goety_storyline/90_fletcher.js}）驱动。
 */
public class FletcherEntity extends WorkServantEntity {
    public FletcherEntity(EntityType<? extends FletcherEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes();
    }
}
