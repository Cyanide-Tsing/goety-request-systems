package com.requiem.servants.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让灾厄村民 / Goety 灾厄仆从能打开任意类型的门（含铁门）与栅栏门。
 *
 * <p>原版 {@link DoorInteractGoal} 用 {@code DoorBlock.isWoodenDoor} 只认木门；
 * 这里把判定放宽为"任意门 + 栅栏门"，并补上栅栏门的开合逻辑。
 */
@Mixin(DoorInteractGoal.class)
public abstract class DoorInteractGoalMixin {
    @Shadow
    protected Mob mob;
    @Shadow
    protected BlockPos doorPos;
    @Shadow
    protected boolean hasDoor;

    @Redirect(method = "canUse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/DoorBlock;isWoodenDoor(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean requiem$isOpenableDoor(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof DoorBlock || block instanceof FenceGateBlock) {
            return true;
        }
        // 原版 canUse 把 doorPos 设为 node.y + 1（门占两格，路径节点在门底）。
        // 栅栏门只有一格，路径节点就在栅栏门本身（node.y），所以实际位置是 pos.below()。
        if (level.getBlockState(pos.below()).getBlock() instanceof FenceGateBlock) {
            this.doorPos = pos.below();
            return true;
        }
        return false;
    }

    @Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
    private void requiem$setOpen(boolean open, CallbackInfo ci) {
        if (!this.hasDoor) {
            ci.cancel();
            return;
        }
        BlockState state = this.mob.level().getBlockState(this.doorPos);
        Block block = state.getBlock();
        if (block instanceof DoorBlock door) {
            door.setOpen(this.mob, this.mob.level(), state, this.doorPos, open);
            ci.cancel();
        } else if (block instanceof FenceGateBlock) {
            if (state.getValue(FenceGateBlock.OPEN) != open) {
                this.mob.level().setBlock(this.doorPos, state.setValue(FenceGateBlock.OPEN, open), 10);
            }
            ci.cancel();
        }
    }
}
