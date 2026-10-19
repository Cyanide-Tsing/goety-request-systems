package com.requiem.servants.client.render;

import com.requiem.servants.client.ModModelLayers;
import com.requiem.servants.client.render.model.ConquerorModel;
import com.requiem.servants.entity.ConquerorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class ConquerorRenderer extends MobRenderer<ConquerorEntity, ConquerorModel> {
    // 复用 goety 征服者（Conquillager）的贴图
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("goety", "textures/entity/illagers/conquillager.png");

    public ConquerorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ConquerorModel(ctx.bakeLayer(ModModelLayers.CONQUEROR)), 0.5f);
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ConquerorEntity entity) {
        return TEXTURE;
    }
}
