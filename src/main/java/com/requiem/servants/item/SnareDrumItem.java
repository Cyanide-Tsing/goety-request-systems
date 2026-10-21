package com.requiem.servants.item;

import net.minecraft.world.item.Item;

/**
 * 小军鼓：鼓手仆从的制式乐器。
 *
 * <p>本身只是个持在手上的物品，鼓点与增益由连队长的鼓手槽逻辑驱动
 * （见 {@link com.requiem.servants.command.CompanyController}）。
 */
public class SnareDrumItem extends Item {
    public SnareDrumItem(Properties properties) {
        super(properties);
    }
}
