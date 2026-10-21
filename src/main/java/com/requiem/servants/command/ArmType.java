package com.requiem.servants.command;

import com.Polarice3.Goety.api.entities.IMobCrafter;
import com.Polarice3.Goety.common.entities.ally.illager.SignalerServant;
import com.Polarice3.Goety.common.entities.ally.illager.SpellcasterIllagerServant;
import com.Polarice3.Goety.common.entities.ally.illager.StormCasterServant;
import com.requiem.servants.entity.ApprenticeEntity;
import com.requiem.servants.entity.ArtilleryEntity;
import com.requiem.servants.entity.ConquerorEntity;
import com.requiem.servants.entity.DrummerEntity;
import com.requiem.servants.entity.GardenerEntity;
import com.requiem.servants.entity.GrenadierEntity;
import com.requiem.servants.entity.RecruiterEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ProjectileWeaponItem;

/**
 * 兵种。决定小队在阵型里的前后次序：近战顶在最前，远程居中，法术再后，辅助压在最末。
 *
 * <p>兵种也决定炮列里谁会被钉死（见 {@link FormationType#anchors}）以及连队长指挥谁齐射。
 */
public enum ArmType {
    /** 近战：顶在第一排。 */
    MELEE("melee", 0),
    /** 远程：退后一排，从近战身后射击；在炮列中会被钉死成固定火线。 */
    RANGED("ranged", 1),
    /** 施法：再退一排，避免被近身。 */
    CASTER("caster", 2),
    /** 辅助/非战斗（传令、工匠、厨师、农夫）：压在最后。 */
    SUPPORT("support", 3);

    private final String serializedName;
    private final int rankOffset;

    ArmType(String serializedName, int rankOffset) {
        this.serializedName = serializedName;
        this.rankOffset = rankOffset;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public MutableComponent getDisplayName() {
        return Component.translatable("requiem.arm." + this.serializedName);
    }

    /** 该兵种相对第一排往后退的排数。 */
    public int rankOffset() {
        return this.rankOffset;
    }

    /** 该兵种是否倾向于近身搏斗（用于判断连队该冲锋还是该保持阵型射击）。 */
    public boolean isMelee() {
        return this == MELEE;
    }

    /** 该兵种是否靠远程输出。 */
    public boolean isRanged() {
        return this == RANGED || this == CASTER;
    }

    /**
     * 该兵种希望与敌人保持的距离（格）。
     *
     * <p>连队长派发独立任务时用它算小队的落脚点：把小队放在"距目标这么远、且在我方这一侧"的位置。
     *
     * <p>远程取 12 格是照着掷弹兵定的——整合包脚本里它的投弹射程是 20 格，
     * 但目标近到 3 格以内就会主动后撤（怕炸到自己），12 格刚好在射程内又远离自伤范围。
     */
    public double preferredStandoff() {
        return switch (this) {
            case MELEE -> 2.0D;
            case RANGED -> 12.0D;
            case CASTER -> 10.0D;
            case SUPPORT -> 20.0D;
        };
    }

    /**
     * 判断一个仆从属于哪个兵种。
     *
     * <p>按从具体到一般的顺序判断，保证 goety 全部灾厄村民仆从都能被正确归类：
     * <ol>
     *   <li>本附属自己的仆从（炮兵 = 远程；征兵官/学徒/园丁 = 辅助）。</li>
     *   <li>{@link IMobCrafter} —— 工匠类（矿工仆从、厨师仆从）归为辅助。</li>
     *   <li>{@link SignalerServant} —— 传令兵归为辅助。</li>
     *   <li>{@link SpellcasterIllagerServant} —— 唤魔者/唤冰者/冰霜法师/地卜师/唤风者。</li>
     *   <li>{@link StormCasterServant} —— 风暴法师<b>直接</b>继承 {@code AbstractIllagerServant}
     *       而不是施法者基类，必须单独点名，否则会被误判成近战。</li>
     *   <li>{@link CrossbowAttackMob} / {@link RangedAttackMob} —— 覆盖掠夺者仆从、
     *       女巫仆从、术士仆从这些用接口标记远程能力的单位。</li>
     *   <li>主手/副手持远程武器。</li>
     *   <li>其余按近战处理。</li>
     * </ol>
     */
    public static ArmType classify(Mob mob) {
        if (mob instanceof ArtilleryEntity || mob instanceof GrenadierEntity || mob instanceof ConquerorEntity) {
            return RANGED;
        }
        if (mob instanceof RecruiterEntity || mob instanceof ApprenticeEntity || mob instanceof GardenerEntity
                || mob instanceof DrummerEntity) {
            return SUPPORT;
        }
        if (mob instanceof IMobCrafter || mob instanceof SignalerServant) {
            return SUPPORT;
        }
        if (mob instanceof SpellcasterIllagerServant || mob instanceof StormCasterServant) {
            return CASTER;
        }
        if (mob instanceof CrossbowAttackMob || mob instanceof RangedAttackMob) {
            return RANGED;
        }
        if (mob.getMainHandItem().getItem() instanceof ProjectileWeaponItem
                || mob.getOffhandItem().getItem() instanceof ProjectileWeaponItem) {
            return RANGED;
        }
        return MELEE;
    }
}
