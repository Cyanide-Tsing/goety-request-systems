package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 掷弹兵仆从：投掷 Supplementaries 炸弹的重火力远程单位，近战用铁刀自卫。
 *
 * <h2>投弹逻辑在哪</h2>
 * 实际投弹由整合包的 KubeJS 脚本
 * {@code server_scripts/goety_storyline/50_grenadier.js}（{@code global.grenadierAI}）完成。
 * 本类只负责把实体注册进游戏、给出属性与渲染，让那套脚本有东西可以驱动。
 *
 * <h2>和炮兵的关键差别</h2>
 * 掷弹兵的脚本<b>不</b>检查 {@code isGuardingArea()}（炮兵、学徒、园丁、征兵官都要检查）。
 * 也就是说掷弹兵的驻留姿态可以随便改，连队可以像指挥普通远程兵一样自由调度它，
 * 不需要像炮兵那样把驻守点绑死在炮位上。
 *
 * <p>脚本的射击参数：有效射程 20 格，目标近到 3 格以内会主动后撤，约每 2 秒投一颗。
 * 所以连队给它安排的站位要比普通远程更靠后，见
 * {@link com.requiem.servants.command.ArmType#preferredStandoff()}。
 */
public class GrenadierEntity extends VindicatorServant {
    public GrenadierEntity(EntityType<? extends GrenadierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes()
                // 比炮兵稍硬一点，但依然是脆皮远程
                .add(Attributes.MAX_HEALTH, 30.0D);
    }

    /** 双手分开，便于渲染手持的刀与炸弹。 */
    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    /** 生成时就装备炸弹，覆盖卫道士默认的斧头，这样生成包直接带上炸弹、客户端立即显示。 */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        Item bomb = ForgeRegistries.ITEMS.getValue(new ResourceLocation("supplementaries", "bomb"));
        if (bomb != null && bomb != Items.AIR) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(bomb));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    /** 自定义索敌：敌对只打玩家（避免与灾厄村民内战）；己方打敌对怪物。 */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
                this::isGrenadierTarget));
    }

    private boolean isGrenadierTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (this.isHostile()) {
            return target instanceof Player;
        }
        return target instanceof Enemy;
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }
}
