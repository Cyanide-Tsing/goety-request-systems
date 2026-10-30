package com.requiem.servants.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.requiem.servants.magic.spells.AcidBallSpell;

public class AcidBallFocus extends MagicFocus {
    public AcidBallFocus() {
        super(new AcidBallSpell());
    }
}
