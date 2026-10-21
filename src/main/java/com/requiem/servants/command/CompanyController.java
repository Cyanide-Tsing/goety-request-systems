package com.requiem.servants.command;

import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.api.entities.ally.IServant;
import com.requiem.servants.ModEntities;
import com.requiem.servants.entity.ArtilleryCaptainEntity;
import com.requiem.servants.entity.ArtilleryEntity;
import com.requiem.servants.entity.CaptainEntity;
import com.requiem.servants.entity.DrummerEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 连级指挥：一个连队长的大脑，在服务端每刻跑一次。
 *
 * <h2>锚点以连队长为基准</h2>
 * 阵型<b>不</b>围着玩家排，而是完全由连队长决定：先在局部坐标系里解算阵型，
 * 量出最后一排的位置 {@link FormationLayout#rearmostZ}，再把锚点放到连队长<b>正前方</b>
 * {@code |rearmostZ| + } {@value #CAPTAIN_STANDOFF} 格处。这样整个阵型都展开在连队长前面，
 * 连队长天然留在最后一排之后，不会被挤到阵前送命。
 *
 * <p>连队长自己<b>不占</b>阵型槽位，它用 goety 原本的跟随 AI 走路。
 * 这一点很关键：如果锚点由连队长位置推导、而连队长又被锚点驱动，就会形成正反馈导致阵型漂移。
 *
 * <h2>为什么阵型能很快稳定下来</h2>
 * <ul>
 *   <li>成员在小队内按 UUID 排序，槽位编号与扫描顺序无关，重新点验名册不会让大家互相换位。</li>
 *   <li>锚点与朝向带迟滞（{@value #ANCHOR_HYSTERESIS} 格 / {@value #FACING_HYSTERESIS} 度），
 *       连队长小幅走动不会重排整个阵型。</li>
 *   <li>姿态切换需要连续两次评估得出同一结论，避免在 坚守/交战/冲锋 之间反复抖动。</li>
 * </ul>
 */
public class CompanyController {
    /** 一个连队最多编几个小队。 */
    public static final int MAX_SQUADS = 6;
    /** 一个小队最多几人，超出就拆成下一个小队。 */
    public static final int SQUAD_SIZE = 8;
    /** 下发/续租站位令的间隔。 */
    public static final int LAYOUT_INTERVAL = 10;
    /** 点验名册的间隔。 */
    public static final int ROSTER_INTERVAL = 40;
    /** 评估姿态的间隔。 */
    public static final int STANCE_INTERVAL = 20;
    /** 站位令租约时长。连队长失联后部队最多再站这么久。 */
    public static final int SLOT_LEASE_TICKS = 60;
    /** 指挥齐射的间隔。 */
    public static final int VOLLEY_INTERVAL = 40;
    /** 连队长留在最后一排之后多远。 */
    public static final double CAPTAIN_STANDOFF = 4.0D;

    /** 招募半径。 */
    private static final double RECRUIT_RADIUS = 24.0D;
    /** 超出这个距离的成员视为走散，退出连队。 */
    private static final double LEASH_RADIUS = 56.0D;
    /** 搜索威胁的半径。 */
    private static final double THREAT_RADIUS = 36.0D;
    /** 威胁远于此 → 前进。 */
    private static final double ADVANCE_RANGE = 24.0D;
    /** 威胁近于此 → 冲锋或交战。 */
    private static final double CHARGE_RANGE = 10.0D;
    /** 超过半数成员离锚点这么远 → 集结重整。 */
    private static final double RALLY_TRIGGER = 24.0D;
    /** 锚点迟滞：漂移不到这个距离就不重排。 */
    private static final double ANCHOR_HYSTERESIS = 1.5D;
    /** 朝向迟滞：转动不到这个角度就不重排。 */
    private static final float FACING_HYSTERESIS = 20.0F;
    /** 齐射时给远程单位的优先目标持续时间。 */
    private static final int VOLLEY_PRIORITY_TICKS = 100;

    /** 大炮的有效射程（与脚本里的 CANNON_RANGE 一致）。 */
    private static final double CANNON_RANGE = 32.0D;
    /** 判定"成群"时的聚集半径。 */
    private static final double CLUSTER_RADIUS = 4.5D;
    /** 达到这个数量才值得用面杀伤坐标轰，否则点杀更划算。 */
    private static final int CLUSTER_THRESHOLD = 3;
    /** 单次聚类最多考虑多少个威胁，避免 O(n²) 在大规模战斗里爆炸。 */
    private static final int CLUSTER_SCAN_CAP = 48;
    /** 敌人进到炮阵这个距离 → 炮组撤退。 */
    private static final double BATTERY_DANGER = 7.0D;
    /** 退到这个距离之外 → 炮组复位架炮（迟滞，避免反复撤回）。 */
    private static final double BATTERY_SAFE = 13.0D;

    /** 侦察阶段持续时间。 */
    private static final int SCOUT_TICKS = 120;
    /** 炮击阶段最长持续时间，打不下来也要上去。 */
    private static final int BOMBARD_TICKS = 900;
    /** 占领阶段持续时间，之后解除作战任务。 */
    private static final int SECURE_TICKS = 200;
    /** 清点守军的半径。 */
    private static final double OBJECTIVE_RADIUS = 18.0D;
    /** 阶段推进的判定间隔。 */
    private static final int ASSAULT_INTERVAL = 20;
    /** 逼近阶段的兜底超时：路被堵住也不能永远卡在第一步。 */
    private static final int APPROACH_TIMEOUT = 1200;
    /** 炮火覆盖区未清空时，指挥位额外后撤的距离（让步兵先撤出覆盖区）。 */
    private static final double BREACH_WITHDRAW_OFFSET = 14.0D;
    /** 连队长站在阵型锚点之后多远（指挥位与锚点的距离）。 */
    private static final double CAPTAIN_REAR_OFFSET = CAPTAIN_STANDOFF * 2.0D;
    /** 到位判定的宽容量。 */
    private static final double ARRIVE_SLACK = 6.0D;
    /** 维护炮兵状态的间隔。 */
    private static final int ARTILLERY_INTERVAL = 20;
    /** 搜索附近炮位的间隔。方块扫描较贵，所以放得很稀。 */
    private static final int CANNON_SCAN_INTERVAL = 200;
    /** 搜索炮位的水平半径。 */
    private static final int CANNON_SCAN_RADIUS = 12;
    /** 搜索炮位的垂直半径。 */
    private static final int CANNON_SCAN_HEIGHT = 4;
    /**
     * 操炮的炮组不受连队牵引距离限制：炮位往往架在远处的阵地上，
     * 不能因为连队长走远就把炮兵开除。这是它们各自的上限。
     */
    private static final double GUNNER_LEASH = 192.0D;
    /** 独立任务小队脱离连队的最远距离。 */
    private static final double DETACH_LIMIT = 40.0D;
    /** 线列轮换一层的间隔：够前排放完一轮。 */
    private static final int VOLLEY_ROTATE_TICKS = 60;
    /** 鼓手发放增益的间隔（刻）。 */
    private static final int DRUMMER_BUFF_INTERVAL = 40;
    /** 鼓手离连队长这个范围以内才发增益。 */
    private static final double DRUMMER_RANGE = 32.0D;
    private static final double DRUMMER_RANGE_SQR = DRUMMER_RANGE * DRUMMER_RANGE;
    /** 鼓手增益的持续时间（刻），略长于发放间隔，保证不失效。 */
    private static final int DRUMMER_BUFF_DURATION = 60;
    /**
     * 炮击弹着点周围这个范围内有自己人就不开火。
     *
     * <p>Supplementaries 的炸弹与炮弹都是范围伤害，所以连队长在下达坐标之前
     * 必须先确认弹着点附近没有自己的部队，否则一轮齐射就把前排的近战犁掉了。
     */
    private static final double FRIENDLY_SAFETY_RADIUS = 7.0D;

    private static final double GUNNER_LEASH_SQR = GUNNER_LEASH * GUNNER_LEASH;

    private static final double LEASH_RADIUS_SQR = LEASH_RADIUS * LEASH_RADIUS;
    private static final double RALLY_TRIGGER_SQR = RALLY_TRIGGER * RALLY_TRIGGER;
    private static final double CLUSTER_RADIUS_SQR = CLUSTER_RADIUS * CLUSTER_RADIUS;
    private static final double BATTERY_DANGER_SQR = BATTERY_DANGER * BATTERY_DANGER;
    private static final double BATTERY_SAFE_SQR = BATTERY_SAFE * BATTERY_SAFE;

    private final CaptainEntity captain;
    private final List<UUID> roster = new ArrayList<>();
    private final List<Squad> squads = new ArrayList<>();

    private FormationType formation = FormationType.LINE;
    private CompanyStance stance = CompanyStance.FORM_UP;

    /** 是否由连队长自行选择阵型。玩家用连旗指定阵型后关闭，绕完一轮再交还。 */
    private boolean autoFormation = true;
    /** 阵型迟滞：候选阵型要连续出现两次才真正切换。 */
    private FormationType pendingFormation;

    /** 上次真正下发给部队的锚点与朝向（带迟滞，不是每刻重算的瞬时值）。 */
    private Vec3 anchor;
    private float facing;

    /** 玩家用连旗钉死的集结点。钉住后锚点与朝向不再自动跟随连队长。 */
    private Vec3 pinnedAnchor;
    private float pinnedFacing;

    /** 姿态迟滞：候选姿态要连续出现两次才真正切换。 */
    private CompanyStance pendingStance;

    /** 玩家手动指定姿态后的保持时间，期间不自动评估。 */
    private int stanceLockTicks;

    /** 炮阵是否已被近身而撤炮。带迟滞，见 {@link #BATTERY_DANGER} / {@link #BATTERY_SAFE}。 */
    private boolean batteryWithdrawn;
    /** 上一轮计算出的炮群中心，用于判断炮阵是否被攻击。 */
    private Vec3 batteryCenter;

    /** 突击作战的目标点；没有任务时为 {@code null}。 */
    private BlockPos objective;
    private AssaultPhase phase = AssaultPhase.NONE;
    private int phaseTicks;
    /** 进入炮击阶段时目标区域的守军数量，用于判断压制效果。 */
    private int defendersAtBombardStart;
    /** 突击阶段开始时的连队总人数，用于判定伤亡是否过半。 */
    private int assaultHeadcount;
    /** 侦察阶段标记的墙面爆破点（目标被墙挡住时）。 */
    private BlockPos breachTarget;

    /** 每支小队当前的任务与落脚点，按 {@link #squads} 的下标索引。 */
    private final Map<Integer, SquadOrder> squadOrders = new HashMap<>();
    /** 上一轮下发的线列层号（小队下标 → 层号），供轮换判定使用。 */
    private final Map<Integer, Integer> lastVolleyRank = new HashMap<>();
    /** 已知的炮位（由炮兵身上的绑定 + 周期性方块扫描汇总而来）。 */
    private final Set<BlockPos> knownCannons = new LinkedHashSet<>();
    /** 支援清单里的炮兵队长 UUID（由连旗加入，炮火支援时吹号开火）。 */
    private final List<UUID> supportCaptains = new ArrayList<>();
    /** 鼓手槽：被连旗绑定的鼓手仆从 UUID，为连队提供节奏增益。 */
    private UUID drummerId;
    /** 鼓手发放增益的节流计时。 */
    private int drummerBuffTimer;

    private int rosterTimer;
    private int stanceTimer;
    private int layoutTimer;
    private int volleyTimer;
    private int assaultTimer;
    private int artilleryTimer;
    private int cannonScanTimer;
    /** 线列轮换计数：决定哪一层在前、获准开火。 */
    private int volleyPhase;
    private int volleyRotateTimer;
    /** 当前是否因为怕误伤而停火（用于避免重复播报）。 */
    private boolean holdingFire;

    /** 一支小队领到的任务。 */
    private static final class SquadOrder {
        private SquadMission mission = SquadMission.HOLD_LINE;
        private Vec3 anchor;
        private float facing;
        /** 拦截任务的目标。 */
        private UUID targetId;
    }

    public CompanyController(CaptainEntity captain) {
        this.captain = captain;
    }

    // ------------------------------------------------------------------
    // 主循环
    // ------------------------------------------------------------------

    public void tick() {
        if (!(this.captain.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.stanceLockTicks > 0) {
            this.stanceLockTicks--;
        }
        if (--this.rosterTimer <= 0) {
            this.rosterTimer = ROSTER_INTERVAL;
            this.refreshRoster(level);
            this.organize(level);
        }
        // 炮兵维护必须独立于阵型流程：pushOrders 有好几条提前返回的路径
        // （冲锋、无小队……），如果把架炮逻辑挂在那里，炮兵会在那些状态下
        // 一直停留在"跟随"而彻底停火。
        if (--this.artilleryTimer <= 0) {
            this.artilleryTimer = ARTILLERY_INTERVAL;
            this.maintainArtillery(level);
        }
        if (--this.assaultTimer <= 0) {
            this.assaultTimer = ASSAULT_INTERVAL;
            this.tickAssault(level);
        }
        if (--this.stanceTimer <= 0) {
            this.stanceTimer = STANCE_INTERVAL;
            this.updateStance(level);
        }
        if (--this.layoutTimer <= 0) {
            this.layoutTimer = LAYOUT_INTERVAL;
            this.pushOrders(level);
        }
        if (--this.volleyRotateTimer <= 0) {
            this.volleyRotateTimer = VOLLEY_ROTATE_TICKS;
            // 轮换线列的层次：掠夺者（弩手）在同一行全部装填完成后才与掷弹兵轮换
            if (!this.formation.usesVolleyRanks() || this.frontRankReady(level)) {
                this.volleyPhase++;
                this.layoutTimer = 0;
            }
        }
        if (--this.volleyTimer <= 0) {
            this.volleyTimer = VOLLEY_INTERVAL;
            this.directVolley(level);
        }
        if (--this.drummerBuffTimer <= 0) {
            this.drummerBuffTimer = DRUMMER_BUFF_INTERVAL;
            this.applyDrummerBuffs(level);
        }
    }

    // ------------------------------------------------------------------
    // 1. 点验名册
    // ------------------------------------------------------------------

    private void refreshRoster(ServerLevel level) {
        Iterator<UUID> it = this.roster.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            if (!(level.getEntity(id) instanceof Mob mob) || !mob.isAlive()) {
                // 实体不在了（阵亡或卸载）：从名册移除，但不动它的入编数据，
                // 重新加载后如果还在附近会被重新收编
                it.remove();
                continue;
            }
            // 炮组的牵引距离要宽得多：炮位常架在远处的阵地上，
            // 不能因为连队长往前走了就把整个炮兵连开除。
            double leash = ArtilleryEmplacement.hasCannon(mob) ? GUNNER_LEASH_SQR : LEASH_RADIUS_SQR;
            if (mob.distanceToSqr(this.captain) > leash || !this.sameAllegiance(mob)) {
                FormationData.discharge(mob);
                it.remove();
            }
        }

        int capacity = MAX_SQUADS * SQUAD_SIZE;
        if (this.roster.size() >= capacity) {
            return;
        }
        AABB box = this.captain.getBoundingBox().inflate(RECRUIT_RADIUS);
        for (Mob candidate : level.getEntitiesOfClass(Mob.class, box, this::canRecruit)) {
            if (this.roster.size() >= capacity) {
                break;
            }
            FormationData.enlist(candidate, this.captain.getUUID());
            this.roster.add(candidate.getUUID());
        }
    }

    private boolean canRecruit(Mob mob) {
        if (mob == this.captain || !mob.isAlive()) {
            return false;
        }
        // 只收灾厄村民，且排除幼年/骑乘/会飞的
        if (!FormationEligibility.canBeRecruited(mob)) {
            return false;
        }
        if (this.roster.contains(mob.getUUID()) || !this.sameAllegiance(mob)) {
            return false;
        }
        // 已经入编在另一个还活着的连队里：不抢人。
        // 这正是入编关系要持久保存的原因——否则两个连队长会来回拉扯同一批部队。
        UUID other = FormationData.getCommander(mob);
        if (other != null && !other.equals(this.captain.getUUID())) {
            return !(level(mob).getEntity(other) instanceof CaptainEntity owner) || !owner.isAlive();
        }
        return true;
    }

    private ServerLevel level(Mob mob) {
        return (ServerLevel) mob.level();
    }

    /** 同一个主人、同一个敌对状态才算自己人。 */
    private boolean sameAllegiance(Mob mob) {
        if (!(mob instanceof IOwned owned)) {
            return false;
        }
        return owned.isHostile() == this.captain.isHostile()
                && Objects.equals(owned.getOwnerId(), this.captain.getOwnerId());
    }

    // ------------------------------------------------------------------
    // 2. 编组小队
    // ------------------------------------------------------------------

    private void organize(ServerLevel level) {
        // 用 TreeMap + UUID 排序，保证编组结果只取决于"有哪些人"，
        // 跟扫描顺序无关；否则每次点验都可能让大家互换槽位，阵型永远稳不下来。
        Map<String, List<UUID>> byType = new TreeMap<>();
        Map<String, EntityType<?>> types = new HashMap<>();
        Map<String, ArmType> arms = new HashMap<>();

        for (UUID id : this.roster) {
            if (level.getEntity(id) instanceof Mob mob) {
                String key = EntityType.getKey(mob.getType()).toString();
                byType.computeIfAbsent(key, k -> new ArrayList<>()).add(id);
                types.putIfAbsent(key, mob.getType());
                arms.putIfAbsent(key, ArmType.classify(mob));
            }
        }

        List<Squad> built = new ArrayList<>();
        for (Map.Entry<String, List<UUID>> entry : byType.entrySet()) {
            List<UUID> members = entry.getValue();
            members.sort(Comparator.comparing(UUID::toString));
            ArmType arm = arms.getOrDefault(entry.getKey(), ArmType.MELEE);
            EntityType<?> type = types.get(entry.getKey());
            for (int i = 0; i < members.size(); i += SQUAD_SIZE) {
                int end = Math.min(members.size(), i + SQUAD_SIZE);
                built.add(new Squad(type, arm, members.subList(i, end)));
            }
        }
        // 近战在前，远程居中，法术与辅助在后；同兵种按注册名稳定排序
        built.sort(Comparator.comparingInt((Squad squad) -> squad.arm().ordinal())
                .thenComparing(squad -> EntityType.getKey(squad.memberType()).toString()));

        if (built.size() > MAX_SQUADS) {
            for (Squad extra : built.subList(MAX_SQUADS, built.size())) {
                for (UUID id : extra.members()) {
                    if (level.getEntity(id) instanceof Mob mob) {
                        FormationData.discharge(mob);
                    }
                    this.roster.remove(id);
                }
            }
            built = new ArrayList<>(built.subList(0, MAX_SQUADS));
        }

        this.squads.clear();
        this.squads.addAll(built);
    }

    // ------------------------------------------------------------------
    // 2.5 自主突击作战
    // ------------------------------------------------------------------

    /**
     * 推进突击作战的阶段机。
     *
     * <p>作战期间阵型、姿态、锚点全部由当前阶段决定，连队长也会自己往前走
     * （见 {@link CaptainEntity#setCommandPost}），所以整套流程不需要玩家在场。
     */
    private void tickAssault(ServerLevel level) {
        if (!this.phase.isActive() || this.objective == null) {
            this.captain.setCommandPost(null);
            return;
        }
        this.phaseTicks += ASSAULT_INTERVAL;

        Vec3 objectiveCenter = Vec3.atCenterOf(this.objective);
        int defenders = this.countDefenders(level, objectiveCenter);
        double distance = this.captain.position().distanceTo(objectiveCenter);

        // 突击/占领阶段伤亡过半 → 突击失败，撤离
        if ((this.phase == AssaultPhase.ASSAULT || this.phase == AssaultPhase.SECURE)
                && this.assaultHeadcount > 0
                && this.roster.size() * 2 < this.assaultHeadcount) {
            this.failAssault();
            return;
        }

        switch (this.phase) {
            case APPROACH -> {
                // 判定距离必须和逼近阶段实际的指挥位距离一致，否则连队长停在站位上
                // 却永远达不到转阶段条件，作战就卡死在第一步。
                double arrive = AssaultPhase.SCOUT.standoff() + CAPTAIN_REAR_OFFSET + ARRIVE_SLACK;
                if (distance <= arrive || this.phaseTicks >= APPROACH_TIMEOUT) {
                    this.enterPhase(AssaultPhase.SCOUT);
                }
            }
            case SCOUT -> {
                this.runScout(level, objectiveCenter);
                if (this.phaseTicks >= SCOUT_TICKS) {
                    // 侦察完毕：有守军且有炮（或支援炮兵队长）就先炮击，否则直接压上去
                    if (defenders > 0 && (this.hasCannons(level) || this.hasSupportCaptains(level))) {
                        this.defendersAtBombardStart = defenders;
                        this.enterPhase(AssaultPhase.BOMBARD);
                    } else {
                        this.enterPhase(AssaultPhase.ASSAULT);
                    }
                }
            }
            case BOMBARD -> {
                boolean suppressed = defenders == 0
                        || defenders * 3 <= this.defendersAtBombardStart;
                if (suppressed || this.phaseTicks >= BOMBARD_TICKS) {
                    this.enterPhase(AssaultPhase.ASSAULT);
                }
            }
            case ASSAULT -> {
                double arrive = AssaultPhase.ASSAULT.standoff() + CAPTAIN_REAR_OFFSET + ARRIVE_SLACK;
                if (defenders == 0 && distance <= arrive) {
                    this.enterPhase(AssaultPhase.SECURE);
                }
            }
            case SECURE -> {
                if (defenders > 0) {
                    // 残敌反扑，退回突击阶段
                    this.enterPhase(AssaultPhase.ASSAULT);
                } else if (this.phaseTicks >= SECURE_TICKS) {
                    this.completeAssault();
                    return;
                }
            }
            default -> {
            }
        }

        this.applyAssaultPosture(level, objectiveCenter);
    }

    /**
     * 作战阶段直接接管阵型、姿态、锚点与朝向。
     *
     * <p>锚点与指挥位都以"<b>目标点出发、朝连队方向</b>"的固定半径来算：
     * <pre>
     * 指挥位 = 目标 + 单位向量(目标→连队长) × (阶段站位 + 退避距离)
     * 锚点   = 目标 + 单位向量(目标→连队长) × 阶段站位
     * </pre>
     * 这样阵型始终夹在连队长与目标之间，而且无论连队长站得太远还是冲得太近，
     * 都会被拉回正确的半径上（炮击阶段因此会自动后退到安全距离）。
     */
    private void applyAssaultPosture(ServerLevel level, Vec3 objectiveCenter) {
        this.applyFormation(this.phase.formation());
        this.stance = this.phase.stance();
        this.pendingStance = null;
        this.pendingFormation = null;

        Vec3 from = this.captain.position();
        Vec3 outward = from.subtract(objectiveCenter);
        double horizontal = outward.horizontalDistance();
        // 连队长正好压在目标上时没有方向可用，退化成用它自身朝向
        Vec3 unit = horizontal < 1.0E-4D
                ? forwardOf(Vec3.ZERO, this.captain.getYRot(), -1.0D)
                : new Vec3(outward.x / horizontal, 0.0D, outward.z / horizontal);

        double standoff = this.phase.standoff();
        // 炮火覆盖区未清空时，把指挥位往后拉，让步兵先撤出覆盖区
        if (this.breachTarget != null && !this.isFireSafe(level, Vec3.atCenterOf(this.breachTarget))) {
            standoff += BREACH_WITHDRAW_OFFSET;
        }
        Vec3 postPos = objectiveCenter.add(unit.scale(standoff + CAPTAIN_REAR_OFFSET));

        // 只把朝向对准目标、把指挥位定在目标方向；不把锚点钉到目标附近，
        // 让锚点跟随连队长推进，队伍才能以线列行进过去，而不是整队传送到目标旁。
        this.facing = FormationLayout.yawTowards(this.captain.position(), objectiveCenter);
        this.pinnedAnchor = null;
        this.captain.setCommandPost(postPos);
        this.layoutTimer = 0;
    }

    private void enterPhase(AssaultPhase next) {
        if (this.phase == next) {
            return;
        }
        this.phase = next;
        this.phaseTicks = 0;
        this.anchor = null;
        this.layoutTimer = 0;
        if (next == AssaultPhase.ASSAULT) {
            // 突击开始时记下总人数，伤亡过半就判失败
            this.assaultHeadcount = this.roster.size();
        }
        if (next != AssaultPhase.SCOUT) {
            this.breachTarget = null;
            this.clearSupportCaptainStrikes();
        }
        this.announce(Component.translatable("requiem.assault.phase", next.getDisplayName()));
    }

    private int countDefenders(ServerLevel level, Vec3 objectiveCenter) {
        AABB box = AABB.ofSize(objectiveCenter,
                OBJECTIVE_RADIUS * 2.0D, OBJECTIVE_RADIUS, OBJECTIVE_RADIUS * 2.0D);
        return level.getEntitiesOfClass(LivingEntity.class, box, this::isThreat).size();
    }

    private boolean hasCannons(ServerLevel level) {
        for (UUID id : this.roster) {
            if (level.getEntity(id) instanceof Mob mob && ArtilleryEmplacement.hasCannon(mob)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 侦察：检查目标是否被墙挡住。
     *
     * <p>每刻从连队长到目标做一次视线射线；被实心方块挡住说明敌人隔墙，
     * 覆盖区清空后直接把爆破点坐标下发给炮兵（自有炮兵与支援队长统一走
     * {@code CompanyStrike}），墙一消失就立即撤回坐标。
     */
    private void runScout(ServerLevel level, Vec3 objectiveCenter) {
        BlockPos breach = this.findBreachPoint(level, this.captain.getEyePosition(), objectiveCenter);
        if (breach == null) {
            // 墙没了：撤回所有定点打击坐标
            this.breachTarget = null;
            this.clearAllStrikes(level);
            return;
        }
        this.breachTarget = breach;
        // 覆盖区还有自己人就不开火，等步兵先撤出（applyAssaultPosture 会把指挥位往后拉）
        if (this.isFireSafe(level, Vec3.atCenterOf(breach))) {
            this.pushStrikeTarget(level, breach);
        }
    }

    /** 支援清单里是否还有活着的炮兵队长。 */
    private boolean hasSupportCaptains(ServerLevel level) {
        for (UUID id : this.supportCaptains) {
            if (level.getEntity(id) instanceof ArtilleryCaptainEntity captain && captain.isAlive()) {
                return true;
            }
        }
        return false;
    }

    /** 清除支援炮兵队长身上的定点打击坐标（离开侦察/结束作战时调用）。 */
    private void clearSupportCaptainStrikes() {
        if (!(this.captain.level() instanceof ServerLevel level)) {
            return;
        }
        for (UUID id : this.supportCaptains) {
            if (level.getEntity(id) instanceof ArtilleryCaptainEntity captain) {
                captain.clearCompanyStrike();
            }
        }
    }

    /** 沿视线找第一堵墙；没有遮挡时返回 {@code null}。 */
    private BlockPos findBreachPoint(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 1.0E-4D) {
            return null;
        }
        Vec3 step = dir.normalize();
        for (double d = 0.5D; d < len; d += 0.5D) {
            BlockPos pos = BlockPos.containing(from.add(step.scale(d)));
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos.immutable();
            }
        }
        return null;
    }

    /**
     * 把爆破点坐标统一下发给自有炮兵与支援炮兵队长。
     *
     * <p>统一走 {@code CompanyStrike}（Java 侧 ForgeData + KubeJS 读 getter），
     * 小队炮兵复用定点炮击流程自行装填重炮弹/火焰弹。
     */
    private void pushStrikeTarget(ServerLevel level, BlockPos breach) {
        for (UUID id : this.roster) {
            if (level.getEntity(id) instanceof ArtilleryEntity artillery) {
                artillery.setCompanyStrike(breach);
            }
        }
        for (UUID id : this.supportCaptains) {
            if (level.getEntity(id) instanceof ArtilleryCaptainEntity captain && captain.isAlive()) {
                captain.setCompanyStrike(breach);
            }
        }
        if (this.phaseTicks % VOLLEY_INTERVAL == 0) {
            this.announce(Component.translatable("requiem.assault.breach",
                    breach.getX(), breach.getY(), breach.getZ()));
        }
    }

    /** 撤回自有炮兵与支援队长的定点打击坐标。 */
    private void clearAllStrikes(ServerLevel level) {
        for (UUID id : this.roster) {
            if (level.getEntity(id) instanceof ArtilleryEntity artillery) {
                artillery.clearCompanyStrike();
            }
        }
        this.clearSupportCaptainStrikes();
    }

    /** 突击伤亡过半，判定失败并撤离。 */
    private void failAssault() {
        this.endAssault();
        this.announce(Component.translatable("requiem.assault.failed"));
    }

    /** 把作战进展汇报给主人。 */
    private void announce(Component message) {
        if (this.captain.getTrueOwner() instanceof Player player) {
            player.displayClientMessage(message, false);
        }
    }

    // ------------------------------------------------------------------
    // 2.7 按敌情给各小队派任务
    // ------------------------------------------------------------------

    /**
     * 连队长逐队下任务。
     *
     * <p>这是"连队"和"一大坨仆从"的区别所在：小队不是阵型里的格子，而是可以单独派出去的单位。
     * 派发规则按兵种对位：
     * <ul>
     *   <li>有炮位的炮兵 → {@link SquadMission#MAN_GUNS}，守炮，不参与阵型。</li>
     *   <li>第一支近战队 → 迎击最近的<b>敌方近战</b>（对面卫道士上来就派自己的卫道士去接）。</li>
     *   <li>第二支近战队 → 去撕最近的<b>敌方远程/施法</b>，不让它们安稳放风筝。</li>
     *   <li>其余近战 → 守阵线。</li>
     *   <li>远程与施法 → {@link SquadMission#SUPPORT_FIRE}，退到本兵种的理想距离射击。</li>
     *   <li>辅助 → 后备。</li>
     * </ul>
     */
    private void assignMissions(ServerLevel level, Vec3 reference) {
        List<LivingEntity> threats = this.nearbyThreats(level, reference, THREAT_RADIUS);
        LivingEntity nearestMelee = null;
        LivingEntity nearestShooter = null;
        double meleeSqr = Double.MAX_VALUE;
        double shooterSqr = Double.MAX_VALUE;

        for (LivingEntity threat : threats) {
            double distSqr = threat.distanceToSqr(reference);
            ArmType arm = threat instanceof Mob enemy ? ArmType.classify(enemy) : ArmType.MELEE;
            if (arm.isRanged()) {
                if (distSqr < shooterSqr) {
                    shooterSqr = distSqr;
                    nearestShooter = threat;
                }
            } else if (distSqr < meleeSqr) {
                meleeSqr = distSqr;
                nearestMelee = threat;
            }
        }

        boolean meleeCounterSent = false;
        boolean shooterCounterSent = false;

        for (int i = 0; i < this.squads.size(); i++) {
            Squad squad = this.squads.get(i);
            SquadOrder order = this.squadOrders.computeIfAbsent(i, key -> new SquadOrder());
            order.targetId = null;

            if (this.squadMansGuns(level, squad)) {
                order.mission = SquadMission.MAN_GUNS;
                continue;
            }

            switch (squad.arm()) {
                case MELEE -> {
                    if (nearestMelee != null && !meleeCounterSent) {
                        order.mission = SquadMission.COUNTER;
                        order.targetId = nearestMelee.getUUID();
                        meleeCounterSent = true;
                    } else if (nearestShooter != null && !shooterCounterSent) {
                        order.mission = SquadMission.COUNTER;
                        order.targetId = nearestShooter.getUUID();
                        shooterCounterSent = true;
                    } else if (!threats.isEmpty()) {
                        order.mission = SquadMission.SCREEN;
                    } else {
                        order.mission = SquadMission.HOLD_LINE;
                    }
                }
                case RANGED, CASTER -> order.mission = threats.isEmpty()
                        ? SquadMission.HOLD_LINE
                        : SquadMission.SUPPORT_FIRE;
                default -> order.mission = SquadMission.RESERVE;
            }
        }
        // 小队数量变少时清掉多余的任务记录
        this.squadOrders.keySet().removeIf(index -> index >= this.squads.size());
    }

    /** 该小队是否有成员守着炮位。 */
    private boolean squadMansGuns(ServerLevel level, Squad squad) {
        if (this.batteryWithdrawn) {
            return false;
        }
        for (UUID id : squad.members()) {
            if (level.getEntity(id) instanceof Mob mob && ArtilleryEmplacement.hasCannon(mob)) {
                return true;
            }
        }
        return false;
    }

    private List<LivingEntity> nearbyThreats(ServerLevel level, Vec3 center, double radius) {
        AABB box = AABB.ofSize(center, radius * 2.0D, radius, radius * 2.0D);
        return level.getEntitiesOfClass(LivingEntity.class, box, this::isThreat);
    }

    /**
     * 算出一支独立任务小队该站哪。
     *
     * <p>统一思路：把小队放在"距相关敌人 {@link ArmType#preferredStandoff()} 格、
     * 且位于我方这一侧"的位置。于是近战贴上去打，远程自动退到射程内的安全距离，
     * 掷弹兵也不会近到炸着自己。
     */
    private void locateSquad(ServerLevel level, int index, Squad squad, Vec3 companyAnchor) {
        SquadOrder order = this.squadOrders.get(index);
        if (order == null) {
            return;
        }
        LivingEntity target = order.targetId != null && level.getEntity(order.targetId) instanceof LivingEntity found
                ? found
                : null;

        switch (order.mission) {
            case COUNTER -> {
                if (target == null || !target.isAlive()) {
                    // 目标没了，回去守阵线
                    order.mission = SquadMission.HOLD_LINE;
                    return;
                }
                Vec3 enemy = target.position();
                order.anchor = standoffFrom(enemy, companyAnchor, squad.arm().preferredStandoff());
                order.facing = FormationLayout.yawTowards(order.anchor, enemy);
            }
            case SUPPORT_FIRE -> {
                LivingEntity main = this.findThreat(level, companyAnchor);
                if (main == null) {
                    order.mission = SquadMission.HOLD_LINE;
                    return;
                }
                Vec3 enemy = main.position();
                order.anchor = standoffFrom(enemy, companyAnchor, squad.arm().preferredStandoff());
                order.facing = FormationLayout.yawTowards(order.anchor, enemy);
            }
            case SCREEN -> {
                order.anchor = forwardOf(companyAnchor, this.facing, 8.0D);
                order.facing = this.facing;
            }
            case RESERVE -> {
                order.anchor = forwardOf(companyAnchor, this.facing, -12.0D);
                order.facing = this.facing;
            }
            default -> {
                order.anchor = null;
                order.facing = this.facing;
            }
        }

        // 不许脱得太远，否则连队会被各自拉散
        if (order.anchor != null && order.anchor.distanceToSqr(companyAnchor) > DETACH_LIMIT * DETACH_LIMIT) {
            order.anchor = standoffFrom(companyAnchor, order.anchor, -DETACH_LIMIT);
        }
    }

    /** 从 {@code enemy} 朝 {@code friendly} 方向退开 {@code distance} 格。 */
    private static Vec3 standoffFrom(Vec3 enemy, Vec3 friendly, double distance) {
        Vec3 delta = friendly.subtract(enemy);
        double horizontal = delta.horizontalDistance();
        if (horizontal < 1.0E-4D) {
            return friendly;
        }
        return enemy.add(delta.x / horizontal * distance, 0.0D, delta.z / horizontal * distance);
    }

    // ------------------------------------------------------------------
    // 3. 评估姿态
    // ------------------------------------------------------------------

    private void updateStance(ServerLevel level) {
        Vec3 reference = this.anchor != null ? this.anchor : this.captain.position();
        this.assignMissions(level, reference);
        if (this.phase.isActive()) {
            // 作战期间由阶段机全权决定阵型与姿态
            return;
        }
        LivingEntity threat = this.findThreat(level, reference);

        if (this.autoFormation) {
            this.autoSelectFormation(level, reference, threat);
        }

        if (!this.isStanceLocked()) {
            CompanyStance candidate = this.decideStance(level, reference, threat);
            // 迟滞：同一结论连续出现两次才切换，防止在临界距离上反复抖动
            if (candidate == this.stance) {
                this.pendingStance = null;
            } else if (candidate == this.pendingStance) {
                this.stance = candidate;
                this.pendingStance = null;
                this.layoutTimer = 0;
            } else {
                this.pendingStance = candidate;
            }
        }

        float desiredFacing = threat != null
                ? FormationLayout.yawTowards(this.captain.position(), threat.position())
                : this.captain.getYRot();
        if (Math.abs(Mth.wrapDegrees(desiredFacing - this.facing)) > FACING_HYSTERESIS) {
            this.facing = desiredFacing;
            this.layoutTimer = 0;
        }
    }

    /**
     * 连队长根据手下的兵种构成与当前敌情，自行决定该列什么阵。
     *
     * <p>同样带两次确认的迟滞，否则敌人在临界距离上来回走动会让阵型反复重排，永远站不定。
     */
    private void autoSelectFormation(ServerLevel level, Vec3 reference, LivingEntity threat) {
        FormationType candidate = this.decideFormation(level, reference, threat);
        if (candidate == this.formation) {
            this.pendingFormation = null;
        } else if (candidate == this.pendingFormation) {
            this.applyFormation(candidate);
            this.pendingFormation = null;
        } else {
            this.pendingFormation = candidate;
        }
    }

    private FormationType decideFormation(ServerLevel level, Vec3 reference, LivingEntity threat) {
        int melee = 0;
        int ranged = 0;
        int total = 0;
        for (Squad squad : this.squads) {
            total += squad.size();
            if (squad.arm().isMelee()) {
                melee += squad.size();
            } else if (squad.arm() == ArmType.RANGED) {
                ranged += squad.size();
            }
        }
        if (total == 0) {
            return FormationType.LINE;
        }

        // 有炮位就架炮列，就地火力压制
        if (ranged * 2 >= total && this.hasCannons(level)) {
            return FormationType.GUN_LINE;
        }
        // 远程步兵为主但没有炮：排成线列轮番射击
        if (ranged * 2 >= total && this.countRangedSquads() >= 2) {
            return FormationType.FIRING_LINE;
        }
        // 被多个敌人围住：空心方阵四面朝外
        if (this.countNearbyThreats(level, reference) >= 4) {
            return FormationType.HOLLOW_SQUARE;
        }
        if (threat == null) {
            return FormationType.LINE;
        }
        double distance = Math.sqrt(threat.distanceToSqr(reference));
        if (distance > ADVANCE_RANGE) {
            // 敌人还远：收成纵队行军，过地形更顺
            return FormationType.COLUMN;
        }
        if (melee * 2 >= total && distance <= CHARGE_RANGE * 1.5D) {
            // 近战为主且已经接敌：楔形突破
            return FormationType.WEDGE;
        }
        return FormationType.LINE;
    }

    /** 有几支远程（射击）小队——至少两支才排得出轮番射击的线列；施法/辅助不算在内。 */
    private int countRangedSquads() {
        int count = 0;
        for (Squad squad : this.squads) {
            if (squad.arm() == ArmType.RANGED) {
                count++;
            }
        }
        return count;
    }

    /**
     * 线列最前一层是否已全部装填完毕。
     *
     * <p>掠夺者（弩手）开完一轮后要重新上弦，这里检查第 0 层里还有没有人在装填；
     * 全部装填完成才轮换，把掷弹兵顶到前面。掷弹兵本身不是弩手，恒视为已就绪。
     */
    private boolean frontRankReady(ServerLevel level) {
        if (this.lastVolleyRank.isEmpty()) {
            return true;
        }
        for (Map.Entry<Integer, Integer> entry : this.lastVolleyRank.entrySet()) {
            if (entry.getValue() != 0) {
                continue;
            }
            int squadIndex = entry.getKey();
            if (squadIndex < 0 || squadIndex >= this.squads.size()) {
                continue;
            }
            for (UUID id : this.squads.get(squadIndex).members()) {
                if (level.getEntity(id) instanceof Mob mob
                        && mob instanceof IOwned owned
                        && owned.isChargingCrossbow()) {
                    return false;
                }
            }
        }
        return true;
    }

    private int countNearbyThreats(ServerLevel level, Vec3 center) {
        double radius = CHARGE_RANGE * 1.5D;
        AABB box = AABB.ofSize(center, radius * 2.0D, radius, radius * 2.0D);
        return level.getEntitiesOfClass(LivingEntity.class, box, this::isThreat).size();
    }

    /** 线列三层架构的层号：掷弹兵(0) → 征服者(1) → 掠夺者(2)，其余远程默认 0。 */
    private static int firingLineRank(EntityType<?> type) {
        if (type == ModEntities.GRENADIER.get()) {
            return 0;
        }
        if (type == ModEntities.CONQUEROR.get()) {
            return 1;
        }
        ResourceLocation key = EntityType.getKey(type);
        if (key != null && "pillager_servant".equals(key.getPath())) {
            return 2;
        }
        return 0;
    }

    private CompanyStance decideStance(ServerLevel level, Vec3 reference, LivingEntity threat) {
        if (this.isScattered(level, reference)) {
            return CompanyStance.RALLY;
        }
        if (threat == null) {
            return CompanyStance.FORM_UP;
        }
        // 线列（轮番射击的固定火线）：守住不压
        if (this.formation == FormationType.FIRING_LINE) {
            return CompanyStance.HOLD;
        }
        // 炮列：炮兵钉死在炮位，线列在敌人靠近时保持阵型有序冲锋（ADVANCE = 整线推进 + 原地射击）
        if (this.formation == FormationType.GUN_LINE) {
            double distance = Math.sqrt(threat.distanceToSqr(reference));
            return distance <= CHARGE_RANGE ? CompanyStance.ADVANCE : CompanyStance.HOLD;
        }
        double distance = Math.sqrt(threat.distanceToSqr(reference));
        if (distance > ADVANCE_RANGE) {
            return CompanyStance.ADVANCE;
        }
        if (distance > CHARGE_RANGE) {
            return CompanyStance.HOLD;
        }
        return this.isMeleeHeavy() ? CompanyStance.CHARGE : CompanyStance.ENGAGE;
    }

    private boolean isScattered(ServerLevel level, Vec3 reference) {
        if (this.roster.size() < 2) {
            return false;
        }
        int far = 0;
        int counted = 0;
        for (UUID id : this.roster) {
            if (level.getEntity(id) instanceof Mob mob) {
                counted++;
                if (mob.distanceToSqr(reference) > RALLY_TRIGGER_SQR) {
                    far++;
                }
            }
        }
        return counted > 0 && far * 2 > counted;
    }

    private boolean isMeleeHeavy() {
        int melee = 0;
        int total = 0;
        for (Squad squad : this.squads) {
            total += squad.size();
            if (squad.arm().isMelee()) {
                melee += squad.size();
            }
        }
        return total > 0 && melee * 2 >= total;
    }

    private LivingEntity findThreat(ServerLevel level, Vec3 center) {
        AABB box = AABB.ofSize(center, THREAT_RADIUS * 2.0D, THREAT_RADIUS, THREAT_RADIUS * 2.0D);
        LivingEntity best = null;
        double bestSqr = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box, this::isThreat)) {
            double distSqr = candidate.distanceToSqr(center);
            if (distSqr < bestSqr) {
                bestSqr = distSqr;
                best = candidate;
            }
        }
        return best;
    }

    private boolean isThreat(LivingEntity entity) {
        if (!entity.isAlive() || entity == this.captain) {
            return false;
        }
        if (entity instanceof IOwned owned && Objects.equals(owned.getOwnerId(), this.captain.getOwnerId())) {
            return false;
        }
        LivingEntity owner = this.captain.getTrueOwner();
        if (owner != null && entity == owner) {
            return false;
        }
        if (this.captain.isHostile()) {
            return entity instanceof Player player && !player.isCreative() && !player.isSpectator();
        }
        return entity instanceof Enemy;
    }

    // ------------------------------------------------------------------
    // 4. 下发站位令
    // ------------------------------------------------------------------

    private void pushOrders(ServerLevel level) {
        if (this.stance.releasesFormation()) {
            // 冲锋：收回站位令让部队散开，但保留入编关系，打完能原地重新列阵
            this.recallOrders(level);
            return;
        }
        if (this.squads.isEmpty()) {
            return;
        }

        // 先把"已就位的火炮"挑出来。它们由 KubeJS 炮兵脚本自己走到炮位、自己操炮，
        // 连队如果再给它们派站位令，两边会抢寻路把炮兵拉得来回抽搐。
        // 所以这类单位不进阵型解算，连队只负责给它们指定打击目标。
        // 只负责分拣：架好炮的单位不参与阵型解算（它们的状态由 maintainArtillery 维护）。
        // 撤下来的炮组当成普通成员，跟着连队机动。
        List<List<Mob>> mobileBySquad = new ArrayList<>(this.squads.size());
        List<ArmType> layoutArms = new ArrayList<>(this.squads.size());
        List<BlockPos> battery = new ArrayList<>();
        for (Squad squad : this.squads) {
            List<Mob> mobile = new ArrayList<>(squad.size());
            boolean withdrawnCrew = false;
            for (UUID id : squad.members()) {
                if (!(level.getEntity(id) instanceof Mob mob)) {
                    continue;
                }
                BlockPos cannon = ArtilleryEmplacement.getCannonPos(mob);
                if (cannon == null) {
                    mobile.add(mob);
                } else if (this.batteryWithdrawn) {
                    mobile.add(mob);
                    withdrawnCrew = true;
                } else {
                    battery.add(cannon);
                }
            }
            mobileBySquad.add(mobile);
            // 撤下来的炮组按辅助兵种排位，于是会被摆到最后排而不是顶在火线上
            layoutArms.add(withdrawnCrew ? ArmType.SUPPORT : squad.arm());
        }

        // 按任务分流：只有"守阵线"的小队进主阵型解算，
        // 其余小队各自在自己的落脚点独立列队。
        List<Integer> lineSquads = new ArrayList<>(this.squads.size());
        List<Integer> detachedSquads = new ArrayList<>(this.squads.size());
        for (int i = 0; i < this.squads.size(); i++) {
            if (mobileBySquad.get(i).isEmpty()) {
                continue;
            }
            SquadOrder order = this.squadOrders.get(i);
            SquadMission mission = order != null ? order.mission : SquadMission.HOLD_LINE;
            if (mission.skipsFormation()) {
                continue;
            }
            if (mission.usesCompanyFormation()) {
                lineSquads.add(i);
            } else {
                detachedSquads.add(i);
            }
        }

        // 线列：给每支远程小队排一个层号。层 0 在最前、获准开火。
        Map<Integer, Integer> volleyRank = new HashMap<>();
        if (this.formation.usesVolleyRanks()) {
            List<Integer> shooters = new ArrayList<>();
            for (int squadIndex : lineSquads) {
                if (layoutArms.get(squadIndex) == ArmType.RANGED) {
                    shooters.add(squadIndex);
                }
            }
            if (!shooters.isEmpty()) {
                if (this.stance == CompanyStance.FORM_UP) {
                    // 整队：固定三层架构（掷弹兵→征服者→掠夺者），不轮换
                    for (int squadIndex : shooters) {
                        volleyRank.put(squadIndex, firingLineRank(this.squads.get(squadIndex).memberType()));
                    }
                } else {
                    // 其余姿态：按节奏轮换各层的前后次序
                    for (int i = 0; i < shooters.size(); i++) {
                        volleyRank.put(shooters.get(i), (i + this.volleyPhase) % shooters.size());
                    }
                }
            }
        }
        this.lastVolleyRank.clear();
        this.lastVolleyRank.putAll(volleyRank);

        FormationLayout.SquadShape[] shapes = new FormationLayout.SquadShape[lineSquads.size()];
        for (int i = 0; i < lineSquads.size(); i++) {
            int squadIndex = lineSquads.get(i);
            ArmType arm = layoutArms.get(squadIndex);
            int rank = volleyRank.getOrDefault(squadIndex, arm.rankOffset());
            shapes[i] = new FormationLayout.SquadShape(mobileBySquad.get(squadIndex).size(), rank, arm);
        }
        List<List<FormationLayout.LocalSlot>> plan = FormationLayout.layout(this.formation, shapes);

        Vec3 desiredAnchor;
        if (this.pinnedAnchor != null) {
            desiredAnchor = this.pinnedAnchor;
            this.facing = this.pinnedFacing;
        } else if (!battery.isEmpty()) {
            // 有炮台：把阵型锚在炮群中心。炮列里掩护小队站在锚点前方，
            // 于是近战自然挡在火炮前面，正好护住炮兵。
            desiredAnchor = centroid(battery);
        } else {
            // 锚点放在连队长正前方，距离 = 阵型纵深 + 退避余量，于是连队长留在最后一排之后
            double standoff = Math.abs(FormationLayout.rearmostZ(plan)) + CAPTAIN_STANDOFF;
            desiredAnchor = forwardOf(this.captain.position(), this.facing, standoff);
        }
        // 迟滞：连队长小幅走动不重排阵型，这样队伍才能真正"站定"。
        // 前进与集结时取消迟滞，让阵型紧跟连队长移动。
        boolean tracking = this.stance.anchorFollows();
        if (this.anchor == null || tracking
                || this.anchor.distanceToSqr(desiredAnchor) > ANCHOR_HYSTERESIS * ANCHOR_HYSTERESIS) {
            this.anchor = desiredAnchor;
        }

        long expire = level.getGameTime() + SLOT_LEASE_TICKS;

        // 主阵型：守阵线的小队
        for (int i = 0; i < lineSquads.size() && i < plan.size(); i++) {
            int squadIndex = lineSquads.get(i);
            ArmType arm = layoutArms.get(squadIndex);
            // 轮番射击：线列里只有第 0 层获准开火，其余层压住装填。
            // 非线列阵型下，所有远程单位都允许开火。
            boolean mayFire = arm.isRanged()
                    && (!this.formation.usesVolleyRanks() || volleyRank.getOrDefault(squadIndex, 0) == 0);
            this.issueSlots(mobileBySquad.get(squadIndex), plan.get(i), squadIndex,
                    this.anchor, this.facing, this.stance,
                    this.formation.anchors(arm), mayFire, expire);
        }

        // 独立任务：每队单独解算一次，摆在自己的落脚点上
        for (int squadIndex : detachedSquads) {
            Squad squad = this.squads.get(squadIndex);
            this.locateSquad(level, squadIndex, squad, this.anchor);
            SquadOrder order = this.squadOrders.get(squadIndex);
            if (order == null || order.anchor == null) {
                continue;
            }
            List<Mob> mobile = mobileBySquad.get(squadIndex);
            ArmType arm = layoutArms.get(squadIndex);
            FormationType shape = order.mission.shape();
            List<List<FormationLayout.LocalSlot>> subPlan = FormationLayout.layout(shape,
                    new FormationLayout.SquadShape[]{
                            new FormationLayout.SquadShape(mobile.size(), 0, arm)
                    });
            if (subPlan.isEmpty()) {
                continue;
            }
            // 独立任务的小队不受轮番射击约束：派出去就是要它打
            this.issueSlots(mobile, subPlan.get(0), squadIndex,
                    order.anchor, order.facing, order.mission.stance(this.stance),
                    false, arm.isRanged(), expire);
        }
    }

    private void issueSlots(List<Mob> members, List<FormationLayout.LocalSlot> slots, int squadIndex,
                            Vec3 anchor, float facing, CompanyStance stance,
                            boolean anchored, boolean mayFire, long expire) {
        for (int slotIndex = 0; slotIndex < members.size() && slotIndex < slots.size(); slotIndex++) {
            Mob mob = members.get(slotIndex);
            FormationLayout.LocalSlot local = slots.get(slotIndex);
            FormationData.enlist(mob, this.captain.getUUID(), false);
            FormationData.order(mob, squadIndex, slotIndex,
                    FormationLayout.toWorld(anchor, facing, local),
                    FormationLayout.toWorldYaw(facing, local),
                    stance, this.formation, anchored, mayFire, expire);
        }
    }

    /**
     * 维护炮兵状态。<b>每 {@value #ARTILLERY_INTERVAL} 刻无条件执行一次</b>，
     * 不受阵型、姿态、作战阶段影响。
     *
     * <p>这一步独立出来是必须的：整合包里<b>全部</b>脚本仆从（学徒、园丁、征兵官、炮兵）
     * 的 AI 开头都是 {@code if (!entity.isGuardingArea()) return;}，
     * 而 goety 的 {@code setFollowing()} 会把 {@code boundPos} 置空使其不成立。
     * 只要有任何一条代码路径让炮兵停在"跟随"，它就永远不再开火。
     * 所以这里每次都把炮兵的驻守点重新绑回它自己的炮位。
     */
    private void maintainArtillery(ServerLevel level) {
        this.assignCannons(level);

        List<BlockPos> guns = new ArrayList<>();
        for (UUID id : this.roster) {
            if (!(level.getEntity(id) instanceof Mob mob)) {
                continue;
            }
            BlockPos cannon = ArtilleryEmplacement.getCannonPos(mob);
            if (cannon == null) {
                continue;
            }
            guns.add(cannon);
        }
        this.batteryCenter = guns.isEmpty() ? null : centroid(guns);
        this.updateBatteryThreat(level);

        for (UUID id : this.roster) {
            if (!(level.getEntity(id) instanceof Mob mob)) {
                continue;
            }
            if (ArtilleryEmplacement.getCannonPos(mob) == null) {
                continue;
            }
            if (this.batteryWithdrawn) {
                // 炮阵失守：放开驻守绑定，炮组才能跟着连队撤走
                ArtilleryEmplacement.withdraw(mob);
                FormationData.enlist(mob, this.captain.getUUID(), false);
                continue;
            }
            // 架炮：驻守点绑回炮位，既满足脚本的开火条件，
            // 又让 goety 的"返回驻守点"把炮兵拉向炮位而不是别处
            FormationData.enlist(mob, this.captain.getUUID(), true);
            ArtilleryEmplacement.emplace(mob);
            this.orderToCannon(level, mob);
        }
    }

    /**
     * 给炮组下一条"钉死在炮位旁"的站位令。
     *
     * <p>这一步是必须的：脚本只有在<b>已经发现目标</b>之后才会把炮兵挪向炮位
     * （见 {@link ArtilleryEmplacement#findCrewStand}），所以平时没人把炮兵送上炮位，
     * 炮列摆出来了却没人操炮。改由连队直接把炮组定位到炮位旁，
     * 目标点与脚本的 {@code moveTo(cannon)} 一致，不会互相拉扯。
     */
    private void orderToCannon(ServerLevel level, Mob mob) {
        BlockPos cannon = ArtilleryEmplacement.getCannonPos(mob);
        if (cannon == null) {
            return;
        }
        Vec3 stand = ArtilleryEmplacement.findCrewStand(level, cannon);
        // 面朝自己的炮，看起来像在操炮
        float yaw = FormationLayout.yawTowards(stand, Vec3.atCenterOf(cannon));
        FormationData.order(mob, -1, -1, stand, yaw,
                CompanyStance.HOLD, this.formation, true, true,
                level.getGameTime() + SLOT_LEASE_TICKS);
    }

    /**
     * 汇总已知炮位，并把没有炮位的炮兵分配到空着的炮上。
     *
     * <p>炮位来源有两个：炮兵身上已有的绑定（玩家用传送石设的），以及周期性的方块扫描
     * （每 {@value #CANNON_SCAN_INTERVAL} 刻在连队长周围找一次 Supplementaries 大炮）。
     * 这样玩家只要把炮摆好、把炮兵交给连队长，连队长就会自己把人铺到炮位上，
     * 不必一个个手动绑定。
     */
    private void assignCannons(ServerLevel level) {
        Map<BlockPos, Integer> manned = new HashMap<>();
        List<Mob> unassigned = new ArrayList<>();

        for (UUID id : this.roster) {
            if (!(level.getEntity(id) instanceof Mob mob) || ArmType.classify(mob) != ArmType.RANGED) {
                continue;
            }
            if (!(mob instanceof ArtilleryEntity)) {
                // 只有炮兵会操炮，掷弹兵之类的远程不参与分配
                continue;
            }
            BlockPos cannon = ArtilleryEmplacement.getCannonPos(mob);
            if (cannon == null) {
                unassigned.add(mob);
            } else {
                this.knownCannons.add(cannon);
                manned.merge(cannon, 1, Integer::sum);
            }
        }

        if (--this.cannonScanTimer <= 0) {
            this.cannonScanTimer = CANNON_SCAN_INTERVAL;
            this.scanForCannons(level);
        }
        // 炮位被拆掉之后要从已知列表里剔除，否则会一直往空地上派人
        this.knownCannons.removeIf(pos -> level.isLoaded(pos) && !ArtilleryEmplacement.isCannon(level, pos));
        if (unassigned.isEmpty()) {
            return;
        }

        for (BlockPos cannon : this.knownCannons) {
            if (unassigned.isEmpty()) {
                break;
            }
            if (manned.getOrDefault(cannon, 0) > 0) {
                continue;
            }
            // 把最近的闲置炮兵派到这门炮
            Mob nearest = null;
            double bestSqr = Double.MAX_VALUE;
            for (Mob candidate : unassigned) {
                double distSqr = candidate.distanceToSqr(Vec3.atCenterOf(cannon));
                if (distSqr < bestSqr) {
                    bestSqr = distSqr;
                    nearest = candidate;
                }
            }
            if (nearest != null) {
                ArtilleryEmplacement.assignCannon(nearest, cannon);
                manned.put(cannon, 1);
                unassigned.remove(nearest);
            }
        }
    }

    private void scanForCannons(ServerLevel level) {
        BlockPos origin = this.captain.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -CANNON_SCAN_HEIGHT; dy <= CANNON_SCAN_HEIGHT; dy++) {
            for (int dx = -CANNON_SCAN_RADIUS; dx <= CANNON_SCAN_RADIUS; dx++) {
                for (int dz = -CANNON_SCAN_RADIUS; dz <= CANNON_SCAN_RADIUS; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (ArtilleryEmplacement.isCannon(level, cursor)) {
                        this.knownCannons.add(cursor.immutable());
                    }
                }
            }
        }
    }

    /**
     * 判断炮阵是否正被近身攻击，决定撤炮还是复位。
     *
     * <p>带迟滞：{@value #BATTERY_DANGER} 格内有敌人就撤炮，
     * 要退到 {@value #BATTERY_SAFE} 格之外才重新架炮，避免在边界上反复撤回。
     */
    private void updateBatteryThreat(ServerLevel level) {
        if (this.batteryCenter == null) {
            this.batteryWithdrawn = false;
            return;
        }
        double trigger = this.batteryWithdrawn ? BATTERY_SAFE_SQR : BATTERY_DANGER_SQR;
        double radius = this.batteryWithdrawn ? BATTERY_SAFE : BATTERY_DANGER;
        AABB box = AABB.ofSize(this.batteryCenter, radius * 2.0D, radius * 2.0D, radius * 2.0D);

        boolean threatened = false;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box, this::isThreat)) {
            if (candidate.distanceToSqr(this.batteryCenter) <= trigger) {
                threatened = true;
                break;
            }
        }
        if (threatened != this.batteryWithdrawn) {
            this.batteryWithdrawn = threatened;
            this.anchor = null;
            this.announce(Component.translatable(threatened
                    ? "requiem.company.battery_withdraw"
                    : "requiem.company.battery_restored"));
        }
    }

    private static Vec3 centroid(List<BlockPos> positions) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (BlockPos pos : positions) {
            x += pos.getX() + 0.5D;
            y += pos.getY();
            z += pos.getZ() + 0.5D;
        }
        int count = positions.size();
        return new Vec3(x / count, y / count, z / count);
    }

    /**
     * 收回所有站位令（保留入编关系）。
     *
     * <p>炮组是例外：它们的站位令指向自己的炮位，是"架炮"而不是"列阵"，
     * 连冲锋也不该把炮兵从炮位上叫走。
     */
    private void recallOrders(ServerLevel level) {
        for (UUID id : this.roster) {
            if (!(level.getEntity(id) instanceof Mob mob)) {
                continue;
            }
            if (!this.batteryWithdrawn && ArtilleryEmplacement.hasCannon(mob)) {
                continue;
            }
            FormationData.clearOrder(mob);
        }
    }

    // ------------------------------------------------------------------
    // 5. 指挥齐射
    // ------------------------------------------------------------------

    /**
     * 连队长统一指定开火目标。
     *
     * <p>这是跟 KubeJS 炮兵脚本对接的地方。{@code findArtilleryTarget} 的第一步就是
     * <pre>
     * if (entity.isPrioritizing()) { let pt = entity.getPriorityTarget(); if (pt alive) return pt }
     * </pre>
     * 也就是脚本本来就优先采用"优先目标"。所以连队长只要通过 goety 的
     * {@link IServant#setPriorityTarget} 把目标压过去，整条火线就会转火——
     * 不需要本附属自己去操作大炮方块。
     *
     * <p>优先目标时长（{@value #VOLLEY_PRIORITY_TICKS} 刻）大于下令间隔
     * （{@value #VOLLEY_INTERVAL} 刻），所以只要威胁还在，火力指示就不会断档。
     */
    private void directVolley(ServerLevel level) {
        if (this.squads.isEmpty()) {
            return;
        }
        Vec3 reference = this.anchor != null ? this.anchor : this.captain.position();
        // 面杀伤优先：敌人成群时直接给坐标，让大炮用炮弹犁一片；
        // 零散单位用"优先目标"点杀，因为坐标不会跟着目标移动。
        BlockPos barrage = this.pickBarrageTarget(level, reference);
        LivingEntity target = this.findThreat(level, reference);

        // 误伤检查：弹着点附近有自己人就不能打。炮弹与炸弹都是范围伤害，
        // 前排近战一旦顶到敌人跟前，再按原目标开火就是自己犁自己。
        if (barrage != null && !this.isFireSafe(level, Vec3.atCenterOf(barrage))) {
            barrage = null;
        }
        if (target != null && !this.isFireSafe(level, target.position())) {
            target = null;
        }
        if (barrage == null && target == null) {
            this.ceaseFire(level);
            return;
        }
        if (this.holdingFire) {
            this.holdingFire = false;
            this.announce(Component.translatable("requiem.company.resume_fire"));
        }
        boolean firebase = this.formation.isFirebase();

        for (Squad squad : this.squads) {
            if (!squad.arm().isRanged()) {
                continue;
            }
            for (UUID id : squad.members()) {
                if (!(level.getEntity(id) instanceof Mob mob) || !(mob instanceof IServant servant)) {
                    continue;
                }
                if (!firebase || target == null) {
                    continue;
                }
                servant.setPriorityTarget(target);
                servant.setPriorityTime(VOLLEY_PRIORITY_TICKS);
                servant.overrideSetTarget(target);
            }
        }

        // 连队只管步兵：炮火支援交给支援清单里的炮兵队长（吹号开火）
        if (barrage != null || target != null) {
            this.requestArtillerySupport(level);
        }
    }

    /**
     * 弹着点附近是否没有自己人。
     *
     * <p>"自己人"包含同主人的仆从、主人本人，以及连队长自己。
     * 炮兵脚本在<b>索敌</b>时会跳过有主人的实体，但范围伤害不认这个——
     * 所以必须由连队长在下令之前自己把关。
     */
    private boolean isFireSafe(ServerLevel level, Vec3 impact) {
        AABB box = AABB.ofSize(impact,
                FRIENDLY_SAFETY_RADIUS * 2.0D, FRIENDLY_SAFETY_RADIUS * 2.0D, FRIENDLY_SAFETY_RADIUS * 2.0D);
        double safeSqr = FRIENDLY_SAFETY_RADIUS * FRIENDLY_SAFETY_RADIUS;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, this::isFriendly)) {
            if (nearby.distanceToSqr(impact) <= safeSqr) {
                return false;
            }
        }
        return true;
    }

    private boolean isFriendly(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (entity == this.captain) {
            return true;
        }
        LivingEntity owner = this.captain.getTrueOwner();
        if (owner != null && entity == owner) {
            return true;
        }
        return entity instanceof IOwned owned
                && Objects.equals(owned.getOwnerId(), this.captain.getOwnerId());
    }

    /**
     * 下令停火：撤回所有炮火任务与优先目标。
     *
     * <p>炮兵随后会回落到脚本自己的索敌逻辑（它至少会跳过有主人的实体），
     * 而连队这一侧不再主动把炮口指向危险的方向。
     */
    private void ceaseFire(ServerLevel level) {
        for (UUID id : this.roster) {
            if (!(level.getEntity(id) instanceof Mob mob)) {
                continue;
            }
            if (ArtilleryEmplacement.hasCannon(mob)) {
                ArtilleryEmplacement.clearCaptainStrike(mob);
            }
            if (mob instanceof IServant servant) {
                servant.setPriorityTime(0);
            }
        }
        if (!this.holdingFire) {
            this.holdingFire = true;
            this.announce(Component.translatable("requiem.company.cease_fire"));
        }
    }

    /**
     * 挑一个值得用炮火覆盖的弹着点。
     *
     * <p>做法是找"周围敌人最多"的那一个，把它脚下作为弹着点；
     * 只有聚集数量达到 {@value #CLUSTER_THRESHOLD} 才认为值得面杀伤。
     * 突击作战的炮击阶段会退一步直接轰目标点，保证哪怕守军散开也在持续压制。
     */
    private BlockPos pickBarrageTarget(ServerLevel level, Vec3 origin) {
        AABB box = AABB.ofSize(origin, CANNON_RANGE * 2.0D, CANNON_RANGE, CANNON_RANGE * 2.0D);
        List<LivingEntity> threats = level.getEntitiesOfClass(LivingEntity.class, box, this::isThreat);
        if (threats.size() > CLUSTER_SCAN_CAP) {
            threats = threats.subList(0, CLUSTER_SCAN_CAP);
        }

        LivingEntity best = null;
        int bestCount = 0;
        for (LivingEntity candidate : threats) {
            int count = 0;
            for (LivingEntity other : threats) {
                if (candidate.distanceToSqr(other) <= CLUSTER_RADIUS_SQR) {
                    count++;
                }
            }
            if (count < CLUSTER_THRESHOLD || count <= bestCount) {
                continue;
            }
            // 在这里就把会误伤的弹着点排掉：否则可能选中最大的一团、
            // 却因为自己人贴着它而整连停火，白放过旁边一团安全的目标。
            if (!this.isFireSafe(level, candidate.position())) {
                continue;
            }
            bestCount = count;
            best = candidate;
        }
        if (best != null) {
            return best.blockPosition();
        }
        // 炮击阶段即使敌人散开也要压制目标点
        if (this.phase.wantsBombardment() && this.objective != null
                && this.isFireSafe(level, Vec3.atCenterOf(this.objective))) {
            return this.objective;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 对外指令
    // ------------------------------------------------------------------

    public FormationType getFormation() {
        return this.formation;
    }

    /** 玩家手动指定阵型，同时关闭自动选择。 */
    public void setFormation(FormationType formation) {
        this.autoFormation = false;
        this.pendingFormation = null;
        this.applyFormation(formation);
    }

    private void applyFormation(FormationType formation) {
        if (this.formation == formation) {
            return;
        }
        this.formation = formation;
        // 换阵型必然要重排，顺手清掉锚点迟滞，让新阵型立即生效
        this.anchor = null;
        this.layoutTimer = 0;
    }

    /** 连队长是否在自行选择阵型。 */
    public boolean isAutoFormation() {
        return this.autoFormation;
    }

    /**
     * 连旗的阵型切换：自动 → 横列 → 纵队 → … → 炮列 → 自动。
     *
     * @return 切换后是否回到自动模式
     */
    public boolean cycleFormationMode() {
        if (this.autoFormation) {
            this.setFormation(FormationType.LINE);
            return false;
        }
        FormationType next = this.formation.next();
        if (next == FormationType.LINE) {
            // 绕回起点，把指挥权交还给连队长
            this.autoFormation = true;
            this.pendingFormation = null;
            this.stanceTimer = 0;
            return true;
        }
        this.setFormation(next);
        return false;
    }

    public CompanyStance getStance() {
        return this.stance;
    }

    /** 手动指定姿态并锁定一段时间，期间不自动评估。 */
    public void lockStance(CompanyStance stance, int ticks) {
        this.stance = stance;
        this.pendingStance = null;
        this.stanceLockTicks = ticks;
        this.layoutTimer = 0;
    }

    public boolean isStanceLocked() {
        return this.stanceLockTicks > 0;
    }

    /** 把集结点钉在指定位置与朝向。 */
    public void pinAnchor(Vec3 pos, float facing) {
        this.pinnedAnchor = pos;
        this.pinnedFacing = facing;
        this.anchor = pos;
        this.facing = facing;
        this.layoutTimer = 0;
    }

    /** 取消钉住，锚点恢复以连队长为基准。 */
    public void unpinAnchor() {
        this.pinnedAnchor = null;
        this.anchor = null;
        this.stanceTimer = 0;
        this.layoutTimer = 0;
    }

    public boolean isPinned() {
        return this.pinnedAnchor != null;
    }

    public Vec3 getAnchor() {
        return this.anchor;
    }

    public float getFacing() {
        return this.facing;
    }

    public int getMemberCount() {
        return this.roster.size();
    }

    public int getSquadCount() {
        return this.squads.size();
    }

    public List<Squad> getSquads() {
        return List.copyOf(this.squads);
    }

    // ------------------------------------------------------------------
    // 炮兵支援清单
    // ------------------------------------------------------------------

    /** 把炮兵队长加入支援清单。 */
    public boolean addSupportCaptain(ArtilleryCaptainEntity captain) {
        if (captain == null || this.supportCaptains.contains(captain.getUUID())) {
            return false;
        }
        this.supportCaptains.add(captain.getUUID());
        return true;
    }

    /** 把炮兵队长移出支援清单。 */
    public boolean removeSupportCaptain(ArtilleryCaptainEntity captain) {
        return captain != null && this.supportCaptains.remove(captain.getUUID());
    }

    public boolean hasSupportCaptain(ArtilleryCaptainEntity captain) {
        return captain != null && this.supportCaptains.contains(captain.getUUID());
    }

    // ------------------------------------------------------------------
    // 鼓手槽
    // ------------------------------------------------------------------

    /** 绑定鼓手到连队长的鼓手槽。 */
    public void setDrummer(DrummerEntity drummer) {
        this.drummerId = drummer == null ? null : drummer.getUUID();
    }

    /** 当前鼓手槽里的鼓手（服务端解析）；没有或已阵亡返回 {@code null}。 */
    public DrummerEntity getDrummer(ServerLevel level) {
        return this.drummerId != null && level.getEntity(this.drummerId) instanceof DrummerEntity drummer && drummer.isAlive()
                ? drummer
                : null;
    }

    public boolean hasDrummer() {
        return this.drummerId != null;
    }

    /**
     * 鼓手按连队姿态发放增益：冲锋给迅捷+抗性，坚守给生命恢复+伤害吸收。
     *
     * <p>鼓手必须还活着且离连队长足够近；增益发给全部在编成员（不含鼓手自己）。
     */
    private void applyDrummerBuffs(ServerLevel level) {
        DrummerEntity drummer = this.getDrummer(level);
        if (drummer == null || this.roster.isEmpty()) {
            return;
        }
        if (drummer.distanceToSqr(this.captain) > DRUMMER_RANGE_SQR) {
            return;
        }
        MobEffect first;
        MobEffect second;
        switch (this.stance) {
            case CHARGE -> {
                first = MobEffects.MOVEMENT_SPEED;
                second = MobEffects.DAMAGE_RESISTANCE;
            }
            case HOLD -> {
                first = MobEffects.REGENERATION;
                second = MobEffects.ABSORPTION;
            }
            default -> {
                return;
            }
        }
        for (UUID id : this.roster) {
            if (level.getEntity(id) instanceof LivingEntity living && living.isAlive()) {
                living.addEffect(new MobEffectInstance(first, DRUMMER_BUFF_DURATION, 0, false, false));
                living.addEffect(new MobEffectInstance(second, DRUMMER_BUFF_DURATION, 0, false, false));
            }
        }
    }

    /**
     * 呼叫炮火支援：让支援清单里还活着的炮兵队长吹号开火（强制一轮炮）。
     *
     * <p>炮兵队长的 {@code HornFire} 标记会被 KubeJS 炮兵 AI 消费并开火。
     */
    public void requestArtillerySupport(ServerLevel level) {
        this.supportCaptains.removeIf(id -> !(level.getEntity(id) instanceof ArtilleryCaptainEntity));
        for (UUID id : this.supportCaptains) {
            if (level.getEntity(id) instanceof ArtilleryCaptainEntity captain && captain.isAlive()) {
                captain.setHornFireTick(level.getGameTime());
            }
        }
    }

    // ------------------------------------------------------------------
    // 突击作战指令
    // ------------------------------------------------------------------

    /** 下达突击命令：连队自行逼近、侦察、炮击、突击、占领。 */
    public void beginAssault(BlockPos objective) {
        this.objective = objective;
        this.phase = AssaultPhase.APPROACH;
        this.phaseTicks = 0;
        this.defendersAtBombardStart = 0;
        this.assaultTimer = 0;
        this.anchor = null;
        this.layoutTimer = 0;
        this.announce(Component.translatable("requiem.assault.begin",
                objective.getX(), objective.getY(), objective.getZ()));
    }

    /** 撤销作战任务，连队恢复常规自动指挥。 */
    public void abortAssault() {
        if (!this.phase.isActive()) {
            return;
        }
        this.endAssault();
        this.announce(Component.translatable("requiem.assault.abort"));
    }

    private void completeAssault() {
        this.endAssault();
        this.announce(Component.translatable("requiem.assault.complete"));
    }

    private void endAssault() {
        this.objective = null;
        this.phase = AssaultPhase.NONE;
        this.phaseTicks = 0;
        this.assaultHeadcount = 0;
        this.breachTarget = null;
        this.clearSupportCaptainStrikes();
        this.pinnedAnchor = null;
        this.anchor = null;
        this.captain.setCommandPost(null);
        this.stanceTimer = 0;
        this.layoutTimer = 0;
    }

    public AssaultPhase getAssaultPhase() {
        return this.phase;
    }

    public BlockPos getObjective() {
        return this.objective;
    }

    /** 炮阵是否因被近身而撤炮。 */
    public boolean isBatteryWithdrawn() {
        return this.batteryWithdrawn;
    }

    /** 解散连队：全员退编并清空名册。连队长阵亡时调用。 */
    public void disband() {
        if (this.captain.level() instanceof ServerLevel level) {
            for (UUID id : this.roster) {
                if (level.getEntity(id) instanceof Mob mob) {
                    FormationData.discharge(mob);
                }
            }
        }
        this.roster.clear();
        this.squads.clear();
    }

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    public void save(CompoundTag tag) {
        tag.putByte("Formation", (byte) this.formation.ordinal());
        tag.putBoolean("AutoFormation", this.autoFormation);
        tag.putByte("Stance", (byte) this.stance.ordinal());
        tag.putInt("StanceLock", this.stanceLockTicks);
        tag.putFloat("Facing", this.facing);
        if (this.pinnedAnchor != null) {
            tag.putDouble("PinX", this.pinnedAnchor.x);
            tag.putDouble("PinY", this.pinnedAnchor.y);
            tag.putDouble("PinZ", this.pinnedAnchor.z);
            tag.putFloat("PinFacing", this.pinnedFacing);
        }
        tag.putByte("Phase", (byte) this.phase.ordinal());
        tag.putInt("PhaseTicks", this.phaseTicks);
        tag.putInt("BombardStart", this.defendersAtBombardStart);
        tag.putBoolean("BatteryWithdrawn", this.batteryWithdrawn);
        if (this.objective != null) {
            tag.put("Objective", NbtUtils.writeBlockPos(this.objective));
        }
        ListTag list = new ListTag();
        for (UUID id : this.roster) {
            CompoundTag member = new CompoundTag();
            member.putUUID("Id", id);
            list.add(member);
        }
        tag.put("Roster", list);
        if (this.drummerId != null) {
            tag.putUUID("Drummer", this.drummerId);
        }
    }

    public void load(CompoundTag tag) {
        this.formation = FormationType.byOrdinal(tag.getByte("Formation"));
        // 旧存档没有这个键，默认交给连队长自动指挥
        this.autoFormation = !tag.contains("AutoFormation") || tag.getBoolean("AutoFormation");
        this.stance = CompanyStance.byOrdinal(tag.getByte("Stance"));
        this.stanceLockTicks = tag.getInt("StanceLock");
        this.facing = tag.getFloat("Facing");
        this.pinnedAnchor = tag.contains("PinX")
                ? new Vec3(tag.getDouble("PinX"), tag.getDouble("PinY"), tag.getDouble("PinZ"))
                : null;
        this.pinnedFacing = tag.getFloat("PinFacing");
        // 锚点不存档：下次 pushOrders 会按连队长当前位置重新推导
        this.anchor = this.pinnedAnchor;

        this.phase = AssaultPhase.byOrdinal(tag.getByte("Phase"));
        this.phaseTicks = tag.getInt("PhaseTicks");
        this.defendersAtBombardStart = tag.getInt("BombardStart");
        this.batteryWithdrawn = tag.getBoolean("BatteryWithdrawn");
        this.objective = tag.contains("Objective")
                ? NbtUtils.readBlockPos(tag.getCompound("Objective"))
                : null;
        if (this.objective == null) {
            this.phase = AssaultPhase.NONE;
        }

        this.roster.clear();
        ListTag list = tag.getList("Roster", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag member = list.getCompound(i);
            if (member.hasUUID("Id")) {
                this.roster.add(member.getUUID("Id"));
            }
        }
        this.squads.clear();
        this.drummerId = tag.contains("Drummer") ? tag.getUUID("Drummer") : null;
    }

    /** 从 {@code from} 沿偏航角 {@code yaw} 的正面方向前进 {@code distance} 格。 */
    private static Vec3 forwardOf(Vec3 from, float yaw, double distance) {
        double rad = Math.toRadians(yaw);
        return from.add(-Math.sin(rad) * distance, 0.0D, Math.cos(rad) * distance);
    }
}
