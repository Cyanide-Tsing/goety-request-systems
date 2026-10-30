package com.requiem.servants.item;

import com.requiem.servants.entity.CavalryCaptainEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 骑兵军旗：骑兵队长的指挥工具。
 *
 * <p>右键骑兵队长绑定；右键空中切换轻骑兵的"冲锋/收拢"号令。
 */
public class CavalryStandardItem extends Item {
    private static final String TAG_CAPTAIN = "Captain";
    private static final String TAG_CAPTAIN_NAME = "CaptainName";

    public CavalryStandardItem(Properties properties) {
        super(properties);
    }

    public static void bindTo(ItemStack stack, CavalryCaptainEntity captain, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_CAPTAIN, captain.getUUID());
        tag.putString(TAG_CAPTAIN_NAME, captain.getDisplayName().getString());
        player.displayClientMessage(Component.translatable("requiem.cavalry_standard.bound",
                captain.getDisplayName()), true);
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_CAPTAIN);
    }

    private static CavalryCaptainEntity resolve(ItemStack stack, Level level) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_CAPTAIN)) {
            return null;
        }
        return server.getEntity(tag.getUUID(TAG_CAPTAIN)) instanceof CavalryCaptainEntity captain && captain.isAlive()
                ? captain
                : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isBound(stack)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("requiem.cavalry_standard.unbound"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        CavalryCaptainEntity captain = resolve(stack, level);
        if (captain == null) {
            player.displayClientMessage(Component.translatable("requiem.cavalry_standard.lost"), true);
            return InteractionResultHolder.fail(stack);
        }
        boolean charge = captain.toggleChargeOrder();
        player.displayClientMessage(Component.translatable(
                charge ? "requiem.cavalry_standard.charge" : "requiem.cavalry_standard.rally"), true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // ------------------------------------------------------------------
    // 对方块右键：指定 / 解除骑兵驻扎地点
    // ------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || !isBound(stack)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        CavalryCaptainEntity captain = resolve(stack, level);
        if (captain == null) {
            player.displayClientMessage(Component.translatable("requiem.cavalry_standard.lost"), true);
            return InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            captain.clearGarrison();
            player.displayClientMessage(Component.translatable("requiem.cavalry_standard.garrison_clear"), true);
        } else {
            BlockPos pos = context.getClickedPos().above();
            captain.setGarrison(pos);
            player.displayClientMessage(Component.translatable("requiem.cavalry_standard.garrison",
                    pos.getX(), pos.getY(), pos.getZ()), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_CAPTAIN_NAME)) {
            tooltip.add(Component.translatable("requiem.cavalry_standard.tooltip.bound",
                    tag.getString(TAG_CAPTAIN_NAME)).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("requiem.cavalry_standard.tooltip.unbound").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("requiem.cavalry_standard.tooltip.bind").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.cavalry_standard.tooltip.order").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.cavalry_standard.tooltip.garrison").withStyle(ChatFormatting.DARK_GRAY));
    }
}
