package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import com.requiem.servants.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 骑兵队长仆从：骑兵编制的指挥军官，持苍白军刀。
 */
public class CavalryCaptainEntity extends VindicatorServant {
    public CavalryCaptainEntity(EntityType<? extends CavalryCaptainEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes().add(Attributes.MAX_HEALTH, 34.0D);
    }

    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PALE_SABER.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public boolean canBeLeader() {
        return true;
    }

    /** 切换冲锋号令，返回切换后的状态（true=冲锋）。 */
    public boolean toggleChargeOrder() {
        boolean next = !this.getChargeOrder();
        this.getPersistentData().putBoolean("cavalryCharge", next);
        return next;
    }

    public boolean getChargeOrder() {
        return this.getPersistentData().getBoolean("cavalryCharge");
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
