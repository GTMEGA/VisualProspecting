package com.sinthoras.visualprospecting.network;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.TransferCache;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class ProspectionSharing implements IMessage {

    private static final int BYTES_OVERHEAD = 2 * Byte.BYTES + 2 * Integer.BYTES;

    final List<OreVeinPosition> oreVeins = new ArrayList<>();
    private int bytesUsed = BYTES_OVERHEAD;
    boolean isFirstMessage = false;
    boolean isLastMessage = false;

    public ProspectionSharing() {}

    public int putOreVeins(List<OreVeinPosition> oreVeins) {
        final int availableBytes = VP.uploadSizePerPacketInBytes - bytesUsed;
        final int maxAddedOreVeins = availableBytes / OreVeinPosition.getMaxBytes();
        final int addedOreVeins = Math.min(oreVeins.size(), maxAddedOreVeins);
        this.oreVeins.addAll(oreVeins.subList(0, addedOreVeins));
        bytesUsed += addedOreVeins * OreVeinPosition.getMaxBytes();
        return addedOreVeins;
    }

    public void setFirstMessage(boolean isFirstMessage) {
        this.isFirstMessage = isFirstMessage;
    }

    public void setLastMessage(boolean isLastMessage) {
        this.isLastMessage = isLastMessage;
    }

    public int getBytes() {
        return BYTES_OVERHEAD + VeinTypeCaching.getLongesOreNameLength() * oreVeins.size();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        isFirstMessage = buf.readByte() > 0;
        isLastMessage = buf.readByte() > 0;

        final int numberOfOreVeins = buf.readInt();
        for (int i = 0; i < numberOfOreVeins; i++) {
            final int dimensionId = buf.readInt();
            final int chunkX = buf.readInt();
            final int chunkZ = buf.readInt();
            final boolean isDepleted = buf.readByte() > 0;
            final String oreVeinName = ByteBufUtils.readUTF8String(buf);
            oreVeins.add(new OreVeinPosition(
                    dimensionId, chunkX, chunkZ, VeinTypeCaching.getVeinType(oreVeinName), isDepleted));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(isFirstMessage ? 1 : 0);
        buf.writeByte(isLastMessage ? 1 : 0);

        buf.writeInt(oreVeins.size());
        for (OreVeinPosition oreVein : oreVeins) {
            buf.writeInt(oreVein.dimensionId);
            buf.writeInt(oreVein.chunkX);
            buf.writeInt(oreVein.chunkZ);
            buf.writeByte(oreVein.isDepleted() ? 1 : 0);
            ByteBufUtils.writeUTF8String(buf, oreVein.veinType.name);
        }
    }

    public static class ServerHandler implements IMessageHandler<ProspectionSharing, IMessage> {

        private static Map<EntityPlayerMP, List<OreVeinPosition>> oreVeins = new HashMap<>();

        @Override
        public IMessage onMessage(ProspectionSharing message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            // Optional todo: Integrate over time for proper checking
            if (message.getBytes() > VP.uploadSizePerPacketInBytes) {
                player.playerNetServerHandler.kickPlayerFromServer(
                        "Do not spam the server! Change your VisualProcessing configuration back to the servers!");
            }
            if (message.isFirstMessage) {
                oreVeins.put(player, new ArrayList<>());
            }
            if (oreVeins.containsKey(player) == false) {
                return null;
            }
            oreVeins.get(player).addAll(message.oreVeins);
            if (message.isLastMessage) {
                TransferCache.instance.addClientProspectionData(player.getPersistentID().toString(), oreVeins.get(player));
                oreVeins.remove(player);
            }
            return null;
        }
    }

    public static class ClientHandler implements IMessageHandler<ProspectionSharing, IMessage> {

        private static List<OreVeinPosition> oreVeins;

        @Override
        public IMessage onMessage(ProspectionSharing message, MessageContext ctx) {
            if (message.isFirstMessage) {
                oreVeins = new ArrayList<>();
            }
            if (oreVeins == null) {
                return null;
            }
            oreVeins.addAll(message.oreVeins);
            if (message.isLastMessage) {
                ClientCache.instance.putOreVeins(oreVeins);
            }
            return null;
        }
    }
}
