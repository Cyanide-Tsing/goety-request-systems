package com.requiem.servants;

import com.Polarice3.Goety.common.items.ServantSpawnEggItem;
import com.requiem.servants.item.ArtilleryHornItem;
import com.requiem.servants.item.AssaultOrderItem;
import com.requiem.servants.item.CompanyStandardItem;
import com.requiem.servants.item.DemolitionScrollItem;
import com.requiem.servants.item.PaleSaberItem;
import com.requiem.servants.item.SnareDrumItem;
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
                        output.accept(COMPANY_STANDARD.get());
                        output.accept(ASSAULT_ORDER.get());
                        output.accept(DEMOLITION_SCROLL.get());
                        output.accept(ARTILLERY_HORN.get());
                        output.accept(PALE_SABER.get());
                        output.accept(SNARE_DRUM.get());
                    })
                    .build());

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        CREATIVE_MODE_TABS.register(bus);
    }
}
