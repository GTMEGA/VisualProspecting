package com.sinthoras.visualprospecting.integration.journeymap.drawsteps;

import com.google.common.collect.Table;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.integration.DrawUtils;
import com.sinthoras.visualprospecting.integration.model.locations.IWaypointAndLocationProvider;
import com.sinthoras.visualprospecting.integration.model.locations.OreVeinLocation;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import gregtech.api.events.GT_OreVeinLocations;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.map.GridRenderer;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkCoordIntPair;

public class OreVeinDrawStep implements ClickableDrawStep {

    private static final ResourceLocation depletedTextureLocation =
            new ResourceLocation(Tags.MOD_ID, "textures/depleted.png");

    private final OreVeinLocation oreVeinLocation;

    private double iconX;
    private double iconY;
    private double iconSize;

    public OreVeinDrawStep(OreVeinLocation oreVeinLocation) {
        this.oreVeinLocation = oreVeinLocation;
    }

    @Override
    public List<String> getTooltip() {
        final List<String> tooltip = new ArrayList<>();

        int oreMax = 0;
        int oreCurrent = 0;

        final int chunkX = this.oreVeinLocation.oreVeinPosition.chunkX;
        final int chunkZ = this.oreVeinLocation.oreVeinPosition.chunkZ;

        Table<Integer, ChunkCoordIntPair, GT_OreVeinLocations.VeinData> map = GT_OreVeinLocations.RecordedOreVeinInChunk.get();

        for (int i = chunkX - 1; i < chunkX + 1; i++) {
            for (int k = chunkZ - 1; k < chunkZ + 1; k++) {
                int dimId = this.oreVeinLocation.oreVeinPosition.dimensionId;

                GT_OreVeinLocations.VeinData veinData = map.get(dimId, new ChunkCoordIntPair(i, k));

                if (veinData == null) {
                    continue;
                }

                oreMax += veinData.oresPlaced;
                oreCurrent += veinData.oresCurrent;
            }
        }

        double oreCount = 100D * oreCurrent / oreMax;

        if (oreVeinLocation.isDepleted()) {
            tooltip.add(oreVeinLocation.getDepletedHint());
        }

        if (oreVeinLocation.isActiveAsWaypoint()) {
            tooltip.add(oreVeinLocation.getActiveWaypointHint());
        }

        tooltip.add(oreVeinLocation.getName());

        EnumChatFormatting color;
        if (oreCount > 50) {
            color = EnumChatFormatting.GREEN;
        } else if (oreCount > 25) {
            color = EnumChatFormatting.YELLOW;
        } else if (oreCount > 10) {
            color = EnumChatFormatting.GOLD;
        } else {
            color = EnumChatFormatting.RED;
        }

        tooltip.addAll(oreVeinLocation.getMaterialNames());

        if (!Double.isNaN(oreCount)) {
            String format = "Ores: " + color + "%.02f%%";
            tooltip.add(String.format(format, oreCount));
        }

        return tooltip;
    }

    @Override
    public void drawTooltip(FontRenderer fontRenderer, int mouseX, int mouseY, int displayWidth, int displayHeight) {}

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= iconX && mouseX <= iconX + iconSize && mouseY >= iconY && mouseY <= iconY + iconSize;
    }

    @Override
    public void onActionKeyPressed() {
        oreVeinLocation.toggleOreVein();
    }

    @Override
    public IWaypointAndLocationProvider getLocationProvider() {
        return oreVeinLocation;
    }

    @Override
    public void draw(
            double draggedPixelX,
            double draggedPixelY,
            GridRenderer gridRenderer,
            float drawScale,
            double fontScale,
            double rotation) {
        iconSize = 16 * fontScale;
        final double iconSizeHalf = iconSize / 2;
        final Point2D.Double blockAsPixel =
                gridRenderer.getBlockPixelInGrid(oreVeinLocation.getBlockX(), oreVeinLocation.getBlockZ());
        final Point2D.Double pixel =
                new Point2D.Double(blockAsPixel.getX() + draggedPixelX, blockAsPixel.getY() + draggedPixelY);

        if (gridRenderer.getZoom() >= Config.minZoomLevelForOreLabel && oreVeinLocation.isDepleted() == false) {
            final int fontColor = oreVeinLocation.drawSearchHighlight() ? 0xFFFFFF : 0x7F7F7F;
            DrawUtil.drawLabel(
                    oreVeinLocation.getName(),
                    pixel.getX(),
                    pixel.getY() - iconSize,
                    DrawUtil.HAlign.Center,
                    DrawUtil.VAlign.Middle,
                    0,
                    180,
                    fontColor,
                    255,
                    fontScale,
                    false,
                    rotation);
        }

        iconX = pixel.getX() - iconSizeHalf;
        iconY = pixel.getY() - iconSizeHalf;
        final IIcon blockIcon = Blocks.stone.getIcon(0, 0);
        DrawUtils.drawQuad(blockIcon, iconX, iconY, iconSize, iconSize, 0xFFFFFF, 192);

        DrawUtils.drawQuad(
                oreVeinLocation.getIconFromPrimaryOre(),
                iconX,
                iconY,
                iconSize,
                iconSize,
                oreVeinLocation.getColor(),
                255);

        if (oreVeinLocation.drawSearchHighlight() == false || oreVeinLocation.isDepleted()) {
            DrawUtil.drawRectangle(iconX, iconY, iconSize, iconSize, 0x000000, 150);
            if (oreVeinLocation.isDepleted()) {
                DrawUtils.drawQuad(depletedTextureLocation, iconX, iconY, iconSize, iconSize, 0xFFFFFF, 192);
            }
        }

        if (oreVeinLocation.isActiveAsWaypoint()) {
            final double thickness = 3;
            final int borderAlpha = 192;
            final int color = 0x2C03FC;
            DrawUtil.drawRectangle(
                    iconX - thickness, iconY - thickness, iconSize + thickness, thickness, color, borderAlpha);
            DrawUtil.drawRectangle(
                    iconX + iconSize, iconY - thickness, thickness, iconSize + thickness, color, borderAlpha);
            DrawUtil.drawRectangle(iconX, iconY + iconSize, iconSize + thickness, thickness, color, borderAlpha);
            DrawUtil.drawRectangle(iconX - thickness, iconY, thickness, iconSize + thickness, color, borderAlpha);
        }
    }
}
