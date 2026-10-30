package com.requiem.servants.client.render.model;

import com.Polarice3.Goety.client.render.model.IllagerServantModel;
import com.requiem.servants.entity.BoticarioHostileEntity;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 敌对药剂师专用模型。
 *
 * <p>{@link IllagerServantModel} 面向灾厄仆从（{@code AbstractIllagerServant}），其
 * {@code setupAnim} 会按仆从的手臂姿势在"交叉双臂（{@code arms}）"与"两侧下垂
 * （{@code RightArm}/{@code LeftArm}）"之间二选一。敌对药剂师不是灾厄仆从，模型不会
 * 收到切换逻辑，导致两套手臂同时显示。这里在 {@code setupAnim} 之后固定隐藏交叉双臂，
 * 只保留两侧下垂手臂。
 */
public class BoticarioHostileModel extends IllagerServantModel<BoticarioHostileEntity> {
    public BoticarioHostileModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(BoticarioHostileEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.arms.visible = false;
        this.RightArm.visible = true;
        this.LeftArm.visible = true;
    }
}
