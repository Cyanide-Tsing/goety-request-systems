package com.requiem.servants;

import com.requiem.servants.worldgen.CampFenceProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructureProcessors {
    private static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, RequiemServants.MOD_ID);

    public static final RegistryObject<StructureProcessorType<CampFenceProcessor>> CAMP_FENCE =
            PROCESSORS.register("camp_fence", () -> CampFenceProcessor.TYPE);

    private ModStructureProcessors() {
    }

    public static void register() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        PROCESSORS.register(modBus);
    }
}
