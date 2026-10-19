package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.requiem.servants.entity.ApprenticeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;

public class ApprenticeRenderer extends MobRenderer<ApprenticeEntity, IllagerServantModel<ApprenticeEntity>> {
    // 暂时复用 goety 的新刌民贴图；后续可换成 requiem:textures/entity/apprentice.png
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("goety", "textures/entity/servants/illager/neollager.png");

    public ApprenticeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IllagerServantModel<>(ctx.bakeLayer(ModModelLayer.ILLAGER_SERVANT)), 0.5f);
        this.addLayer(new CrossedArmsItemLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ApprenticeEntity entity) {
        return TEXTURE;
    }
}
