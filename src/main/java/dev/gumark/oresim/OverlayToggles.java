package dev.gumark.oresim;

import java.util.EnumSet;

/** Which overlay categories are currently visible. All enabled by default. */
public final class OverlayToggles {
    private static final EnumSet<OreType> DISABLED = EnumSet.noneOf(OreType.class);

    private OverlayToggles() {
    }

    public static boolean isEnabled(OreType type) {
        return !DISABLED.contains(type);
    }

    public static void toggle(OreType type) {
        if (!DISABLED.remove(type)) {
            DISABLED.add(type);
        }
    }
}
