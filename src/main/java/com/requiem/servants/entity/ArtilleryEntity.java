package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

// 炮兵仆从：操控 Supplementaries 大炮，用农夫乐事铁刀自卫，血量 14
public class ArtilleryEntity extends VindicatorServant {
    public ArtilleryEntity(EntityType<? extends ArtilleryEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D);
    }

    // 两手分开：持刀
    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    // 生成时就装备火把（开火姿态），覆盖卫道士默认的斧头，这样生成包直接带上火把、客户端立即显示
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TORCH));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    // 禁止 goety 默认的主动索敌（SummonTargetGoal），避免开炮后冲向敌人；只保留反击自卫
    @Override
    public void targetSelectGoal() {
    }

    // 玩家用命令聚晶/敕令聚晶把本炮兵编入队长麾下时，goety 会调用 setCommandPosEntity(队长)。
    // 这里额外把队长 UUID 持久化，供队长 AI（KubeJS findSquadArtillery）稳定识别小队成员，
    // 兼容聚晶编组的同时，队长也能直接指挥队员（无需每次再查聚晶堆栈）。
    @Override
    public void setCommandPosEntity(LivingEntity entity) {
        super.setCommandPosEntity(entity);
        if (entity != null) {
            this.getPersistentData().putString("artilleryCaptainUuid", entity.getStringUUID());
        } else {
            this.getPersistentData().remove("artilleryCaptainUuid");
        }
    }

    // 供 KubeJS 读取：持久化的队长 UUID（存于 ForgeData，与 KubeJS persistentData 不同侧）
    public String getArtilleryCaptainUuid() {
        return this.getPersistentData().getString("artilleryCaptainUuid");
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }
}
