package com.requiem.servants.logic;

import com.requiem.servants.RequiemServants;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 替代 KubeJS {@code ServerEvents.tick}：每 20 刻遍历所有维度，分发仆从 AI。
 */
@Mod.EventBusSubscriber(modid = RequiemServants.MOD_ID)
public final class ServantTickHandler {
    private static int tickCounter = 0;

    private ServantTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tickCounter++;
        if (tickCounter % 20 != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            ServantAI.serverTick(level);
        }
    }
}
