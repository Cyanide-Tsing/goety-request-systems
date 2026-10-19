package com.requiem.servants.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 阵型解算器：把"阵型 + 各小队人数"算成每个人站哪、朝哪。
 *
 * <h2>局部坐标系</h2>
 * 解算先在连队自己的局部坐标系里进行：<b>+x 为连队正面的右手方向，+z 为连队正面方向</b>。
 * 锚点（anchor）是局部原点，一般落在阵型的第一排中央（环形与空心方阵落在阵中心）。
 * 排越靠后，z 越小（负数）。
 *
 * <p>随后用 {@link #toWorld} 按连队朝向旋转到世界坐标。Minecraft 偏航角约定为
 * 0=南(+Z)、90=西(-X)、180=北(-Z)、-90=东(+X)，对应的正面与右手单位向量是：
 * <pre>
 *   forward = (-sin F,  cos F)
 *   right   = (-cos F, -sin F)
 * </pre>
 * 所以 {@code world = anchor + right * localX + forward * localZ}。
 */
public final class FormationLayout {
    /** 左右相邻两人的间距。 */
    public static final double FILE_SPACING = 1.4D;
    /** 前后相邻两排的间距。 */
    public static final double RANK_SPACING = 1.5D;
    /** 相邻小队之间额外留出的空隙。 */
    public static final double SQUAD_GAP = 1.2D;
    /** 横列阵/梯次阵中单个小队的默认纵深（排数）。 */
    public static final int SQUAD_DEPTH = 2;
    /** 纵队的正面宽度。 */
    public static final int COLUMN_WIDTH = 2;

    private FormationLayout() {
    }

    /** 炮列中火炮之间的间距：架炮要留出余量。 */
    public static final double GUN_SPACING = 2.2D;
    /** 炮列两翼掩护小队比火线前出的距离。 */
    public static final double SCREEN_FORWARD = 3.0D;
    /** 线列各层之间的间距：比普通排距大，留出轮换走位的空间。 */
    public static final double VOLLEY_RANK_SPACING = 2.4D;

    /** 一个小队在解算中的形状描述：人数、该兵种往后退的排数、兵种。 */
    public record SquadShape(int size, int rankOffset, ArmType arm) {
        public SquadShape(int size) {
            this(size, 0, ArmType.MELEE);
        }
    }

    /** 局部坐标系中的一个槽位。{@code yawOffset} 是相对连队正面的朝向偏移（度）。 */
    public record LocalSlot(double x, double z, float yawOffset) {
        public static final LocalSlot ORIGIN = new LocalSlot(0.0D, 0.0D, 0.0F);
    }

    /**
     * 解算阵型。
     *
     * @return 与 {@code squads} 一一对应的槽位列表；每个子列表按小队内成员顺序排列。
     */
    public static List<List<LocalSlot>> layout(FormationType type, SquadShape[] squads) {
        if (squads == null || squads.length == 0) {
            return List.of();
        }
        return switch (type) {
            case LINE -> line(squads);
            case COLUMN -> column(squads);
            case SQUARE -> square(squads);
            case HOLLOW_SQUARE -> hollowSquare(squads);
            case WEDGE -> wedge(squads);
            case ECHELON -> echelon(squads);
            case CIRCLE -> circle(squads);
            case SKIRMISH -> skirmish(squads);
            case GUN_LINE -> gunLine(squads);
            case FIRING_LINE -> firingLine(squads);
        };
    }

    // ------------------------------------------------------------------
    // 各阵型
    // ------------------------------------------------------------------

    /** 横列阵：小队从左到右并排，每队 {@link #SQUAD_DEPTH} 排深，兵种决定整队再往后退几排。 */
    private static List<List<LocalSlot>> line(SquadShape[] squads) {
        int[] widths = new int[squads.length];
        double totalWidth = 0.0D;
        for (int s = 0; s < squads.length; s++) {
            widths[s] = Math.max(1, ceilDiv(squads[s].size(), SQUAD_DEPTH));
            totalWidth += (widths[s] - 1) * FILE_SPACING;
            if (s > 0) {
                totalWidth += SQUAD_GAP + FILE_SPACING;
            }
        }

        double cursor = -totalWidth / 2.0D;
        List<List<LocalSlot>> plan = new ArrayList<>(squads.length);
        for (int s = 0; s < squads.length; s++) {
            int size = squads[s].size();
            int width = widths[s];
            double baseZ = -squads[s].rankOffset() * RANK_SPACING;
            List<LocalSlot> slots = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int file = i % width;
                int rank = i / width;
                slots.add(new LocalSlot(cursor + file * FILE_SPACING, baseZ - rank * RANK_SPACING, 0.0F));
            }
            plan.add(slots);
            cursor += (width - 1) * FILE_SPACING + SQUAD_GAP + FILE_SPACING;
        }
        return plan;
    }

    /** 纵队：小队前后相继，每队 {@link #COLUMN_WIDTH} 人宽，队间空一排。 */
    private static List<List<LocalSlot>> column(SquadShape[] squads) {
        List<List<LocalSlot>> plan = new ArrayList<>(squads.length);
        int rankCursor = 0;
        for (SquadShape squad : squads) {
            int size = squad.size();
            List<LocalSlot> slots = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int file = i % COLUMN_WIDTH;
                int rank = i / COLUMN_WIDTH;
                double x = (file - (COLUMN_WIDTH - 1) / 2.0D) * FILE_SPACING;
                slots.add(new LocalSlot(x, -(rankCursor + rank) * RANK_SPACING, 0.0F));
            }
            plan.add(slots);
            rankCursor += ceilDiv(size, COLUMN_WIDTH) + 1;
        }
        return plan;
    }

    /** 实心方阵：全连打散重排成正方形，以锚点为中心。 */
    private static List<List<LocalSlot>> square(SquadShape[] squads) {
        int total = Math.max(1, totalSize(squads));
        int side = Math.max(1, (int) Math.ceil(Math.sqrt(total)));
        double offset = (side - 1) / 2.0D;
        return distribute(squads, k -> {
            int file = k % side;
            int rank = k / side;
            return new LocalSlot((file - offset) * FILE_SPACING, -(rank - offset) * RANK_SPACING, 0.0F);
        });
    }

    /** 空心方阵：每个小队守一面，槽位朝外；一个面放不下时向内再加一层。 */
    private static List<List<LocalSlot>> hollowSquare(SquadShape[] squads) {
        int total = Math.max(1, totalSize(squads));
        int perSide = Math.max(2, ceilDiv(total, 4));
        double half = Math.max(1.5D, (perSide - 1) * FILE_SPACING / 2.0D);

        List<List<LocalSlot>> plan = new ArrayList<>(squads.length);
        for (int s = 0; s < squads.length; s++) {
            int size = squads[s].size();
            int side = s % 4;
            List<LocalSlot> slots = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                double along = (i % perSide - (perSide - 1) / 2.0D) * FILE_SPACING;
                double depth = half - (i / perSide) * RANK_SPACING;
                slots.add(sideSlot(side, along, depth));
            }
            plan.add(slots);
        }
        return plan;
    }

    /** 空心方阵的单面槽位。局部朝外方向 (sinθ, cosθ) 对应 yawOffset = θ。 */
    private static LocalSlot sideSlot(int side, double along, double depth) {
        return switch (side) {
            case 0 -> new LocalSlot(along, depth, 0.0F);       // 前面，朝前
            case 1 -> new LocalSlot(depth, -along, 90.0F);     // 右面，朝右
            case 2 -> new LocalSlot(-along, -depth, 180.0F);   // 后面，朝后
            default -> new LocalSlot(-depth, along, 270.0F);   // 左面，朝左
        };
    }

    /** 楔形阵：第一人为锋尖，其余左右交替向后展开成 V 形。 */
    private static List<List<LocalSlot>> wedge(SquadShape[] squads) {
        return distribute(squads, k -> {
            if (k == 0) {
                return LocalSlot.ORIGIN;
            }
            int step = (k - 1) / 2 + 1;
            int arm = (k - 1) % 2 == 0 ? -1 : 1;
            return new LocalSlot(arm * step * FILE_SPACING, -step * RANK_SPACING * 0.75D, 0.0F);
        });
    }

    /** 梯次阵：小队依次向右后方错开，形成斜线掩护一侧翼。 */
    private static List<List<LocalSlot>> echelon(SquadShape[] squads) {
        List<List<LocalSlot>> plan = new ArrayList<>(squads.length);
        for (int s = 0; s < squads.length; s++) {
            int size = squads[s].size();
            int width = Math.max(1, ceilDiv(size, SQUAD_DEPTH));
            double baseX = s * FILE_SPACING * 2.5D;
            double baseZ = -(s * 2.0D + squads[s].rankOffset()) * RANK_SPACING;
            double offset = (width - 1) / 2.0D;
            List<LocalSlot> slots = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int file = i % width;
                int rank = i / width;
                slots.add(new LocalSlot(baseX + (file - offset) * FILE_SPACING, baseZ - rank * RANK_SPACING, 0.0F));
            }
            plan.add(slots);
        }
        return recenterX(plan);
    }

    /** 环形阵：全员等角分布在一个圆上，各自朝外。 */
    private static List<List<LocalSlot>> circle(SquadShape[] squads) {
        int total = Math.max(1, totalSize(squads));
        double radius = Math.max(2.0D, total * FILE_SPACING / (2.0D * Math.PI));
        return distribute(squads, k -> {
            double theta = 2.0D * Math.PI * k / total;
            return new LocalSlot(radius * Math.sin(theta), radius * Math.cos(theta),
                    (float) Math.toDegrees(theta));
        });
    }

    /** 散兵线：大间距，并且隔排错位，避免所有人挤在一条直线上吃范围伤害。 */
    private static List<List<LocalSlot>> skirmish(SquadShape[] squads) {
        int total = Math.max(1, totalSize(squads));
        int width = Math.max(1, (int) Math.ceil(Math.sqrt(total * 2.0D)));
        double spacing = FILE_SPACING * 2.2D;
        double offset = (width - 1) / 2.0D;
        return distribute(squads, k -> {
            int file = k % width;
            int rank = k / width;
            double stagger = (rank % 2 == 0) ? 0.0D : spacing * 0.5D;
            return new LocalSlot((file - offset) * spacing + stagger, -rank * spacing * 0.8D, 0.0F);
        });
    }

    /**
     * 炮列：远程小队在 z=0 拉成一条宽火线（之后会被钉死），
     * 近战小队左右交替前出掩护两翼，施法与辅助依次压在火线后方。
     */
    private static List<List<LocalSlot>> gunLine(SquadShape[] squads) {
        // 先量出火线总宽，才能把它摆在锚点正中
        double gunWidth = 0.0D;
        int gunSquads = 0;
        for (SquadShape squad : squads) {
            if (squad.arm() == ArmType.RANGED) {
                gunWidth += (squad.size() - 1) * GUN_SPACING;
                if (gunSquads > 0) {
                    gunWidth += SQUAD_GAP + GUN_SPACING;
                }
                gunSquads++;
            }
        }
        double cursor = -gunWidth / 2.0D;
        // 掩护小队站在火线两端之外，别挡住射界
        double flank = Math.max(3.0D, gunWidth / 2.0D + 2.0D);

        int meleeSeen = 0;
        int rearRank = 2;
        List<List<LocalSlot>> plan = new ArrayList<>(squads.length);
        for (SquadShape squad : squads) {
            int size = squad.size();
            List<LocalSlot> slots = new ArrayList<>(size);
            switch (squad.arm()) {
                case RANGED -> {
                    for (int i = 0; i < size; i++) {
                        slots.add(new LocalSlot(cursor + i * GUN_SPACING, 0.0D, 0.0F));
                    }
                    cursor += (size - 1) * GUN_SPACING + SQUAD_GAP + GUN_SPACING;
                }
                case MELEE -> {
                    int side = (meleeSeen++ % 2 == 0) ? -1 : 1;
                    double baseX = side * flank;
                    for (int i = 0; i < size; i++) {
                        int file = i % 2;
                        int rank = i / 2;
                        slots.add(new LocalSlot(baseX + side * file * FILE_SPACING,
                                SCREEN_FORWARD - rank * RANK_SPACING, 0.0F));
                    }
                }
                default -> {
                    int width = Math.max(1, ceilDiv(size, SQUAD_DEPTH));
                    double offset = (width - 1) / 2.0D;
                    for (int i = 0; i < size; i++) {
                        int file = i % width;
                        int rank = i / width;
                        slots.add(new LocalSlot((file - offset) * FILE_SPACING,
                                -(rearRank + rank) * RANK_SPACING, 0.0F));
                    }
                    rearRank += 2;
                }
            }
            plan.add(slots);
        }
        return plan;
    }

    /**
     * 线列：每支远程小队占一层横列，层号由 {@link SquadShape#rankOffset()} 给出
     * （由连队长的轮换逻辑填入，0 = 最前层 = 正在开火的那一层）。
     *
     * <p>层间距比普通阵型大一些（{@value #VOLLEY_RANK_SPACING} 格），
     * 一是让后排不至于贴在前排背上，二是轮换走位时不会互相挤住。
     * 近战掩护两翼，辅助压在最后。
     */
    private static List<List<LocalSlot>> firingLine(SquadShape[] squads) {
        // 先量出最宽的一层，用来决定两翼掩护站多远
        double widest = 0.0D;
        for (SquadShape squad : squads) {
            if (squad.arm().isRanged()) {
                widest = Math.max(widest, (squad.size() - 1) * FILE_SPACING);
            }
        }
        double flank = Math.max(3.0D, widest / 2.0D + 2.0D);

        int meleeSeen = 0;
        int rearRank = 0;
        List<List<LocalSlot>> plan = new ArrayList<>(squads.length);
        for (SquadShape squad : squads) {
            int size = squad.size();
            List<LocalSlot> slots = new ArrayList<>(size);
            if (squad.arm() == ArmType.RANGED) {
                // 一层一队，整层横向居中
                double offset = (size - 1) / 2.0D;
                double z = -squad.rankOffset() * VOLLEY_RANK_SPACING;
                for (int i = 0; i < size; i++) {
                    slots.add(new LocalSlot((i - offset) * FILE_SPACING, z, 0.0F));
                }
            } else if (squad.arm() == ArmType.MELEE) {
                int side = (meleeSeen++ % 2 == 0) ? -1 : 1;
                double baseX = side * flank;
                for (int i = 0; i < size; i++) {
                    int file = i % 2;
                    int rank = i / 2;
                    slots.add(new LocalSlot(baseX + side * file * FILE_SPACING,
                            SCREEN_FORWARD - rank * RANK_SPACING, 0.0F));
                }
            } else {
                int width = Math.max(1, ceilDiv(size, SQUAD_DEPTH));
                double offset = (width - 1) / 2.0D;
                for (int i = 0; i < size; i++) {
                    int file = i % width;
                    int rank = i / width;
                    slots.add(new LocalSlot((file - offset) * FILE_SPACING,
                            -(4 + rearRank + rank) * RANK_SPACING, 0.0F));
                }
                rearRank += 2;
            }
            plan.add(slots);
        }
        return plan;
    }

    // ------------------------------------------------------------------
    // 坐标变换
    // ------------------------------------------------------------------

    /** 把局部槽位按连队朝向旋转、平移到世界坐标。y 取锚点的 y，具体落地高度由寻路决定。 */
    public static Vec3 toWorld(Vec3 anchor, float facing, LocalSlot slot) {
        double rad = Math.toRadians(facing);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double x = anchor.x - cos * slot.x() - sin * slot.z();
        double z = anchor.z - sin * slot.x() + cos * slot.z();
        return new Vec3(x, anchor.y, z);
    }

    /** 槽位在世界坐标下的朝向。 */
    public static float toWorldYaw(float facing, LocalSlot slot) {
        return Mth.wrapDegrees(facing + slot.yawOffset());
    }

    /**
     * 阵型最后一排的局部 z（通常为负值；没有任何槽位时返回 0）。
     *
     * <p>{@link CompanyController} 用它算连队长的"退避距离"：把锚点放在连队长正前方
     * {@code |rearmostZ| + 余量} 处，整个阵型就完全展开在连队长前面，
     * 连队长自然留在最后排之后，而不会被挤到阵前。
     */
    public static double rearmostZ(List<List<LocalSlot>> plan) {
        double minZ = 0.0D;
        for (List<LocalSlot> squad : plan) {
            for (LocalSlot slot : squad) {
                minZ = Math.min(minZ, slot.z());
            }
        }
        return minZ;
    }

    /** 从一点指向另一点的 Minecraft 偏航角。 */
    public static float yawTowards(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        if (dx * dx + dz * dz < 1.0E-6D) {
            return 0.0F;
        }
        return Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(-dx, dz))));
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 把全连成员按顺序编号后交给 {@code fn} 逐个定位，再切回按小队分组的结构。 */
    private static List<List<LocalSlot>> distribute(SquadShape[] squads, IntFunction<LocalSlot> fn) {
        List<List<LocalSlot>> plan = new ArrayList<>(squads.length);
        int index = 0;
        for (SquadShape squad : squads) {
            List<LocalSlot> slots = new ArrayList<>(squad.size());
            for (int i = 0; i < squad.size(); i++) {
                slots.add(fn.apply(index++));
            }
            plan.add(slots);
        }
        return plan;
    }

    /** 把整个阵型左右平移，使其在 x 方向以锚点为中心。 */
    private static List<List<LocalSlot>> recenterX(List<List<LocalSlot>> plan) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (List<LocalSlot> squad : plan) {
            for (LocalSlot slot : squad) {
                min = Math.min(min, slot.x());
                max = Math.max(max, slot.x());
            }
        }
        if (min > max) {
            return plan;
        }
        double shift = (min + max) / 2.0D;
        if (Math.abs(shift) < 1.0E-6D) {
            return plan;
        }
        List<List<LocalSlot>> shifted = new ArrayList<>(plan.size());
        for (List<LocalSlot> squad : plan) {
            List<LocalSlot> slots = new ArrayList<>(squad.size());
            for (LocalSlot slot : squad) {
                slots.add(new LocalSlot(slot.x() - shift, slot.z(), slot.yawOffset()));
            }
            shifted.add(slots);
        }
        return shifted;
    }

    private static int totalSize(SquadShape[] squads) {
        int total = 0;
        for (SquadShape squad : squads) {
            total += squad.size();
        }
        return total;
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}
