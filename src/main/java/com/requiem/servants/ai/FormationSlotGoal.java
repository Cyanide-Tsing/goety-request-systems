package com.requiem.servants.ai;

import com.Polarice3.Goety.api.entities.ally.IServant;
import com.requiem.servants.command.CompanyStance;
import com.requiem.servants.command.FormationData;
import com.requiem.servants.command.FormationLayout;
import com.requiem.servants.entity.CaptainEntity;
import com.requiem.servants.entity.ServantRotation;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 成员侧的列阵目标：把灾厄村民仆从带到连队长分配的槽位，并按阵型要求保持朝向。
 *
 * <p>由 {@link com.requiem.servants.ForgeEventHandler} 注入到所有符合条件的 goety 灾厄村民仆从身上，
 * 优先级 {@value #PRIORITY} —— 高于 goety 的近战攻击(4)与跟随主人(5)，
 * 所以列阵能压住"各自追着敌人乱跑"；但低于破门(1)与开门(2)，不会让部队卡在门口。
 *
 * <h2>只占用 MOVE，不占用 LOOK</h2>
 * 这个目标只接管移动，<b>不</b>接管视线。朝向靠直接写 yaw 实现，不走 {@code LookControl}。
 * 这样远程与施法单位站在队列里时，仍然能用自己的攻击目标瞄准敌人（头转过去），
 * 而身体朝向继续由阵型锁定。
 *
 * <h2>让位规则</h2>
 * <ul>
 *   <li><b>钉死的槽位</b>（炮列里的炮兵）：到位后永不让位、永不移动，只管站着开火。</li>
 *   <li>{@link CompanyStance#CHARGE}：完全放开，全员自由突击。</li>
 *   <li>{@link CompanyStance#ENGAGE}：可以离开槽位厮杀，但超出 {@value #ENGAGE_LEASH} 格会被拉回。</li>
 *   <li>其余姿态：只有敌人贴到 {@value #MELEE_YIELD_RANGE} 格以内才让位反击，否则死守阵线。</li>
 * </ul>
 */
public class FormationSlotGoal extends Goal {
    /** 注入优先级。见类注释里对 goety 既有优先级的说明。 */
    public static final int PRIORITY = 3;

    /** 站进槽位的判定半径（水平距离）。 */
    private static final double ARRIVE_TOLERANCE = 0.8D;
    /**
     * 钉死槽位（炮组操作位）的判定半径，放宽一些。
     *
     * <p>炮位旁的落点未必正好能站到 0.8 格以内，而脚本判定"能操炮"的窗口是 2.5 格，
     * 所以这里取一个比 0.8 宽、又明显小于 2.5 的值：到位即停，不要原地来回挪。
     */
    private static final double ARRIVE_TOLERANCE_ANCHORED = 1.6D;
    /** 交战姿态下允许脱离槽位的最大距离。 */
    private static final double ENGAGE_LEASH = 7.0D;
    /** 非交战姿态下，敌人进到这个距离才让位给攻击目标。 */
    private static final double MELEE_YIELD_RANGE = 3.0D;
    /** 掉队超过这个距离就直接归位。 */
    private static final double WARP_DISTANCE = 24.0D;
    /** 整队/集结时的归位阈值放宽到这里，好让队伍尽快站定。 */
    private static final double WARP_DISTANCE_FORMING = 12.0D;
    /** 连续这么多刻还没走到槽位，就直接归位，避免被地形卡住导致阵型迟迟不成形。 */
    private static final int MARCH_PATIENCE = 60;
    /** 重新寻路的间隔。 */
    private static final int RECALC_INTERVAL = 10;
    /** 检查连队长是否还在的间隔。 */
    private static final int COMMANDER_CHECK_INTERVAL = 40;

    private static final double ARRIVE_TOLERANCE_SQR = ARRIVE_TOLERANCE * ARRIVE_TOLERANCE;
    private static final double ARRIVE_TOLERANCE_ANCHORED_SQR = ARRIVE_TOLERANCE_ANCHORED * ARRIVE_TOLERANCE_ANCHORED;
    private static final double ENGAGE_LEASH_SQR = ENGAGE_LEASH * ENGAGE_LEASH;
    private static final double MELEE_YIELD_SQR = MELEE_YIELD_RANGE * MELEE_YIELD_RANGE;

    private final Mob mob;
    private final IServant servant;
    private int recalcCooldown;
    private int marchTicks;
    private int commanderCheckCooldown;

    public FormationSlotGoal(Mob mob) {
        if (!(mob instanceof IServant)) {
            throw new IllegalArgumentException("FormationSlotGoal 只能用于 goety 仆从: " + mob.getType());
        }
        this.mob = mob;
        this.servant = (IServant) mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.shouldHoldSlot();
    }

    @Override
    public boolean canContinueToUse() {
        return this.shouldHoldSlot();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.recalcCooldown = 0;
        this.marchTicks = 0;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.marchTicks = 0;
    }

    @Override
    public void tick() {
        Vec3 slot = FormationData.getSlotPos(this.mob);
        if (slot == null) {
            return;
        }
        CompanyStance stance = FormationData.getStance(this.mob);
        boolean anchored = FormationData.isAnchored(this.mob);
        double distSqr = this.horizontalDistSqr(slot);
        double tolerance = anchored ? ARRIVE_TOLERANCE_ANCHORED_SQR : ARRIVE_TOLERANCE_SQR;

        if (distSqr > tolerance) {
            // 钉死的单位也要能回位：被击退或炮位改绑后必须重新走过去，
            // 所以这里只按距离判断，不做"一次到位就永久锁死"的处理。
            this.marchTo(slot, stance, distSqr);
        } else {
            this.marchTicks = 0;
            this.standFast(stance);
        }
    }

    /** 还没到位：寻路过去；太远或走不动就直接归位，让阵型尽快成形。 */
    private void marchTo(Vec3 slot, CompanyStance stance, double distSqr) {
        this.marchTicks++;
        double warpAt = (stance == CompanyStance.FORM_UP || stance == CompanyStance.RALLY)
                ? WARP_DISTANCE_FORMING
                : WARP_DISTANCE;
        boolean stuck = this.marchTicks > MARCH_PATIENCE;

        if (--this.recalcCooldown <= 0) {
            this.recalcCooldown = RECALC_INTERVAL;
            boolean warped = (distSqr > warpAt * warpAt || stuck) && this.tryWarpToSlot(slot);
            if (warped) {
                this.marchTicks = 0;
            } else {
                double speed = Math.min(1.6D, Math.max(0.6D, this.servant.getCommandSpeed())) * stance.speedModifier();
                this.mob.getNavigation().moveTo(slot.x, slot.y, slot.z, speed);
            }
        }
    }

    /** 已经站进槽位：停下，并按阵型要求定住朝向。 */
    private void standFast(CompanyStance stance) {
        if (!this.mob.getNavigation().isDone()) {
            this.mob.getNavigation().stop();
        }

        boolean anchored = FormationData.isAnchored(this.mob);
        // 朝外的阵型（空心方阵/环形）与钉死的火线，无论什么姿态都必须守住朝向
        boolean strict = anchored
                || stance.requiresStrictFacing()
                || FormationData.getFormation(this.mob).facesOutward();
        if (!strict) {
            LivingEntity target = this.mob.getTarget();
            if (target != null && target.isAlive()) {
                // 不严格要求朝向时，让身体也转向敌人
                this.applyFacing(FormationLayout.yawTowards(this.mob.position(), target.position()));
                return;
            }
        }
        this.applyFacing(FormationData.getSlotYaw(this.mob));
    }

    /** 直接定住朝向，并通知 {@link ServantRotation} 本刻不要用移动方向覆盖它。 */
    private void applyFacing(float yaw) {
        this.mob.setYRot(yaw);
        this.mob.setYBodyRot(yaw);
        this.mob.setYHeadRot(yaw);
        ServantRotation.lockFacing(this.mob, yaw);
    }

    private boolean shouldHoldSlot() {
        if (this.dischargeIfCommanderGone()) {
            return false;
        }
        if (!FormationData.hasActiveOrder(this.mob, this.mob.level().getGameTime())) {
            return false;
        }
        // 骑在坐骑上时站位没有意义，交还移动控制权
        if (this.mob.isPassenger()) {
            return false;
        }
        Vec3 slot = FormationData.getSlotPos(this.mob);
        if (slot == null) {
            return false;
        }

        // 钉死的阵位（炮组操作位）：无论发生什么都守住。
        // 炮兵的开火由 KubeJS 脚本直接执行，不走攻击目标，所以一直攥着 MOVE 不影响它开炮。
        if (FormationData.isAnchored(this.mob)) {
            return true;
        }

        LivingEntity shootAt = this.mob.getTarget();
        boolean inPosition = this.horizontalDistSqr(slot) <= ARRIVE_TOLERANCE_SQR;
        if (inPosition && FormationData.mayFire(this.mob) && shootAt != null && shootAt.isAlive()) {
            // 已站定且这一排获准开火：交还 MOVE，让 goety/原版的攻击目标得以运行。
            // 不交还的话远程单位会举着弩站在队列里一箭不发（攻击目标抢不到 MOVE）。
            // 装填中的排不会走到这一支，于是继续被压住，形成轮番射击。
            return false;
        }

        CompanyStance stance = FormationData.getStance(this.mob);
        if (stance.releasesFormation()) {
            return false;
        }

        LivingEntity target = this.mob.getTarget();
        if (target != null && target.isAlive()) {
            if (stance.allowsPursuit()) {
                // 交战：放手去打，只在跑太远时把人拉回阵线
                return this.horizontalDistSqr(slot) > ENGAGE_LEASH_SQR;
            }
            if (this.mob.distanceToSqr(target) <= MELEE_YIELD_SQR) {
                // 敌人已经贴身，让位给攻击目标
                return false;
            }
        }
        return true;
    }

    /**
     * 连队长已经不在了就自行退编。
     *
     * <p>连队长正常阵亡时会主动解散全连，但它也可能在成员未加载时死亡、或跨维度失联。
     * 这里周期性自查，避免仆从永远挂着一个无效的连队长 UUID 而无法被新连队长收编。
     *
     * @return 是否刚刚退编
     */
    private boolean dischargeIfCommanderGone() {
        if (--this.commanderCheckCooldown > 0) {
            return false;
        }
        this.commanderCheckCooldown = COMMANDER_CHECK_INTERVAL;

        UUID commander = FormationData.getCommander(this.mob);
        if (commander == null) {
            return false;
        }
        if (!(this.mob.level() instanceof ServerLevel level)) {
            return false;
        }
        if (level.getEntity(commander) instanceof CaptainEntity captain && captain.isAlive()) {
            return false;
        }
        // 连队长不在本维度也可能是暂时卸载；只有站位令同时已过期才判定失联
        if (FormationData.hasActiveOrder(this.mob, level.getGameTime())) {
            return false;
        }
        FormationData.discharge(this.mob);
        return true;
    }

    /** 直接落到槽位上，向上下各找几格安全落点。 */
    private boolean tryWarpToSlot(Vec3 slot) {
        Level level = this.mob.level();
        BlockPos base = BlockPos.containing(slot);
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            Vec3 dest = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            if (!level.noCollision(this.mob, this.mob.getBoundingBox().move(dest.subtract(this.mob.position())))) {
                continue;
            }
            this.mob.moveTo(dest.x, dest.y, dest.z, FormationData.getSlotYaw(this.mob), this.mob.getXRot());
            this.mob.getNavigation().stop();
            return true;
        }
        return false;
    }

    /** 只比水平距离：槽位的 y 来自锚点，实际落地高度交给寻路。 */
    private double horizontalDistSqr(Vec3 slot) {
        double dx = this.mob.getX() - slot.x;
        double dz = this.mob.getZ() - slot.z;
        return dx * dx + dz * dz;
    }
}
