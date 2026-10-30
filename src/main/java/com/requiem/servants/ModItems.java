package com.requiem.servants;

import com.Polarice3.Goety.common.items.ServantSpawnEggItem;
import com.requiem.servants.item.AppointmentItem;
import com.requiem.servants.item.ArtilleryHornItem;
import com.requiem.servants.item.AssaultOrderItem;
import com.requiem.servants.item.AcidBallFocus;
import com.requiem.servants.item.BritishGrenadiersDiscItem;
import com.requiem.servants.item.CavalryStandardItem;
import com.requiem.servants.item.ChainLightningFocus;
import com.requiem.servants.item.CompanyStandardItem;
import com.requiem.servants.item.CurrentThrowFocus;
import com.requiem.servants.item.DemolitionScrollItem;
import com.requiem.servants.item.HealingAuraFocus;
import com.requiem.servants.item.HighSpeedArrowFocus;
import com.requiem.servants.item.NotepadItem;
import com.requiem.servants.item.PaleSaberItem;
import com.requiem.servants.item.SnareDrumItem;
import com.requiem.servants.item.SnarelingWebFocus;
import com.requiem.servants.item.VoidMissileFocus;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RequiemServants.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RequiemServants.MOD_ID);

    // 刷怪蛋颜色沿用 goety：学徒=新刌民(2629137/4604468)，园丁=卫道士仆从(9804699/3295566)
    public static final RegistryObject<Item> APPRENTICE_SPAWN_EGG = ITEMS.register("apprentice_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.APPRENTICE, 2629137, 4604468, new Item.Properties()));

    public static final RegistryObject<Item> GARDENER_SPAWN_EGG = ITEMS.register("gardener_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.GARDENER, 9804699, 3295566, new Item.Properties()));

    public static final RegistryObject<Item> RECRUITER_SPAWN_EGG = ITEMS.register("recruiter_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.RECRUITER, 3421236, 9109504, new Item.Properties()));

    public static final RegistryObject<Item> ARTILLERY_SPAWN_EGG = ITEMS.register("artillery_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.ARTILLERY, 6052956, 9855796, new Item.Properties()));

    public static final RegistryObject<Item> ARTILLERY_HOSTILE_SPAWN_EGG = ITEMS.register("artillery_hostile_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ARTILLERY_HOSTILE, 3421236, 9109504, new Item.Properties()));

    // 掷弹兵刷怪蛋
    public static final RegistryObject<Item> GRENADIER_SPAWN_EGG = ITEMS.register("grenadier_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.GRENADIER, 4152387, 12833152, new Item.Properties()));

    public static final RegistryObject<Item> GRENADIER_HOSTILE_SPAWN_EGG = ITEMS.register("grenadier_hostile_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GRENADIER_HOSTILE, 4152387, 12833152, new Item.Properties()));

    // 征服者刷怪蛋
    public static final RegistryObject<Item> CONQUEROR_SPAWN_EGG = ITEMS.register("conqueror_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.CONQUEROR, 0x5C6E91, 0xB08D57, new Item.Properties()));

    // 炮兵队长刷怪蛋
    public static final RegistryObject<Item> ARTILLERY_CAPTAIN_SPAWN_EGG = ITEMS.register("artillery_captain_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.ARTILLERY_CAPTAIN, 0x3D2B1F, 0xD4AF37, new Item.Properties()));

    // 鼓手刷怪蛋
    public static final RegistryObject<Item> DRUMMER_SPAWN_EGG = ITEMS.register("drummer_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.DRUMMER, 0x6B3E2E, 0xD4AF37, new Item.Properties()));

    // 轻骑兵刷怪蛋
    public static final RegistryObject<Item> LIGHT_CAVALRY_SPAWN_EGG = ITEMS.register("light_cavalry_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.LIGHT_CAVALRY, 0x4A5D23, 0xD4AF37, new Item.Properties()));

    // 骑兵队长刷怪蛋
    public static final RegistryObject<Item> CAVALRY_CAPTAIN_SPAWN_EGG = ITEMS.register("cavalry_captain_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.CAVALRY_CAPTAIN, 0x2E1F3D, 0xD4AF37, new Item.Properties()));

    // 马倌刷怪蛋
    public static final RegistryObject<Item> GROOM_SPAWN_EGG = ITEMS.register("groom_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.GROOM, 0x6B5B3E, 0xD4AF37, new Item.Properties()));

    // 制箭师刷怪蛋
    public static final RegistryObject<Item> FLETCHER_SPAWN_EGG = ITEMS.register("fletcher_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.FLETCHER, 0x7A5C3E, 0x9DB8D2, new Item.Properties()));

    // 药剂师仆从刷怪蛋
    public static final RegistryObject<Item> BOTICARIO_SPAWN_EGG = ITEMS.register("boticario_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.BOTICARIO, 0x2F6B4F, 0xD4AF37, new Item.Properties()));

    // 敌对药剂师刷怪蛋
    public static final RegistryObject<Item> BOTICARIO_HOSTILE_SPAWN_EGG = ITEMS.register("boticario_hostile_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOTICARIO_HOSTILE, 0x2F6B4F, 0x8B0000, new Item.Properties()));

    // 连队长刷怪蛋：底色沿用传令兵，副色用金色以示军官身份
    // 用 goety 的 ServantSpawnEggItem 而不是普通刷怪蛋，这样生成时会把使用者绑定为主人
    public static final RegistryObject<Item> CAPTAIN_SPAWN_EGG = ITEMS.register("captain_spawn_egg",
            () -> new ServantSpawnEggItem(ModEntities.CAPTAIN, 3421236, 13092807, new Item.Properties()));

    // 连旗：对连队长下令的工具
    public static final RegistryObject<Item> COMPANY_STANDARD = ITEMS.register("company_standard",
            () -> new CompanyStandardItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // 突击令：指定目标点，连队自主完成侦察、炮击、突击
    public static final RegistryObject<Item> ASSAULT_ORDER = ITEMS.register("assault_order",
            () -> new AssaultOrderItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // 爆破卷轴：首次阅读解锁爆破研究（炮兵、掷弹兵与征服者训练）
    public static final RegistryObject<Item> DEMOLITION_SCROLL = ITEMS.register("demolition_scroll",
            DemolitionScrollItem::new);

    // 炮兵号角：绑定炮兵队长、号令炮兵开火
    public static final RegistryObject<Item> ARTILLERY_HORN = ITEMS.register("artillery_horn",
            () -> new ArtilleryHornItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // 苍白军刀：炮兵/掷弹兵/炮兵队长/连队长的近战武器
    public static final RegistryObject<Item> PALE_SABER = ITEMS.register("pale_saber",
            () -> new PaleSaberItem(new Item.Properties().stacksTo(1)));

    // 小军鼓：鼓手仆从的乐器
    public static final RegistryObject<Item> SNARE_DRUM = ITEMS.register("snare_drum",
            () -> new SnareDrumItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // 音乐唱片：不列颠掷弹兵（美国军乐队），暂无获取方式
    public static final RegistryObject<Item> BRITISH_GRENADIERS_DISC = ITEMS.register("british_grenadiers_disc",
            () -> new BritishGrenadiersDiscItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // 骑兵军旗：骑兵队长的指挥工具
    public static final RegistryObject<Item> CAVALRY_STANDARD = ITEMS.register("cavalry_standard",
            () -> new CavalryStandardItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // 委任状：右键绑定无工作仆从，放入工作方块槽位完成分配
    public static final RegistryObject<Item> APPOINTMENT = ITEMS.register("appointment",
            () -> new AppointmentItem(new Item.Properties().stacksTo(1)));

    // 征兵令：右键连队长切换自动/手动征兵
    public static final RegistryObject<Item> RECRUITMENT_ORDER = ITEMS.register("recruitment_order",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // 记事本：记录存储/烧炼/火炬方块位置的通用工具
    public static final RegistryObject<Item> NOTEPAD = ITEMS.register("notepad",
            () -> new NotepadItem(new Item.Properties().stacksTo(1)));

    // 炮射地图：记录炮击坐标，右键方块/空气标记，右键炮兵/炮兵队长下达打击坐标
    public static final RegistryObject<Item> STRIKE_MAP = ITEMS.register("strike_map",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // 闪电链聚晶：风暴类瞬发，以选定目标为中心弹跳闪电链
    public static final RegistryObject<Item> CHAIN_LIGHTNING_FOCUS = ITEMS.register("chain_lightning_focus",
            ChainLightningFocus::new);

    // 治愈光环聚晶：魔法类吟唱，治疗周围友方
    public static final RegistryObject<Item> HEALING_AURA_FOCUS = ITEMS.register("healing_aura_focus",
            HealingAuraFocus::new);

    // 电流投掷聚晶：风暴类瞬发，投射一簇微型电流
    public static final RegistryObject<Item> CURRENT_THROW_FOCUS = ITEMS.register("current_throw_focus",
            CurrentThrowFocus::new);

    // 酸液球聚晶：荒野类瞬发，投射酸液球并生成酸液池
    public static final RegistryObject<Item> ACID_BALL_FOCUS = ITEMS.register("acid_ball_focus",
            AcidBallFocus::new);

    // 虚空飞弹聚晶：虚空类瞬发，投射高速飞弹
    public static final RegistryObject<Item> VOID_MISSILE_FOCUS = ITEMS.register("void_missile_focus",
            VoidMissileFocus::new);

    // 凝滞聚晶：虚空类瞬发，发射迷惑灵粘液网
    public static final RegistryObject<Item> SNARELING_WEB_FOCUS = ITEMS.register("snareling_web_focus",
            SnarelingWebFocus::new);

    // 高速箭聚晶：烈风类瞬发，发射高速魔法箭矢
    public static final RegistryObject<Item> HIGH_SPEED_ARROW_FOCUS = ITEMS.register("high_speed_arrow_focus",
            HighSpeedArrowFocus::new);

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("requiem",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.requiem"))
                    .icon(() -> new ItemStack(APPRENTICE_SPAWN_EGG.get()))
                    .displayItems((params, output) -> {
                        output.accept(APPRENTICE_SPAWN_EGG.get());
                        output.accept(GARDENER_SPAWN_EGG.get());
                        output.accept(RECRUITER_SPAWN_EGG.get());
                        output.accept(ARTILLERY_SPAWN_EGG.get());
                        output.accept(ARTILLERY_HOSTILE_SPAWN_EGG.get());
                        output.accept(GRENADIER_SPAWN_EGG.get());
                        output.accept(GRENADIER_HOSTILE_SPAWN_EGG.get());
                        output.accept(CONQUEROR_SPAWN_EGG.get());
                        output.accept(ARTILLERY_CAPTAIN_SPAWN_EGG.get());
                        output.accept(CAPTAIN_SPAWN_EGG.get());
                        output.accept(DRUMMER_SPAWN_EGG.get());
                        output.accept(LIGHT_CAVALRY_SPAWN_EGG.get());
                        output.accept(CAVALRY_CAPTAIN_SPAWN_EGG.get());
                        output.accept(GROOM_SPAWN_EGG.get());
                        output.accept(FLETCHER_SPAWN_EGG.get());
                        output.accept(BOTICARIO_SPAWN_EGG.get());
                        output.accept(BOTICARIO_HOSTILE_SPAWN_EGG.get());
                        output.accept(ModBlocks.HORSE_FARM_REGISTER_ITEM.get());
                        output.accept(ModBlocks.PROCESSING_TABLE_ITEM.get());
                        output.accept(ModBlocks.GARDENING_TABLE_ITEM.get());
                        output.accept(COMPANY_STANDARD.get());
                        output.accept(ASSAULT_ORDER.get());
                        output.accept(DEMOLITION_SCROLL.get());
                        output.accept(ARTILLERY_HORN.get());
                        output.accept(PALE_SABER.get());
                        output.accept(SNARE_DRUM.get());
                        output.accept(BRITISH_GRENADIERS_DISC.get());
                        output.accept(CAVALRY_STANDARD.get());
                        output.accept(APPOINTMENT.get());
                        output.accept(RECRUITMENT_ORDER.get());
                        output.accept(NOTEPAD.get());
                        output.accept(STRIKE_MAP.get());
                        output.accept(CHAIN_LIGHTNING_FOCUS.get());
                        output.accept(HEALING_AURA_FOCUS.get());
                        output.accept(CURRENT_THROW_FOCUS.get());
                        output.accept(ACID_BALL_FOCUS.get());
                        output.accept(VOID_MISSILE_FOCUS.get());
                        output.accept(SNARELING_WEB_FOCUS.get());
                        output.accept(HIGH_SPEED_ARROW_FOCUS.get());
                    })
                    .build());

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        CREATIVE_MODE_TABS.register(bus);
    }
}
