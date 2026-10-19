package com.requiem.servants.client.render.model;

import com.Polarice3.Goety.client.render.model.ConquillagerModel;
import com.requiem.servants.entity.ConquerorEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.item.CrossbowItem;

/**
 * 征服者模型：直接复用 goety {@link ConquillagerModel} 的网格（村民形头部 + 胡须/鼻子/帽子），
 * 但泛型改成 {@link ConquerorEntity}，绕开 goety 模型对原生 {@code AbstractIllager} 的类型约束。
 *
 * <p>这样 {@code conquillager.png} 的 UV 布局才能正确映射到模型上。
 */
public class ConquerorModel extends HumanoidModel<ConquerorEntity> {
    public ConquerorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return ConquillagerModel.createBodyLayer();
    }

    @Override
    public void setupAnim(ConquerorEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 持弩时摆出持弩手臂姿势
        boolean crossbow = entity.getMainHandItem().getItem() instanceof CrossbowItem;
        this.rightArmPose = crossbow ? ArmPose.CROSSBOW_HOLD : ArmPose.EMPTY;
        this.leftArmPose = crossbow ? ArmPose.CROSSBOW_HOLD : ArmPose.EMPTY;
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }
}
