package com.requiem.servants.item;

import com.Polarice3.Goety.Goety;
import com.Polarice3.Goety.common.items.research.ResearchScroll;
import com.Polarice3.Goety.common.research.Research;
import com.Polarice3.Goety.common.research.ResearchList;
import com.Polarice3.Goety.utils.SEHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 爆破卷轴：继承 goety 的卷轴基类，模仿先锋卷轴（{@code front_scroll}）。
 *
 * <p>首次右键阅读时，goety 的 {@code ResearchScroll.use} 会把 {@link #DEMOLITION} 研究写入玩家
 * （{@code SEHelper.addResearch}），随后显示 {@link #researchGet()} 的文本并消耗卷轴；
 * 之后再用只会提示"已阅读"。解锁的"爆破"研究可供征兵官/兵营等训练逻辑查询
 * （{@code SEHelper.hasResearch(player, DemolitionScrollItem.DEMOLITION)}）。
 */
public class DemolitionScrollItem extends ResearchScroll {
    /** 本附属自己的爆破研究，id 为 {@code goety:demolition}。 */
    public static final Research DEMOLITION = new Research("demolition");

    static {
        // 注册进 goety 的研究列表，这样 /goety research 等命令才能识别、添加、移除它
        ResearchList.registerResearch("demolition", DEMOLITION);
    }

    public DemolitionScrollItem() {
        super(DEMOLITION);
    }

    /** 首次阅读时显示的解锁文本。 */
    @Override
    public Component researchGet() {
        return Component.translatable("requiem.scroll.demolition.read");
    }

    /** 复刻 goety 原生卷轴 tooltip：研究名金色，已阅读蓝色 / 右键阅读青色。 */
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("requiem.scroll.demolition.name").withStyle(ChatFormatting.GOLD));
        Player player = Goety.PROXY.getPlayer();
        if (player != null && SEHelper.hasResearch(player, DEMOLITION)) {
            tooltip.add(Component.translatable("info.goety.research.learned").withStyle(ChatFormatting.BLUE));
        } else {
            tooltip.add(Component.translatable("info.goety.items.scroll").withStyle(ChatFormatting.AQUA));
        }
    }
}
