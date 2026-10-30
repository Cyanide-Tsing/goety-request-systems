package com.requiem.servants.item;

import com.requiem.servants.command.FormationEligibility;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 委任状：右键一个无工作的仆从，把它的委任关系写进 NBT，
 * 再放进工作方块的槽位完成工作分配。
 *
 * <p>绑定不在 {@link #interactLivingEntity} 里做，而是由
 * {@link com.requiem.servants.ForgeEventHandler} 监听
 * {@code PlayerInteractEvent.EntityInteract} 处理——这样能绕过 goety 仆从
 * {@code mobInteract} 在创造模式下先消费交互的问题，生存/创造都能绑定。
 */
public class AppointmentItem extends Item {
    private static final String TAG_SERVANT = "Servant";
    private static final String TAG_SERVANT_NAME = "ServantName";
    private static final String TAG_SERVANT_TYPE = "ServantType";

    public AppointmentItem(Properties properties) {
        super(properties);
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_SERVANT);
    }

    public static UUID getBoundServantId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_SERVANT) ? tag.getUUID(TAG_SERVANT) : null;
    }

    public static String getBoundServantType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_SERVANT_TYPE) ? tag.getString(TAG_SERVANT_TYPE) : null;
    }

    public static boolean isValidTarget(Mob mob) {
        return FormationEligibility.isScriptedWorker(mob);
    }

    /** 把无工作仆从绑定到委任状上。 */
    public static void bindTo(ItemStack stack, Mob servant, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_SERVANT, servant.getUUID());
        tag.putString(TAG_SERVANT_NAME, servant.getDisplayName().getString());
        tag.putString(TAG_SERVANT_TYPE, servant.getType().getDescriptionId());
        player.displayClientMessage(Component.translatable("requiem.appointment.bound",
                servant.getDisplayName()), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_SERVANT_NAME)) {
            String typeKey = tag.contains(TAG_SERVANT_TYPE) ? tag.getString(TAG_SERVANT_TYPE) : null;
            Component type = typeKey != null ? Component.translatable(typeKey) : Component.literal("?");
            Component name = Component.literal(tag.getString(TAG_SERVANT_NAME));
            tooltip.add(Component.translatable("requiem.appointment.tooltip.bound", type, name)
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("requiem.appointment.tooltip.unbound").withStyle(ChatFormatting.GRAY));
        }
    }
}
