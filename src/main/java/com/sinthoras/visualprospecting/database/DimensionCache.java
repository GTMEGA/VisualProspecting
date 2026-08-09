package com.sinthoras.visualprospecting.database;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import java.nio.ByteBuffer;
import java.util.*;
import net.minecraft.world.ChunkCoordIntPair;

public class DimensionCache {

    public enum UpdateResult {
        AlreadyKnown,
        Updated,
        New
    }

    private final Map<ChunkCoordIntPair, OreVeinPosition> oreChunks = new HashMap<>();
    private final Set<ChunkCoordIntPair> changedOrNewOreChunks = new HashSet<>();
    public final int dimensionId;

    public DimensionCache(int dimensionId) {
        this.dimensionId = dimensionId;
    }

    public ByteBuffer saveOreChunks() {
        if (changedOrNewOreChunks.isEmpty() == false) {
            final ByteBuffer byteBuffer =
                    ByteBuffer.allocate(changedOrNewOreChunks.size() * (2 * Integer.BYTES + Short.BYTES));
            changedOrNewOreChunks.stream().map(oreChunks::get).forEach(oreVeinPosition -> {
                byteBuffer.putInt(oreVeinPosition.chunkX);
                byteBuffer.putInt(oreVeinPosition.chunkZ);
                short veinTypeId = VeinTypeCaching.getVeinTypeId(oreVeinPosition.veinType);
                if (oreVeinPosition.isDepleted()) {
                    veinTypeId |= 0x8000;
                }
                byteBuffer.putShort(veinTypeId);
            });
            changedOrNewOreChunks.clear();
            byteBuffer.flip();
            return byteBuffer;
        }
        return null;
    }

    public void loadCache(ByteBuffer oreChunksBuffer) {
        if (oreChunksBuffer != null) {
            while (oreChunksBuffer.remaining() >= Integer.BYTES * 2 + Short.BYTES) {
                final int chunkX = oreChunksBuffer.getInt();
                final int chunkZ = oreChunksBuffer.getInt();
                final short veinTypeId = oreChunksBuffer.getShort();
                final boolean depleted = (veinTypeId & 0x8000) > 0;
                final VeinType veinType = VeinTypeCaching.getVeinType((short) (veinTypeId & 0x7FFF));
                oreChunks.put(
                        getOreVeinKey(chunkX, chunkZ),
                        new OreVeinPosition(dimensionId, chunkX, chunkZ, veinType, depleted));
            }
        }
    }

    private ChunkCoordIntPair getOreVeinKey(int chunkX, int chunkZ) {
        return new ChunkCoordIntPair(Utils.mapToCenterOreChunkCoord(chunkX), Utils.mapToCenterOreChunkCoord(chunkZ));
    }

    public UpdateResult putOreVein(final OreVeinPosition oreVeinPosition) {
        final ChunkCoordIntPair key = getOreVeinKey(oreVeinPosition.chunkX, oreVeinPosition.chunkZ);
        if (oreChunks.containsKey(key) == false) {
            oreChunks.put(key, oreVeinPosition);
            changedOrNewOreChunks.add(key);
            return UpdateResult.New;
        }
        final OreVeinPosition storedOreVeinPosition = oreChunks.get(key);
        if (storedOreVeinPosition.veinType != oreVeinPosition.veinType) {
            oreChunks.put(key, oreVeinPosition.joinDepletedState(storedOreVeinPosition));
            changedOrNewOreChunks.add(key);
            return UpdateResult.New;
        }
        return UpdateResult.AlreadyKnown;
    }

    public void toggleOreVein(int chunkX, int chunkZ) {
        final ChunkCoordIntPair key = getOreVeinKey(chunkX, chunkZ);
        if (oreChunks.containsKey(key)) {
            oreChunks.get(key).toggleDepleted();
            changedOrNewOreChunks.add(key);
        }
    }

    public OreVeinPosition getOreVein(int chunkX, int chunkZ) {
        final ChunkCoordIntPair key = getOreVeinKey(chunkX, chunkZ);
        return oreChunks.getOrDefault(key, new OreVeinPosition(dimensionId, chunkX, chunkZ, VeinType.NO_VEIN, true));
    }

    public Collection<OreVeinPosition> getAllOreVeins() {
        return oreChunks.values();
    }
}
