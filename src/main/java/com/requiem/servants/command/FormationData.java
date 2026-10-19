package com.requiem.servants.command;

import com.Polarice3.Goety.api.entities.ally.IServant;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 成员侧的阵型状态存取。
 *
 * <p>连队成员是 goety 本体的灾厄村民仆从，这些类不在本附属里，没法给它们加字段。
 * 所以这里把数据写进 Forge 的 {@code getPersistentData()}（随实体存档，键为 {@code ForgeData}），
 * 无需 mixin 或 capability 就能给任意仆从挂上编制与槽位。
 *
 * <h2>两层状态：入编 与 站位令</h2>
 * 这两件事寿命完全不同，混在一起会导致反复入队出队：
 * <dl>
 *   <dt><b>入编</b>（{@link #enlist} / {@link #discharge}）</dt>
 *   <dd>持久，没有有效期。记录"我属于哪个连队长"以及入队前的 goety 驻留姿态。
 *       只要连队长还活着，成员就一直算这个连队的人，不会被别的连队长反复抢走。</dd>
 *   <dt><b>站位令</b>（{@link #order} / {@link #clearOrder}）</dt>
 *   <dd>带 {@value #LEASE_NOTE} 性质的短期租约，连队长每隔几刻续租一次。
 *       冲锋时会被主动清掉，让部队散开自由作战；连队长失联时自然过期，
 *       部队立刻恢复 goety 原本的 AI，而<b>入编关系仍然保留</b>，
 *       等连队长回来就能原地重新列阵。</dd>
 * </dl>
 *
 * <p>连队长阵亡时由 {@link CompanyController#disband()} 统一 {@link #discharge}；
 * 万一连队长在成员未加载时死亡，{@link com.requiem.servants.ai.FormationSlotGoal}
 * 会发现自己的连队长已不存在并自行退编。
 */
public final class FormationData {
    /** 持久化数据里的根标签名。 */
    public static final String ROOT = "RequiemFormation";

    static final String LEASE_NOTE = "租约";

    // --- 入编（持久） ---
    private static final String KEY_COMMANDER = "Commander";
    private static final String KEY_PREV_STANCE = "PrevStance";

    // --- 站位令（租约） ---
    private static final String KEY_SQUAD = "Squad";
    private static final String KEY_SLOT = "Slot";
    private static final String KEY_X = "X";
    private static final String KEY_Y = "Y";
    private static final String KEY_Z = "Z";
    private static final String KEY_YAW = "Yaw";
    private static final String KEY_STANCE = "Stance";
    private static final String KEY_FORMATION = "Formation";
    private static final String KEY_ANCHORED = "Anchored";
    private static final String KEY_MAY_FIRE = "MayFire";
    private static final String KEY_EXPIRE = "Expire";

    /** 炮火任务标记（持久，不随站位令回收）。 */
    private static final String KEY_CAPTAIN_STRIKE = "CaptainStrike";

    private static final String[] ORDER_KEYS = {
            KEY_SQUAD, KEY_SLOT, KEY_X, KEY_Y, KEY_Z, KEY_YAW,
            KEY_STANCE, KEY_FORMATION, KEY_ANCHORED, KEY_MAY_FIRE, KEY_EXPIRE
    };

    private static final byte STANCE_NONE = 0;
    private static final byte STANCE_WANDER = 1;
    private static final byte STANCE_STAY = 2;
    private static final byte STANCE_GUARD = 3;
    private static final byte STANCE_FOLLOW = 4;

    private FormationData() {
    }

    private static CompoundTag read(Mob mob) {
        return mob.getPersistentData().getCompound(ROOT);
    }

    private static void write(Mob mob, CompoundTag tag) {
        mob.getPersistentData().put(ROOT, tag);
    }

    // ------------------------------------------------------------------
    // 入编
    // ------------------------------------------------------------------

    /**
     * 把仆从编入某个连队长麾下。
     *
     * <p>首次入编时记下它原来的 goety 驻留姿态，并切到"跟随"，
     * 以免 goety 自己的游荡/驻守目标跟阵型抢移动控制权。
     *
     * <p><b>已就位的火炮是例外</b>（{@code keepStance} 为 true）：
     * 整合包的炮兵脚本有一道硬门槛 {@code if (!hostile && !entity.isGuardingArea()) return;}，
     * 也就是己方炮兵<b>只有在"驻守"姿态才会操炮</b>。如果入编时把它改成"跟随"，
     * 炮兵会一声不响地彻底停火。所以这类单位的姿态一律不动，
     * 并且把原姿态记为"未记录"，退编时也不会去改它。
     *
     * @param keepStance 是否保留仆从当前的驻留姿态
     * @return 这次调用是否产生了新的入编（已经在同一连队里则返回 false）
     */
    public static boolean enlist(Mob mob, UUID commander, boolean keepStance) {
        CompoundTag tag = read(mob);
        if (tag.hasUUID(KEY_COMMANDER) && commander.equals(tag.getUUID(KEY_COMMANDER))) {
            return false;
        }
        boolean firstTime = !tag.contains(KEY_PREV_STANCE);
        tag.putUUID(KEY_COMMANDER, commander);
        if (firstTime) {
            tag.putByte(KEY_PREV_STANCE, keepStance ? STANCE_NONE : captureStance(mob));
        }
        write(mob, tag);
        if (!keepStance && mob instanceof IServant servant) {
            servant.setFollowing();
        }
        return true;
    }

    /**
     * 自动决定要不要接管驻留姿态。
     *
     * <p>炮兵一律保留姿态（哪怕此刻还没绑炮位），因为它随时可能被玩家绑上炮位，
     * 而一旦被改成"跟随"，{@code isGuardingArea()} 不成立，脚本就再也不会让它开火。
     */
    public static boolean enlist(Mob mob, UUID commander) {
        return enlist(mob, commander,
                FormationEligibility.requiresGuardStance(mob) || ArtilleryEmplacement.hasCannon(mob));
    }

    public static boolean isEnlisted(Mob mob) {
        return read(mob).hasUUID(KEY_COMMANDER);
    }

    /** 所属连队长的 UUID；未入编时返回 {@code null}。 */
    public static UUID getCommander(Mob mob) {
        CompoundTag tag = read(mob);
        return tag.hasUUID(KEY_COMMANDER) ? tag.getUUID(KEY_COMMANDER) : null;
    }

    /** 退编：清掉全部阵型数据，撤销连队下的炮火任务，并还原入队之前的驻留姿态。 */
    public static void discharge(Mob mob) {
        CompoundTag root = mob.getPersistentData();
        if (!root.contains(ROOT)) {
            return;
        }
        // 先撤任务：它要读 ROOT 里的标记，所以必须在移除之前做
        ArtilleryEmplacement.clearCaptainStrike(mob);
        byte prev = root.getCompound(ROOT).getByte(KEY_PREV_STANCE);
        root.remove(ROOT);
        restoreStance(mob, prev);
    }

    // ------------------------------------------------------------------
    // 站位令
    // ------------------------------------------------------------------

    /**
     * 下发/续租一个槽位。调用前应已 {@link #enlist}。
     *
     * @param mayFire 这一刻是否<b>允许开火</b>。见 {@link #mayFire}。
     */
    public static void order(Mob mob, int squadIndex, int slotIndex, Vec3 pos, float yaw,
                             CompanyStance stance, FormationType formation,
                             boolean anchored, boolean mayFire, long expireGameTime) {
        CompoundTag tag = read(mob);
        tag.putBoolean(KEY_MAY_FIRE, mayFire);
        tag.putInt(KEY_SQUAD, squadIndex);
        tag.putInt(KEY_SLOT, slotIndex);
        tag.putDouble(KEY_X, pos.x);
        tag.putDouble(KEY_Y, pos.y);
        tag.putDouble(KEY_Z, pos.z);
        tag.putFloat(KEY_YAW, yaw);
        tag.putByte(KEY_STANCE, (byte) stance.ordinal());
        tag.putByte(KEY_FORMATION, (byte) formation.ordinal());
        tag.putBoolean(KEY_ANCHORED, anchored);
        tag.putLong(KEY_EXPIRE, expireGameTime);
        write(mob, tag);
    }

    /** 只收回站位令，保留入编关系。 */
    public static void clearOrder(Mob mob) {
        CompoundTag root = mob.getPersistentData();
        if (!root.contains(ROOT)) {
            return;
        }
        CompoundTag tag = root.getCompound(ROOT);
        for (String key : ORDER_KEYS) {
            tag.remove(key);
        }
        root.put(ROOT, tag);
    }

    /** 该仆从是否持有一个仍然有效的站位令。 */
    public static boolean hasActiveOrder(Mob mob, long gameTime) {
        CompoundTag tag = read(mob);
        return tag.contains(KEY_EXPIRE) && tag.getLong(KEY_EXPIRE) > gameTime;
    }

    public static int getSquadIndex(Mob mob) {
        return read(mob).getInt(KEY_SQUAD);
    }

    public static int getSlotIndex(Mob mob) {
        return read(mob).getInt(KEY_SLOT);
    }

    /** 槽位的目标世界坐标；没有站位令时返回 {@code null}。 */
    public static Vec3 getSlotPos(Mob mob) {
        CompoundTag tag = read(mob);
        if (!tag.contains(KEY_X)) {
            return null;
        }
        return new Vec3(tag.getDouble(KEY_X), tag.getDouble(KEY_Y), tag.getDouble(KEY_Z));
    }

    /** 槽位要求的朝向（Minecraft 偏航角）。 */
    public static float getSlotYaw(Mob mob) {
        return read(mob).getFloat(KEY_YAW);
    }

    public static CompanyStance getStance(Mob mob) {
        return CompanyStance.byOrdinal(read(mob).getByte(KEY_STANCE));
    }

    public static FormationType getFormation(Mob mob) {
        return FormationType.byOrdinal(read(mob).getByte(KEY_FORMATION));
    }

    /**
     * 该槽位是否"钉死"。
     *
     * <p>钉死的单位（炮列里的炮兵）到位后就不再移动，也不会因为敌人靠近而让出移动控制权——
     * 它们只负责站在原地按连队长的号令开火。
     */
    public static boolean isAnchored(Mob mob) {
        return read(mob).getBoolean(KEY_ANCHORED);
    }

    /**
     * 这一刻是否允许开火。
     *
     * <h2>为什么"开火许可"要靠移动锁来实现</h2>
     * goety 没有停火接口（{@code IOwned.onStopAttack()} 只是广播一个粒子事件，
     * {@code onCeaseFire} 是空实现）。但原版与 goety 的远程攻击目标都申请
     * {@code Goal.Flag.MOVE}，而 {@code GoalSelector} 只允许<b>优先级数字更小</b>的目标抢占已占用的标志。
     * 列阵目标占着 MOVE 且优先级为 3，所以只要它继续持有，优先级 4 以后的攻击目标就跑不起来
     * —— 这正好等价于"不许开火"。
     *
     * <p>于是轮番射击就落在这一个开关上：允许开火的那一排在站定后<b>交还</b> MOVE，
     * 攻击目标得以运行；装填中的排继续<b>攥着</b> MOVE，于是举着武器站在原地不放箭。
     *
     * <p>注意：这套机制只能约束走 goety/原版攻击目标的单位（掠夺者仆从等）。
     * 掷弹兵的投弹由 KubeJS 脚本直接执行，完全不经过目标系统，因此无法用这种方式停火。
     */
    public static boolean mayFire(Mob mob) {
        return read(mob).getBoolean(KEY_MAY_FIRE);
    }

    /**
     * 当前脚本里的打击坐标是否由连队长下达。
     *
     * <p>用来区分"连队的炮火任务"和"玩家用炮射地图手动指定的目标"，
     * 后者优先，连队不会去覆盖或清除。见 {@link ArtilleryEmplacement#setStrikeTarget}。
     */
    public static boolean isCaptainStrike(Mob mob) {
        return read(mob).getBoolean(KEY_CAPTAIN_STRIKE);
    }

    public static void setCaptainStrike(Mob mob, boolean captainStrike) {
        CompoundTag tag = read(mob);
        if (captainStrike) {
            tag.putBoolean(KEY_CAPTAIN_STRIKE, true);
        } else {
            tag.remove(KEY_CAPTAIN_STRIKE);
        }
        write(mob, tag);
    }

    // ------------------------------------------------------------------
    // goety 驻留姿态的保存与还原
    // ------------------------------------------------------------------

    private static byte captureStance(Mob mob) {
        if (!(mob instanceof IServant servant)) {
            return STANCE_NONE;
        }
        if (servant.isWandering()) {
            return STANCE_WANDER;
        }
        if (servant.isStaying()) {
            return STANCE_STAY;
        }
        if (servant.isGuardingArea()) {
            return STANCE_GUARD;
        }
        return STANCE_FOLLOW;
    }

    private static void restoreStance(Mob mob, byte stance) {
        if (!(mob instanceof IServant servant)) {
            return;
        }
        switch (stance) {
            case STANCE_WANDER -> servant.setWandering();
            case STANCE_STAY -> servant.setStaying();
            case STANCE_GUARD -> servant.setGuarding();
            case STANCE_FOLLOW -> servant.setFollowing();
            default -> {
                // 入队前没有记录过姿态，保持现状
            }
        }
    }
}
