package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.model.SoulBoltModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.requiem.servants.entity.projectile.RequiemAcidBall;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RequiemAcidBallRenderer extends EntityRenderer<RequiemAcidBall> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("requiem", "textures/entity/projectiles/acid_ball.png");
    private final SoulBoltModel<RequiemAcidBall> model;

    public RequiemAcidBallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new SoulBoltModel<>(ctx.bakeLayer(ModModelLayer.SOUL_BOLT));
    }

    @Override
    protected int getBlockLightLevel(RequiemAcidBall entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(RequiemAcidBall entity, float yaw, float partialTicks, PoseStack stack, MultiBufferSource buffer,
            int light) {
        stack.pushPose();
        stack.scale(-1.25F, -1.25F, 1.25F);
        float f = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        float f1 = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity)));
        this.model.setupAnim(0.0F, f, f1);
        this.model.renderToBuffer(stack, consumer, 0xF00000, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.5F);
        stack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RequiemAcidBall entity) {
        return TEXTURE;
    }
}
