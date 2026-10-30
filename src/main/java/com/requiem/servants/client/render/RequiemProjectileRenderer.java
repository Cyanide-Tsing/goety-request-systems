package com.requiem.servants.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class RequiemProjectileRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation DUMMY = new ResourceLocation("minecraft", "textures/misc/white.png");

    public RequiemProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(T entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight) {
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return DUMMY;
    }
}
