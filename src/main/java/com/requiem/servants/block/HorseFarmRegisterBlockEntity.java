package com.requiem.servants.block;

import com.requiem.servants.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 马场登记站的方块实体：继承 {@link WorkingBlockEntity}，额外存储马匹繁殖开关。
 */
public class HorseFarmRegisterBlockEntity extends WorkingBlockEntity {
    private static final String KEY_BREEDING = "Breeding";

    private boolean breeding = true;

    public HorseFarmRegisterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HORSE_FARM_REGISTER.get(), pos, state);
    }

    public boolean isBreeding() {
        return this.breeding;
    }

    public void setBreeding(boolean breeding) {
        if (this.breeding != breeding) {
            this.breeding = breeding;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(KEY_BREEDING, this.breeding);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.breeding = !tag.contains(KEY_BREEDING) || tag.getBoolean(KEY_BREEDING);
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
