package com.requiem.servants.client;

import com.requiem.servants.ModBlockEntities;
import com.requiem.servants.ModEntities;
import com.requiem.servants.ModItems;
import com.requiem.servants.ModMenuTypes;
import com.requiem.servants.client.render.ApprenticeRenderer;
import com.requiem.servants.client.render.ArtilleryCaptainRenderer;
import com.requiem.servants.client.render.ArtilleryRenderer;
import com.requiem.servants.client.render.BoticarioHostileRenderer;
import com.requiem.servants.client.render.BoticarioRenderer;
import com.requiem.servants.client.render.CaptainRenderer;
import com.requiem.servants.client.render.CavalryCaptainRenderer;
import com.requiem.servants.client.render.ConquerorRenderer;
import com.requiem.servants.client.render.DemandWorkRenderer;
import com.requiem.servants.client.render.DrummerRenderer;
import com.requiem.servants.client.render.FletcherRenderer;
import com.requiem.servants.client.render.GardenerRenderer;
import com.requiem.servants.client.render.GrenadierRenderer;
import com.requiem.servants.client.render.GroomRenderer;
import com.requiem.servants.client.render.HorseFarmRegisterRenderer;
import com.requiem.servants.client.render.LightCavalryRenderer;
import com.requiem.servants.client.render.RecruiterRenderer;
import com.requiem.servants.client.render.RequiemAcidBallRenderer;
import com.requiem.servants.client.render.RequiemIceologerRenderer;
import com.requiem.servants.client.render.RequiemProjectileRenderer;
import com.requiem.servants.client.render.model.ConquerorModel;
import com.requiem.servants.client.screen.DemandWorkScreen;
import com.requiem.servants.client.screen.HorseFarmRegisterScreen;
import com.requiem.servants.item.AppointmentItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

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
        event.registerEntityRenderer(ModEntities.DRUMMER.get(), DrummerRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHT_CAVALRY.get(), LightCavalryRenderer::new);
        event.registerEntityRenderer(ModEntities.CAVALRY_CAPTAIN.get(), CavalryCaptainRenderer::new);
        event.registerEntityRenderer(ModEntities.GROOM.get(), GroomRenderer::new);
        event.registerEntityRenderer(ModEntities.FLETCHER.get(), FletcherRenderer::new);
        event.registerEntityRenderer(ModEntities.BOTICARIO.get(), BoticarioRenderer::new);
        event.registerEntityRenderer(ModEntities.BOTICARIO_HOSTILE.get(), BoticarioHostileRenderer::new);
        event.registerEntityRenderer(ModEntities.ICEOLOGER.get(), RequiemIceologerRenderer::new);
        event.registerEntityRenderer(ModEntities.REQUIEM_BOLT.get(), RequiemProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.REQUIEM_ACID_BALL.get(), RequiemAcidBallRenderer::new);
        event.registerEntityRenderer(ModEntities.REQUIEM_SNARE_WEB.get(), RequiemProjectileRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HORSE_FARM_REGISTER.get(), HorseFarmRegisterRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DEMAND_WORK.get(), DemandWorkRenderer::new);
    }

    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.HORSE_FARM_REGISTER.get(), HorseFarmRegisterScreen::new);
            MenuScreens.register(ModMenuTypes.DEMAND_WORK.get(), DemandWorkScreen::new);
        });
    }

    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.APPOINTMENT.get(),
                new ResourceLocation("requiem", "bound"),
                (stack, level, entity, seed) -> AppointmentItem.isBound(stack) ? 1.0F : 0.0F));
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.CONQUEROR, ConquerorModel::createBodyLayer);
    }
}
