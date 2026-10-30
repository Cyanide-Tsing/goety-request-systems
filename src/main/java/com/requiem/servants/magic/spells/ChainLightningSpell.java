package com.requiem.servants.magic.spells;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.effects.GoetyEffects;
import com.Polarice3.Goety.common.enchantments.ModEnchantments;
import com.Polarice3.Goety.common.items.ModItems;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.Polarice3.Goety.common.network.ModNetwork;
import com.Polarice3.Goety.common.network.server.SThunderBoltPacket;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.ColorUtil;
import com.Polarice3.Goety.utils.MathHelper;
import com.Polarice3.Goety.utils.MobUtil;
import com.Polarice3.Goety.utils.ModDamageSource;
import com.Polarice3.Goety.utils.Vec3Util;
import com.Polarice3.Goety.utils.WandUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChainLightningSpell extends Spell {
    @Override
    public int defaultSoulCost() {
        return 20;
    }

    @Override
    public int defaultCastDuration() {
        return 0;
    }

    @Override
    public int defaultSpellCooldown() {
        return 160;
    }

    @Override
    public SoundEvent CastingSound() {
        return ModSounds.PREPARE_SPELL.get();
    }

    @Override
    public SpellType getSpellType() {
        return SpellType.STORM;
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
        boolean stormWand = staff.is(ModItems.STORM_STAFF.get());

        int bounces = 1 + potency + (stormWand ? 2 : 0);
        int maxEntities = 4 + 2 * potency + (stormWand ? 4 : 0);
        float damage = 5.0F + 1.5F * potency + (stormWand ? 5.0F : 0.0F);
        double jumpRange = (3.0 + 2.0 * potency) * (stormWand ? 2.0 : 1.0);

        LivingEntity target = this.getTarget(caster, spellStat.getRange());
        if (target == null) {
            return;
        }

        this.chain(worldIn, caster, target, bounces, maxEntities, damage, jumpRange, stormWand);
        this.playSound(worldIn, caster, ModSounds.THUNDERBOLT.get(), 1.0F, 1.0F);
    }

    private void chain(ServerLevel level, LivingEntity caster, LivingEntity target, int bounces, int maxEntities,
            float damage, double jumpRange, boolean stormWand) {
        ColorUtil color = stormWand ? new ColorUtil(0x5FB8FF) : new ColorUtil(11643889);
        DamageSource source = ModDamageSource.directShock(caster);
        List<Entity> harmed = new ArrayList<>();
        harmed.add(target);
        this.hit(level, target, source, damage, stormWand);
        List<LivingEntity> frontier = new ArrayList<>();
        frontier.add(target);
        for (int depth = 0; depth < bounces && harmed.size() < maxEntities; depth++) {
            List<LivingEntity> nextFrontier = new ArrayList<>();
            for (LivingEntity node : frontier) {
                if (harmed.size() >= maxEntities) {
                    break;
                }
                final LivingEntity finalNode = node;
                AABB aabb = new AABB(Vec3Util.subtract(node.position(), jumpRange), Vec3Util.add(node.position(), jumpRange));
                List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, aabb,
                        e -> e != caster
                                && !harmed.contains(e)
                                && MobUtil.canAttack(caster, e)
                                && MobUtil.hasLineOfSight(finalNode, e));
                for (LivingEntity next : candidates) {
                    if (harmed.size() >= maxEntities) {
                        break;
                    }
                    Vec3 from = node.position().add(0.0, node.getEyeHeight() * 0.5, 0.0);
                    Vec3 to = next.position().add(0.0, next.getEyeHeight() * 0.5, 0.0);
                    ModNetwork.sendToALL(new SThunderBoltPacket(from, to, color, 8));
                    this.hit(level, next, source, damage, stormWand);
                    harmed.add(next);
                    nextFrontier.add(next);
                }
            }
            frontier = nextFrontier;
        }
    }

    private void hit(ServerLevel level, LivingEntity entity, DamageSource source, float damage, boolean stormWand) {
        entity.hurt(source, damage);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getEyeY(), entity.getZ(), 6, 0.3, 0.5, 0.3, 0.05);
        if (stormWand) {
            entity.addEffect(new MobEffectInstance((MobEffect) GoetyEffects.SPASMS.get(), MathHelper.secondsToTicks(5)));
        }
    }
}
