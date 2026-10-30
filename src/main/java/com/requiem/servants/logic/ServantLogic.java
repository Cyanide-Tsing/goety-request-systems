package com.requiem.servants.logic;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant;
import com.google.gson.Gson;
import com.requiem.servants.ModEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 从 KubeJS {@code 00_library.js} 移植的共享工具。
 *
 * <p>仆从状态（任务、位置、命名等）统一存于 {@code entity.getPersistentData()}（ForgeData），
 * 替代原先 KubeJS 的 {@code entity.persistentData}。
 */
public final class ServantLogic {
    public static final int FIRST_NAME_COUNT = 55;
    public static final int LAST_NAME_COUNT = 55;
    public static final int NAME_COUNT = FIRST_NAME_COUNT * LAST_NAME_COUNT;
    private static final int[] RECRUITER_TITLE_WEIGHTS = {10, 9, 7, 6, 5, 3, 10, 9, 8, 7, 5, 4, 3, 2, 2};
    private static final Gson GSON = new Gson();

    private ServantLogic() {
    }

    // ---------- 列表物品（torch_list / store_list / smelt_list 等，NBT 存 JSON） ----------

    public static List<BlockPos> readListPositions(ItemStack stack) {
        List<BlockPos> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return result;
        }
        String s = tag.getString("positions");
        if (s.isEmpty()) {
            return result;
        }
        try {
            int[][] arr = GSON.fromJson(s, int[][].class);
            for (int[] a : arr) {
                if (a.length == 3) {
                    result.add(new BlockPos(a[0], a[1], a[2]));
                }
            }
        } catch (RuntimeException e) {
            // 忽略损坏数据
        }
        return result;
    }

    public static void writeListPositions(ItemStack stack, List<BlockPos> positions) {
        int[][] arr = new int[positions.size()][3];
        for (int i = 0; i < positions.size(); i++) {
            arr[i][0] = positions.get(i).getX();
            arr[i][1] = positions.get(i).getY();
            arr[i][2] = positions.get(i).getZ();
        }
        stack.getOrCreateTag().putString("positions", GSON.toJson(arr));
    }

    public static List<BlockPos> addListPosition(ItemStack stack, BlockPos pos) {
        List<BlockPos> arr = readListPositions(stack);
        for (BlockPos q : arr) {
            if (q.equals(pos)) {
                return arr;
            }
        }
        arr.add(pos);
        writeListPositions(stack, arr);
        return arr;
    }

    // ---------- 任务 ----------

    public static void setTask(LivingEntity entity, String task) {
        CompoundTag tag = entity.getPersistentData();
        if (!task.equals(tag.getString("currentTask"))) {
            tag.putString("currentTask", task);
        }
    }

    // ---------- 方块位置（NBT） ----------

    public static List<BlockPos> readBlockPosList(LivingEntity entity, String key) {
        List<BlockPos> list = new ArrayList<>();
        ListTag tag = entity.getPersistentData().getList(key, Tag.TAG_INT_ARRAY);
        for (int i = 0; i < tag.size(); i++) {
            int[] a = tag.getIntArray(i);
            if (a.length == 3) {
                list.add(new BlockPos(a[0], a[1], a[2]));
            }
        }
        return list;
    }

    public static void writeBlockPosList(LivingEntity entity, String key, List<BlockPos> positions) {
        ListTag tag = new ListTag();
        for (BlockPos p : positions) {
            tag.add(new IntArrayTag(new int[]{p.getX(), p.getY(), p.getZ()}));
        }
        entity.getPersistentData().put(key, tag);
    }

    public static BlockPos readBlockPos(LivingEntity entity, String key) {
        CompoundTag tag = entity.getPersistentData();
        if (tag.contains(key, Tag.TAG_INT_ARRAY)) {
            int[] a = tag.getIntArray(key);
            if (a.length == 3) {
                return new BlockPos(a[0], a[1], a[2]);
            }
        }
        return null;
    }

    public static void writeBlockPos(LivingEntity entity, String key, BlockPos pos) {
        if (pos == null) {
            entity.getPersistentData().remove(key);
        } else {
            entity.getPersistentData().put(key, new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
    }

    // ---------- 命名 ----------

    /** 随机命名（只在未命名时执行一次）。 */
    public static void assignServantName(AbstractIllagerServant servant) {
        if (servant.getPersistentData().contains("ServantNameIndex")) {
            return;
        }
        LivingEntity owner = servant.getTrueOwner();
        boolean[] used = new boolean[NAME_COUNT];
        if (owner != null) {
            Level level = servant.level();
            List<AbstractIllagerServant> nearby = level.getEntitiesOfClass(AbstractIllagerServant.class,
                    new AABB(servant.getX() - 256, servant.getY() - 256, servant.getZ() - 256,
                            servant.getX() + 256, servant.getY() + 256, servant.getZ() + 256),
                    e -> e != servant && isWorkServantType(e.getType()) && e.getTrueOwner() == owner);
            for (AbstractIllagerServant e : nearby) {
                int idx = e.getPersistentData().getInt("ServantNameIndex");
                if (idx >= 0 && idx < NAME_COUNT) {
                    used[idx] = true;
                }
            }
        }
        int idx = servant.getRandom().nextInt(NAME_COUNT);
        int probe = 0;
        while (used[idx] && probe < NAME_COUNT) {
            idx = (idx + 1) % NAME_COUNT;
            probe++;
        }
        servant.getPersistentData().putInt("ServantNameIndex", idx);
        int titleIdx = titleIndexFor(servant.getType());
        servant.getPersistentData().putInt("ServantTitleIndex", titleIdx);
        servant.setCustomName(servantFullName(idx, titleIdx));
        servant.setCustomNameVisible(true);
    }

    public static MutableComponent servantFullName(int idx, int titleIdx) {
        int i = Math.abs(idx) % NAME_COUNT;
        MutableComponent name = Component.translatable("requiem.servant.first_name." + (i / LAST_NAME_COUNT))
                .append(" ")
                .append(Component.translatable("requiem.servant.last_name." + (i % LAST_NAME_COUNT)));
        if (titleIdx >= 0) {
            return Component.translatable("requiem.servant.title." + titleIdx).append(" ").append(name);
        }
        return name;
    }

    public static MutableComponent servantLabel(AbstractIllagerServant servant) {
        int idx = servant.getPersistentData().getInt("ServantNameIndex");
        int titleIdx = servant.getPersistentData().contains("ServantTitleIndex")
                ? servant.getPersistentData().getInt("ServantTitleIndex")
                : -1;
        return Component.translatable(typeKey(servant.getType())).append("·").append(servantFullName(idx, titleIdx));
    }

    private static int titleIndexFor(EntityType<?> type) {
        if (type == ModEntities.RECRUITER.get()) {
            return pickRecruiterTitle();
        }
        if (type == ModEntities.ARTILLERY.get()) {
            return 15;
        }
        if (type == ModEntities.GRENADIER.get()) {
            return 16;
        }
        if (type == ModEntities.ARTILLERY_CAPTAIN.get()) {
            return 17 + (int) (Math.random() * 3);
        }
        if (type == ModEntities.CAPTAIN.get()) {
            return 20;
        }
        if (type == ModEntities.CAVALRY_CAPTAIN.get()) {
            return 21;
        }
        return -1;
    }

    private static int pickRecruiterTitle() {
        int total = 0;
        for (int w : RECRUITER_TITLE_WEIGHTS) {
            total += w;
        }
        int r = (int) (Math.random() * total);
        int acc = 0;
        for (int i = 0; i < RECRUITER_TITLE_WEIGHTS.length; i++) {
            acc += RECRUITER_TITLE_WEIGHTS[i];
            if (r < acc) {
                return i;
            }
        }
        return 0;
    }

    private static boolean isWorkServantType(EntityType<?> type) {
        return type == ModEntities.APPRENTICE.get() || type == ModEntities.GARDENER.get()
                || type == ModEntities.RECRUITER.get() || type == ModEntities.FLETCHER.get();
    }

    private static String typeKey(EntityType<?> type) {
        if (type == ModEntities.GARDENER.get()) return "requiem.servant.type.gardener";
        if (type == ModEntities.RECRUITER.get()) return "requiem.servant.type.recruiter";
        if (type == ModEntities.ARTILLERY.get()) return "requiem.servant.type.artillery";
        if (type == ModEntities.ARTILLERY_HOSTILE.get()) return "requiem.servant.type.artillery_hostile";
        if (type == ModEntities.GRENADIER.get()) return "requiem.servant.type.grenadier";
        if (type == ModEntities.GRENADIER_HOSTILE.get()) return "requiem.servant.type.grenadier_hostile";
        if (type == ModEntities.CONQUEROR.get()) return "requiem.servant.type.conqueror";
        if (type == ModEntities.CAPTAIN.get()) return "requiem.servant.type.captain";
        if (type == ModEntities.ARTILLERY_CAPTAIN.get()) return "requiem.servant.type.artillery_captain";
        if (type == ModEntities.DRUMMER.get()) return "requiem.servant.type.drummer";
        if (type == ModEntities.LIGHT_CAVALRY.get()) return "requiem.servant.type.light_cavalry";
        if (type == ModEntities.CAVALRY_CAPTAIN.get()) return "requiem.servant.type.cavalry_captain";
        if (type == ModEntities.GROOM.get()) return "requiem.servant.type.groom";
        if (type == ModEntities.FLETCHER.get()) return "requiem.servant.type.fletcher";
        return "requiem.servant.type.apprentice";
    }

    // ---------- 反馈 ----------

    public static boolean servantInfoEnabled(Player player) {
        CompoundTag tag = player.getPersistentData();
        return !tag.contains("servantInfo") || tag.getBoolean("servantInfo");
    }

    public static void tellOwner(AbstractIllagerServant servant, String msg) {
        LivingEntity owner = servant.getTrueOwner();
        if (!(owner instanceof Player player) || !servantInfoEnabled(player)) {
            return;
        }
        player.displayClientMessage(Component.literal("§7[").append(servantLabel(servant)).append("] " + msg), true);
    }
}
