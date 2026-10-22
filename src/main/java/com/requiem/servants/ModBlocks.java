package com.requiem.servants;

import com.requiem.servants.block.HorseFarmRegisterBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, RequiemServants.MOD_ID);

    public static final RegistryObject<Block> HORSE_FARM_REGISTER = BLOCKS.register("horse_farm_register",
            () -> new HorseFarmRegisterBlock(BlockBehaviour.Properties.of().strength(2.0F).noOcclusion()));

    public static final RegistryObject<Item> HORSE_FARM_REGISTER_ITEM = ModItems.ITEMS.register("horse_farm_register",
            () -> new BlockItem(HORSE_FARM_REGISTER.get(), new Item.Properties()));

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
    }
}
