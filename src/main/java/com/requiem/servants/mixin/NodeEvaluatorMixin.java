package com.requiem.servants.mixin;

import com.Polarice3.Goety.common.entities.ally.illager.raider.RaiderServant;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灾厄村民（原版 Raider）与 Goety 灾厄仆从的寻路器：
 * 让 canOpenDoors / canPassDoors 恒为 true，使它们能把门当作可通行路径（WALKABLE_DOOR）。
 */
@Mixin(NodeEvaluator.class)
public abstract class NodeEvaluatorMixin {

    @Shadow
    protected Mob mob;

    @Inject(method = "canOpenDoors", at = @At("HEAD"), cancellable = true)
    private void requiem$canOpenDoors(CallbackInfoReturnable<Boolean> cir) {
        if (this.isIllager()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canPassDoors", at = @At("HEAD"), cancellable = true)
    private void requiem$canPassDoors(CallbackInfoReturnable<Boolean> cir) {
        if (this.isIllager()) {
            cir.setReturnValue(true);
        }
    }

    private boolean isIllager() {
        return this.mob instanceof Raider || this.mob instanceof RaiderServant;
    }
}
