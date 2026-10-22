package com.requiem.servants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RequiemServants.MOD_ID);

    public static final RegistryObject<SoundEvent> BRITISH_GRENADIERS =
            SOUND_EVENTS.register("music_disc.british_grenadiers",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation("requiem", "music_disc.british_grenadiers")));

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        SOUND_EVENTS.register(bus);
    }
}
