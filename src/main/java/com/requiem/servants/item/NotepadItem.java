package com.requiem.servants.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 记事本：合并了存储列表、烧炼列表、火炬列表的通用记录工具。
 *
 * <p>蹲下右键方块把该方块记录进记事本（自动记录方块类型），再对同一方块蹲下右键则移除记录。
 * 读取时按方块类型自动归类：
 * <ul>
 *   <li>{@link #getStorage} → 木桶列表 + 箱子列表；</li>
 *   <li>{@link #getFurnaces} → 熔炉列表 + 高炉列表 + 烟熏炉列表；</li>
 *   <li>{@link #getTorches} → 不祥火炬列表。</li>
 * </ul>
 */
public class NotepadItem extends Item {
    private static final String KEY_BLOCKS = "Blocks";

    public NotepadItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // 记录 / 移除
    // ------------------------------------------------------------------

    /** 记录一个方块位置（含类型）。若该位置已存在则返回 {@code false}。 */
    public static boolean add(ItemStack stack, BlockPos pos, BlockState state) {
        if (contains(stack, pos)) {
            return false;
        }
        String id = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ListTag list = stack.getOrCreateTag().getList(KEY_BLOCKS, Tag.TAG_COMPOUND);
        CompoundTag entry = new CompoundTag();
        entry.putInt("x", pos.getX());
        entry.putInt("y", pos.getY());
        entry.putInt("z", pos.getZ());
        entry.putString("id", id);
        list.add(entry);
        stack.getOrCreateTag().put(KEY_BLOCKS, list);
        return true;
    }

    /** 移除一个方块位置，返回是否确实移除。 */
    public static boolean remove(ItemStack stack, BlockPos pos) {
        ListTag list = stack.getOrCreateTag().getList(KEY_BLOCKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getInt("x") == pos.getX() && entry.getInt("y") == pos.getY()
                    && entry.getInt("z") == pos.getZ()) {
                list.remove(i);
                stack.getOrCreateTag().put(KEY_BLOCKS, list);
                return true;
            }
        }
        return false;
    }

    public static boolean contains(ItemStack stack, BlockPos pos) {
        ListTag list = stack.getOrCreateTag().getList(KEY_BLOCKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getInt("x") == pos.getX() && entry.getInt("y") == pos.getY()
                    && entry.getInt("z") == pos.getZ()) {
                return true;
            }
        }
        return false;
    }

    /** 切换：已存在则移除（返回 false），否则记录（返回 true）。 */
    public static boolean toggle(ItemStack stack, BlockPos pos, BlockState state) {
        if (remove(stack, pos)) {
            return false;
        }
        add(stack, pos, state);
        return true;
    }

    // ------------------------------------------------------------------
    // 归类读取
    // ------------------------------------------------------------------

    /** 存储类：木桶列表 + 箱子列表。 */
    public static StorageResult getStorage(ItemStack stack) {
        List<BlockPos> barrels = new ArrayList<>();
        List<BlockPos> chests = new ArrayList<>();
        forEach(stack, (pos, id) -> {
            if ("minecraft:barrel".equals(id)) {
                barrels.add(pos);
            } else if ("minecraft:chest".equals(id) || "minecraft:trapped_chest".equals(id)) {
                chests.add(pos);
            }
        });
        return new StorageResult(barrels, chests);
    }

    /** 烧炼类：熔炉列表 + 高炉列表 + 烟熏炉列表。 */
    public static FurnaceResult getFurnaces(ItemStack stack) {
        List<BlockPos> furnaces = new ArrayList<>();
        List<BlockPos> blastFurnaces = new ArrayList<>();
        List<BlockPos> smokers = new ArrayList<>();
        forEach(stack, (pos, id) -> {
            switch (id) {
                case "minecraft:furnace" -> furnaces.add(pos);
                case "minecraft:blast_furnace" -> blastFurnaces.add(pos);
                case "minecraft:smoker" -> smokers.add(pos);
                default -> {
                }
            }
        });
        return new FurnaceResult(furnaces, blastFurnaces, smokers);
    }

    /** 火炬类：不祥火炬列表。 */
    public static List<BlockPos> getTorches(ItemStack stack) {
        List<BlockPos> torches = new ArrayList<>();
        forEach(stack, (pos, id) -> {
            if ("goety:ominous_pyre".equals(id)) {
                torches.add(pos);
            }
        });
        return torches;
    }

    /** 大炮类：Supplementaries 大炮列表。 */
    public static List<BlockPos> getCannons(ItemStack stack) {
        List<BlockPos> cannons = new ArrayList<>();
        forEach(stack, (pos, id) -> {
            if ("supplementaries:cannon".equals(id)) {
                cannons.add(pos);
            }
        });
        return cannons;
    }

    /** 已记录方块总数。 */
    public static int size(ItemStack stack) {
        return stack.getOrCreateTag().getList(KEY_BLOCKS, Tag.TAG_COMPOUND).size();
    }

    private static void forEach(ItemStack stack, BlockConsumer consumer) {
        ListTag list = stack.getOrCreateTag().getList(KEY_BLOCKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            consumer.accept(pos, entry.getString("id"));
        }
    }

    @FunctionalInterface
    private interface BlockConsumer {
        void accept(BlockPos pos, String blockId);
    }

    public static final class StorageResult {
        public final List<BlockPos> barrels;
        public final List<BlockPos> chests;

        StorageResult(List<BlockPos> barrels, List<BlockPos> chests) {
            this.barrels = barrels;
            this.chests = chests;
        }
    }

    public static final class FurnaceResult {
        public final List<BlockPos> furnaces;
        public final List<BlockPos> blastFurnaces;
        public final List<BlockPos> smokers;

        FurnaceResult(List<BlockPos> furnaces, List<BlockPos> blastFurnaces, List<BlockPos> smokers) {
            this.furnaces = furnaces;
            this.blastFurnaces = blastFurnaces;
            this.smokers = smokers;
        }
    }
}
