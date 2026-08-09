package com.sinthoras.visualprospecting.network;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class ProspectingNotification implements IMessage {
    private List<OreVeinPosition> oreVeins;

    public ProspectingNotification() {}

    public ProspectingNotification(OreVeinPosition oreVeinPosition) {
        oreVeins = Collections.singletonList(oreVeinPosition);
    }

    public ProspectingNotification(List<OreVeinPosition> oreVeins) {
        this.oreVeins = oreVeins;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        final int numberOfOreVeins = buf.readInt();
        oreVeins = new ArrayList<>(numberOfOreVeins);
        for (int i = 0; i < numberOfOreVeins; i++) {
            final int dimensionId = buf.readInt();
            final int chunkX = buf.readInt();
            final int chunkZ = buf.readInt();
            final String oreVeinName = ByteBufUtils.readUTF8String(buf);
            oreVeins.add(new OreVeinPosition(dimensionId, chunkX, chunkZ, VeinTypeCaching.getVeinType(oreVeinName)));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(oreVeins.size());
        for (OreVeinPosition oreVein : oreVeins) {
            buf.writeInt(oreVein.dimensionId);
            buf.writeInt(oreVein.chunkX);
            buf.writeInt(oreVein.chunkZ);
            ByteBufUtils.writeUTF8String(buf, oreVein.veinType.name);
        }
    }

    public static class Handler implements IMessageHandler<ProspectingNotification, IMessage> {

        @Override
        public IMessage onMessage(ProspectingNotification message, MessageContext ctx) {
            ClientCache.instance.putOreVeins(message.oreVeins);
            return null;
        }
    }
}
