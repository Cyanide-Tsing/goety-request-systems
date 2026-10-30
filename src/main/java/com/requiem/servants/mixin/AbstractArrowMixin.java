package com.requiem.servants.mixin;

import com.Polarice3.Goety.utils.MobUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让原版箭矢也遵守盟友判定，避免箭射到同队/同主单位上引发内讧。
 *
 * <p>goety 自己的箭矢（RainArrow 等）已经用 {@link MobUtil#canHitEntity} 判断盟友，
 * 但原版骷髅/流髑射出的原版箭矢没有这一步，会误伤同队亡灵。
 * 这里在 {@code AbstractArrow.canHitEntity} 头部统一补上盟友判定。
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void requiem$preventAllyHit(Entity target, CallbackInfoReturnable<Boolean> cir) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (!MobUtil.canHitEntity(arrow, target)) {
            cir.setReturnValue(false);
        }
    }
}
