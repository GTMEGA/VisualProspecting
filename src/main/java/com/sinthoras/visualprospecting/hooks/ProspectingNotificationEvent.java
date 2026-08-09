package com.sinthoras.visualprospecting.hooks;

import com.sinthoras.visualprospecting.database.OreVeinPosition;

import cpw.mods.fml.common.eventhandler.Event;

public class ProspectingNotificationEvent extends Event {

    @Override
    public boolean isCancelable() {
        return false;
    }

    public static class OreVein extends ProspectingNotificationEvent {

        private final OreVeinPosition position;

        public OreVein(OreVeinPosition position) {
            this.position = position;
        }

        public OreVeinPosition getPosition() {
            return this.position;
        }
    }
}
