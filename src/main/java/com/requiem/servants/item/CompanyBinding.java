package com.requiem.servants.item;

import com.requiem.servants.entity.CaptainEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 指挥类物品与连队长的绑定关系，存在物品 NBT 里。
 *
 * <p>连旗与突击令共用这套逻辑。绑定信息放在 NBT 的好处是客户端也能判断"是否已绑定"，
 * 于是两端可以返回一致的交互结果，不需要额外的网络包。
 */
public final class CompanyBinding {
    public static final String TAG_CAPTAIN = "Captain";
    public static final String TAG_CAPTAIN_NAME = "CaptainName";

    private CompanyBinding() {
    }

    public static void bind(ItemStack stack, CaptainEntity captain) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_CAPTAIN, captain.getUUID());
        tag.putString(TAG_CAPTAIN_NAME, captain.getDisplayName().getString());
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_CAPTAIN);
    }

    /** 只能在服务端解析出实体。 */
    public static CaptainEntity resolve(ItemStack stack, Level level) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_CAPTAIN)) {
            return null;
        }
        return server.getEntity(tag.getUUID(TAG_CAPTAIN)) instanceof CaptainEntity captain && captain.isAlive()
                ? captain
                : null;
    }
}
