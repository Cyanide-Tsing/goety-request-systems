package com.requiem.servants.mixin;

import com.requiem.servants.ModBlockEntities;
import com.requiem.servants.block.DemandWorkBlockEntity;
import com.requiem.servants.block.DemandWorkMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FletchingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给原版制箭台（fletching_table）加方块实体与 GUI，使其成为制箭师的工作方块。
 */
@Mixin(FletchingTableBlock.class)
public abstract class FletchingTableBlockMixin implements EntityBlock {

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemandWorkBlockEntity(ModBlockEntities.DEMAND_WORK.get(), pos, state);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void requiem$use(BlockState state, Level level, BlockPos pos, Player player,
                             InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) {
            cir.setReturnValue(InteractionResult.sidedSuccess(true));
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof DemandWorkBlockEntity be) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new DemandWorkMenu(id, inv, be),
                    Component.translatable("block.minecraft.fletching_table"));
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(false));
    }
}
