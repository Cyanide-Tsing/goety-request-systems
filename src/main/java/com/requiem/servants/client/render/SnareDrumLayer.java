package com.requiem.servants.client.render;

import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.requiem.servants.ModItems;
import com.requiem.servants.entity.DrummerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 把小军鼓固定在鼓手的躯干（腰部）前方，不随手臂摆动。
 *
 * <p>鼓手平时双臂会随行走摆动，普通 {@code ItemInHandLayer} 会把鼓画在手上跟着甩。
 * 这里改用躯干的 {@code body} 部件作为锚点，把鼓固定在腰部前方；主手/副手只要持有小军鼓都会画在这里。
 */
public class SnareDrumLayer extends RenderLayer<DrummerEntity, IllagerServantModel<DrummerEntity>> {
    private final ItemRenderer itemRenderer;

    public SnareDrumLayer(RenderLayerParent<DrummerEntity, IllagerServantModel<DrummerEntity>> parent,
            ItemRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack stack, MultiBufferSource buffer, int light, DrummerEntity entity,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        ItemStack drum = entity.getMainHandItem();
        if (drum.getItem() != ModItems.SNARE_DRUM.get()) {
            drum = entity.getOffhandItem();
        }
        if (drum.getItem() != ModItems.SNARE_DRUM.get()) {
            return;
        }

        stack.pushPose();
        // 以躯干为锚点，前伸到腰部前方；不读取手臂的旋转，因此不会随摆动改变位置。
        this.getParentModel().body.translateAndRotate(stack);
        stack.translate(0.0D, 0.6D, -0.3D);
        stack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        stack.mulPose(Axis.YP.rotationDegrees(180.0F));
        stack.scale(1.2F, 1.2F, 1.2F);
        this.itemRenderer.renderStatic(drum, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, stack, buffer,
                entity.level(), entity.getId());
        stack.popPose();
    }
}
