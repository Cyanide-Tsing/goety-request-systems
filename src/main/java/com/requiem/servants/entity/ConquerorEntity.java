package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.PillagerServant;
import com.Polarice3.Goety.utils.MobUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 征服者仆从：使用弩发射烟花的重装线列步兵，可替代掠夺者/掷弹兵编入线列。
 *
 * <p>战斗 AI 直接沿用 goety 的 {@link PillagerServant}（弩手），
 * 但把弩的弹药用 {@link #getProjectile} 换成烟花火箭，复刻 goety 征服者（Conquillager）的打法。
 */
public class ConquerorEntity extends PillagerServant {
    public ConquerorEntity(EntityType<? extends ConquerorEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PillagerServant.setCustomAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D);
    }

    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    /** 弩的弹药：烟花火箭（飞行时长 2，全色）。 */
    @Override
    public ItemStack getProjectile(ItemStack crossbowStack) {
        return MobUtil.createFirework(2, DyeColor.values());
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }
}
