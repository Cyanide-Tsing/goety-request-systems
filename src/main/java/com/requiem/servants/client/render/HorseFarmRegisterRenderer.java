package com.requiem.servants.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.requiem.servants.block.HorseFarmRegisterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 马场登记站渲染器：当槽位里放有委任状时，在方块上表面中心叠加一个平放的委任状材质。
 */
public class HorseFarmRegisterRenderer implements BlockEntityRenderer<HorseFarmRegisterBlockEntity> {
    private static final double TOP_Y = 0.75D;

    public HorseFarmRegisterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HorseFarmRegisterBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack stack = be.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        poseStack.pushPose();
        poseStack.translate(0.5D, TOP_Y + 0.02D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, be.getLevel(), 0);
        poseStack.popPose();
    }
}
