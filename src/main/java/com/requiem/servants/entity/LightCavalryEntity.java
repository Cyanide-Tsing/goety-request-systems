package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import com.requiem.servants.ModItems;
import com.requiem.servants.ai.CavalryAttackGoal;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 轻骑兵仆从：骑乘原版马的机动单位。
 *
 * <p>有马时持弓射击，并给坐骑迅捷I+抗性提升I；没有马时下马持苍白军刀近战。
 * 生成时默认不带马，靠"半自动化"流程在附近寻找被驯服的马并骑上去。
 */
public class LightCavalryEntity extends VindicatorServant {
    private static final double HORSE_SCAN_RANGE = 8.0D;

    private int cavalryTick;

    public LightCavalryEntity(EntityType<? extends LightCavalryEntity> type, Level level) {
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
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PALE_SABER.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public void targetSelectGoal() {
    }

    /** 自定义攻击 AI：骑乘远程射箭、下马近战，压过 goety 的近战 AI。 */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new CavalryAttackGoal(this));
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
                this::isCavalryTarget));
    }

    private boolean isCavalryTarget(LivingEntity target) {
        return target != null && target.isAlive() && target instanceof Enemy;
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
        if (!this.level().isClientSide()) {
            this.cavalryTick++;
            if (this.cavalryTick % 20 == 0) {
                this.tickCavalry();
            }
        }
    }

    private void tickCavalry() {
        if (this.isPassenger()) {
            this.tickMounted();
        } else {
            this.tickDismounted();
        }
    }

    private void tickMounted() {
        if (this.getVehicle() instanceof LivingEntity horse) {
            horse.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            horse.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
        }
        if (this.getMainHandItem().getItem() != Items.BOW) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
    }

    private void tickDismounted() {
        if (this.getMainHandItem().getItem() != ModItems.PALE_SABER.get()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PALE_SABER.get()));
        }
        this.findAndMountHorse();
    }

    /** 半自动化：在附近寻找被驯服且未被骑乘的马，骑上去。 */
    private void findAndMountHorse() {
        AABB box = this.getBoundingBox().inflate(HORSE_SCAN_RANGE);
        for (AbstractHorse horse : this.level().getEntitiesOfClass(AbstractHorse.class, box,
                h -> h.isTamed() && !h.isVehicle() && h.isAlive())) {
            if (this.startRiding(horse)) {
                break;
            }
        }
    }
}
