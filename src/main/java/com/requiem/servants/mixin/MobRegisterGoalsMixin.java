package com.requiem.servants.mixin;

import com.Polarice3.Goety.api.entities.ally.IServant;
import com.Polarice3.Goety.common.entities.ally.illager.raider.RaiderServant;
import com.requiem.servants.ai.AvoidSpellEntityGoal;
import com.requiem.servants.ai.OrbitBlockedTargetGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版灾厄村民（Raider）没有开门 AI；Goety 灾厄仆从的 RaiderOpenDoorGoal 又只在掠夺时生效。
 * 这里统一给两者补一个常驻 OpenDoorGoal，并打开 canOpenDoors/canPassDoors 寻路标志。
 */
@Mixin(Mob.class)
public abstract class MobRegisterGoalsMixin {

    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void requiem$registerGoals(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        // 所有玩家仆从：感知并避让附近法术实体
        if (mob instanceof IServant) {
            this.goalSelector.addGoal(1, new AvoidSpellEntityGoal(mob));
        }
        if (!(mob instanceof Raider) && !(mob instanceof RaiderServant)) {
            return;
        }
        this.goalSelector.addGoal(2, new OpenDoorGoal(mob, true));
        this.goalSelector.addGoal(3, new OrbitBlockedTargetGoal(mob));
        PathNavigation nav = mob.getNavigation();
        if (nav instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanPassDoors(true);
        }
        nav.setMaxVisitedNodesMultiplier(16.0F);

        // 提升跟随范围，让灾厄村民/仆从能索敌到楼上（不同 y 坐标）的敌人。
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(followRange.getBaseValue() * 2.0D);
        }
    }
}
