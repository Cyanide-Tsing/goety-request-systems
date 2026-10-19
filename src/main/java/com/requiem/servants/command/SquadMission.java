package com.requiem.servants.command;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 小队任务。
 *
 * <p>连队不是把所有人揉成一个方阵——每个小队是一个<b>可独立调动的单位</b>，
 * 连队长根据敌情给每支小队单独下任务，小队各自在自己的落脚点列队。
 * 只有领到 {@link #HOLD_LINE} 的小队才会去占连队主阵型里的位置，
 * 其余小队都按任务脱离主阵独立行动。
 *
 * <p>这正是"派卫道士去迎击对面的卫道士"之类的战术调度能够成立的基础。
 */
public enum SquadMission {
    /** 守阵线：占据连队主阵型中属于自己的那一块。 */
    HOLD_LINE("hold_line"),
    /** 前出掩护：在连队前方散开，替后排挡住冲上来的敌人。 */
    SCREEN("screen"),
    /** 对位拦截：开进到指定敌军面前并交战（派近战去打近战、去撕远程）。 */
    COUNTER("counter"),
    /** 火力支援：拉开到自己兵种的理想距离射击。 */
    SUPPORT_FIRE("support_fire"),
    /** 操炮：守着自己的炮位，完全不参与阵型。 */
    MAN_GUNS("man_guns"),
    /** 后备：压在连队后方待命。 */
    RESERVE("reserve");

    private static final SquadMission[] VALUES = values();

    private final String serializedName;

    SquadMission(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public MutableComponent getDisplayName() {
        return Component.translatable("requiem.mission." + this.serializedName);
    }

    public static SquadMission byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : HOLD_LINE;
    }

    /** 该任务是否占用连队主阵型里的位置。 */
    public boolean usesCompanyFormation() {
        return this == HOLD_LINE;
    }

    /** 该任务是否完全不接受阵型站位（炮组由炮位管）。 */
    public boolean skipsFormation() {
        return this == MAN_GUNS;
    }

    /** 独立行动的小队自己采用的队形。 */
    public FormationType shape() {
        return switch (this) {
            case COUNTER -> FormationType.WEDGE;
            case SCREEN -> FormationType.SKIRMISH;
            default -> FormationType.LINE;
        };
    }

    /**
     * 该任务下小队的姿态。
     *
     * <p>拦截任务必须用 {@link CompanyStance#ENGAGE}：这样成员在接敌后会被允许脱离槽位厮杀
     * （见 {@link com.requiem.servants.ai.FormationSlotGoal}），否则派出去也只会站着挨打。
     */
    public CompanyStance stance(CompanyStance companyStance) {
        return switch (this) {
            case COUNTER -> CompanyStance.ENGAGE;
            case SCREEN -> CompanyStance.HOLD;
            case MAN_GUNS -> CompanyStance.HOLD;
            default -> companyStance;
        };
    }
}
