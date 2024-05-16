package com.sinthoras.visualprospecting.mixins.gregtech;

import static gregtech.api.util.GT_Utility.ItemNBT.getNBT;
import static gregtech.api.util.GT_Utility.ItemNBT.setNBT;

import com.sinthoras.visualprospecting.ServerTranslations;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import com.sinthoras.visualprospecting.network.ProspectingNotification;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.events.GT_OreVeinLocations;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine;
import gregtech.api.util.GT_Utility;
import gregtech.common.GT_Worldgen_GT_Ore_Layer;
import gregtech.common.blocks.GT_Block_Ore;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_AdvSeismicProspector;
import ic2.core.Ic2Items;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.IChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GT_MetaTileEntity_AdvSeismicProspector.class, remap = false)
public abstract class GT_MetaTileEntity_AdvSeismicProspectorMixin extends GT_MetaTileEntity_BasicMachine {

    @Shadow(remap = false)
    boolean ready = false;

    @Shadow(remap = false)
    int radius;

    public GT_MetaTileEntity_AdvSeismicProspectorMixin() {
        super(0, "", "", 0, 0, "", 0, 0, "", "", (ITexture[]) null);
    }

    /**
     * @author SinTh0r4s
     * @reason Redirect game mechanics onto VP database
     */
    @Overwrite(remap = false)
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isServerSide()) {
            ItemStack aStack = aPlayer.getCurrentEquippedItem();

            if (ready && mMaxProgresstime == 0) {

                final int chunkXCenter = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(aBaseMetaTileEntity.getXCoord()));
                final int chunkZCenter = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(aBaseMetaTileEntity.getZCoord()));
                int chunkRadius = (radius/16) + 1;

                int lastChunkCoordX = Integer.MAX_VALUE;
                int lastChunkCoordZ = Integer.MAX_VALUE;

                IChunkProvider provider = aBaseMetaTileEntity.getWorld().getChunkProvider();

                for (int i = -chunkRadius; i < chunkRadius + 1; i++) {
                    for (int j = -chunkRadius; j < chunkRadius + 1; j++) {
                        int chunkCoordX = Utils.mapToCenterOreChunkCoord(chunkXCenter + i);
                        int chunkCoordZ = Utils.mapToCenterOreChunkCoord(chunkZCenter + j);
                        if (lastChunkCoordX == chunkCoordX && lastChunkCoordZ == chunkCoordZ) continue;
                        lastChunkCoordX = chunkCoordX;
                        lastChunkCoordZ = chunkCoordZ;
                        provider.loadChunk(chunkCoordX,chunkCoordZ); // we load the chunk to make sure wqe have the data
                        int dimId = aBaseMetaTileEntity.getWorld().provider.dimensionId;
                        final GT_Worldgen_GT_Ore_Layer centerOreVeinPosition = GT_OreVeinLocations.getOreVeinInChunk(dimId, new ChunkCoordIntPair(chunkXCenter,chunkCoordZ));
                        if (centerOreVeinPosition != null) {
                            VeinType veinType = VeinTypeCaching.getVeinType(centerOreVeinPosition.mWorldGenName);
                            if (veinType != null) {
                                if (aPlayer instanceof EntityPlayerMP)
                                    VP.network.sendTo(new ProspectingNotification(new OreVeinPosition(dimId,chunkCoordX,chunkCoordZ,veinType)), (EntityPlayerMP) aPlayer);
                            }
                        }
                    }
                }
            }

            Item compoundExp = GameRegistry.findItem("htx","item.explosivecompound");
            if (!ready
                    && (compoundExp != null && GT_Utility.consumeItems(aPlayer, aStack, compoundExp, Math.min(64,radius/2)))) {
                this.ready = true;
                this.mMaxProgresstime = (aPlayer.capabilities.isCreativeMode ? 20 : 800);

            } else if (ready
                    && mMaxProgresstime == 0
                    && aStack != null
                    && aStack.stackSize == 1
                    && aStack.getItem() == ItemList.Tool_DataStick.getItem()) {
                this.ready = false;

                final NBTTagCompound compound = getNBT(aStack);
                compound.setString(Tags.BOOK_TITLE, "Raw Prospection Data");
                compound.setBoolean(Tags.VISUALPROSPECTING_FLAG, true);
                compound.setByte(Tags.PROSPECTION_TIER, mTier);
                compound.setInteger(
                        Tags.PROSPECTION_DIMENSION_ID, getBaseMetaTileEntity().getWorld().provider.dimensionId);
                compound.setInteger(
                        Tags.PROSPECTION_BLOCK_X, getBaseMetaTileEntity().getXCoord());
                compound.setInteger(
                        Tags.PROSPECTION_BLOCK_Y, getBaseMetaTileEntity().getYCoord());
                compound.setInteger(
                        Tags.PROSPECTION_BLOCK_Z, getBaseMetaTileEntity().getZCoord());
                compound.setInteger(Tags.PROSPECTION_ORE_RADIUS, radius);

                final List<UndergroundFluidPosition> undergroundFluidPositions =
                        ServerCache.instance.prospectUndergroundFluidBlockRadius(
                                aPlayer.worldObj,
                                getBaseMetaTileEntity().getXCoord(),
                                getBaseMetaTileEntity().getZCoord(),
                                VP.undergroundFluidChunkProspectingBlockRadius);
                compound.setInteger(Tags.PROSPECTION_NUMBER_OF_UNDERGROUND_FLUID, undergroundFluidPositions.size());

                String[] fluidStrings = new String[9];
                final int minUndergroundFluidX = Utils.mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(
                        getBaseMetaTileEntity().getXCoord() - VP.undergroundFluidChunkProspectingBlockRadius));
                final int minUndergroundFluidZ = Utils.mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(
                        getBaseMetaTileEntity().getZCoord() - VP.undergroundFluidChunkProspectingBlockRadius));
                for (UndergroundFluidPosition undergroundFluidPosition : undergroundFluidPositions) {
                    final int offsetUndergroundFluidX =
                            (Utils.mapToCornerUndergroundFluidChunkCoord(undergroundFluidPosition.chunkX)
                                            - minUndergroundFluidX)
                                    >> 3;
                    final int offsetUndergroundFluidZ =
                            (Utils.mapToCornerUndergroundFluidChunkCoord(undergroundFluidPosition.chunkZ)
                                            - minUndergroundFluidZ)
                                    >> 3;
                    final int undergroundFluidBookId = offsetUndergroundFluidX + offsetUndergroundFluidZ * 3;
                    fluidStrings[undergroundFluidBookId] =
                            "" + undergroundFluidBookId + ": " + undergroundFluidPosition.getMinProduction() + "-"
                                    + undergroundFluidPosition.getMaxProduction() + " "
                                    + ServerTranslations.getEnglishLocalization(undergroundFluidPosition.fluid);
                }
                compound.setString(Tags.PROSPECTION_FLUIDS, String.join("|", fluidStrings));

                setNBT(aStack, compound);
            }
        }
        return true;
    }
}
