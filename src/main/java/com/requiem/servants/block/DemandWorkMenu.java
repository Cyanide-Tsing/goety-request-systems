package com.requiem.servants.block;

import com.requiem.servants.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 需求型工作方块的容器菜单：委任状槽 + 需求槽 + 玩家背包 + 需求开关按钮。
 */
public class DemandWorkMenu extends AbstractContainerMenu {
    private final DemandWorkBlockEntity blockEntity;

    public DemandWorkMenu(int id, Inventory inventory, DemandWorkBlockEntity blockEntity) {
        super(ModMenuTypes.DEMAND_WORK.get(), id);
        this.blockEntity = blockEntity;
        this.addSlot(new Slot(blockEntity, 0, 80, 96));
        this.addSlot(new Slot(blockEntity.getDemandContainer(), 0, 80, 32));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 197));
        }
    }

    public DemandWorkBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public boolean isDemandEnabled() {
        return this.blockEntity.isDemandEnabled();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == 0) {
            this.blockEntity.setDemandEnabled(!this.blockEntity.isDemandEnabled());
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!this.moveItemStackTo(stack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
