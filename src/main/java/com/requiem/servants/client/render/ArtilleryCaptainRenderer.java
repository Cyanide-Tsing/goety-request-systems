package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.requiem.servants.entity.ArtilleryCaptainEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class ArtilleryCaptainRenderer extends MobRenderer<ArtilleryCaptainEntity, IllagerServantModel<ArtilleryCaptainEntity>> {
    // 暂时复用 goety 的传令兵仆从贴图；后续可换成 requiem:textures/entity/artillery_captain.png
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("goety", "textures/entity/servants/illager/signaler.png");

    public ArtilleryCaptainRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IllagerServantModel<>(ctx.bakeLayer(ModModelLayer.ILLAGER_SERVANT)), 0.5f);
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ArtilleryCaptainEntity entity) {
        return TEXTURE;
    }
}
