package dev.lukebemish.excavatedvariants.impl.worldgen;

import dev.lukebemish.excavatedvariants.api.data.Ore;
import dev.lukebemish.excavatedvariants.api.data.Stone;
import dev.lukebemish.excavatedvariants.impl.ExcavatedVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jspecify.annotations.NonNull;

public class OreReplacer extends Feature<NoneFeatureConfiguration> {
    private static final int[] xs = new int[]{-1, 0, 1, 1, -1, -1, 0, 1};
    private static final int[] zs = new int[]{-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int[] as = {1, 0, 0, -1, 0, 0};
    private static final int[] bs = {0, -1, 1, 0, 0, 0};
    private static final int[] ys = {0, 0, 0, 0, -1, 1};

    public OreReplacer() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(@NonNull FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!ExcavatedVariants.getConfig().attemptWorldgenReplacement)
            return true;
        return modifyUnmodifiedNeighboringChunks(ctx.level(), ctx.origin());
    }

    public boolean modifyUnmodifiedNeighboringChunks(WorldGenLevel level, BlockPos pos) {
        OreGenMapSavedData data = OreGenMapSavedData.getOrCreate(level);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        BlockPos.MutableBlockPos newPos = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        for (int i = 0; i < xs.length; i++) {
            newPos.setX(pos.getX() + xs[i] * 16);
            newPos.setZ(pos.getZ() + zs[i] * 16);
            OreGenMapSavedData.ChunkKey chunkPos = new OreGenMapSavedData.ChunkKey(newPos.getX(), newPos.getZ());
            data.incrEdgeCount(chunkPos);
            if (data.getEdgeCount(chunkPos) >= 8 && data.didChunkRun(chunkPos)) {
                ChunkAccess chunkAccess = level.getChunk(newPos);
                modifyChunk(chunkAccess, minY, maxY);
                data.setEdgeCount(chunkPos, 9);
            }
        }
        OreGenMapSavedData.ChunkKey chunkPos = new OreGenMapSavedData.ChunkKey(pos.getX(), pos.getZ());
        data.chunkRan(chunkPos);
        if (data.getEdgeCount(chunkPos) >= 8) {
            ChunkAccess chunkAccess = level.getChunk(pos);
            modifyChunk(chunkAccess, minY, maxY);
            data.setEdgeCount(chunkPos, 9);
        }
        return true;
    }

    public void modifyChunk(ChunkAccess chunkAccess, int minY, int maxY) {
        LevelChunkSection chunkSection = chunkAccess.getSection(chunkAccess.getSectionIndex(minY));
        int oldSectionIndex = SectionPos.blockToSectionCoord(minY);
        for (int y = minY; y < maxY; y++) {
            BlockState[][][] cache = new BlockState[16][16][16];
            int sectionIndex = SectionPos.blockToSectionCoord(y);
            if (oldSectionIndex != sectionIndex) {
                chunkSection = chunkAccess.getSection(chunkAccess.getSectionIndex(y));
                oldSectionIndex++;
            }
            if (chunkSection.hasOnlyAir()) {
                y += 15;
                continue;
            }
            for (int i = 0; i < 16; i++) {
                inner_loop:
                for (int j = 0; j < 16; j++) {
                    BlockState newState = cache[i][y & 15][j] == null ? chunkSection.getBlockState(i, y & 15, j) : cache[i][y & 15][j];
                    if (cache[i][y & 15][j] == null) {
                        cache[i][y & 15][j] = newState;
                    }
                    Ore ore = ((OreFound) newState.getBlock()).excavated_variants$getOre();
                    if (ore != null) {
                        Stone oreStone = ((OreFound) newState.getBlock()).excavated_variants$getOreStone();
                        for (int c = 0; c < as.length; c++) {
                            if (i + as[c] < 16 && i + as[c] >= 0 && j + bs[c] < 16 && j + bs[c] >= 0 && y + ys[c] >= SectionPos.sectionToBlockCoord(sectionIndex) && y + ys[c] < SectionPos.sectionToBlockCoord(sectionIndex + 1)) {
                                BlockState thisState = cache[i + as[c]][y + ys[c] & 15][j + bs[c]];
                                if (thisState == null) {
                                    thisState = chunkSection.getBlockState(i + as[c], y + ys[c] & 15, j + bs[c]);
                                    cache[i + as[c]][y + ys[c] & 15][j + bs[c]] = thisState;
                                }
                                Stone stone = ((OreFound) thisState.getBlock()).excavated_variants$getStone();
                                if (stone != null && stone != oreStone) {
                                    Block newOreBlock = OreFinderUtil.getBlock(ore, stone);
                                    if (newOreBlock != null) {
                                        BlockState def = newOreBlock.withPropertiesOf(thisState);
                                        chunkSection.setBlockState(i, y & 15, j, def, false);
                                        continue inner_loop;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
