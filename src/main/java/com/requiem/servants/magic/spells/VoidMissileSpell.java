package com.requiem.servants.magic.spells;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.effects.GoetyEffects;
import com.Polarice3.Goety.common.enchantments.ModEnchantments;
import com.Polarice3.Goety.common.items.ModItems;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.WandUtil;
import com.requiem.servants.entity.projectile.RequiemBolt;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

public class VoidMissileSpell extends Spell {
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
        return SpellType.VOID;
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
        boolean voidWand = staff.is(ModItems.VOID_STAFF.get());
        float damage = 5.0F + 2.0F * potency;
        MobEffect effect = voidWand ? GoetyEffects.VOID_TOUCHED.get() : null;

        Vec3 view = caster.getViewVector(1.0F);
        RequiemBolt bolt = new RequiemBolt(worldIn,
                caster.getX() + view.x / 2.0, caster.getEyeY() - 0.2, caster.getZ() + view.z / 2.0,
                view.x, view.y, view.z);
        bolt.setOwner(caster);
        bolt.configure(1, damage, 0.0F, effect, 200, 0, true);
        bolt.setDeltaMovement(view.scale(1.8F + velocity * 0.25F));
        worldIn.addFreshEntity(bolt);
        this.playSound(worldIn, (Entity) caster, ModSounds.CAST_SPELL.get(), 1.0F, this.projPitch(worldIn.getRandom()));
    }
}
