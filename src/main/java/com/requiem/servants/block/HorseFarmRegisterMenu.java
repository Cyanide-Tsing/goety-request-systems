package com.requiem.servants.block;

import com.requiem.servants.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 马场登记站的容器菜单：一个委任状槽位 + 繁殖开关按钮。
 */
public class HorseFarmRegisterMenu extends AbstractContainerMenu {
    private final HorseFarmRegisterBlockEntity blockEntity;

    public HorseFarmRegisterMenu(int id, Inventory inventory, HorseFarmRegisterBlockEntity blockEntity) {
        super(ModMenuTypes.HORSE_FARM_REGISTER.get(), id);
        this.blockEntity = blockEntity;
        this.addSlot(new Slot(blockEntity, 0, 80, 96));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 197));
        }
    }

    public HorseFarmRegisterBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public boolean isBreeding() {
        return this.blockEntity.isBreeding();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == 0) {
            this.blockEntity.setBreeding(!this.blockEntity.isBreeding());
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
        if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
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
