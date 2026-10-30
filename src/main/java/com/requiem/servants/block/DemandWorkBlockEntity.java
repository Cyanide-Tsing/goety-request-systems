package com.requiem.servants.block;

import com.requiem.servants.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 带需求系统的工作方块：在委任状槽之外，额外提供一个"需求槽"和"需求开关"。
 *
 * <p>玩家把想要制作的成品放入需求槽（类似基座），仆从读取需求槽并按配方制作后交付；
 * 需求开关决定是否响应玩家的需求。
 */
public class DemandWorkBlockEntity extends WorkingBlockEntity {
    private static final String KEY_DEMAND_ENABLED = "DemandEnabled";

    private final SimpleContainer demand = new SimpleContainer(1);
    private boolean demandEnabled = true;

    public DemandWorkBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.DEMAND_WORK.get(), pos, state);
    }

    public DemandWorkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Container getDemandContainer() {
        return this.demand;
    }

    public boolean isDemandEnabled() {
        return this.demandEnabled;
    }

    public void setDemandEnabled(boolean enabled) {
        if (this.demandEnabled != enabled) {
            this.demandEnabled = enabled;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ItemStack d = this.demand.getItem(0);
        if (!d.isEmpty()) {
            tag.put("DemandItem", d.save(new CompoundTag()));
        }
        tag.putBoolean(KEY_DEMAND_ENABLED, this.demandEnabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.demand.clearContent();
        this.demand.setItem(0, tag.contains("DemandItem")
                ? ItemStack.of(tag.getCompound("DemandItem")) : ItemStack.EMPTY);
        this.demandEnabled = !tag.contains(KEY_DEMAND_ENABLED) || tag.getBoolean(KEY_DEMAND_ENABLED);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
