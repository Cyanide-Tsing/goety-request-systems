package com.requiem.servants.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 修复 goety 部分灾厄村民（如 Preacher/传教士）getCelebrateSound() 返回 null，
 * 导致 RaiderCelebration 庆祝时 playSound(null) 崩溃的问题。
 * 这里在取庆祝音时给一个原版庆祝音兜底。
 */
@Mixin(Raider.RaiderCelebration.class)
public abstract class RaiderCelebrationMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/raid/Raider;getCelebrateSound()Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent requiem$getCelebrateSound(Raider raider) {
        SoundEvent sound = raider.getCelebrateSound();
        return sound != null ? sound : SoundEvents.PILLAGER_CELEBRATE;
    }
}
