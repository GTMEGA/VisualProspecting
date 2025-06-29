package com.sinthoras.visualprospecting.database.veintypes;

import com.sinthoras.visualprospecting.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import gregtech.common.GT_Worldgen_GT_Ore_Layer;
import gregtech.common.blocks.GT_Block_Ore;

import net.minecraft.block.Block;
import net.minecraft.util.EnumChatFormatting;

public class VeinType {

    public static final int veinHeight = 9;

    public final String name;
    public short veinId;
    public final IOreMaterialProvider oreMaterialProvider;
    public final int blockSize;
    public final GT_Block_Ore primaryOreMeta;
    public final GT_Block_Ore secondaryOreMeta;
    public final GT_Block_Ore inBetweenOreMeta;
    public final GT_Block_Ore sporadicOreMeta;
    public final int minBlockY;
    public final int maxBlockY;
    public final Set<GT_Block_Ore> oresAsSet;
    private boolean isHighlighted = true;

    // Available after VisualProspecting post GT initialization
    public static final VeinType NO_VEIN =
            new VeinType(Constants.ORE_MIX_NONE_NAME, null, 0, null, null, null, null, 0, 0);

    public VeinType(
            String name,
            IOreMaterialProvider oreMaterialProvider,
            int blockSize,
            GT_Block_Ore primaryOreMeta,
            GT_Block_Ore secondaryOreMeta,
            GT_Block_Ore inBetweenOreMeta,
            GT_Block_Ore sporadicOreMeta,
            int minBlockY,
            int maxBlockY) {
        this.name = name;
        this.oreMaterialProvider = oreMaterialProvider;
        this.blockSize = blockSize;
        this.primaryOreMeta = primaryOreMeta;
        this.secondaryOreMeta = secondaryOreMeta;
        this.inBetweenOreMeta = inBetweenOreMeta;
        this.sporadicOreMeta = sporadicOreMeta;
        this.minBlockY = minBlockY;
        this.maxBlockY = maxBlockY;
        oresAsSet = new HashSet<>();
        oresAsSet.add(primaryOreMeta);
        oresAsSet.add(secondaryOreMeta);
        oresAsSet.add(inBetweenOreMeta);
        oresAsSet.add(sporadicOreMeta);
    }

    public boolean matches(Set<Short> foundOres) {
        return foundOres.containsAll(oresAsSet);
    }

    public boolean matchesWithSpecificPrimaryOrSecondary(Set<GT_Block_Ore> foundOres, Block specificMeta) {
        return (primaryOreMeta == specificMeta || secondaryOreMeta == specificMeta) && foundOres.containsAll(oresAsSet);
    }

    public boolean canOverlapIntoNeighborOreChunk() {
        return blockSize > 24;
    }

    // Ore chunks on coordinates -1 and 1 are one chunk less apart
    public boolean canOverlapIntoNeighborOreChunkAtCoordinateAxis() {
        return blockSize > 16;
    }

    public boolean containsOre(Block block) {
        return primaryOreMeta == block
                || secondaryOreMeta == block
                || inBetweenOreMeta == block
                || sporadicOreMeta == block;
    }

    static public boolean containsOre(GT_Worldgen_GT_Ore_Layer oreVein, GT_Block_Ore block) {
        return oreVein.mPrimary == block.getOreType()
                || oreVein.mSecondary == block.getOreType()
                || oreVein.mBetween == block.getOreType()
                || oreVein.mSporadic == block.getOreType();
    }

    public List<String> getOreMaterialNames() {
        if (this == VeinType.NO_VEIN) {
            return new ArrayList<>(0);
        }

        return oresAsSet.stream()
                .map(block -> block.material().name())
                .filter(Objects::nonNull)
                .map(material -> EnumChatFormatting.GRAY + material)
                .collect(Collectors.toList());
    }

    public Set<GT_Block_Ore> getOresAtLayer(int layerBlockY) {
        final Set<GT_Block_Ore> result = new HashSet<>();
        switch (layerBlockY) {
            case 0:
            case 1:
            case 2:
                result.add(secondaryOreMeta);
                result.add(sporadicOreMeta);
                return result;
            case 3:
                result.add(secondaryOreMeta);
                result.add(inBetweenOreMeta);
                result.add(sporadicOreMeta);
                return result;
            case 4:
                result.add(inBetweenOreMeta);
                result.add(sporadicOreMeta);
                return result;
            case 5:
            case 6:
                result.add(primaryOreMeta);
                result.add(inBetweenOreMeta);
                result.add(sporadicOreMeta);
                return result;
            case 7:
            case 8:
                result.add(primaryOreMeta);
                result.add(sporadicOreMeta);
                return result;
            default:
                return result;
        }
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setNEISearchHeighlight(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }
}
