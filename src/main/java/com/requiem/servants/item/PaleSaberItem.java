package com.requiem.servants.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 苍白军刀：炮兵、掷弹兵、炮兵队长与连队长的近战武器。
 *
 * <p>攻击伤害 7.0，攻击速度 2.5，耐久 962。
 */
public class PaleSaberItem extends SwordItem {
    private static final Tier PALE_TIER = new Tier() {
        @Override
        public int getUses() {
            return 962;
        }

        @Override
        public float getSpeed() {
            return 8.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 6.0F;
        }

        @Override
        public int getLevel() {
            return 2;
        }

        @Override
        public int getEnchantmentValue() {
            return 14;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    public PaleSaberItem(Properties properties) {
        super(PALE_TIER, 0, -1.5F, properties);
    }
}
