package com.requiem.servants.mixin;

import com.Polarice3.Goety.common.entities.ally.illager.VindicatorChefServant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让卫道士厨师仆从把农夫乐事的厨锅/煎锅也视为"烹饪灶"（furnace）。
 */
@Mixin(VindicatorChefServant.class)
public abstract class VindicatorChefServantMixin {

    @Inject(method = "isFurnace", at = @At("HEAD"), cancellable = true, remap = false)
    private void requiem$isFurnace(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null && ("farmersdelight:cooking_pot".equals(id.toString())
                || "farmersdelight:skillet".equals(id.toString()))) {
            cir.setReturnValue(true);
        }
    }
}
