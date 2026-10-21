package com.requiem.servants;

import com.requiem.servants.entity.ApprenticeEntity;
import com.requiem.servants.entity.ArtilleryCaptainEntity;
import com.requiem.servants.entity.ArtilleryEntity;
import com.requiem.servants.entity.CaptainEntity;
import com.requiem.servants.entity.ConquerorEntity;
import com.requiem.servants.entity.DrummerEntity;
import com.requiem.servants.entity.GardenerEntity;
import com.requiem.servants.entity.GrenadierEntity;
import com.requiem.servants.entity.RecruiterEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RequiemServants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetup {
    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.APPRENTICE.get(), ApprenticeEntity.createAttributes().build());
        event.put(ModEntities.GARDENER.get(), GardenerEntity.createAttributes().build());
        event.put(ModEntities.RECRUITER.get(), RecruiterEntity.createAttributes().build());
        event.put(ModEntities.ARTILLERY.get(), ArtilleryEntity.createAttributes().build());
        event.put(ModEntities.ARTILLERY_HOSTILE.get(), ArtilleryEntity.createAttributes().build());
        event.put(ModEntities.GRENADIER.get(), GrenadierEntity.createAttributes().build());
        event.put(ModEntities.GRENADIER_HOSTILE.get(), GrenadierEntity.createAttributes().build());
        event.put(ModEntities.CONQUEROR.get(), ConquerorEntity.createAttributes().build());
        event.put(ModEntities.ARTILLERY_CAPTAIN.get(), ArtilleryCaptainEntity.createAttributes().build());
        event.put(ModEntities.CAPTAIN.get(), CaptainEntity.createAttributes().build());
        event.put(ModEntities.DRUMMER.get(), DrummerEntity.createAttributes().build());
    }
}
