package com.requiem.servants.block;

import com.requiem.servants.item.AppointmentItem;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 工作方块基类：管理并储存该工作方块对应的仆从。
 *
 * <p>含一个"委任状"槽位，放入绑定了仆从的委任状即完成工作分配；
 * 后续的工作方块（马场登记站等）继承本类，并各自拥有 GUI。
 */
public abstract class WorkingBlockEntity extends BlockEntity implements Container {
    private static final String TAG_SERVANT = "AssignedServant";

    private final SimpleContainer appointment = new SimpleContainer(1);
    private UUID assignedServantId;

    protected WorkingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Container getAppointmentContainer() {
        return this.appointment;
    }

    public UUID getAssignedServantId() {
        return this.assignedServantId;
    }

    protected void refreshAssignment() {
        ItemStack stack = this.appointment.getItem(0);
        this.assignedServantId = AppointmentItem.getBoundServantId(stack);
        this.setChanged();
    }

    // ---------- Container ----------

    @Override
    public int getContainerSize() {
        return this.appointment.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.appointment.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.appointment.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = this.appointment.removeItem(slot, amount);
        this.refreshAssignment();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = this.appointment.removeItemNoUpdate(slot);
        this.refreshAssignment();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.appointment.setItem(slot, stack);
        this.refreshAssignment();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.appointment.clearContent();
        this.refreshAssignment();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ItemStack appt = this.appointment.getItem(0);
        if (!appt.isEmpty()) {
            tag.put("AppointmentItem", appt.save(new CompoundTag()));
        }
        if (this.assignedServantId != null) {
            tag.putUUID(TAG_SERVANT, this.assignedServantId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.appointment.clearContent();
        this.appointment.setItem(0, tag.contains("AppointmentItem")
                ? ItemStack.of(tag.getCompound("AppointmentItem")) : ItemStack.EMPTY);
        this.assignedServantId = tag.hasUUID(TAG_SERVANT) ? tag.getUUID(TAG_SERVANT) : null;
    }
}
