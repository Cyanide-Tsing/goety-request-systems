package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.requiem.servants.client.render.model.BoticarioHostileModel;
import com.requiem.servants.entity.BoticarioHostileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * 敌对药剂师渲染器。
 *
 * <p>与药剂师仆从用同一个模型（{@code IllagerServantModel}），只是贴图不同，
 * 避免灾厄村民（Illager）与仆从（Villager）两套 UV 对不上导致的贴图爆炸；
 * 用 {@link BoticarioHostileModel} 隐藏交叉双臂，避免双臂重影。
 */
public class BoticarioHostileRenderer extends MobRenderer<BoticarioHostileEntity, BoticarioHostileModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("requiem", "textures/entity/boticario_hostile.png");

    public BoticarioHostileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BoticarioHostileModel(ctx.bakeLayer(ModModelLayer.ILLAGER_SERVANT)), 0.5f);
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(BoticarioHostileEntity entity) {
        return TEXTURE;
    }
}
