package com.requiem.servants.item;

import com.requiem.servants.command.CompanyController;
import com.requiem.servants.command.CompanyStance;
import com.requiem.servants.entity.ArtilleryCaptainEntity;
import com.requiem.servants.entity.CaptainEntity;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 连旗：玩家用来对某个连队下令的工具。
 *
 * <h2>操作</h2>
 * <ul>
 *   <li>右键<b>连队长</b> —— 把连旗绑定到那个连队。</li>
 *   <li>潜行 + 右键<b>空中</b> —— 切换阵型（横列 → 纵队 → 方阵 → 空心方阵 → 楔形 → 梯次 → 环形 → 散兵）。</li>
 *   <li>右键<b>空中</b> —— 切换姿态（整队 → 坚守 → 冲锋），手动指定会压制自动评估 30 秒。</li>
 *   <li>右键<b>方块</b> —— 把集结点钉在那里，连队朝向取玩家视角。</li>
 *   <li>潜行 + 右键<b>方块</b> —— 取消钉住，集结点恢复自动跟随主人。</li>
 * </ul>
 *
 * <p>绑定信息存在物品 NBT 里，所以客户端也能判断"是否已绑定"，
 * 两端据此返回一致的 {@link InteractionResult}，不需要额外的网络包。
 */
public class CompanyStandardItem extends Item {
    private static final String TAG_CAPTAIN = "Captain";
    private static final String TAG_CAPTAIN_NAME = "CaptainName";

    /** 手动指定姿态后的锁定时长（30 秒）。 */
    private static final int STANCE_LOCK_TICKS = 600;

    private static final ResourceLocation ORDER_SOUND = new ResourceLocation("minecraft", "event.raid.horn");

    public CompanyStandardItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // 绑定
    // ------------------------------------------------------------------

    /** 由 {@link CaptainEntity#mobInteract} 调用。 */
    public static void bindTo(ItemStack stack, CaptainEntity captain, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_CAPTAIN, captain.getUUID());
        tag.putString(TAG_CAPTAIN_NAME, captain.getDisplayName().getString());
        CompanyController company = captain.getCompany();
        player.displayClientMessage(Component.translatable("requiem.standard.bound",
                company.getSquadCount(), company.getMemberCount(),
                company.isAutoFormation()
                        ? Component.translatable("requiem.formation.auto")
                        : company.getFormation().getDisplayName()), true);
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_CAPTAIN);
    }

    /** 只能在服务端解析出实体。 */
    private static CaptainEntity resolve(ItemStack stack, Level level) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_CAPTAIN)) {
            return null;
        }
        return server.getEntity(tag.getUUID(TAG_CAPTAIN)) instanceof CaptainEntity captain && captain.isAlive()
                ? captain
                : null;
    }

    /**
     * 用连旗把炮兵队长加入（或移出）连队的炮火支援清单。
     *
     * <p>连队只管步兵，炮兵队长不编入名册，只登记到支援清单里；
     * 连队需要炮火支援时会叫这些队长吹号开火（见 {@link CompanyController#requestArtillerySupport}）。
     */
    public static void toggleSupportCaptain(ItemStack stack, ArtilleryCaptainEntity artilleryCaptain, Player player) {
        if (!isBound(stack)) {
            player.displayClientMessage(Component.translatable("requiem.standard.unbound"), true);
            return;
        }
        CaptainEntity companyCaptain = resolve(stack, artilleryCaptain.level());
        if (companyCaptain == null) {
            player.displayClientMessage(Component.translatable("requiem.standard.lost"), true);
            return;
        }
        CompanyController company = companyCaptain.getCompany();
        if (company.hasSupportCaptain(artilleryCaptain)) {
            company.removeSupportCaptain(artilleryCaptain);
            player.displayClientMessage(Component.translatable("requiem.standard.support.removed",
                    artilleryCaptain.getDisplayName()), true);
        } else {
            company.addSupportCaptain(artilleryCaptain);
            player.displayClientMessage(Component.translatable("requiem.standard.support.added",
                    artilleryCaptain.getDisplayName()), true);
        }
    }

    // ------------------------------------------------------------------
    // 空手右键：切阵型 / 切姿态
    // ------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isBound(stack)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("requiem.standard.unbound"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        CaptainEntity captain = resolve(stack, level);
        if (captain == null) {
            player.displayClientMessage(Component.translatable("requiem.standard.lost"), true);
            return InteractionResultHolder.fail(stack);
        }

        CompanyController company = captain.getCompany();
        if (player.isShiftKeyDown()) {
            // 绕完一轮阵型后会回到"自动"，把指挥权交还给连队长
            boolean auto = company.cycleFormationMode();
            player.displayClientMessage(auto
                    ? Component.translatable("requiem.standard.formation",
                            Component.translatable("requiem.formation.auto"))
                    : Component.translatable("requiem.standard.formation",
                            company.getFormation().getDisplayName()), true);
        } else {
            CompanyStance stance = cycleStance(company);
            player.displayClientMessage(Component.translatable("requiem.standard.stance",
                    stance.getDisplayName()), true);
        }
        playOrderSound(level, player);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // ------------------------------------------------------------------
    // 对方块右键：钉住 / 解除集结点
    // ------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || !isBound(stack)) {
            // 交给 use() 去提示未绑定
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        CaptainEntity captain = resolve(stack, level);
        if (captain == null) {
            player.displayClientMessage(Component.translatable("requiem.standard.lost"), true);
            return InteractionResult.FAIL;
        }

        CompanyController company = captain.getCompany();
        if (player.isShiftKeyDown()) {
            company.unpinAnchor();
            player.displayClientMessage(Component.translatable("requiem.standard.unpinned"), true);
        } else {
            BlockPos pos = context.getClickedPos().above();
            company.pinAnchor(new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D), player.getYRot());
            company.lockStance(CompanyStance.FORM_UP, STANCE_LOCK_TICKS);
            player.displayClientMessage(Component.translatable("requiem.standard.pinned",
                    pos.getX(), pos.getY(), pos.getZ(), company.getFormation().getDisplayName()), true);
        }
        playOrderSound(level, player);
        return InteractionResult.SUCCESS;
    }

    /** 手动姿态循环：整队 → 坚守 → 冲锋 → 整队。 */
    private static CompanyStance cycleStance(CompanyController company) {
        CompanyStance next = switch (company.getStance()) {
            case FORM_UP -> CompanyStance.HOLD;
            case HOLD -> CompanyStance.CHARGE;
            default -> CompanyStance.FORM_UP;
        };
        company.lockStance(next, STANCE_LOCK_TICKS);
        return next;
    }

    /**
     * 用注册表按 ID 取号音。
     * 不直接引用 {@code SoundEvents} 的字段，因为部分字段在 1.20.1 是 {@code Holder} 而不是
     * {@code SoundEvent}，按 ID 查更稳妥。
     */
    private static void playOrderSound(Level level, Player player) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ORDER_SOUND);
        if (sound != null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.PLAYERS, 0.5F, 1.2F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_CAPTAIN_NAME)) {
            tooltip.add(Component.translatable("requiem.standard.tooltip.bound",
                    tag.getString(TAG_CAPTAIN_NAME)).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("requiem.standard.tooltip.unbound").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("requiem.standard.tooltip.bind").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.standard.tooltip.formation").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.standard.tooltip.stance").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.standard.tooltip.pin").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("requiem.standard.tooltip.unpin").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isBound(stack);
    }
}
