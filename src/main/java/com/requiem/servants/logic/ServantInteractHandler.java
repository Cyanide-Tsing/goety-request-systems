package com.requiem.servants.logic;

import com.Polarice3.Goety.common.items.WaystoneItem;
import com.requiem.servants.ModItems;
import com.requiem.servants.RequiemServants;
import com.requiem.servants.entity.ApprenticeEntity;
import com.requiem.servants.entity.ArtilleryCaptainEntity;
import com.requiem.servants.entity.ArtilleryEntity;
import com.requiem.servants.entity.FletcherEntity;
import com.requiem.servants.entity.GardenerEntity;
import com.requiem.servants.entity.RecruiterEntity;
import com.requiem.servants.item.NotepadItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 替代 KubeJS 的 {@code ItemEvents.entityInteracted} / {@code BlockEvents.rightClicked} /
 * {@code ItemEvents.rightClicked}：列表物品、路标石、号角、地图、空手等交互。
 */
@Mod.EventBusSubscriber(modid = RequiemServants.MOD_ID)
public final class ServantInteractHandler {
    private ServantInteractHandler() {
    }

    // ---------- 右键实体 ----------

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        Entity target = event.getTarget();

        // 记事本 → 征兵官（火炬）/ 学徒（熔炉+存储）/ 园丁·制箭师·炮兵·炮兵队长（存储）
        if (held.getItem() == ModItems.NOTEPAD.get()) {
            if (target instanceof RecruiterEntity recruiter) {
                List<BlockPos> torches = NotepadItem.getTorches(held);
                ServantLogic.writeBlockPosList(recruiter, "Torches", torches);
                player.displayClientMessage(Component.literal("已设置征兵官 " + torches.size() + " 个不祥火炬位置"), true);
            } else if (target instanceof ApprenticeEntity apprentice) {
                NotepadItem.FurnaceResult fr = NotepadItem.getFurnaces(held);
                List<BlockPos> furnaces = new ArrayList<>(fr.furnaces);
                furnaces.addAll(fr.blastFurnaces);
                furnaces.addAll(fr.smokers);
                ServantLogic.writeBlockPosList(apprentice, "Furnaces", furnaces);
                NotepadItem.StorageResult sr = NotepadItem.getStorage(held);
                ServantLogic.writeBlockPosList(apprentice, "Barrels", sr.barrels);
                player.displayClientMessage(Component.literal("已设置学徒 " + furnaces.size() + " 个熔炉、"
                        + sr.barrels.size() + " 个木桶"), true);
            } else if (target instanceof GardenerEntity gardener) {
                NotepadItem.StorageResult sr = NotepadItem.getStorage(held);
                ServantLogic.writeBlockPosList(gardener, "Barrels", sr.barrels);
                player.displayClientMessage(Component.literal("已设置园丁 " + sr.barrels.size() + " 个木桶"), true);
            } else if (target instanceof FletcherEntity fletcher) {
                NotepadItem.StorageResult sr = NotepadItem.getStorage(held);
                ServantLogic.writeBlockPosList(fletcher, "Barrels", sr.barrels);
                player.displayClientMessage(Component.literal("已设置制箭师 " + sr.barrels.size() + " 个木桶"), true);
            } else if (target instanceof ArtilleryEntity artillery) {
                List<BlockPos> cannons = NotepadItem.getCannons(held);
                if (!cannons.isEmpty()) {
                    ServantLogic.writeBlockPos(artillery, "Cannon", cannons.get(0));
                }
                NotepadItem.StorageResult sr = NotepadItem.getStorage(held);
                ServantLogic.writeBlockPosList(artillery, "Barrels", sr.barrels);
                player.displayClientMessage(Component.literal("已设置炮兵 " + cannons.size() + " 个炮位、"
                        + sr.barrels.size() + " 个弹药木桶"), true);
            } else if (target instanceof ArtilleryCaptainEntity captain) {
                List<BlockPos> cannons = NotepadItem.getCannons(held);
                if (!cannons.isEmpty()) {
                    captain.setBattery(cannons.get(0));
                }
                NotepadItem.StorageResult sr = NotepadItem.getStorage(held);
                ServantLogic.writeBlockPosList(captain, "Barrels", sr.barrels);
                player.displayClientMessage(Component.literal("已设置炮兵队长 " + cannons.size() + " 个炮位、"
                        + sr.barrels.size() + " 个弹药木桶"), true);
            } else {
                return;
            }
            event.setCanceled(true);
            return;
        }
        // 炮射地图 → 炮兵/炮兵队长
        if (held.getItem() == ModItems.STRIKE_MAP.get()
                && (target instanceof ArtilleryEntity || target instanceof ArtilleryCaptainEntity)) {
            LivingEntity living = (LivingEntity) target;
            CompoundTag tag = held.getTag();
            if (tag == null || !tag.contains("strikeX")) {
                player.displayClientMessage(Component.literal("炮射地图未设置坐标"), true);
                event.setCanceled(true);
                return;
            }
            BlockPos strike = new BlockPos(tag.getInt("strikeX"), tag.getInt("strikeY"), tag.getInt("strikeZ"));
            ServantLogic.writeBlockPos(living, "StrikeTarget", strike);
            if (living instanceof ArtilleryCaptainEntity) {
                living.getPersistentData().putString("captainFireMode", "marked");
            }
            player.displayClientMessage(Component.literal("已设置打击坐标 (" + strike.getX() + "," + strike.getY()
                    + "," + strike.getZ() + ")"), true);
            event.setCanceled(true);
            return;
        }
        // 号角 → 炮兵（编入号角绑定的队长小队）
        if (held.getItem() == ModItems.ARTILLERY_HORN.get() && target instanceof ArtilleryEntity artillery) {
            ArtilleryCaptainEntity captain = resolveHornCaptain(held, event.getLevel());
            if (captain == null) {
                player.displayClientMessage(Component.literal("号角未绑定炮兵队长"), true);
                event.setCanceled(true);
                return;
            }
            artillery.setCommandPosEntity(captain);
            player.displayClientMessage(Component.literal("已把炮兵编入炮兵队长小队"), true);
            event.setCanceled(true);
            return;
        }
        // 空手 → 炮兵队长（切换开火模式）
        if (held.isEmpty() && target instanceof ArtilleryCaptainEntity captain) {
            String mode = captain.getPersistentData().getString("captainFireMode");
            if (mode.isEmpty()) {
                mode = "free";
            }
            String next = mode.equals("free") ? "marked" : mode.equals("marked") ? "cease" : "free";
            captain.getPersistentData().putString("captainFireMode", next);
            String name = next.equals("cease") ? "停火" : next.equals("marked") ? "射击标记地点" : "自由射击";
            player.displayClientMessage(Component.literal("炮兵开火模式：" + name), true);
            event.setCanceled(true);
            return;
        }
        // 路标石 → 学徒/园丁/炮兵
        if (held.getItem() instanceof WaystoneItem && target instanceof LivingEntity living
                && (living instanceof ApprenticeEntity || living instanceof GardenerEntity
                        || living instanceof ArtilleryEntity)) {
            if (!WaystoneItem.hasBlock(held)) {
                return;
            }
            BlockPos pos = WaystoneItem.getBlockPos(held);
            if (pos == null) {
                return;
            }
            BlockState state = event.getLevel().getBlockState(pos);
            String key = mapBlockToNbtKey(state);
            boolean valid = key != null;
            if (living instanceof ArtilleryEntity) {
                valid = "Cannon".equals(key);
            } else if (living instanceof GardenerEntity) {
                valid = "Workstation".equals(key) || "BrewingStand".equals(key) || "Hopper".equals(key)
                        || "Chest".equals(key) || "RaiderChest".equals(key);
            }
            if (!valid) {
                player.displayClientMessage(Component.literal(living instanceof ArtilleryEntity
                        ? "炮兵只能绑定大炮" : "该仆从无法使用该方块"), true);
                event.setCanceled(true);
                return;
            }
            ServantLogic.writeBlockPos(living, key, pos);
            player.displayClientMessage(Component.literal("已设置位置：" + key), true);
            event.setCanceled(true);
        }
    }

    // ---------- 右键方块（列表物品记录位置） ----------

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.isEmpty()) {
            return;
        }
        Player player = event.getEntity();
        BlockState state = event.getLevel().getBlockState(event.getPos());
        Block block = state.getBlock();

        if (held.getItem() == ModItems.STRIKE_MAP.get()) {
            recordStrikeTarget(held, event.getPos(), player);
            event.setCanceled(true);
            return;
        }
        // 记事本：蹲下右键切换（记录 / 移除）方块
        if (held.getItem() == ModItems.NOTEPAD.get() && player.isShiftKeyDown()) {
            boolean added = NotepadItem.toggle(held, event.getPos(), state);
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            player.displayClientMessage(Component.literal(added
                    ? "已记录方块：" + id + "（共 " + NotepadItem.size(held) + " 个）"
                    : "已移除方块位置（剩 " + NotepadItem.size(held) + " 个）"), true);
            event.setCanceled(true);
        }
    }

    // ---------- 右键空气（炮射地图） ----------

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.getItem() != ModItems.STRIKE_MAP.get()) {
            return;
        }
        Player player = event.getEntity();
        HitResult hit = player.pick(256.0, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.literal("视线内没有目标方块"), true);
            event.setCanceled(true);
            return;
        }
        recordStrikeTarget(held, ((BlockHitResult) hit).getBlockPos(), player);
        event.setCanceled(true);
    }

    // ---------- 辅助 ----------

    private static void recordStrikeTarget(ItemStack held, BlockPos pos, Player player) {
        CompoundTag nbt = held.getOrCreateTag();
        nbt.putInt("strikeX", pos.getX());
        nbt.putInt("strikeY", pos.getY());
        nbt.putInt("strikeZ", pos.getZ());
        player.displayClientMessage(Component.literal("已设置打击坐标 (" + pos.getX() + "," + pos.getY() + ","
                + pos.getZ() + ")"), true);
    }

    private static ArtilleryCaptainEntity resolveHornCaptain(ItemStack horn, net.minecraft.world.level.LevelAccessor level) {
        CompoundTag tag = horn.getTag();
        if (tag == null || !tag.hasUUID("Captain")) {
            return null;
        }
        UUID uuid = tag.getUUID("Captain");
        if (level instanceof ServerLevel server) {
            return server.getEntity(uuid) instanceof ArtilleryCaptainEntity captain && captain.isAlive() ? captain : null;
        }
        return null;
    }

    private static String mapBlockToNbtKey(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.CRAFTING_TABLE) return "Workstation";
        if (b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE || b == Blocks.SMOKER) return "Furnace";
        if (b == Blocks.HOPPER) return "Hopper";
        if (b == Blocks.ENCHANTING_TABLE) return "EnchantingTable";
        if (b == Blocks.COMPOSTER) return "Composter";
        if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL) return "Chest";
        if (b == Blocks.BREWING_STAND) return "BrewingStand";
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(b);
        if (key != null) {
            String id = key.toString();
            if ("goety:cursed_infuser".equals(id)) return "CursedInfuser";
            if ("goety:raiding_chest".equals(id)) return "RaiderChest";
            if ("goety:pedestal".equals(id) || "supplementaries:pedestal".equals(id)) return "Pedestal";
            if ("supplementaries:cannon".equals(id)) return "Cannon";
        }
        return null;
    }
}
