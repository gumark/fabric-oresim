package dev.gumark.oresim;

import net.minecraft.client.Minecraft;

/**
 * Holds the world seed used by the ore simulation.
 *
 * <p>On singleplayer worlds the seed is read directly from the integrated server.
 * On multiplayer servers it must be provided by the player with
 * <code>/setoresimseed &lt;seed&gt;</code>.
 */
public final class SeedStore {
    private static Long manualSeed = null;

    private SeedStore() {
    }

    /** Sets the seed manually (from {@code /setoresimseed}). */
    public static void setSeed(long seed) {
        manualSeed = seed;
    }

    /** Clears the manually set seed. */
    public static void clearSeed() {
        manualSeed = null;
    }

    /** The seed to simulate with, or {@code null} if none is known. */
    public static Long getSeed() {
        if (manualSeed != null) return manualSeed;

        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return mc.getSingleplayerServer().overworld().getSeed();
        }

        return null;
    }

    /**
     * Parses a seed the same way vanilla does: numeric text is a literal seed,
     * anything else is hashed with {@link String#hashCode()}.
     */
    public static long parseSeed(String input) {
        String stripped = input.strip();
        try {
            return Long.parseLong(stripped);
        } catch (NumberFormatException e) {
            return stripped.hashCode();
        }
    }
}
