package com.requiem.servants.logic;

import com.requiem.servants.RequiemServants;
import com.requiem.servants.entity.WorkServantEntity;
import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 替代 KubeJS {@code 96_spell_protection.js}：玩家自己的法术/投射物不会伤到自己的马；
 * 并让工作仆从不会踩坏耕地。
 */
@Mod.EventBusSubscriber(modid = RequiemServants.MOD_ID)
public final class ServantMiscHandler {
    private ServantMiscHandler() {
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof WorkServantEntity) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof AbstractHorse horse) || !horse.isTamed()) {
            return;
        }
        UUID owner = horse.getOwnerUUID();
        if (owner == null) {
            return;
        }
        DamageSource source = event.getSource();
        Entity caster = source.getEntity();
        if (!(caster instanceof Player player) || !owner.equals(player.getUUID())) {
            return;
        }
        Entity direct = source.getDirectEntity();
        if (direct == null || direct == caster) {
            return; // 近战（直接来源就是玩家本人），不免疫
        }
        event.setCanceled(true);
    }
}
