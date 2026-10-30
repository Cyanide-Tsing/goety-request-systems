package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.requiem.servants.entity.DrummerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DrummerRenderer extends MobRenderer<DrummerEntity, IllagerServantModel<DrummerEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("requiem", "textures/entity/drummer.png");

    public DrummerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IllagerServantModel<>(ctx.bakeLayer(ModModelLayer.ILLAGER_SERVANT)), 0.5f);
        this.addLayer(new SnareDrumLayer(this, ctx.getItemRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(DrummerEntity entity) {
        return TEXTURE;
    }
}
