package com.requiem.servants.command;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant;
import com.Polarice3.Goety.common.entities.ally.illager.cultist.CultistServant;
import com.Polarice3.Goety.common.entities.ally.illager.cultist.WitchServant;
import com.requiem.servants.entity.ApprenticeEntity;
import com.requiem.servants.entity.ArtilleryCaptainEntity;
import com.requiem.servants.entity.ArtilleryEntity;
import com.requiem.servants.entity.CaptainEntity;
import com.requiem.servants.entity.GardenerEntity;
import com.requiem.servants.entity.RecruiterEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;

/**
 * 判断一个仆从能不能列阵。
 *
 * <p>这是本附属与 goety 本体的兼容层：连队不限于本附属新增的仆从，<b>任何灾厄村民仆从</b>
 * （卫道士仆从、掠夺者仆从、唤魔者仆从、传令兵仆从、矿工仆从、唤冰者仆从……）都能被编入连队。
 * 判定对 {@link com.requiem.servants.ForgeEventHandler} 的目标注入和 {@link CompanyController}
 * 的招募共用同一套标准，避免两边口径不一致。
 *
 * <h2>只收灾厄村民</h2>
 * 连队长只指挥灾厄村民。让僵尸、骷髅、狼、劫掠兽去站军阵既违和也不合理，
 * 所以这里卡在 goety 的两个灾厄村民基类上：
 * <ul>
 *   <li>{@link AbstractIllagerServant} —— 绝大多数灾厄村民仆从的基类
 *       （{@code Neollager}、{@code VindicatorServant}、{@code PillagerServant}、
 *       {@code SignalerServant}、{@code SpellcasterIllagerServant} 等）。</li>
 *   <li>{@link CultistServant} —— 教徒系（异端、叛徒、堕落者、术士仆从）。
 *       它直接继承 {@code RaiderServant} 而不是 {@code AbstractIllagerServant}，所以要单独列出。</li>
 *   <li>{@link WitchServant} —— 女巫仆从又是个例外：它<b>既不</b>继承
 *       {@code AbstractIllagerServant} <b>也不</b>继承 {@code CultistServant}，
 *       而是直接继承 {@code RaiderServant}，所以也必须单独点名，否则会被漏掉。</li>
 * </ul>
 * 刻意<b>不</b>卡在它们共同的父类 {@code RaiderServant} 上：那一层还包含
 * {@code AnimalRaiderServant}（撕裂者等动物）、{@code ModRavager}（劫掠兽）、
 * {@code AllyVex}/{@code AllyIrk}（会飞）这些不该列阵的单位。
 */
public final class FormationEligibility {
    private FormationEligibility() {
    }

    /**
     * 该仆从是否<b>有能力</b>保持阵型。用于决定要不要给它注入列阵目标。
     *
     * <p>只看类型与寻路方式这些不会变的属性，因为目标注入只在实体加入世界时发生一次。
     */
    public static boolean canHoldFormation(Mob mob) {
        if (!isIllagerServant(mob)) {
            return false;
        }
        // 兜底：只要地面寻路的单位。灾厄村民本来都是地面单位，
        // 这一条是防止以后 goety 加入会飞的灾厄村民仆从时出现怪异行为。
        return mob.getNavigation() instanceof GroundPathNavigation;
    }

    /** 是否为灾厄村民仆从（含教徒系与女巫仆从）。 */
    public static boolean isIllagerServant(Mob mob) {
        return mob instanceof AbstractIllagerServant
                || mob instanceof CultistServant
                || mob instanceof WitchServant;
    }

    /**
     * 该仆从是否由整合包的 KubeJS 脚本驱动自己的工作流程。
     *
     * <p>学徒、园丁、征兵官都有各自的 {@code global.*AI} 循环，而<b>这些循环开头一律是</b>
     * <pre>if (!entity.isGuardingArea()) return;</pre>
     * 也就是说它们只有在"驻守"姿态下才会干活。连队入编会把成员改成"跟随"
     * （goety 的 {@code setFollowing()} 把 {@code boundPos} 置空），
     * 那会直接让它们停工。
     *
     * <p>这些是经济单位而不是战斗单位，本来也不该被拉进战列，所以直接排除在招募之外。
     * 炮兵虽然同样是脚本驱动，但它是火力单位，由
     * {@link ArtilleryEmplacement#emplace} 以"驻守点绑到炮位"的方式特殊处理。
     */
    public static boolean isScriptedWorker(Mob mob) {
        return mob instanceof ApprenticeEntity
                || mob instanceof GardenerEntity
                || mob instanceof RecruiterEntity;
    }

    /**
     * 该仆从的驻留姿态是否绝不能被连队改动。
     *
     * <p>炮兵要靠"驻守"姿态才会操炮，见 {@link ArtilleryEmplacement}。
     */
    public static boolean requiresGuardStance(Mob mob) {
        return mob instanceof ArtilleryEntity || isScriptedWorker(mob);
    }

    /**
     * 该仆从<b>当前</b>是否可以被编入连队。
     *
     * <p>在 {@link #canHoldFormation} 之上追加会随时间变化的条件，
     * 因此只在点验名册时判断，幼年仆从长大、骑乘状态解除之后会自动被接收。
     */
    public static boolean canBeRecruited(Mob mob) {
        if (!canHoldFormation(mob)) {
            return false;
        }
        // 连队长之间不互相编队，否则两个连队会抢人
        if (mob instanceof CaptainEntity) {
            return false;
        }
        // 连队只管步兵：不收炮兵与炮兵队长（它们由炮兵号角单独指挥）
        if (mob instanceof ArtilleryEntity || mob instanceof ArtilleryCaptainEntity) {
            return false;
        }
        // 脚本驱动的工作仆从有自己的活干，拉进战列会让它们停工
        if (isScriptedWorker(mob)) {
            return false;
        }
        // 幼年仆从不入列
        if (mob.isBaby()) {
            return false;
        }
        // 正骑在坐骑上时站位没有意义
        return !mob.isPassenger();
    }
}
