package com.requiem.servants.client;

import com.requiem.servants.ModEntities;
import com.requiem.servants.client.render.ApprenticeRenderer;
import com.requiem.servants.client.render.ArtilleryCaptainRenderer;
import com.requiem.servants.client.render.ArtilleryRenderer;
import com.requiem.servants.client.render.CaptainRenderer;
import com.requiem.servants.client.render.ConquerorRenderer;
import com.requiem.servants.client.render.GardenerRenderer;
import com.requiem.servants.client.render.GrenadierRenderer;
import com.requiem.servants.client.render.RecruiterRenderer;
import com.requiem.servants.client.render.model.ConquerorModel;
import net.minecraftforge.client.event.EntityRenderersEvent;

public class ClientSetup {
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.APPRENTICE.get(), ApprenticeRenderer::new);
        event.registerEntityRenderer(ModEntities.GARDENER.get(), GardenerRenderer::new);
        event.registerEntityRenderer(ModEntities.RECRUITER.get(), RecruiterRenderer::new);
        event.registerEntityRenderer(ModEntities.ARTILLERY.get(), ArtilleryRenderer::new);
        event.registerEntityRenderer(ModEntities.ARTILLERY_HOSTILE.get(), ArtilleryRenderer::new);
        event.registerEntityRenderer(ModEntities.GRENADIER.get(), GrenadierRenderer::new);
        event.registerEntityRenderer(ModEntities.GRENADIER_HOSTILE.get(), GrenadierRenderer::new);
        event.registerEntityRenderer(ModEntities.CONQUEROR.get(), ConquerorRenderer::new);
        event.registerEntityRenderer(ModEntities.ARTILLERY_CAPTAIN.get(), ArtilleryCaptainRenderer::new);
        event.registerEntityRenderer(ModEntities.CAPTAIN.get(), CaptainRenderer::new);
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.CONQUEROR, ConquerorModel::createBodyLayer);
    }
}
