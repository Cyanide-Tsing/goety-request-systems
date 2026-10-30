package com.requiem.servants.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.requiem.servants.magic.spells.HealingAuraSpell;

public class HealingAuraFocus extends MagicFocus {
    public HealingAuraFocus() {
        super(new HealingAuraSpell());
    }
}
