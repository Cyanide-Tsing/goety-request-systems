package com.requiem.servants.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 寻路原始类型：把任意门与栅栏门都视为"关着的木门"（可通过），
 * 这样再配合 MobRegisterGoalsMixin 设置的 canOpenDoors/canPassDoors，灾厄村民/仆从即可穿门。
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {

    @Inject(method = "getBlockPathTypeRaw", at = @At("HEAD"), cancellable = true)
    private static void requiem$getBlockPathTypeRaw(BlockGetter level, BlockPos pos,
                                                    CallbackInfoReturnable<BlockPathTypes> cir) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof DoorBlock) {
            cir.setReturnValue(state.getValue(DoorBlock.OPEN)
                    ? BlockPathTypes.DOOR_OPEN : BlockPathTypes.WALKABLE_DOOR);
            cir.cancel();
        } else if (block instanceof FenceGateBlock) {
            cir.setReturnValue(state.getValue(FenceGateBlock.OPEN)
                    ? BlockPathTypes.DOOR_OPEN : BlockPathTypes.WALKABLE_DOOR);
            cir.cancel();
        }
    }
}
