package com.requiem.servants.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.requiem.servants.magic.spells.CurrentThrowSpell;

public class CurrentThrowFocus extends MagicFocus {
    public CurrentThrowFocus() {
        super(new CurrentThrowSpell());
    }
}
