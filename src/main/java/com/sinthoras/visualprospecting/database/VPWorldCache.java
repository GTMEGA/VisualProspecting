package com.sinthoras.visualprospecting.database;

import com.sinthoras.visualprospecting.VPUtils;
import com.sinthoras.visualprospecting.database.veintypes.VPVeinType;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;

public abstract class VPWorldCache {

    private HashMap<Integer, VPDimensionCache> dimensions = new HashMap<>();
    private boolean needsSaving = false;
    private File worldCacheDirectory;
    private String worldId = "";

    protected abstract File getStorageDirectory();

    public boolean loadVeinCache(String worldId) {
        if(this.worldId.equals(worldId))
            return true;
        this.worldId = worldId;
        worldCacheDirectory = new File(getStorageDirectory(), worldId);
        worldCacheDirectory.mkdirs();
        final HashMap<Integer, ByteBuffer> dimensionBuffers = VPUtils.getDIMFiles(worldCacheDirectory);
        if(dimensionBuffers.isEmpty())
            return false;

        dimensions.clear();
        for(int dimensionId : dimensionBuffers.keySet()) {
            final VPDimensionCache dimension = new VPDimensionCache(dimensionId);
            dimension.loadVeinCache(dimensionBuffers.get(dimensionId));
            dimensions.put(dimensionId, dimension);
        }
        return true;
    }

    public void saveVeinCache() {
        if(needsSaving) {
            for (VPDimensionCache dimension : dimensions.values()) {
                final ByteBuffer byteBuffer = dimension.saveVeinCache();
                if (byteBuffer != null)
                    VPUtils.appendToFile(new File(worldCacheDirectory.toPath() + "/DIM" + dimension.dimensionId), byteBuffer);
            }
            needsSaving = false;
        }
    }

    public void reset() {
        dimensions = new HashMap<>();
        needsSaving = false;
    }

    protected boolean putVeinType(int dimensionId, int chunkX, int chunkZ, final VPVeinType veinType) {
        VPDimensionCache dimension = dimensions.get(dimensionId);
        if(dimension == null) {
            dimension = new VPDimensionCache(dimensionId);
            dimensions.put(dimensionId, dimension);
        }
        final boolean updatedDimensionCache = dimension.putVeinType(chunkX, chunkZ, veinType);
        needsSaving |= updatedDimensionCache;
        return updatedDimensionCache;
    }

    public VPVeinType getVeinType(int dimensionId, int chunkX, int chunkZ) {
        VPDimensionCache dimension = dimensions.get(dimensionId);
        if(dimension == null)
            return VPVeinType.NO_VEIN;
        return dimension.getVeinType(chunkX, chunkZ);
    }
}
