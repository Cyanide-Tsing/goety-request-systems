package com.requiem.servants.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.requiem.servants.block.DemandWorkBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 需求型工作方块渲染器：委任状平放在上表面右上角，需求物品平放在左上角。
 */
public class DemandWorkRenderer implements BlockEntityRenderer<DemandWorkBlockEntity> {
    private static final double TOP_Y = 1.0D;

    public DemandWorkRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DemandWorkBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        ItemStack appointment = be.getAppointmentContainer().getItem(0);
        ItemStack demand = be.getDemandContainer().getItem(0);
        if (!demand.isEmpty()) {
            renderItem(demand, 0.25D, TOP_Y + 0.02D, 0.25D, poseStack, buffer, packedLight, packedOverlay, level);
        }
        if (!appointment.isEmpty()) {
            renderItem(appointment, 0.75D, TOP_Y + 0.02D, 0.25D, poseStack, buffer, packedLight, packedOverlay, level);
        }
    }

    private void renderItem(ItemStack stack, double x, double y, double z, PoseStack poseStack,
                            MultiBufferSource buffer, int light, int overlay, Level level) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, level, 0);
        poseStack.popPose();
    }
}
