package dev.gumark.oresim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remembers the seed the player set for each multiplayer server, so
 * rejoining the same server does not require {@code /setoresimseed} again.
 *
 * <p>Entries are keyed by the server address exactly as Minecraft joins it
 * (e.g. {@code hypixel.net}), stored in
 * {@code config/oresim-server-seeds.json}. The in-memory copy loads lazily
 * on first use; {@link #remember} saves the whole map back afterwards, so a
 * missing or corrupt file only risks one session's memory.
 */
public final class ServerSeedMemory {
    private static final Path FILE = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("oresim-server-seeds.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Write side of the config file; kept small and rebuilt on every save. */
    private static final Map<String, Long> seeds = new LinkedHashMap<>();

    private ServerSeedMemory() {
    }

    private static void load() {
        if (!Files.isRegularFile(FILE)) return;
        try {
            ServerSeedMemoryFile parsed = GSON.fromJson(Files.readString(FILE), ServerSeedMemoryFile.class);
            seeds.clear();
            if (parsed != null && parsed.seeds != null) {
                seeds.putAll(parsed.seeds);
            }
        } catch (Exception e) {
            // Leave an empty in-memory map; saving will repair the file.
        }
    }

    /** Stores the seed for this server address and persists the map. */
    public static void remember(String serverAddress, long seed) {
        if (serverAddress == null || serverAddress.isBlank()) return;
        load();
        seeds.put(serverAddress, seed);
        save();
    }

    /** The remembered seed for this server address, or {@code null}. */
    public static Long get(String serverAddress) {
        if (serverAddress == null || serverAddress.isBlank()) return null;
        load();
        return seeds.get(serverAddress);
    }

    private static void save() {
        try {
            Path parent = FILE.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(new ServerSeedMemoryFile(seeds)), StandardCharsets.UTF_8);
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Non-fatal: the seed stays active for this session either way.
        }
    }

    /** JSON shape of the config file. */
    private static final class ServerSeedMemoryFile {
        Map<String, Long> seeds = new LinkedHashMap<>();

        ServerSeedMemoryFile() {
        }

        ServerSeedMemoryFile(Map<String, Long> seedMap) {
            this.seeds = seedMap;
        }
    }
}
