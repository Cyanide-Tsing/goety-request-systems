package com.requiem.servants.train;

import com.Polarice3.Goety.api.entities.ally.illager.ITrainIllager;
import com.Polarice3.Goety.common.entities.ally.illager.Neollager;
import com.Polarice3.Goety.common.ritual.RitualChecker;
import com.requiem.servants.ModBlocks;
import com.requiem.servants.ModEntities;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Requiem 仆从的不祥火炬训练类型。
 *
 * <p>与 Goety 的 {@code ModIllagerType} / GoetyAwaken 的 {@code GoetyAwakenIllagerType} 一样，
 * 通过 {@code IllagerType.create("REQUIEM", ...)} 挂进不祥火炬（不祥篝火）的训练判定：
 * 依据火炬周围摆放的方块，把忠诚的刌民训练成对应的 Requiem 仆从。
 */
public class RequiemIllagerType implements ITrainIllager {

    @Override
    public boolean canSpawn(Level level, BlockPos blockPos, int range) {
        return this.getIllager(level, blockPos, range) != null;
    }

    @Override
    public boolean mobCanTrainTo(Mob mob, Level level, BlockPos blockPos, int range) {
        return mob instanceof Neollager;
    }

    private static boolean isBlock(BlockState state, String namespace, String path) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key != null && namespace.equals(key.getNamespace()) && path.equals(key.getPath());
    }

    private static boolean isItem(ItemStack stack, String namespace, String path) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && namespace.equals(key.getNamespace()) && path.equals(key.getPath());
    }

    /** 检查范围内是否有 {@code required} 个 Supplementaries 展示台，其槽位上放了符合 {@code itemCheck} 的物品。 */
    private static boolean hasPedestalsWithItem(Level level, BlockPos center, int range,
            Predicate<ItemStack> itemCheck, int required) {
        int count = 0;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (!isBlock(level.getBlockState(p), "supplementaries", "pedestal")) {
                        continue;
                    }
                    BlockEntity be = level.getBlockEntity(p);
                    if (be == null) {
                        continue;
                    }
                    IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
                    if (handler == null || handler.getSlots() == 0) {
                        continue;
                    }
                    ItemStack stack = handler.getStackInSlot(0);
                    if (!stack.isEmpty() && itemCheck.test(stack) && ++count >= required) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public EntityType<? extends Mob> getIllager(Level level, BlockPos blockPos, int range) {
        RitualChecker checker = new RitualChecker(level, blockPos, blockState -> true, range, 0);

        // 学徒：熔炉/高炉 + 工作台 + 木桶
        if (checker.hasBlocks(state -> state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE), 4)
                && checker.hasBlocks(state -> state.is(Blocks.CRAFTING_TABLE), 2)
                && checker.hasBlocks(state -> state.is(Blocks.BARREL), 6)) {
            return ModEntities.APPRENTICE.get();
        }

        // 园丁：种植盆 + 酿造台 + 瓮 + 堆肥桶
        if (checker.hasBlocks(state -> isBlock(state, "supplementaries", "planter"), 4)
                && checker.hasBlocks(state -> state.is(Blocks.BREWING_STAND), 2)
                && checker.hasBlocks(state -> isBlock(state, "supplementaries", "urn"), 4)
                && checker.hasBlocks(state -> state.is(Blocks.COMPOSTER), 2)) {
            return ModEntities.GARDENER.get();
        }

        // 征兵官：床 + 讲台 + 旗帜 + 木桶
        if (checker.hasBlocks(state -> state.getBlock() instanceof BedBlock, 6)
                && checker.hasBlocks(state -> state.is(Blocks.LECTERN), 2)
                && checker.hasBlocks(state -> state.is(BlockTags.BANNERS), 2)
                && checker.hasBlocks(state -> state.is(Blocks.BARREL), 2)) {
            return ModEntities.RECRUITER.get();
        }

        // 马倌：干草块 + 栅栏 + 马场登记站 + 木桶
        if (checker.hasBlocks(state -> state.is(Blocks.HAY_BLOCK), 12)
                && checker.hasBlocks(state -> state.is(Tags.Blocks.FENCES_WOODEN), 12)
                && checker.hasBlocks(state -> state.is(ModBlocks.HORSE_FARM_REGISTER.get()), 1)
                && checker.hasBlocks(state -> state.is(Blocks.BARREL), 2)) {
            return ModEntities.GROOM.get();
        }

        // 制箭师：制箭台 + 木桶 + 羽毛块 + 燧石块 + 标靶
        if (checker.hasBlocks(state -> state.is(Blocks.FLETCHING_TABLE), 3)
                && checker.hasBlocks(state -> state.is(Blocks.BARREL), 6)
                && checker.hasBlocks(state -> isBlock(state, "supplementaries", "feather_block"), 2)
                && checker.hasBlocks(state -> isBlock(state, "supplementaries", "flint_block"), 2)
                && checker.hasBlocks(state -> state.is(Blocks.TARGET), 2)) {
            return ModEntities.FLETCHER.get();
        }

        // 炮兵：加农炮 + 炮弹 + 黑板 + 铁块 + 木桶 + 红砖块 + 木板 + 展台(炸弹) + 展台(火焰弹)
        if (checker.hasBlocks(state -> isBlock(state, "supplementaries", "cannon"), 2)
                && checker.hasBlocks(state -> isBlock(state, "supplementaries", "cannonball"), 16)
                && checker.hasBlocks(state -> isBlock(state, "supplementaries", "blackboard"), 12)
                && checker.hasBlocks(state -> state.is(Blocks.IRON_BLOCK), 16)
                && checker.hasBlocks(state -> state.is(Blocks.BARREL), 8)
                && checker.hasBlocks(state -> state.is(Blocks.BRICKS), 32)
                && checker.hasBlocks(state -> state.is(BlockTags.PLANKS), 32)
                && hasPedestalsWithItem(level, blockPos, range,
                        stack -> isItem(stack, "supplementaries", "bomb"), 1)
                && hasPedestalsWithItem(level, blockPos, range,
                        stack -> isItem(stack, "minecraft", "fire_charge"), 1)) {
            return ModEntities.ARTILLERY.get();
        }

        // 掷弹兵：TNT + 弹射台 + 木桶 + 红砖块 + 木板 + 展台(炸弹)×2
        if (checker.hasBlocks(state -> state.is(Blocks.TNT), 24)
                && checker.hasBlocks(state -> isBlock(state, "supplementaries", "spring_launcher"), 8)
                && checker.hasBlocks(state -> state.is(Blocks.BARREL), 16)
                && checker.hasBlocks(state -> state.is(Blocks.BRICKS), 32)
                && checker.hasBlocks(state -> state.is(BlockTags.PLANKS), 32)
                && hasPedestalsWithItem(level, blockPos, range,
                        stack -> isItem(stack, "supplementaries", "bomb"), 2)) {
            return ModEntities.GRENADIER.get();
        }

        // 征服者：铁砧 + 锻造台 + 旗帜 + 栅栏 + 标靶 + 红石灯 + 红砖块 + 木板 + 展台(烟花火箭)
        if (checker.hasBlocks(state -> state.getBlock() instanceof AnvilBlock, 8)
                && checker.hasBlocks(state -> state.is(Blocks.SMITHING_TABLE), 8)
                && checker.hasBlocks(state -> state.is(BlockTags.BANNERS), 4)
                && checker.hasBlocks(state -> state.is(Tags.Blocks.FENCES_WOODEN), 32)
                && checker.hasBlocks(state -> state.is(Blocks.TARGET), 2)
                && checker.hasBlocks(state -> state.is(Blocks.REDSTONE_LAMP), 2)
                && checker.hasBlocks(state -> state.is(Blocks.BRICKS), 32)
                && checker.hasBlocks(state -> state.is(BlockTags.PLANKS), 32)
                && hasPedestalsWithItem(level, blockPos, range,
                        stack -> isItem(stack, "minecraft", "firework_rocket"), 1)) {
            return ModEntities.CONQUEROR.get();
        }

        // 药剂师：酿造台 + 书架 + 炼药锅 + 展台(金苹果)
        if (checker.hasBlocks(state -> state.is(Blocks.BREWING_STAND), 8)
                && checker.hasBlocks(state -> state.is(Blocks.BOOKSHELF), 32)
                && checker.hasBlocks(state -> state.is(Blocks.CAULDRON), 8)
                && hasPedestalsWithItem(level, blockPos, range,
                        stack -> isItem(stack, "minecraft", "golden_apple"), 1)) {
            return ModEntities.BOTICARIO.get();
        }

        return null;
    }
}
