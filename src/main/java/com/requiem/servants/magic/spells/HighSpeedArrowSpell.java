package com.requiem.servants.magic.spells;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.enchantments.ModEnchantments;
import com.Polarice3.Goety.common.entities.projectiles.RainArrow;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.WandUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class HighSpeedArrowSpell extends Spell {
    @Override
    public int defaultSoulCost() {
        return 5;
    }

    @Override
    public int defaultCastDuration() {
        return 0;
    }

    @Override
    public int defaultSpellCooldown() {
        return 30;
    }

    @Override
    public SoundEvent CastingSound() {
        return ModSounds.PREPARE_SPELL.get();
    }

    @Override
    public SpellType getSpellType() {
        return SpellType.WIND;
    }

    @Override
    public List<Enchantment> acceptedEnchantments() {
        List<Enchantment> list = new ArrayList<>();
        list.add(ModEnchantments.VELOCITY.get());
        list.add(ModEnchantments.POTENCY.get());
        return list;
    }

    @Override
    public void SpellResult(ServerLevel worldIn, LivingEntity caster, ItemStack staff, SpellStat spellStat) {
        int potency = 0;
        int velocity = 0;
        if (WandUtil.enchantedFocus(caster)) {
            potency = WandUtil.getLevels(ModEnchantments.POTENCY.get(), caster);
            velocity = WandUtil.getLevels(ModEnchantments.VELOCITY.get(), caster);
        }
        float arrowVelocity = 2.0F * (1.0F + 0.4F * velocity);

        RainArrow arrow = new RainArrow(worldIn, caster);
        arrow.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, arrowVelocity, 1.0F);
        arrow.setPierceLevel((byte) potency);
        worldIn.addFreshEntity(arrow);
        this.playSound(worldIn, (Entity) caster, ModSounds.CAST_SPELL.get(), 1.0F, this.projPitch(worldIn.getRandom()));
    }
}
