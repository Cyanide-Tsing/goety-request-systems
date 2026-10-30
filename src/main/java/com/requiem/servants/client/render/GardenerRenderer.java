package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.requiem.servants.entity.GardenerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class GardenerRenderer extends MobRenderer<GardenerEntity, IllagerServantModel<GardenerEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("requiem", "textures/entity/gardener.png");

    public GardenerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IllagerServantModel<>(ctx.bakeLayer(ModModelLayer.ILLAGER_SERVANT)), 0.5f);
        // 园丁两手分开（NEUTRAL），用普通手持物品层
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(GardenerEntity entity) {
        return TEXTURE;
    }
}
