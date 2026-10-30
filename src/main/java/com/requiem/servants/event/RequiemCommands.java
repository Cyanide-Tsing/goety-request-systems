package com.requiem.servants.event;

import com.requiem.servants.RequiemServants;
import com.requiem.servants.ModEntities;
import com.requiem.servants.entity.WorkServantEntity;
import com.requiem.servants.logic.ServantLogic;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RequiemServants.MOD_ID)
public final class RequiemCommands {
    private RequiemCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("requiem")
                        .then(Commands.literal("events")
                                .then(Commands.literal("undead_march")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> triggerMarch(ctx.getSource()))))
                        .then(Commands.literal("toggle")
                                .then(Commands.literal("info")
                                        .executes(ctx -> toggleInfo(ctx.getSource())))
                                .then(Commands.literal("undead_march")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> toggleMarch(ctx.getSource()))))
                        .then(Commands.literal("debug")
                                .then(Commands.literal("inventory")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .executes(ctx -> showInventory(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "type")))))));
    }

    private static int showInventory(CommandSourceStack source, String type) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("requiem.march.command.require_player"));
            return 0;
        }
        EntityType<?> targetType = switch (type) {
            case "apprentice" -> ModEntities.APPRENTICE.get();
            case "gardener" -> ModEntities.GARDENER.get();
            case "fletcher" -> ModEntities.FLETCHER.get();
            case "recruiter" -> ModEntities.RECRUITER.get();
            default -> null;
        };
        if (targetType == null) {
            source.sendFailure(Component.literal("未知仆从类型：" + type + "（可选 apprentice / gardener / fletcher / recruiter）"));
            return 0;
        }
        ServerLevel level = source.getLevel();
        AABB aabb = new AABB(player.getX() - 64, player.getY() - 64, player.getZ() - 64,
                player.getX() + 64, player.getY() + 64, player.getZ() + 64);
        WorkServantEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity e : level.getEntitiesOfClass(Entity.class, aabb)) {
            if (e.getType() == targetType && e instanceof WorkServantEntity ws && ws.isAlive()) {
                double d = e.distanceToSqr(player);
                if (d < best) {
                    best = d;
                    nearest = ws;
                }
            }
        }
        if (nearest == null) {
            source.sendSuccess(() -> Component.literal("附近没有 " + type), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : nearest.getInventoryMap().entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("x").append(entry.getValue());
        }
        String inv = sb.length() == 0 ? "空" : sb.toString();
        WorkServantEntity target = nearest;
        source.sendSuccess(() -> Component.literal("[" + target.getDisplayName().getString() + " 背包] " + inv), false);
        return 1;
    }

    private static int toggleInfo(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("requiem.march.command.require_player"));
            return 0;
        }
        boolean cur = ServantLogic.servantInfoEnabled(player);
        player.getPersistentData().putBoolean("servantInfo", !cur);
        source.sendSuccess(() -> Component.literal("§e仆从反馈已" + (cur ? "关闭" : "开启")), false);
        return 1;
    }

    private static int triggerMarch(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("requiem.march.command.require_player"));
            return 0;
        }
        UndeadMarchManager.forceTrigger(level, player);
        source.sendSuccess(() -> Component.translatable("requiem.march.command.trigger", player.getDisplayName()), true);
        return 1;
    }

    private static int toggleMarch(CommandSourceStack source) {
        boolean now = UndeadMarchManager.toggle();
        source.sendSuccess(
                () -> Component.translatable(now ? "requiem.march.command.toggle_on" : "requiem.march.command.toggle_off"),
                true);
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            UndeadMarchManager.onServerTick(level);
        }
    }
}
