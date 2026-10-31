package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.IceologerModel;
import com.requiem.servants.entity.RequiemIceologerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 怪物型冰术师渲染器：复用 goety 的冰术师模型与贴图。
 */
public class RequiemIceologerRenderer extends MobRenderer<RequiemIceologerEntity, IceologerModel<RequiemIceologerEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("goety", "textures/entity/servants/illager/iceologer.png");

    public RequiemIceologerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IceologerModel<>(ctx.bakeLayer(ModModelLayer.ICEOLOGER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(RequiemIceologerEntity entity) {
        return TEXTURE;
    }
}
