package com.requiem.servants.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.requiem.servants.magic.spells.ChainLightningSpell;

public class ChainLightningFocus extends MagicFocus {
    public ChainLightningFocus() {
        super(new ChainLightningSpell());
    }
}
