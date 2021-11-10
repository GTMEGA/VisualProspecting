package com.sinthoras.visualprospecting.gui.model.layers;

import com.sinthoras.visualprospecting.gui.model.SupportedMods;
import com.sinthoras.visualprospecting.gui.model.waypoints.Waypoint;
import com.sinthoras.visualprospecting.gui.model.buttons.ButtonManager;
import com.sinthoras.visualprospecting.gui.model.locations.IWaypointAndLocationProvider;
import com.sinthoras.visualprospecting.gui.model.waypoints.WaypointManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public abstract class WaypointProviderManager extends LayerManager {

    private List<? extends IWaypointAndLocationProvider> visibleElements = new ArrayList<>();
    private Map<SupportedMods, WaypointManager> waypointManagers = new EnumMap<>(SupportedMods.class);

    protected Waypoint activeWaypoint = null;

    public WaypointProviderManager(ButtonManager buttonManager) {
        super(buttonManager);
    }

    public void setActiveWaypoint(Waypoint waypoint) {
        activeWaypoint = waypoint;
        visibleElements.forEach(element -> element.onWaypointUpdated(waypoint));
        waypointManagers.values().forEach(translator -> translator.updateActiveWaypoint(waypoint));
    }

    public void clearActiveWaypoint() {
        activeWaypoint = null;
        visibleElements.forEach(IWaypointAndLocationProvider::onWaypointCleared);
        waypointManagers.values().forEach(WaypointManager::clearActiveWaypoint);
    }

    public boolean hasActiveWaypoint() {
        return activeWaypoint != null;
    }

    public void registerWaypointManager(SupportedMods map, WaypointManager waypointManager) {
        waypointManagers.put(map, waypointManager);
    }

    public WaypointManager getWaypointManager(SupportedMods map) {
        return waypointManagers.get(map);
    }

    protected abstract List<? extends IWaypointAndLocationProvider> generateVisibleElements(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ);

    public void recacheVisibleElements(int centerBlockX, int centerBlockZ, int widthBlocks, int heightBlocks) {
        final int radiusBlockX = (widthBlocks + 1) >> 1;
        final int radiusBlockZ = (heightBlocks + 1) >> 1;

        final int minBlockX = centerBlockX - radiusBlockX;
        final int minBlockZ = centerBlockZ - radiusBlockZ;
        final int maxBlockX = centerBlockX + radiusBlockX;
        final int maxBlockZ = centerBlockZ + radiusBlockZ;

        if(forceRefresh || needsRegenerateVisibleElements(minBlockX, minBlockZ, maxBlockX, maxBlockZ)) {
            visibleElements = generateVisibleElements(minBlockX, minBlockZ, maxBlockX, maxBlockZ);

            if(hasActiveWaypoint()) {
                for (IWaypointAndLocationProvider element : visibleElements) {
                    element.onWaypointUpdated(activeWaypoint);
                }
            }

            layerRenderer.values().forEach(layer -> layer.updateVisibleElements(visibleElements));
            forceRefresh = false;
        }
    }
}