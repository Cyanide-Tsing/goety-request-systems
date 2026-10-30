package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.hostile.illagers.HuntingIllagerEntity;
import com.Polarice3.Goety.utils.MobUtil;
import com.requiem.servants.ai.CleanseNegativeEffectsGoal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;

/**
 * 敌对药剂师：支援型灾厄村民，清除己方灾厄村民身上的负面效果，袭击中出现。
 */
public class BoticarioHostileEntity extends HuntingIllagerEntity {
    public BoticarioHostileEntity(EntityType<? extends HuntingIllagerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected SoundEvent getCastingSoundEvent() {
        return SoundEvents.EVOKER_CAST_SPELL;
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.EVOKER_CELEBRATE;
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
        // 支援单位不需要袭击增益
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 清除己方灾厄村民（Raider）的负面效果
        this.goalSelector.addGoal(1, new CleanseNegativeEffectsGoal(this,
                target -> target instanceof Raider && MobUtil.areAllies(this, target)));
    }
}
