package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.requiem.servants.entity.CavalryCaptainEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class CavalryCaptainRenderer extends MobRenderer<CavalryCaptainEntity, IllagerServantModel<CavalryCaptainEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("requiem", "textures/entity/cavalry_captain.png");

    public CavalryCaptainRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IllagerServantModel<>(ctx.bakeLayer(ModModelLayer.ILLAGER_SERVANT)), 0.5f);
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(CavalryCaptainEntity entity) {
        return TEXTURE;
    }
}
