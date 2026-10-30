package com.requiem.servants.magic.spells;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.enchantments.ModEnchantments;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.WandUtil;
import com.requiem.servants.entity.projectile.RequiemSnareWeb;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

public class SnarelingWebSpell extends Spell {
    @Override
    public int defaultSoulCost() {
        return 10;
    }

    @Override
    public int defaultCastDuration() {
        return 0;
    }

    @Override
    public int defaultSpellCooldown() {
        return 50;
    }

    @Override
    public SoundEvent CastingSound() {
        return ModSounds.PREPARE_SPELL.get();
    }

    @Override
    public SpellType getSpellType() {
        return SpellType.VOID;
    }

    @Override
    public List<Enchantment> acceptedEnchantments() {
        List<Enchantment> list = new ArrayList<>();
        list.add(ModEnchantments.DURATION.get());
        list.add(ModEnchantments.RADIUS.get());
        return list;
    }

    @Override
    public void SpellResult(ServerLevel worldIn, LivingEntity caster, ItemStack staff, SpellStat spellStat) {
        int duration = 0;
        int radius = 0;
        if (WandUtil.enchantedFocus(caster)) {
            duration = WandUtil.getLevels(ModEnchantments.DURATION.get(), caster);
            radius = WandUtil.getLevels(ModEnchantments.RADIUS.get(), caster);
        }
        int lifeSpanTicks = 60 + duration * 20;
        float inflation = radius * 0.5F;

        Vec3 view = caster.getViewVector(1.0F);
        RequiemSnareWeb web = new RequiemSnareWeb(worldIn,
                caster.getX() + view.x / 2.0, caster.getEyeY() - 0.2, caster.getZ() + view.z / 2.0,
                view.x, view.y, view.z);
        web.configure(lifeSpanTicks, inflation);
        web.setOwner(caster);
        web.setDeltaMovement(view.scale(1.2F));
        worldIn.addFreshEntity(web);
        this.playSound(worldIn, (Entity) caster, ModSounds.CAST_SPELL.get(), 1.0F, this.projPitch(worldIn.getRandom()));
    }
}
