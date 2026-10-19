package com.requiem.servants.command;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 连队长可以下令采用的阵型。
 *
 * <p>阵型只描述"形状"，具体的槽位坐标与朝向由 {@link FormationLayout} 解算。
 */
public enum FormationType {
    /** 横列阵：宽正面、浅纵深，适合齐射与拦阻。 */
    LINE("line"),
    /** 纵队：窄正面、深纵深，用于行军与穿越狭窄通道。 */
    COLUMN("column"),
    /** 实心方阵：各方向均衡，适合混战。 */
    SQUARE("square"),
    /** 空心方阵：四面朝外，防止被包围。 */
    HOLLOW_SQUARE("hollow_square"),
    /** 楔形阵（锋矢）：尖端朝前，用于突破。 */
    WEDGE("wedge"),
    /** 梯次阵：各小队斜向错开，掩护一侧翼。 */
    ECHELON("echelon"),
    /** 环形阵：全员成环朝外，原地固守。 */
    CIRCLE("circle"),
    /** 散兵线：大间距松散队形，减少范围伤害。 */
    SKIRMISH("skirmish"),
    /**
     * 炮列：远程小队在前排拉开成固定火线并<b>钉死不动</b>，近战小队掩护两翼，施法与辅助压在后方。
     * 这是唯一会让炮兵"就地架炮"的阵型，开火由连队长统一号令。
     */
    GUN_LINE("gun_line"),
    /**
     * 线列：远程步兵排成二至三层横列轮番射击。
     *
     * <p>每支远程小队占一层，最前一层开火、后面各层"装填"（被压住不放箭），
     * 连队长按节奏轮换各层的前后次序——掷弹兵投完一轮就换到后面，
     * 由后排的掠夺者接着输出。近战掩护两翼，辅助压在最后。
     */
    FIRING_LINE("firing_line");

    private static final FormationType[] VALUES = values();

    private final String serializedName;

    FormationType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getTranslationKey() {
        return "requiem.formation." + this.serializedName;
    }

    public MutableComponent getDisplayName() {
        return Component.translatable(this.getTranslationKey());
    }

    /** 按固定顺序切换到下一个阵型（连旗右键用）。 */
    public FormationType next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public static FormationType byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : LINE;
    }

    public static FormationType byName(String name) {
        for (FormationType type : VALUES) {
            if (type.serializedName.equals(name)) {
                return type;
            }
        }
        return LINE;
    }

    /**
     * 该阵型的槽位是否各自朝外，而不是统一朝向连队正面。
     * 朝外的阵型同时也把指挥位放在阵中心。
     */
    public boolean facesOutward() {
        return this == HOLLOW_SQUARE || this == CIRCLE;
    }

    /** 该阵型在移动中是否仍能保持队形（行军阵型）。 */
    public boolean isMarchFormation() {
        return this == COLUMN || this == ECHELON;
    }

    /**
     * 该阵型是否按小队整块摆放（而不是把全连人员打散重排）。
     * 整块摆放的阵型会用到 {@link ArmType#rankOffset()} 把远程与辅助小队压到后排。
     */
    public boolean keepsSquadBlocks() {
        return this == LINE || this == COLUMN || this == ECHELON || this == HOLLOW_SQUARE || this == GUN_LINE;
    }

    /**
     * 该阵型是否把指定兵种的小队"钉死"在槽位上。
     *
     * <p>炮列重构后，炮兵不再通过阵型解算钉死——它们由
     * {@code CompanyController#maintainArtillery} 单独下发炮位令（{@code orderToCannon}）。
     * 留在阵型里的线列单位（掷弹兵、近战等）一律可发起冲锋，因此这里恒为 {@code false}。
     */
    public boolean anchors(ArmType arm) {
        return false;
    }

    /** 该阵型是否以远程火力为主（连队长会主动指挥齐射）。 */
    public boolean isFirebase() {
        return this == GUN_LINE || this == FIRING_LINE;
    }

    /**
     * 该阵型是否按层轮番射击。
     *
     * <p>为真时连队长会给每支远程小队排一个层号，只有最前层获准开火，
     * 其余层被压住装填，并按节奏轮换（见 {@code CompanyController} 的齐射轮换）。
     */
    public boolean usesVolleyRanks() {
        return this == FIRING_LINE;
    }
}
