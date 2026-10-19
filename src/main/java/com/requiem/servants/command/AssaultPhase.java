package com.requiem.servants.command;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 自主突击作战的阶段。
 *
 * <p>玩家用"突击令"指定一个目标点之后，连队长会自己把这套流程走完，全程不需要玩家在场：
 * 逼近 → 侦察 → 炮击 → 突击 → 占领。每个阶段会决定连队的锚点、阵型与姿态。
 */
public enum AssaultPhase {
    /** 没有作战任务，连队按常规自动指挥。 */
    NONE("none", 0.0D),
    /**
     * 逼近：收成纵队行军到侦察距离。
     *
     * <p>站位距离<b>必须</b>和 {@link #SCOUT} 一致——这个阶段的任务就是"走到侦察位置"，
     * 如果它的站位比转阶段的判定距离还远，连队长就会停在原地永远走不到，作战卡在第一步。
     */
    APPROACH("approach", 28.0D),
    /** 侦察：在目标外围散开观察，清点守军。 */
    SCOUT("scout", 28.0D),
    /** 炮击：架炮列轰击目标区域，压制守军。 */
    BOMBARD("bombard", 22.0D),
    /** 突击：楔形压上去，近战与远程一起接敌。 */
    ASSAULT("assault", 6.0D),
    /** 占领：在目标点整队肃清残敌。 */
    SECURE("secure", 3.0D);

    private static final AssaultPhase[] VALUES = values();

    private final String serializedName;
    private final double standoff;

    AssaultPhase(String serializedName, double standoff) {
        this.serializedName = serializedName;
        this.standoff = standoff;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public MutableComponent getDisplayName() {
        return Component.translatable("requiem.assault." + this.serializedName);
    }

    /** 该阶段连队锚点与目标点保持的距离。 */
    public double standoff() {
        return this.standoff;
    }

    public boolean isActive() {
        return this != NONE;
    }

    public static AssaultPhase byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
    }

    /** 该阶段适用的阵型。 */
    public FormationType formation() {
        return switch (this) {
            case APPROACH -> FormationType.COLUMN;
            case SCOUT -> FormationType.SKIRMISH;
            case BOMBARD -> FormationType.GUN_LINE;
            case ASSAULT -> FormationType.WEDGE;
            default -> FormationType.LINE;
        };
    }

    /** 该阶段适用的姿态。 */
    public CompanyStance stance() {
        return switch (this) {
            case APPROACH -> CompanyStance.ADVANCE;
            case SCOUT, BOMBARD -> CompanyStance.HOLD;
            case ASSAULT -> CompanyStance.ENGAGE;
            default -> CompanyStance.FORM_UP;
        };
    }

    /** 该阶段是否需要炮兵开火。 */
    public boolean wantsBombardment() {
        return this == BOMBARD;
    }
}
