package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 工作仆从基类：拥有真实背包、捡拾物品与容器存取能力。
 *
 * <p>此前工作仆从的"背包"是 KubeJS 里存在 NBT 里的 JSON 字符串模拟；这里改成真正的
 * {@link SimpleContainer} 背包（{@value #INVENTORY_SIZE} 格），并提供：
 * <ul>
 *   <li>真实背包：{@link #getWorkInventory()}、NBT 持久化；</li>
 *   <li>捡拾物品：内置 {@link PickUpGoal}，自动捡起附近掉落物；</li>
 *   <li>容器存取：{@link #depositToContainer}/{@link #withdrawFromContainer}；</li>
 *   <li>地图视图：{@link #getInventoryMap}/{@link #setInventoryFromMap}（供 KubeJS 兼容层调用）。</li>
 * </ul>
 */
public abstract class WorkServantEntity extends VindicatorServant {
    public static final int INVENTORY_SIZE = 27;

    private final SimpleContainer workInventory = new SimpleContainer(INVENTORY_SIZE);

    protected WorkServantEntity(EntityType<? extends WorkServantEntity> type, Level level) {
        super(type, level);
    }

    public SimpleContainer getWorkInventory() {
        return this.workInventory;
    }

    // ---------- 背包内容 ----------

    /** 以 itemId -> 数量 的映射返回背包内容（供 KubeJS 读）。 */
    public Map<String, Integer> getInventoryMap() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < this.workInventory.getContainerSize(); i++) {
            ItemStack s = this.workInventory.getItem(i);
            if (!s.isEmpty()) {
                String id = ForgeRegistries.ITEMS.getKey(s.getItem()).toString();
                map.merge(id, s.getCount(), Integer::sum);
            }
        }
        return map;
    }

    /** 用 itemId -> 数量 的映射重建背包（供 KubeJS 写回）。 */
    public void setInventoryFromMap(Map<String, ? extends Number> map) {
        this.workInventory.clearContent();
        int slot = 0;
        for (Map.Entry<String, ? extends Number> e : map.entrySet()) {
            Number value = e.getValue();
            int remaining = value == null ? 0 : value.intValue();
            if (remaining <= 0) {
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(e.getKey()));
            if (item == null) {
                continue;
            }
            while (remaining > 0 && slot < this.workInventory.getContainerSize()) {
                int put = Math.min(remaining, item.getMaxStackSize());
                this.workInventory.setItem(slot++, new ItemStack(item, put));
                remaining -= put;
            }
        }
    }

    /** 向背包加入物品，返回未能放入的数量。 */
    public int addItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < this.workInventory.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slot = this.workInventory.getItem(i);
            if (slot.isEmpty()) {
                this.workInventory.setItem(i, stack.copy());
                stack.setCount(0);
            } else if (ItemStack.isSameItemSameTags(slot, stack)) {
                int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(move);
                stack.shrink(move);
            }
        }
        return stack.getCount();
    }

    /** 从背包移除指定 id 的物品，返回移除的数量。 */
    public int removeItem(String id, int count) {
        int removed = 0;
        for (int i = 0; i < this.workInventory.getContainerSize() && removed < count; i++) {
            ItemStack s = this.workInventory.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (key != null && key.toString().equals(id)) {
                int take = Math.min(s.getCount(), count - removed);
                s.shrink(take);
                removed += take;
                if (s.isEmpty()) {
                    this.workInventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        return removed;
    }

    public boolean hasItem(String id, int count) {
        int total = 0;
        for (int i = 0; i < this.workInventory.getContainerSize() && total < count; i++) {
            ItemStack s = this.workInventory.getItem(i);
            if (!s.isEmpty()) {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(s.getItem());
                if (key != null && key.toString().equals(id)) {
                    total += s.getCount();
                }
            }
        }
        return total >= count;
    }

    public int countItem(String id) {
        int total = 0;
        for (int i = 0; i < this.workInventory.getContainerSize(); i++) {
            ItemStack s = this.workInventory.getItem(i);
            if (!s.isEmpty()) {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(s.getItem());
                if (key != null && key.toString().equals(id)) {
                    total += s.getCount();
                }
            }
        }
        return total;
    }

    // ---------- 捡拾物品 ----------

    /** 捡起附近掉落物，返回捡起的物品总数。 */
    public int pickUpNearbyItems(double radius) {
        AABB aabb = new AABB(this.getX() - radius, this.getY() - radius, this.getZ() - radius,
                this.getX() + radius, this.getY() + radius, this.getZ() + radius);
        int picked = 0;
        for (ItemEntity itemEntity : this.level().getEntitiesOfClass(ItemEntity.class, aabb)) {
            if (!itemEntity.isAlive()) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            int leftover = this.addItem(stack);
            if (leftover <= 0) {
                picked += stack.getCount();
                itemEntity.discard();
            } else {
                picked += stack.getCount() - leftover;
                stack.setCount(leftover);
            }
        }
        return picked;
    }

    // ---------- 容器存取 ----------

    /** 把物品存进容器，返回未能存入的数量。 */
    public int depositToContainer(Container container, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            if (!container.canPlaceItem(i, stack)) {
                continue;
            }
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stack.copy());
                stack.setCount(0);
            } else if (ItemStack.isSameItemSameTags(slot, stack)) {
                int move = Math.min(stack.getCount(), Math.min(slot.getMaxStackSize(), container.getMaxStackSize()) - slot.getCount());
                if (move > 0) {
                    slot.grow(move);
                    stack.shrink(move);
                }
            }
        }
        return stack.getCount();
    }

    /** 从容器取出指定 id 的物品，返回取出的数量。 */
    public int withdrawFromContainer(Container container, String id, int count) {
        int removed = 0;
        for (int i = 0; i < container.getContainerSize() && removed < count; i++) {
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (key != null && key.toString().equals(id)) {
                int take = Math.min(s.getCount(), count - removed);
                ItemStack moved = s.split(take);
                int leftover = this.addItem(moved);
                if (leftover > 0) {
                    // 放不回去则归还容器
                    s.grow(leftover);
                    break;
                }
                removed += take;
                if (s.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        return removed;
    }

    // ---------- 目标与姿势 ----------

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new PickUpGoal(this));
    }

    @Override
    public void targetSelectGoal() {
    }

    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    // ---------- 持久化 ----------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < this.workInventory.getContainerSize(); i++) {
            ItemStack s = this.workInventory.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                s.save(itemTag);
                list.add(itemTag);
            }
        }
        tag.put("WorkInventory", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.workInventory.clearContent();
        ListTag list = tag.getList("WorkInventory", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < this.workInventory.getContainerSize()) {
                this.workInventory.setItem(slot, ItemStack.of(itemTag));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide()) {
            for (int i = 0; i < this.workInventory.getContainerSize(); i++) {
                ItemStack stack = this.workInventory.getItem(i);
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(stack);
                    this.workInventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        super.die(cause);
    }

    /** 捡起附近掉落物到工作背包。 */
    private static class PickUpGoal extends Goal {
        private final WorkServantEntity servant;
        private ItemEntity target;

        PickUpGoal(WorkServantEntity servant) {
            this.servant = servant;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.servant.isGuardingArea()) {
                return false;
            }
            AABB aabb = new AABB(this.servant.getX() - 8, this.servant.getY() - 4, this.servant.getZ() - 8,
                    this.servant.getX() + 8, this.servant.getY() + 4, this.servant.getZ() + 8);
            double best = Double.MAX_VALUE;
            for (ItemEntity itemEntity : this.servant.level().getEntitiesOfClass(ItemEntity.class, aabb)) {
                if (!itemEntity.isAlive()) {
                    continue;
                }
                double d = this.servant.distanceToSqr(itemEntity);
                if (d < best) {
                    best = d;
                    this.target = itemEntity;
                }
            }
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.target.isAlive()
                    && this.servant.distanceToSqr(this.target) <= 64.0
                    && this.servant.isGuardingArea();
        }

        @Override
        public void tick() {
            this.servant.getNavigation().moveTo(this.target, 1.0);
            if (this.servant.distanceToSqr(this.target) <= 2.25) {
                ItemStack stack = this.target.getItem();
                int leftover = this.servant.addItem(stack);
                if (leftover <= 0) {
                    this.target.discard();
                } else {
                    stack.setCount(leftover);
                }
                this.target = null;
            }
        }

        @Override
        public void stop() {
            this.target = null;
        }
    }
}
