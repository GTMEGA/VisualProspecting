package com.sinthoras.visualprospecting.database;

import com.sinthoras.visualprospecting.Constants;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import com.sinthoras.visualprospecting.integration.gregtech.UndergroundFluidsWrapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gregtech.api.events.GT_OreVeinLocations;
import gregtech.common.GT_Worldgen_GT_Ore_Layer;
import lombok.val;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class ServerCache extends WorldCache {

    public static final ServerCache instance = new ServerCache();

    protected File getStorageDirectory() {
        return Utils.getSubDirectory(Constants.SERVER_DIR);
    }

    public synchronized void notifyOreVeinGeneration(int dimensionId, int chunkX, int chunkZ, final VeinType veinType) {
        if (veinType != VeinType.NO_VEIN) {
            super.putOreVein(new OreVeinPosition(dimensionId, chunkX, chunkZ, veinType));
        }
    }

    public void notifyOreVeinGeneration(int dimensionId, int chunkX, int chunkZ, final String veinName) {
        notifyOreVeinGeneration(dimensionId, chunkX, chunkZ, VeinTypeCaching.getVeinType(veinName));
    }

    public List<OreVeinPosition> prospectOreChunks(
            int dimensionId, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        minChunkX = Utils.mapToCenterOreChunkCoord(minChunkX);
        minChunkZ = Utils.mapToCenterOreChunkCoord(minChunkZ);
        maxChunkX = Utils.mapToCenterOreChunkCoord(maxChunkX);
        maxChunkZ = Utils.mapToCenterOreChunkCoord(maxChunkZ);

        List<OreVeinPosition> oreVeinPositions = new ArrayList<>();

        val world = DimensionManager.getWorld(dimensionId);
        if (world == null) {
            return oreVeinPositions;
        }

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX = Utils.mapToCenterOreChunkCoord(chunkX + 3)) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ = Utils.mapToCenterOreChunkCoord(chunkZ + 3)) {
                final GT_Worldgen_GT_Ore_Layer centerOreVeinPosition = GT_OreVeinLocations.getOreVeinInChunk(world, new ChunkCoordIntPair(chunkX,chunkZ));
                if (centerOreVeinPosition == null) continue;
                VeinType veinType = VeinTypeCaching.getVeinType(centerOreVeinPosition.mWorldGenName);
                if (veinType == null) continue;
                final OreVeinPosition oreVeinPosition = new OreVeinPosition(dimensionId,chunkX,chunkZ,veinType);
                if (oreVeinPosition.veinType != VeinType.NO_VEIN) {
                    oreVeinPositions.add(oreVeinPosition);
                }
            }
        }
        return oreVeinPositions;
    }

    public List<OreVeinPosition> prospectOreBlocks(
            int dimensionId, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        return prospectOreChunks(
                dimensionId,
                Utils.coordBlockToChunk(minBlockX),
                Utils.coordBlockToChunk(minBlockZ),
                Utils.coordBlockToChunk(maxBlockX),
                Utils.coordBlockToChunk(maxBlockZ));
    }

    public List<OreVeinPosition> prospectOreBlockRadius(int dimensionId, int blockX, int blockZ, int blockRadius) {
        return prospectOreBlocks(
                dimensionId, blockX - blockRadius, blockZ - blockRadius, blockX + blockRadius, blockZ + blockRadius);
    }

    public List<UndergroundFluidPosition> prospectUndergroundFluidBlockRadius(
            World world, int blockX, int blockZ, int undergroundFluidBlockRadius) {
        final int minChunkX = Utils.mapToCornerUndergroundFluidChunkCoord(
                Utils.coordBlockToChunk(blockX - undergroundFluidBlockRadius));
        final int minChunkZ = Utils.mapToCornerUndergroundFluidChunkCoord(
                Utils.coordBlockToChunk(blockZ - undergroundFluidBlockRadius));

        // Equals to ceil(undergroundFluidBlockRadius / (VP.undergroundFluidFieldSizeChunkX * VP.chunkWidth))
        final int undergroundFluidRadius =
                (undergroundFluidBlockRadius + VP.undergroundFluidSizeChunkX * VP.chunkWidth - 1)
                        / (VP.undergroundFluidSizeChunkX * VP.chunkWidth);

        List<UndergroundFluidPosition> foundUndergroundFluids =
                new ArrayList<>((2 * undergroundFluidRadius + 1) * (2 * undergroundFluidRadius + 1));

        for (int undergroundFluidX = 0; undergroundFluidX < 2 * undergroundFluidRadius + 1; undergroundFluidX++) {
            for (int undergroundFluidZ = 0; undergroundFluidZ < 2 * undergroundFluidRadius + 1; undergroundFluidZ++) {
                final int chunkX = minChunkX + undergroundFluidX * VP.undergroundFluidSizeChunkX;
                final int chunkZ = minChunkZ + undergroundFluidZ * VP.undergroundFluidSizeChunkZ;
                final int[][] chunks = new int[VP.undergroundFluidSizeChunkX][VP.undergroundFluidSizeChunkZ];
                Fluid fluid = null;
                for (int offsetChunkX = 0; offsetChunkX < VP.undergroundFluidSizeChunkX; offsetChunkX++) {
                    for (int offsetChunkZ = 0; offsetChunkZ < VP.undergroundFluidSizeChunkZ; offsetChunkZ++) {
                        final FluidStack prospectedFluid = UndergroundFluidsWrapper.prospectFluid(
                                world, chunkX + offsetChunkX, chunkZ + offsetChunkZ);
                        if (prospectedFluid != null) {
                            fluid = prospectedFluid.getFluid();
                            chunks[offsetChunkX][offsetChunkZ] = prospectedFluid.amount;
                        }
                    }
                }
                if (fluid != null) {
                    foundUndergroundFluids.add(
                            new UndergroundFluidPosition(world.provider.dimensionId, chunkX, chunkZ, fluid, chunks));
                }
            }
        }
        return foundUndergroundFluids;
    }
}
