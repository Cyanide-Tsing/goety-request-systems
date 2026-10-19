package com.requiem.servants.item;

import com.requiem.servants.command.AssaultPhase;
import com.requiem.servants.command.CompanyController;
import com.requiem.servants.entity.CaptainEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
 * 突击令：给连队指定一个攻击目标点，之后整套作战由连队长自己完成。
 *
 * <h2>操作</h2>
 * <ul>
 *   <li>右键<b>连队长</b> —— 绑定到那个连队。</li>
 *   <li>右键<b>方块</b> —— 以该方块为目标下达突击命令。</li>
 *   <li>潜行 + 右键<b>空中</b> —— 撤销作战任务，连队恢复常规指挥。</li>
 *   <li>右键<b>空中</b> —— 查询当前作战阶段。</li>
 * </ul>
 *
 * <p>下令之后连队长会依次完成：逼近 → 侦察 → 炮击 → 突击 → 占领
 * （见 {@link AssaultPhase}），全程不需要玩家跟着。
 */
public class AssaultOrderItem extends Item {
    public AssaultOrderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  net.minecraft.world.entity.LivingEntity target,
                                                  InteractionHand hand) {
        if (!(target instanceof CaptainEntity captain)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            CompanyBinding.bind(stack, captain);
            player.displayClientMessage(Component.translatable("requiem.order.bound",
                    captain.getCompany().getSquadCount(), captain.getCompany().getMemberCount()), true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!CompanyBinding.isBound(stack)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("requiem.order.unbound"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        CaptainEntity captain = CompanyBinding.resolve(stack, level);
        if (captain == null) {
            player.displayClientMessage(Component.translatable("requiem.order.lost"), true);
            return InteractionResultHolder.fail(stack);
        }

        CompanyController company = captain.getCompany();
        if (player.isShiftKeyDown()) {
            company.abortAssault();
            player.displayClientMessage(Component.translatable("requiem.order.aborted"), true);
        } else {
            BlockPos objective = company.getObjective();
            player.displayClientMessage(objective == null
                    ? Component.translatable("requiem.order.idle")
                    : Component.translatable("requiem.order.status",
                            company.getAssaultPhase().getDisplayName(),
                            objective.getX(), objective.getY(), objective.getZ()), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || !CompanyBinding.isBound(stack)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        CaptainEntity captain = CompanyBinding.resolve(stack, level);
        if (captain == null) {
            player.displayClientMessage(Component.translatable("requiem.order.lost"), true);
            return InteractionResult.FAIL;
        }
        BlockPos objective = context.getClickedPos();
        captain.getCompany().beginAssault(objective);
        player.displayClientMessage(Component.translatable("requiem.order.issued",
                objective.getX(), objective.getY(), objective.getZ()), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(CompanyBinding.TAG_CAPTAIN_NAME)) {
            tooltip.add(Component.translatable("requiem.order.tooltip.bound",
                    tag.getString(CompanyBinding.TAG_CAPTAIN_NAME)).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("requiem.order.tooltip.unbound").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("requiem.order.tooltip.bind").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.order.tooltip.issue").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.order.tooltip.abort").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.order.tooltip.status").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.order.tooltip.phases").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return CompanyBinding.isBound(stack);
    }
}
