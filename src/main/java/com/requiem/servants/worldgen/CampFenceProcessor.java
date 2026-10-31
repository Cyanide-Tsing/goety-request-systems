package com.requiem.servants.worldgen;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 结构处理器：结构生成时记录整座营地的包围盒与道路方块位置，生成结束后在营地外围绕一圈
 * 去皮原木柱栅栏（原木 2-5 格 + 顶上一格栅栏），柱位做平滑随机游走形成自然凹凸弧线且保持连续，
 * 大门动态对齐道路出口；再在营地内部空旷的草方块上种一些对应树种的树。
 */
public final class CampFenceProcessor extends StructureProcessor {
    public static final Codec<CampFenceProcessor> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(Codec.STRING.fieldOf("wood").forGetter(p -> p.wood))
                    .apply(inst, CampFenceProcessor::new));

    public static final StructureProcessorType<CampFenceProcessor> TYPE = () -> CODEC;

    private static final Map<BlockPos, CampTracker> ACTIVE = new ConcurrentHashMap<>();

    private final String wood;

    public CampFenceProcessor(String wood) {
        this.wood = wood;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos piecePos, BlockPos centerPos,
                                                             StructureTemplate.StructureBlockInfo raw,
                                                             StructureTemplate.StructureBlockInfo block,
                                                             StructurePlaceSettings settings) {
        if (block != null && !block.state().isAir()) {
            CampTracker tracker = ACTIVE.computeIfAbsent(centerPos, k -> new CampTracker());
            tracker.include(block.pos());
            Block b = block.state().getBlock();
            if (b == Blocks.COBBLESTONE || b == Blocks.MOSSY_COBBLESTONE || b == Blocks.GRAVEL) {
                tracker.roads.add(block.pos().asLong());
            }
            if (!tracker.scheduled && level instanceof ServerLevel sl) {
                tracker.scheduled = true;
                sl.getServer().execute(() -> buildFence(sl, centerPos));
            }
        }
        return block;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE;
    }

    private void buildFence(ServerLevel level, BlockPos centerPos) {
        CampTracker t = ACTIVE.remove(centerPos);
        if (t == null) {
            return;
        }
        Block log = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", "stripped_" + wood + "_log"));
        Block fence = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", wood + "_fence"));
        if (log == null || fence == null) {
            return;
        }

        int minX = t.minX - 2;
        int maxX = t.maxX + 2;
        int minZ = t.minZ - 2;
        int maxZ = t.maxZ + 2;
        int y = t.minY;

        // 北边（z=minZ），随机游走偏移在 z 方向
        int off = 0;
        for (int x = minX; x <= maxX; x++) {
            off = clamp(off + randOffset(), -2, 2);
            if (!isRoadExit(x, minZ, y, 0, 1, t)) {
                placePillar(level, new BlockPos(x, y, minZ + off), log, fence);
            }
        }
        // 南边（z=maxZ）
        off = 0;
        for (int x = minX; x <= maxX; x++) {
            off = clamp(off + randOffset(), -2, 2);
            if (!isRoadExit(x, maxZ, y, 0, -1, t)) {
                placePillar(level, new BlockPos(x, y, maxZ - off), log, fence);
            }
        }
        // 西边（x=minX）
        off = 0;
        for (int z = minZ; z <= maxZ; z++) {
            off = clamp(off + randOffset(), -2, 2);
            if (!isRoadExit(minX, z, y, 1, 0, t)) {
                placePillar(level, new BlockPos(minX + off, y, z), log, fence);
            }
        }
        // 东边（x=maxX）
        off = 0;
        for (int z = minZ; z <= maxZ; z++) {
            off = clamp(off + randOffset(), -2, 2);
            if (!isRoadExit(maxX, z, y, -1, 0, t)) {
                placePillar(level, new BlockPos(maxX - off, y, z), log, fence);
            }
        }

        plantTrees(level, t);
    }

    private int randOffset() {
        return (int) (Math.random() * 3) - 1;
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private boolean isRoadExit(int x, int z, int y, int inwardX, int inwardZ, CampTracker t) {
        for (int i = 1; i <= 3; i++) {
            int cx = x + inwardX * i;
            int cz = z + inwardZ * i;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (t.roads.contains(BlockPos.asLong(cx + dx, y, cz + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void placePillar(ServerLevel level, BlockPos base, Block log, Block fence) {
        int logHeight = 4 + (int) (Math.random() * 4); // 4-8
        BlockState logState = log.defaultBlockState();
        BlockState fenceState = fence.defaultBlockState();
        for (int i = 0; i < logHeight; i++) {
            BlockPos p = base.above(i);
            if (level.getBlockState(p).isAir()) {
                level.setBlock(p, logState, 3);
            }
        }
        BlockPos top = base.above(logHeight);
        if (level.getBlockState(top).isAir()) {
            level.setBlock(top, fenceState, 3);
        }
    }

    private void plantTrees(ServerLevel level, CampTracker t) {
        String treeName = "acasia".equals(wood) ? "acacia" : wood;
        Holder<ConfiguredFeature<?, ?>> tree = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation("minecraft", treeName)))
                .orElse(null);
        if (tree == null) {
            return;
        }
        for (int x = t.minX + 2; x <= t.maxX - 2; x += 2) {
            for (int z = t.minZ + 2; z <= t.maxZ - 2; z += 2) {
                if (Math.random() > 0.04) {
                    continue;
                }
                BlockPos grass = findGrass(level, x, z, t.minY);
                if (grass != null && level.getBlockState(grass.above()).isAir()) {
                    tree.value().place(level, level.getChunkSource().getGenerator(), level.random, grass.above());
                }
            }
        }
    }

    private BlockPos findGrass(ServerLevel level, int x, int z, int baseY) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos p = new BlockPos(x, baseY + dy, z);
            if (level.getBlockState(p).is(Blocks.GRASS_BLOCK)) {
                return p;
            }
            if (!level.getBlockState(p).isAir() && !level.getBlockState(p).is(Blocks.GRASS_BLOCK)) {
                return null;
            }
        }
        return null;
    }

    private static final class CampTracker {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        boolean scheduled;
        final Set<Long> roads = ConcurrentHashMap.newKeySet();

        void include(BlockPos p) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
    }
}
