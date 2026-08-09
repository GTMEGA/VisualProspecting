package com.sinthoras.visualprospecting.database;

import com.sinthoras.visualprospecting.Constants;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gregtech.common.GT_OreVeinStats;
import gregtech.common.misc.ClientOreVeinStats;
import lombok.val;

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
                final GT_OreVeinStats.Stats stats = ClientOreVeinStats.getVeinStats(world, chunkX, chunkZ);

                VeinType veinType = VeinTypeCaching.getVeinType(stats.oreMix().toString());

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
}
