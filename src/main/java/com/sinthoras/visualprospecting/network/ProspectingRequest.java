package com.sinthoras.visualprospecting.network;

import static com.sinthoras.visualprospecting.Utils.isSmallOreId;
import static com.sinthoras.visualprospecting.Utils.oreIdToMaterialId;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gregtech.api.events.GT_OreVeinLocations;
import gregtech.common.GT_Worldgen_GT_Ore_Layer;
import gregtech.common.blocks.GT_Block_Ore;
import gregtech.common.blocks.GT_Block_Ore_Abstract;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;

public class ProspectingRequest implements IMessage {

    public static long timestampLastRequest = 0;

    private int dimensionId;
    private int blockX;
    private int blockY;
    private int blockZ;
    private Block block;

    public ProspectingRequest() {}

    public ProspectingRequest(int dimensionId, int blockX, int blockY, int blockZ, GT_Block_Ore_Abstract ore) {
        this.dimensionId = dimensionId;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.block = ore;
    }

    public static boolean canSendRequest() {
        final long timestamp = System.currentTimeMillis();
        if (timestamp - timestampLastRequest > Config.minDelayBetweenVeinRequests) {
            timestampLastRequest = timestamp;
            return true;
        }
        return false;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimensionId = buf.readInt();
        blockX = buf.readInt();
        blockY = buf.readInt();
        blockZ = buf.readInt();
        block = Block.getBlockById(buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimensionId);
        buf.writeInt(blockX);
        buf.writeInt(blockY);
        buf.writeInt(blockZ);
        buf.writeInt(Block.getIdFromBlock(block));
    }

    public static class Handler implements IMessageHandler<ProspectingRequest, IMessage> {

        private static final Map<UUID, Long> lastRequestPerPlayer = new HashMap<>();

        @Override
        public IMessage onMessage(ProspectingRequest message, MessageContext ctx) {
            // Check if request is valid/not tempered with
            final UUID uuid = ctx.getServerHandler().playerEntity.getUniqueID();
            final long lastRequest = lastRequestPerPlayer.containsKey(uuid) ? lastRequestPerPlayer.get(uuid) : 0;
            final long timestamp = System.currentTimeMillis();
            final float distanceSquared = ctx.getServerHandler()
                    .playerEntity
                    .getPlayerCoordinates()
                    .getDistanceSquared(message.blockX, message.blockY, message.blockZ);
            final World world = ctx.getServerHandler().playerEntity.getEntityWorld();
            final int chunkX = Utils.coordBlockToChunk(message.blockX);
            final int chunkZ = Utils.coordBlockToChunk(message.blockZ);
            final boolean isChunkLoaded = world.getChunkProvider().chunkExists(chunkX, chunkZ);
            if (ctx.getServerHandler().playerEntity.dimension == message.dimensionId
                    && distanceSquared <= 1024 // max 32 blocks distance
                    && timestamp - lastRequest >= Config.minDelayBetweenVeinRequests
                    && isChunkLoaded) {
                final Block block = world.getBlock(message.blockX, message.blockY, message.blockZ);
                if (block instanceof GT_Block_Ore_Abstract) {

                    // we check the gt ore map first
                    if (block instanceof GT_Block_Ore) {
                        lastRequestPerPlayer.put(uuid, timestamp);
                        // Prioritise center vein
                        final GT_Worldgen_GT_Ore_Layer centerOreVeinPosition = GT_OreVeinLocations.getOreVeinInChunk(message.dimensionId, new ChunkCoordIntPair(chunkX,chunkZ));
                        if (centerOreVeinPosition != null) {
                            VeinType veinType = VeinTypeCaching.getVeinType(centerOreVeinPosition.mWorldGenName);
                            if (veinType != null) {
                                if (VeinType.containsOre(centerOreVeinPosition, (GT_Block_Ore) message.block)) {
                                    return new ProspectingNotification(new OreVeinPosition(message.dimensionId,chunkX,chunkZ,veinType));
                                }
                            }
                        }
//
//                        // if we don't find anything then use ore protecting map
//                        final int centerChunkX = Utils.mapToCenterOreChunkCoord(chunkX);
//                        final int centerChunkZ = Utils.mapToCenterOreChunkCoord(chunkZ);
//                        final int distanceBlocks = Math.max(
//                                Math.abs(centerChunkX - chunkX), Math.abs(centerChunkZ - chunkZ));
//                        final OreVeinPosition neighborOreVeinPosition = ServerCache.instance.getOreVein(
//                                message.dimensionId, centerChunkX, centerChunkZ);
//                        final int maxDistance = ((neighborOreVeinPosition.veinType.blockSize + 16) >> 4)
//                                + 1; // Equals to: ceil(blockSize / 16.0) + 1
//                        if (neighborOreVeinPosition.veinType.containsOre(message.block)
//                                && distanceBlocks <= maxDistance) {
//                            return new ProspectingNotification(neighborOreVeinPosition);
//
//                        }
                    }
                }
            }
            return null;
        }
    }
}
