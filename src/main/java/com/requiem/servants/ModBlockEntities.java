package com.requiem.servants;

import com.requiem.servants.block.DemandWorkBlockEntity;
import com.requiem.servants.block.HorseFarmRegisterBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RequiemServants.MOD_ID);

    public static final RegistryObject<BlockEntityType<HorseFarmRegisterBlockEntity>> HORSE_FARM_REGISTER =
            BLOCK_ENTITIES.register("horse_farm_register",
                    () -> BlockEntityType.Builder.of(HorseFarmRegisterBlockEntity::new,
                            ModBlocks.HORSE_FARM_REGISTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<DemandWorkBlockEntity>> DEMAND_WORK =
            BLOCK_ENTITIES.register("demand_work",
                    () -> BlockEntityType.Builder.of(DemandWorkBlockEntity::new,
                            ModBlocks.PROCESSING_TABLE.get(),
                            ModBlocks.GARDENING_TABLE.get(),
                            Blocks.FLETCHING_TABLE).build(null));

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCK_ENTITIES.register(bus);
    }
}
