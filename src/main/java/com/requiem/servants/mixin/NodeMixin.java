package com.requiem.servants.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Node;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让寻路启发式里竖直方向"更贵"：优先走能上升/下降的通道（楼梯、台阶等），
 * 而不是傻站在目标正下方（同一 x/z、差 y 的那个"最近可达点"）。
 *
 * <p>把 y 轴距离乘以一个系数后，正下方节点的启发值会变大，而"绕到楼梯再上来"
 * 的路径启发值相对变小，于是 A* 会主动去爬楼/下井，而不是停在正下方。
 */
@Mixin(Node.class)
public abstract class NodeMixin {

    @Shadow
    @Final
    public int x;
    @Shadow
    @Final
    public int y;
    @Shadow
    @Final
    public int z;

    /** y 轴加权系数：竖直方向的距离按这个倍数计入启发值。 */
    private static final float Y_WEIGHT = 2.0F;

    @Inject(method = "distanceTo", at = @At("HEAD"), cancellable = true)
    private void requiem$distanceTo(Node other, CallbackInfoReturnable<Float> cir) {
        float dx = (float) (other.x - this.x);
        float dy = (float) (other.y - this.y) * Y_WEIGHT;
        float dz = (float) (other.z - this.z);
        cir.setReturnValue(Mth.sqrt(dx * dx + dy * dy + dz * dz));
    }
}
