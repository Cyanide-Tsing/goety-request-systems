package com.requiem.servants.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.requiem.servants.magic.spells.SnarelingWebSpell;

public class SnarelingWebFocus extends MagicFocus {
    public SnarelingWebFocus() {
        super(new SnarelingWebSpell());
    }
}
