package com.requiem.servants;

import com.requiem.servants.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;

@Mod(RequiemServants.MOD_ID)
public class RequiemServants {
    public static final String MOD_ID = "requiem";

    public RequiemServants() {
        ModEntities.register();
        ModItems.register();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(ClientSetup::registerRenderers);
            modBus.addListener(ClientSetup::registerLayerDefinitions);
        });
    }
}
