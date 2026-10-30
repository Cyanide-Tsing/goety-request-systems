package com.requiem.servants.logic;

import com.requiem.servants.entity.WorkServantEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 从 KubeJS {@code 00_library.js} 需求系统移植的简化版：
 * 工作仆从向附近其他工作仆从请求材料（从对方背包直接取）。
 */
public final class ServantDemand {
    private static final long DEMAND_COOLDOWN = 40;

    private ServantDemand() {
    }

    public static void requestMaterials(WorkServantEntity entity, String goal, String... items) {
        CompoundTag tag = entity.getPersistentData();
        tag.putString("DemandGoal", goal);
        ListTag list = new ListTag();
        for (String item : items) {
            if (item != null) {
                list.add(StringTag.valueOf(item));
            }
        }
        tag.put("DemandItems", list);
        tag.remove("DemandRetry");
    }

    public static void clearDemand(WorkServantEntity entity) {
        entity.getPersistentData().remove("DemandGoal");
        entity.getPersistentData().remove("DemandItems");
        entity.getPersistentData().remove("DemandRetry");
    }

    public static List<String> demandItems(WorkServantEntity entity) {
        List<String> result = new ArrayList<>();
        ListTag list = entity.getPersistentData().getList("DemandItems", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }

    /**
     * 处理需求。返回 {@code true} 表示正在前往提供者（本 tick 到此为止），
     * {@code false} 表示已取到或暂无提供者（继续其它工作）。
     */
    public static boolean processDemand(WorkServantEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.contains("DemandGoal")) {
            return false;
        }
        List<String> items = demandItems(entity);
        if (items.isEmpty()) {
            clearDemand(entity);
            return false;
        }
        ServerLevel level = (ServerLevel) entity.level();
        long now = level.getGameTime();
        if (now < tag.getLong("DemandRetry")) {
            return false;
        }

        List<WorkServantEntity> providers = new ArrayList<>();
        AABB aabb = new AABB(entity.getX() - 16, entity.getY() - 16, entity.getZ() - 16,
                entity.getX() + 16, entity.getY() + 16, entity.getZ() + 16);
        for (Entity e : level.getEntitiesOfClass(Entity.class, aabb)) {
            if (e instanceof WorkServantEntity ws && ws != entity) {
                providers.add(ws);
            }
        }
        if (providers.isEmpty()) {
            ServantLogic.tellOwner(entity, "需求 " + String.join("/", items) + " 失败：附近无可提供的工作仆从");
            tag.putLong("DemandRetry", now + DEMAND_COOLDOWN);
            return false;
        }
        providers.sort(Comparator.comparingDouble(entity::distanceToSqr));
        WorkServantEntity provider = providers.get(0);

        if (entity.distanceToSqr(provider) > 6.25) {
            entity.getNavigation().moveTo(provider.getX(), provider.getY(), provider.getZ(), 1.0);
            ServantLogic.setTask(entity, "向工作仆从请求 " + String.join("/", items));
            return true;
        }

        for (String item : items) {
            if (provider.removeItem(item, 1) >= 1) {
                Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(item));
                if (it != null) {
                    entity.addItem(new ItemStack(it));
                }
                clearDemand(entity);
                ServantLogic.tellOwner(entity, "从工作仆从取得 " + item);
                return false;
            }
        }
        ServantLogic.tellOwner(entity, "工作仆从无 " + String.join("/", items));
        tag.putLong("DemandRetry", now + DEMAND_COOLDOWN);
        return false;
    }
}
