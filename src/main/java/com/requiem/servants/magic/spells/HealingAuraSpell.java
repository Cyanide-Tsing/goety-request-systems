package com.requiem.servants.magic.spells;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.enchantments.ModEnchantments;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.MobUtil;
import com.Polarice3.Goety.utils.SEHelper;
import com.Polarice3.Goety.utils.WandUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;

public class HealingAuraSpell extends Spell {
    @Override
    public int defaultSoulCost() {
        return 30;
    }

    @Override
    public int defaultCastDuration() {
        return 40;
    }

    @Override
    public int defaultSpellCooldown() {
        return 200;
    }

    @Override
    public SoundEvent CastingSound() {
        return ModSounds.PREPARE_SPELL.get();
    }

    @Override
    public SpellType getSpellType() {
        return SpellType.NONE;
    }

    @Override
    public List<Enchantment> acceptedEnchantments() {
        List<Enchantment> list = new ArrayList<>();
        list.add(ModEnchantments.POTENCY.get());
        return list;
    }

    @Override
    public void SpellResult(ServerLevel worldIn, LivingEntity caster, ItemStack staff, SpellStat spellStat) {
        int potency = 0;
        if (WandUtil.enchantedFocus(caster)) {
            potency = WandUtil.getLevels(ModEnchantments.POTENCY.get(), caster);
        }
        float heal = 8.0F * (potency + 1);
        AABB aabb = caster.getBoundingBox().inflate(8.0);
        for (LivingEntity living : worldIn.getEntitiesOfClass(LivingEntity.class, aabb, e -> this.shouldHeal(caster, e))) {
            living.heal(heal);
            worldIn.sendParticles(ParticleTypes.HEART, living.getX(), living.getEyeY(), living.getZ(), 1, 0.3, 0.5, 0.3, 0.0);
        }
        this.playSound(worldIn, caster, ModSounds.SOUL_HEAL.get(), 1.0F, 1.0F);
    }

    private boolean shouldHeal(LivingEntity caster, Entity entity) {
        if (entity instanceof Player) {
            return true;
        }
        if (entity instanceof LivingEntity living && MobUtil.getOwner(living) == caster) {
            return true;
        }
        return caster instanceof Player player && entity instanceof LivingEntity living
                && SEHelper.isAlly(player, living);
    }
}
