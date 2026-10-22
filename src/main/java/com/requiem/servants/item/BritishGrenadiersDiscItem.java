package com.requiem.servants.item;

import com.requiem.servants.ModSounds;
import net.minecraft.world.item.RecordItem;

/**
 * 音乐唱片：不列颠掷弹兵（The British Grenadiers），演奏者美国军乐队。
 *
 * <p>暂无获取方式，也不进帕秋莉手册；只作为音乐唱片注册进游戏。
 */
public class BritishGrenadiersDiscItem extends RecordItem {
    public BritishGrenadiersDiscItem(Properties properties) {
        super(13, ModSounds.BRITISH_GRENADIERS.get(), properties, 104);
    }
}
