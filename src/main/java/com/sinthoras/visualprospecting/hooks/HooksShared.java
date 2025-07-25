package com.sinthoras.visualprospecting.hooks;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.RedoServerCacheCommand;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.WorldIdHandler;
import com.sinthoras.visualprospecting.database.cachebuilder.WorldAnalysis;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import com.sinthoras.visualprospecting.item.ProspectorsLog;
import com.sinthoras.visualprospecting.network.ProspectingNotification;
import com.sinthoras.visualprospecting.network.ProspectingRequest;
import com.sinthoras.visualprospecting.network.ProspectionSharing;
import com.sinthoras.visualprospecting.network.WorldIdNotification;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GT_OreDictUnificator;
import java.io.IOException;
import java.util.zip.DataFormatException;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public class HooksShared {

    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void fmlLifeCycleEvent(FMLPreInitializationEvent event) {
        Config.syncronizeConfiguration(event.getSuggestedConfigurationFile());

        VP.network = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);
        int networkId = 0;
        VP.network.registerMessage(
                ProspectingRequest.Handler.class, ProspectingRequest.class, networkId++, Side.SERVER);
        VP.network.registerMessage(
                ProspectingNotification.Handler.class, ProspectingNotification.class, networkId++, Side.CLIENT);
        VP.network.registerMessage(
                WorldIdNotification.Handler.class, WorldIdNotification.class, networkId++, Side.CLIENT);
        VP.network.registerMessage(
                ProspectionSharing.ServerHandler.class, ProspectionSharing.class, networkId++, Side.SERVER);
        VP.network.registerMessage(
                ProspectionSharing.ClientHandler.class, ProspectionSharing.class, networkId++, Side.CLIENT);

        ProspectorsLog.instance = new ProspectorsLog();
        GameRegistry.registerItem(ProspectorsLog.instance, ProspectorsLog.instance.getUnlocalizedName());
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void fmlLifeCycleEvent(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new HooksEventBus());
        FMLCommonHandler.instance().bus().register(new HooksFML());
    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void fmlLifeCycleEvent(FMLPostInitializationEvent event) {
        GregTech_API.sAfterGTPostload.add(new VeinTypeCaching());
        GregTech_API.sAfterGTPostload.add(() -> GT_Values.RA.addAssemblerRecipe(
                new ItemStack[] {
                    GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 2L),
                    new ItemStack(Items.writable_book, 1, 0x7FFF),
                    new ItemStack(Items.gold_nugget, 1, 0x7FFF)
                },
                Materials.Glue.getFluid(20L),
                new ItemStack(ProspectorsLog.instance, 1, 0),
                128,
                8));
    }

    public void fmlLifeCycleEvent(FMLServerAboutToStartEvent event) {}

    // register server commands in this event handler
    public void fmlLifeCycleEvent(FMLServerStartingEvent event) {
        final MinecraftServer minecraftServer = event.getServer();
        WorldIdHandler.load(minecraftServer.worldServers[0]);
        if (ServerCache.instance.loadVeinCache(WorldIdHandler.getWorldId()) == false || Config.recacheVeins) {
            try {
                WorldAnalysis world = new WorldAnalysis(
                        minecraftServer.getEntityWorld().getSaveHandler().getWorldDirectory());
                world.cacheVeins();
            } catch (IOException | DataFormatException e) {
                VP.info("Could not load world save files to build vein cache!");
                e.printStackTrace();
            }
        }
        event.registerServerCommand(new RedoServerCacheCommand());
    }

    public void fmlLifeCycleEvent(FMLServerStartedEvent event) {}

    public void fmlLifeCycleEvent(FMLServerStoppingEvent event) {
        ServerCache.instance.saveVeinCache();
        ServerCache.instance.reset();
    }

    public void fmlLifeCycleEvent(FMLServerStoppedEvent event) {}
}
