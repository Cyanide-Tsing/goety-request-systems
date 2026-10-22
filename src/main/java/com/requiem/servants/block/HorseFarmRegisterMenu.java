package com.requiem.servants.block;

import com.requiem.servants.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 马场登记站的容器菜单：没有物品槽，只有繁殖开关按钮。
 */
public class HorseFarmRegisterMenu extends AbstractContainerMenu {
    private final HorseFarmRegisterBlockEntity blockEntity;

    public HorseFarmRegisterMenu(int id, Inventory inventory, HorseFarmRegisterBlockEntity blockEntity) {
        super(ModMenuTypes.HORSE_FARM_REGISTER.get(), id);
        this.blockEntity = blockEntity;
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
