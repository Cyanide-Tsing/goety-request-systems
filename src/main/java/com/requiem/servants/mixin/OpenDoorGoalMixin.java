package com.requiem.servants.mixin;

import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 延长门 / 栅栏门保持开启的时间。
 *
 * <p>原版 {@link OpenDoorGoal#start()} 把 {@code forgetTime} 设为 20 刻（1 秒），
 * 时间一到或通过门后就调用 {@code stop()} 关门。这里把上限拉长到 100 刻（5 秒），
 * 避免仆从刚开门就立刻关门把自己挡在门外。
 */
@Mixin(OpenDoorGoal.class)
public abstract class OpenDoorGoalMixin {
    @Shadow
    private int forgetTime;

    @Inject(method = "start", at = @At("TAIL"))
    private void requiem$extendDoorOpenTime(CallbackInfo ci) {
        this.forgetTime = 100;
    }
}
