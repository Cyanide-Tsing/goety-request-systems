package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.requiem.servants.entity.ArtilleryEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class ArtilleryRenderer extends MobRenderer<ArtilleryEntity, IllagerServantModel<ArtilleryEntity>> {
    // 暂时使用 goety 的卫道士仆从贴图；后续可换成 requiem:textures/entity/artillery.png
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("goety", "textures/entity/servants/illager/vindicator.png");

    public ArtilleryRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IllagerServantModel<>(ctx.bakeLayer(ModModelLayer.ILLAGER_SERVANT)), 0.5f);
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ArtilleryEntity entity) {
        return TEXTURE;
    }
}
