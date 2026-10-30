package com.requiem.servants.mixin;

import com.Polarice3.Goety.common.entities.ally.illager.raider.RaiderServant;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灾厄村民 / Goety 灾厄仆从的索敌视线：隔墙（关着门的屋子）也能锁定目标，
 * 让它们能发现门后的玩家并寻路进屋。
 */
@Mixin(Sensing.class)
public abstract class SensingMixin {

    @Shadow
    @Final
    private Mob mob;

    @Inject(method = "hasLineOfSight", at = @At("HEAD"), cancellable = true)
    private void requiem$hasLineOfSight(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof Raider || this.mob instanceof RaiderServant) {
            cir.setReturnValue(true);
        }
    }
}
