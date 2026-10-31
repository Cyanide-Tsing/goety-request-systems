package com.requiem.servants.mixin;

import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 解除原版 {@code JigsawStructure} 里拼图结构 {@code size} 字段 0~7 的上限，
 * 放宽到 128，从而让营地可以超过 7 层拼图深度。
 */
@Mixin(JigsawStructure.class)
public abstract class JigsawStructureMixin {
    @ModifyConstant(method = "m_227639_", remap = false, constant = @Constant(intValue = 7))
    private static int requiem$raiseJigsawSizeLimit(int original) {
        return 128;
    }
}
