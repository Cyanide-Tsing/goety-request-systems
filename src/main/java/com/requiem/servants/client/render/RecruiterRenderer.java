package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.requiem.servants.entity.RecruiterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class RecruiterRenderer extends MobRenderer<RecruiterEntity, IllagerServantModel<RecruiterEntity>> {
    // 暂时使用 goety 的传令兵仆从贴图（双手分开模型）；后续可换成 requiem:textures/entity/recruiter.png
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("goety", "textures/entity/servants/illager/signaler.png");

    public RecruiterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IllagerServantModel<>(ctx.bakeLayer(ModModelLayer.ILLAGER_SERVANT)), 0.5f);
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(RecruiterEntity entity) {
        return TEXTURE;
    }
}
