package com.requiem.servants.command;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 连队姿态。决定阵型约束有多硬：成员是死守槽位、还是可以脱离阵型追击。
 *
 * <p>姿态既可以由 {@link CompanyController} 根据威胁自动评估，也可以由玩家用连旗锁定一段时间。
 */
public enum CompanyStance {
    /** 整队：回到槽位、统一朝向，不主动交战。 */
    FORM_UP("form_up"),
    /** 坚守：守住槽位并保持朝向，只打已经贴到身上的敌人，绝不追击。 */
    HOLD("hold"),
    /** 前进：锚点随威胁方向推进，成员保持槽位跟着走。 */
    ADVANCE("advance"),
    /** 交战：保持阵型，但允许在一定范围内脱离槽位反击。 */
    ENGAGE("engage"),
    /** 冲锋：解散阵型，全员自由突击。 */
    CHARGE("charge"),
    /** 集结：向连队长紧缩收拢，用于队伍被打散后重整。 */
    RALLY("rally");

    private static final CompanyStance[] VALUES = values();

    private final String serializedName;

    CompanyStance(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getTranslationKey() {
        return "requiem.stance." + this.serializedName;
    }

    public MutableComponent getDisplayName() {
        return Component.translatable(this.getTranslationKey());
    }

    public static CompanyStance byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : FORM_UP;
    }

    /** 冲锋不再解散阵型：成员的槽位始终保留，让线列保持队形推进。 */
    public boolean releasesFormation() {
        return false;
    }

    /** 该姿态下成员是否可以主动脱离槽位去追击目标。 */
    public boolean allowsPursuit() {
        return this == ENGAGE || this == CHARGE;
    }

    /** 该姿态下成员是否必须严格保持阵型朝向（而不是朝着移动方向）。 */
    public boolean requiresStrictFacing() {
        return this == FORM_UP || this == HOLD;
    }

    /** 该姿态下锚点是否跟着连队长/主人连续移动。 */
    public boolean anchorFollows() {
        return this == ADVANCE || this == RALLY || this == CHARGE;
    }

    /** 该姿态下的移动速度倍率。 */
    public double speedModifier() {
        return switch (this) {
            case RALLY, CHARGE -> 1.25D;
            case ADVANCE -> 1.1D;
            default -> 1.0D;
        };
    }
}
