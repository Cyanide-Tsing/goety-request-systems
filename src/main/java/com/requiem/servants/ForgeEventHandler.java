package com.requiem.servants;

import com.requiem.servants.ai.FormationSlotGoal;
import com.requiem.servants.command.FormationEligibility;
import com.requiem.servants.entity.ServantRotation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 把列阵能力挂到 goety 本体的仆从身上。
 *
 * <p>这是连队系统能用在<b>原版 goety 仆从</b>（卫道士仆从、掠夺者仆从、唤魔者仆从……）上的关键：
 * 那些类不在本附属里，没法改它们的 {@code registerGoals()}。所以这里在实体加入世界时，
 * 直接往它的 {@code goalSelector} 里插一个 {@link FormationSlotGoal}。
 *
 * <p>这条路之所以走得通，是因为 {@code Mob.goalSelector} 在 1.20.1 是 {@code public final}，
 * 不需要 mixin 也不需要反射。
 *
 * <p>注入是无条件的——只要是地面仆从就插。目标本身在没有槽位时 {@code canUse()} 直接返回 false，
 * 所以没被编入连队的仆从行为完全不受影响。
 */
@Mod.EventBusSubscriber(modid = RequiemServants.MOD_ID)
public final class ForgeEventHandler {
    private ForgeEventHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Mob mob && FormationEligibility.canHoldFormation(mob)) {
            attachFormationGoal(mob);
        }
    }

    /** 幂等：同一个实体实例重复加入世界时不会插第二个目标。 */
    private static void attachFormationGoal(Mob mob) {
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof FormationSlotGoal) {
                return;
            }
        }
        mob.goalSelector.addGoal(FormationSlotGoal.PRIORITY, new FormationSlotGoal(mob));
    }

    /** 实体离开世界时清掉旋转辅助里的缓存，避免按实体 id 累积泄漏。 */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            ServantRotation.forget(mob);
        }
    }
}
