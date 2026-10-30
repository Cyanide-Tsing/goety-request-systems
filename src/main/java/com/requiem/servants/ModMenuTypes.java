package com.requiem.servants;

import com.requiem.servants.block.DemandWorkBlockEntity;
import com.requiem.servants.block.DemandWorkMenu;
import com.requiem.servants.block.HorseFarmRegisterBlockEntity;
import com.requiem.servants.block.HorseFarmRegisterMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, RequiemServants.MOD_ID);

    public static final RegistryObject<MenuType<HorseFarmRegisterMenu>> HORSE_FARM_REGISTER =
            MENU_TYPES.register("horse_farm_register",
                    () -> IForgeMenuType.create((id, inv, data) -> {
                        var pos = data.readBlockPos();
                        var be = inv.player.level().getBlockEntity(pos);
                        return new HorseFarmRegisterMenu(id, inv, (HorseFarmRegisterBlockEntity) be);
                    }));

    public static final RegistryObject<MenuType<DemandWorkMenu>> DEMAND_WORK =
            MENU_TYPES.register("demand_work",
                    () -> IForgeMenuType.create((id, inv, data) -> {
                        var pos = data.readBlockPos();
                        var be = inv.player.level().getBlockEntity(pos);
                        return new DemandWorkMenu(id, inv, (DemandWorkBlockEntity) be);
                    }));

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MENU_TYPES.register(bus);
    }
}
