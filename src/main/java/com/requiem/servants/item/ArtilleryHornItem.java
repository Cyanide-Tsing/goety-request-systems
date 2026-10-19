package com.requiem.servants.item;

import com.requiem.servants.entity.ArtilleryCaptainEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 炮兵号角：绑定炮兵队长、建立炮兵小队，并亲自号令炮兵开火。
 *
 * <ul>
 *   <li>右键<b>炮兵队长</b> —— 把号角绑定到那个炮兵队长（建立炮兵小队）。</li>
 *   <li>右键<b>空中</b> —— 吹号，强制该小队立刻开一轮炮。</li>
 * </ul>
 *
 * <p>绑定信息存在物品 NBT 里，小队成员（炮兵）通过 goety 的
 * {@code setLeader} 跟随队长；队长阵亡后炮兵停火，除非玩家再次吹号。
 */
public class ArtilleryHornItem extends Item {
    private static final String TAG_CAPTAIN = "Captain";
    private static final String TAG_CAPTAIN_NAME = "CaptainName";
    private static final ResourceLocation HORN_SOUND = new ResourceLocation("minecraft", "event.raid.horn");

    public ArtilleryHornItem(Properties properties) {
        super(properties);
    }

    /** 把号角绑定到指定炮兵队长。 */
    public static void bindTo(ItemStack stack, ArtilleryCaptainEntity captain, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_CAPTAIN, captain.getUUID());
        tag.putString(TAG_CAPTAIN_NAME, captain.getDisplayName().getString());
        player.displayClientMessage(Component.translatable("requiem.horn.bound", captain.getDisplayName()), true);
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_CAPTAIN);
    }

    private static ArtilleryCaptainEntity resolve(ItemStack stack, Level level) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_CAPTAIN)) {
            return null;
        }
        return server.getEntity(tag.getUUID(TAG_CAPTAIN)) instanceof ArtilleryCaptainEntity captain && captain.isAlive()
                ? captain
                : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isBound(stack)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("requiem.horn.unbound"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        ArtilleryCaptainEntity captain = resolve(stack, level);
        if (captain == null) {
            player.displayClientMessage(Component.translatable("requiem.horn.lost"), true);
            return InteractionResultHolder.fail(stack);
        }
        // 吹号：给队长下一条开火命令，炮兵据此强制开一轮炮
        captain.setHornFireTick(level.getGameTime());
        playHornSound(level, player);
        player.displayClientMessage(Component.translatable("requiem.horn.fire"), true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

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
        ArtilleryCaptainEntity captain = resolve(stack, level);
        if (captain == null) {
            player.displayClientMessage(Component.translatable("requiem.horn.lost"), true);
            return InteractionResult.FAIL;
        }
        // 号角右键地面：阵地由 KubeJS 写入（persistentData），这里只负责播放号音与提示
        BlockPos pos = context.getClickedPos();
        playHornSound(level, player);
        player.displayClientMessage(Component.translatable("requiem.horn.battery",
                pos.getX(), pos.getY(), pos.getZ()), true);
        return InteractionResult.SUCCESS;
    }

    private static void playHornSound(Level level, Player player) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(HORN_SOUND);
        if (sound != null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_CAPTAIN_NAME)) {
            tooltip.add(Component.translatable("requiem.horn.tooltip.bound",
                    tag.getString(TAG_CAPTAIN_NAME)).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("requiem.horn.tooltip.unbound").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("requiem.horn.tooltip.bind").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.horn.tooltip.fire").withStyle(ChatFormatting.DARK_GRAY));
    }
}
