package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import com.requiem.servants.ModItems;
import com.requiem.servants.ai.CavalryAttackGoal;
import net.minecraft.core.BlockPos;
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
        // 优先索敌传教士/恶妒者/唤魔者，其次普通敌对怪物
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
                this::isPriorityTarget));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
                this::isCavalryTarget));
    }

    private boolean isCavalryTarget(LivingEntity target) {
        return target != null && target.isAlive() && target instanceof Enemy;
    }

    /** 优先远程攻击的敌方：传教士、恶妒者、唤魔者。 */
    private boolean isPriorityTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        String path = EntityType.getKey(target.getType()).getPath();
        return path.contains("minister") || path.contains("envioker") || path.equals("evoker");
    }

    /** 玩家用敕令/骑兵军旗把轻骑兵编入骑兵队长麾下时，goety 会调用 setCommandPosEntity(队长)。
     *  这里额外持久化队长 UUID，供轻骑兵 AI 识别自己的骑兵队长并读取冲锋/收拢号令。 */
    @Override
    public void setCommandPosEntity(LivingEntity entity) {
        super.setCommandPosEntity(entity);
        if (entity != null) {
            this.getPersistentData().putString("cavalryCaptainUuid", entity.getStringUUID());
        } else {
            this.getPersistentData().remove("cavalryCaptainUuid");
        }
    }

    /** 持久化的骑兵队长 UUID（存于 ForgeData 侧）。 */
    public String getCavalryCaptainUuid() {
        return this.getPersistentData().getString("cavalryCaptainUuid");
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
        this.tickGarrison();
        if (this.isPassenger()) {
            this.tickMounted();
        } else {
            this.tickDismounted();
        }
    }

    /** 骑兵队长下达驻扎时，驻守驻扎地点；解除驻扎后恢复跟随。 */
    private void tickGarrison() {
        if (!(this.getLeader() instanceof CavalryCaptainEntity captain)) {
            return;
        }
        if (captain.isGarrisoning()) {
            BlockPos garrison = captain.getGarrison();
            if (garrison != null && !garrison.equals(this.getBoundPos())) {
                this.setBoundPos(garrison);
                this.setWandering(false);
                this.setStaying(false);
            }
        } else if (this.getBoundPos() != null) {
            this.setFollowing();
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
