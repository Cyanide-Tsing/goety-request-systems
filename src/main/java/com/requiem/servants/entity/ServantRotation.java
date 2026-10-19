package com.requiem.servants.entity;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.Mob;

public class ServantRotation {
    private static final Map<Integer, double[]> prev = new HashMap<>();
    private static final Map<Integer, Float> bodyYaw = new HashMap<>();
    private static final Map<Integer, Float> lockedFacing = new HashMap<>();

    // 与原 KubeJS 的 8 方向查表一致，避免 atan2 的平台差异。
    // 返回 Minecraft 偏航角：0=南(+Z)，90=西(-X)，180/-180=北(-Z)，-90=东(+X)。
    public static float yawFromDelta(double dx, double dz) {
        double adx = Math.abs(dx), adz = Math.abs(dz);
        if (adx < 0.0001 && adz < 0.0001) return Float.NaN;
        if (adx > adz * 2.4142) return dx > 0 ? -90f : 90f;
        if (adz > adx * 2.4142) return dz > 0 ? 0f : 180f;
        if (dx > 0 && dz > 0) return -45f;
        if (dx > 0) return -135f;
        if (dz > 0) return 45f;
        return 135f;
    }

    public static float wrapDegrees(float d) {
        d = d % 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    // 锁定本刻朝向：FormationSlotGoal 列阵时已设置好 yaw，这里标记一下，
    // 让 tick 不再根据移动方向覆盖它。
    public static void lockFacing(Mob entity, float yaw) {
        lockedFacing.put(entity.getId(), yaw);
    }

    // 实体离开世界时清掉缓存。这些 Map 按实体 id 索引，不清理的话会随着
    // 实体死亡/卸载不断累积；而且实体 id 会被复用，残留数据会让新实体
    // 第一刻转向错误。由 ForgeEventHandler 在 EntityLeaveLevelEvent 时调用。
    public static void forget(Mob entity) {
        int id = entity.getId();
        prev.remove(id);
        bodyYaw.remove(id);
        lockedFacing.remove(id);
    }

    public static void tick(Mob entity) {
        if (entity.level().isClientSide()) return;
        int id = entity.getId();
        double x = entity.getX(), z = entity.getZ();
        Float locked = lockedFacing.remove(id);
        if (locked != null) {
            entity.setYHeadRot(locked);
            entity.setYBodyRot(locked);
            prev.put(id, new double[]{x, z});
            return;
        }
        double[] p = prev.get(id);
        if (p != null) {
            double dx = x - p[0], dz = z - p[1];
            if (dx * dx + dz * dz > 0.0001) {
                float target = yawFromDelta(dx, dz);
                if (!Float.isNaN(target)) {
                    entity.setYHeadRot(target);
                    float cur = bodyYaw.getOrDefault(id, target);
                    float diff = wrapDegrees(target - cur);
                    if (diff > 15f) diff = 15f;
                    else if (diff < -15f) diff = -15f;
                    bodyYaw.put(id, cur + diff);
                    entity.setYBodyRot(bodyYaw.get(id));
                }
            }
        }
        prev.put(id, new double[]{x, z});
    }
}
