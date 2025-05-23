package com.sinthoras.visualprospecting.mixins.gregtech;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.ServerCache;
import gregtech.api.world.GT_Worldgen;
import gregtech.common.GT_Worldgen_GT_Ore_Layer;
import gregtech.common.GT_Worldgenerator;
import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GT_Worldgenerator.WorldGenContainer.class)
public class WorldGenContainerMixin {

    // Redirect both calls to ensure that Bartworks ore veins are captured as well
    @Redirect(
            method = "worldGenFindVein",
            at = @At(value = "INVOKE",
                     target = "Lgregtech/common/GT_Worldgen_GT_Ore_Layer;executeWorldgenChunkified(Lnet/minecraft/world/World;Ljava/util/Random;Ljava/lang/String;IIIIILnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/chunk/IChunkProvider;)Lgregtech/api/world/GT_Worldgen$WorldGenResult;"),
            remap = false,
            require = 2)
    protected GT_Worldgen.WorldGenResult onOreVeinPlaced(
            GT_Worldgen_GT_Ore_Layer instance,
            World aWorld,
            Random aRandom,
            String aBiome,
            int aDimensionType,
            int aChunkX,
            int aChunkZ,
            int aSeedX,
            int aSeedZ,
            IChunkProvider aChunkGenerator,
            IChunkProvider aChunkProvider) {
        final GT_Worldgen.WorldGenResult result = instance.executeWorldgenChunkified(
                aWorld,
                aRandom,
                aBiome,
                aDimensionType,
                aChunkX,
                aChunkZ,
                aSeedX,
                aSeedZ,
                aChunkGenerator,
                aChunkProvider);

//        return ServerCache.instance.notifyOreVeinGeneration(
//                aWorld.provider.dimensionId,
//                Utils.coordBlockToChunk(aSeedX),
//                Utils.coordBlockToChunk(aSeedZ),
//                instance.mWorldGenName,
//                result,aSeedX,aSeedZ,aWorld,instance);
//
        if (result.status == GT_Worldgen.WorldGenStatus.ORE_PLACED && !instance.mWorldGenName.equals("NoOresInVein")) {
            ServerCache.instance.notifyOreVeinGeneration(
                    aWorld.provider.dimensionId,
                    Utils.coordBlockToChunk(aSeedX),
                    Utils.coordBlockToChunk(aSeedZ),
                    instance.mWorldGenName);
        }
        return result;
    }
}
