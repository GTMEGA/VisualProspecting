package com.sinthoras.visualprospecting.database;

import com.sinthoras.visualprospecting.Constants;
import com.sinthoras.visualprospecting.Tags;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;

public class WorldIdHandler extends WorldSavedData {

    private static WorldIdHandler instance;
    private String worldId;

    public WorldIdHandler() {
        super(Tags.MOD_ID);
    }

    public WorldIdHandler(String name) {
        super(name);
    }

    public static void load(WorldServer world) {
        instance = (WorldIdHandler) world.mapStorage.loadData(WorldIdHandler.class, Tags.MOD_ID);
        if (instance == null) {
            instance = new WorldIdHandler(Tags.MOD_ID);
            instance.worldId = world.func_73046_m().getFolderName() + "_" + UUID.randomUUID();
            world.mapStorage.setData(Tags.MOD_ID, instance);
            instance.markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        worldId = compound.getString(Constants.worldId);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setString(Constants.worldId, worldId);
    }

    public static String getWorldId() {
        return instance.worldId;
    }
}
