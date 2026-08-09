package com.sinthoras.visualprospecting.network;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

import gregtech.common.GT_OreVeinStats;
import gregtech.common.GT_Worldgen_GT_Ore_Layer;
import gregtech.common.blocks.GT_Block_Ore;
import gregtech.common.blocks.GT_Block_Ore_Abstract;
import gregtech.common.misc.ClientOreVeinStats;
import io.netty.buffer.ByteBuf;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

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

            final EntityPlayer player = ctx.getServerHandler().playerEntity;

            final float distanceSquared = player.getPlayerCoordinates()
                                                .getDistanceSquared(message.blockX, message.blockY, message.blockZ);

            final World world = player.getEntityWorld();

            final int chunkX = Utils.coordBlockToChunk(message.blockX);
            final int chunkZ = Utils.coordBlockToChunk(message.blockZ);
            final boolean isChunkLoaded = world.getChunkProvider().chunkExists(chunkX, chunkZ);

            if (player.dimension != message.dimensionId
                || distanceSquared > 1024 // max 32 blocks distance
                || timestamp - lastRequest < Config.minDelayBetweenVeinRequests
                || !isChunkLoaded) {
                return null;
            }

            final Block block = world.getBlock(message.blockX, message.blockY, message.blockZ);

            // we check the gt ore map first
            if (!(block instanceof GT_Block_Ore)) {
                return null;
            }

            lastRequestPerPlayer.put(uuid, timestamp);
            // Prioritise center vein
            final GT_OreVeinStats.Stats stats = GT_OreVeinStats.getOreVeinStatsInChunk(world, chunkX, chunkZ);

            VeinType veinType = VeinTypeCaching.getVeinType(stats.oreMix().unlocalizedName());
            if (veinType != null) {
                final GT_Worldgen_GT_Ore_Layer oreLayer = GT_OreVeinStats.ORE_MIX_LOOKUP.getOrDefault(stats.oreMix().unlocalizedName(),
                                                                                                      GT_Worldgen_GT_Ore_Layer.EMPTY_VEIN);

                if (VeinType.containsOre(oreLayer, (GT_Block_Ore) message.block)) {
                    return new ProspectingNotification(new OreVeinPosition(message.dimensionId,chunkX,chunkZ,veinType));
                }
            }

            return null;
        }
    }
}
