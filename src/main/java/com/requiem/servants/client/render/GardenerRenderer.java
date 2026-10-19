package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.requiem.servants.entity.GardenerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class GardenerRenderer extends MobRenderer<GardenerEntity, IllagerServantModel<GardenerEntity>> {
    // 暂时复用 goety 的卫道士仆从贴图；后续可换成 requiem:textures/entity/gardener.png
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("goety", "textures/entity/servants/illager/vindicator.png");

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
