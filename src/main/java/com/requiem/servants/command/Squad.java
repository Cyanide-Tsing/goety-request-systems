package com.requiem.servants.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;

/**
 * 一个小队：连队的最小编制单位。
 *
 * <p>goety 本体并没有"小队"这种实体或分组概念，所以这里把<b>同一类型的仆从</b>编为一个小队
 * （炮兵小队、卫道士小队……），超过 {@link CompanyController#SQUAD_SIZE} 人就自动拆成下一个小队。
 * 连队就是这些小队的集合。
 */
public final class Squad {
    private final EntityType<?> memberType;
    private final ArmType arm;
    private final List<UUID> members;

    public Squad(EntityType<?> memberType, ArmType arm, List<UUID> members) {
        this.memberType = memberType;
        this.arm = arm;
        this.members = new ArrayList<>(members);
    }

    public EntityType<?> memberType() {
        return this.memberType;
    }

    public ArmType arm() {
        return this.arm;
    }

    /** 小队成员，顺序即为槽位顺序。 */
    public List<UUID> members() {
        return this.members;
    }

    public int size() {
        return this.members.size();
    }

    public boolean isEmpty() {
        return this.members.isEmpty();
    }

    /** 该小队在阵型解算中的形状。 */
    public FormationLayout.SquadShape shape() {
        return new FormationLayout.SquadShape(this.size(), this.arm.rankOffset(), this.arm);
    }

    /** 例如"炮兵小队（远程）"。 */
    public MutableComponent getDisplayName() {
        return Component.translatable("requiem.squad.name", this.memberType.getDescription(), this.arm.getDisplayName());
    }
}
