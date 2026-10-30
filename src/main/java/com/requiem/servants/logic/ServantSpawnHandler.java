package com.requiem.servants.logic;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant;
import com.mojang.logging.LogUtils;
import com.requiem.servants.ModEntities;
import com.requiem.servants.RequiemServants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * 替代 KubeJS {@code EntityEvents.spawned}：绑定主人、随机命名、统一服装/武器。
 */
@Mod.EventBusSubscriber(modid = RequiemServants.MOD_ID)
public final class ServantSpawnHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ServantSpawnHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractIllagerServant servant)) {
            return;
        }
        EntityType<?> type = servant.getType();
        if (!isRequiemServant(type)) {
            return;
        }

        boolean hostile = type == ModEntities.ARTILLERY_HOSTILE.get()
                || type == ModEntities.GRENADIER_HOSTILE.get()
                || type == ModEntities.BOTICARIO_HOSTILE.get();

        if (hostile) {
            servant.setHostile(true);
        } else if (servant.getTrueOwner() == null) {
            ServerPlayer nearest = null;
            double nearestDist = 1024.0;
            for (ServerPlayer player : level.players()) {
                double dx = player.getX() - servant.getX();
                double dy = player.getY() - servant.getY();
                double dz = player.getZ() - servant.getZ();
                double d = dx * dx + dy * dy + dz * dz;
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = player;
                }
            }
            if (nearest != null) {
                servant.setTrueOwner(nearest);
                servant.setHostile(false);
                LOGGER.info("Requiem: assigned owner {} to {}", nearest.getGameProfile().getName(),
                        EntityType.getKey(type));
            } else {
                LOGGER.warn("Requiem: no player found to own {}", EntityType.getKey(type));
            }
        }

        ServantLogic.assignServantName(servant);
        applyEquipment(servant, type);
    }

    private static boolean isRequiemServant(EntityType<?> type) {
        return type == ModEntities.APPRENTICE.get() || type == ModEntities.GARDENER.get()
                || type == ModEntities.RECRUITER.get() || type == ModEntities.ARTILLERY.get()
                || type == ModEntities.ARTILLERY_HOSTILE.get() || type == ModEntities.GRENADIER.get()
                || type == ModEntities.GRENADIER_HOSTILE.get() || type == ModEntities.CONQUEROR.get()
                || type == ModEntities.CAPTAIN.get() || type == ModEntities.ARTILLERY_CAPTAIN.get()
                || type == ModEntities.DRUMMER.get() || type == ModEntities.LIGHT_CAVALRY.get()
                || type == ModEntities.CAVALRY_CAPTAIN.get() || type == ModEntities.GROOM.get()
                || type == ModEntities.FLETCHER.get() || type == ModEntities.BOTICARIO.get()
                || type == ModEntities.BOTICARIO_HOSTILE.get();
    }

    private static void applyEquipment(AbstractIllagerServant servant, EntityType<?> type) {
        if (type == ModEntities.ARTILLERY.get() || type == ModEntities.ARTILLERY_HOSTILE.get()) {
            servant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TORCH));
        } else if (type == ModEntities.GRENADIER.get() || type == ModEntities.GRENADIER_HOSTILE.get()) {
            servant.setItemSlot(EquipmentSlot.MAINHAND, bombStack());
        } else if (type == ModEntities.CONQUEROR.get()) {
            servant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        } else if (type == ModEntities.ARTILLERY_CAPTAIN.get()
                || type == ModEntities.CAVALRY_CAPTAIN.get()
                || type == ModEntities.CAPTAIN.get()) {
            servant.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.WHITE_BANNER));
        }
    }

    private static ItemStack bombStack() {
        var bomb = ForgeRegistries.ITEMS.getValue(new ResourceLocation("supplementaries", "bomb"));
        return bomb == null ? ItemStack.EMPTY : new ItemStack(bomb);
    }
}
