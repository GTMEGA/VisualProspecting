package com.sinthoras.visualprospecting.integration.voxelmap;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.hooks.ProspectingNotificationEvent;
import com.thevoxelbox.voxelmap.interfaces.AbstractVoxelMap;
import com.thevoxelbox.voxelmap.interfaces.IWaypointManager;
import com.thevoxelbox.voxelmap.util.Waypoint;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import java.util.TreeSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public class VoxelMapEventHandler {

    @SubscribeEvent
    public void onVeinProspected(ProspectingNotificationEvent.OreVein event) {

        if (event.isCanceled()) {
            return;
        }

        OreVeinPosition pos = event.getPosition();
        IWaypointManager waypointManager = AbstractVoxelMap.getInstance().getWaypointManager();
        short[] color = pos.veinType.primaryOreMeta.material().getRGBA();
        TreeSet<Integer> dim = new TreeSet<>();
        dim.add(pos.dimensionId);

        waypointManager.addWaypoint(new Waypoint(
                StatCollector.translateToLocal(pos.veinType.name), // name
                pos.getBlockX(), // X
                pos.getBlockZ(), // Z
                getY(), // Y
                Config.enableVoxelMapWaypointsByDefault, // enabled
                (float) color[0] / 255.0f, // red
                (float) color[1] / 255.0f, // green
                (float) color[2] / 255.0f, // blue
                "Pickaxe", // icon
                IWaypointManagerReflection.getCurrentSubworldDescriptor(waypointManager, false), // world
                dim)); // dimension
    }

    private static int getY() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !heldItem.getUnlocalizedName().contains("gt.detrav.metatool.01")) {
            return (int) player.posY;
        }
        return 65;
    }
}
