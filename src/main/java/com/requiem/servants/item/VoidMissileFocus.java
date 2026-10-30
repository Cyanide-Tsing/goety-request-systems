package com.requiem.servants.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.requiem.servants.magic.spells.VoidMissileSpell;

public class VoidMissileFocus extends MagicFocus {
    public VoidMissileFocus() {
        super(new VoidMissileSpell());
    }
}
