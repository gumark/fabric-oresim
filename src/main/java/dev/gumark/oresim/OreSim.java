package dev.gumark.oresim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simulates vanilla ore decoration per chunk from the world seed and stores the
 * resulting ore positions so they can be rendered as boxes through walls.
 *
 * <p>Derived from the OreSim module of
 * <a href="https://github.com/AntiCope/meteor-rejects">meteor-rejects</a>, ported
 * to standalone Fabric on Minecraft 26.3.
 */
public class OreSim {
    /** How many chunks around the player are simulated and rendered. */
    public static final int CHUNK_RANGE = 5;

    private final Minecraft mc = Minecraft.getInstance();
    private final Map<Long, Map<Ore, Set<BlockPos>>> chunkRenderers = new ConcurrentHashMap<>();

    private long worldSeed;
    private Map<ResourceKey<Biome>, List<Ore>> oreConfig;
    private boolean active;

    private ClientLevel lastLevel;
    private LocalPlayer lastPlayer;

    public boolean isActive() {
        return active;
    }

    public void toggle() {
        setActive(!active);
    }

    public void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;

        if (active) {
            if (SeedStore.getSeed() == null) {
                this.active = false;
                if (mc.player != null) {
                    mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "OreSim: no seed set. Use /setoresimseed <seed> first."));
                }
                return;
            }
            reload();
        } else {
            clear();
        }
    }

    /** Clears all simulated data. */
    public void clear() {
        chunkRenderers.clear();
        oreConfig = null;
    }

    /** Called when the seed changes while the simulation may be running. */
    public void onSeedChanged() {
        if (active) reload();
    }

    /** Detects world changes (dimension switch, respawn) and reloads the simulation. */
    public void tick() {
        if (mc.level != lastLevel || mc.player != lastPlayer) {
            lastLevel = mc.level;
            lastPlayer = mc.player;
            if (active) reload();
        }
    }

    /** Called for every chunk that finishes loading on the client. */
    public void onChunkLoad(LevelChunk chunk) {
        if (active && oreConfig != null) {
            doMathOnChunk(chunk);
        }
    }

    private void reload() {
        clear();
        Long seed = SeedStore.getSeed();
        if (seed == null || mc.level == null) return;

        worldSeed = seed;
        oreConfig = Ore.getRegistry(mc.level.dimension());

        for (int range = 0; range <= CHUNK_RANGE; range++) {
            for (int x = -range; x <= range; x++) {
                loadChunk(x, CHUNK_RANGE - range);
                if (range != 0) loadChunk(x, -(CHUNK_RANGE - range));
            }
        }
    }

    private void loadChunk(int offsetX, int offsetZ) {
        if (mc.player == null) return;
        ChunkPos center = mc.player.chunkPosition();
        LevelChunk chunk = mc.level.getChunkSource().getChunk(center.x() + offsetX, center.z() + offsetZ, ChunkStatus.FULL, false);
        if (chunk != null) doMathOnChunk(chunk);
    }

    private void doMathOnChunk(ChunkAccess chunk) {
        var chunkPos = chunk.getPos();
        long chunkKey = chunkPos.pack();
        ClientLevel world = mc.level;
        if (world == null || oreConfig == null || chunkRenderers.containsKey(chunkKey)) return;

        // Collect every biome present in this chunk and its neighbours so the
        // correct ore set can be selected.
        Set<ResourceKey<Biome>> biomes = new HashSet<>();
        ChunkPos.rangeClosed(chunkPos, 1).forEach(neighborPos -> {
            ChunkAccess neighbor = world.getChunkSource().getChunk(neighborPos.x(), neighborPos.z(), ChunkStatus.BIOMES, false);
            if (neighbor == null) return;
            for (LevelChunkSection section : neighbor.getSections()) {
                section.getBiomes().getAll(entry -> entry.unwrapKey().ifPresent(biomes::add));
            }
        });

        Set<Ore> oreSet = biomes.stream().flatMap(b -> getDefaultOres(b).stream()).collect(Collectors.toSet());

        int chunkX = chunkPos.x() << 4;
        int chunkZ = chunkPos.z() << 4;
        WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(0L));
        long populationSeed = random.setDecorationSeed(worldSeed, chunkX, chunkZ);

        HashMap<Ore, Set<BlockPos>> h = new HashMap<>();
        for (Ore ore : oreSet) {
            HashSet<BlockPos> ores = new HashSet<>();
            random.setFeatureSeed(populationSeed, ore.index, ore.step);

            int repeat = ore.count.sample(random);
            for (int i = 0; i < repeat; i++) {
                if (ore.rarity != 1F && random.nextFloat() >= 1 / ore.rarity) {
                    continue;
                }
                int x = random.nextInt(16) + chunkX;
                int z = random.nextInt(16) + chunkZ;
                int y = ore.heightProvider.sample(random, ore.heightContext);
                BlockPos origin = new BlockPos(x, y, z);

                ResourceKey<Biome> biome = chunk.getNoiseBiome(x, y, z).unwrapKey().orElse(null);
                if (biome == null || !getDefaultOres(biome).contains(ore)) {
                    continue;
                }

                if (ore.scattered) {
                    ores.addAll(generateHidden(world, random, origin, ore));
                } else {
                    ores.addAll(generateNormal(world, random, origin, ore));
                }
            }
            if (!ores.isEmpty()) {
                h.put(ore, ores);
            }
        }
        chunkRenderers.put(chunkKey, h);
    }

    private List<Ore> getDefaultOres(ResourceKey<Biome> biomeRegistryKey) {
        if (oreConfig.containsKey(biomeRegistryKey)) {
            return oreConfig.get(biomeRegistryKey);
        }
        return oreConfig.values().stream().findFirst().orElse(List.of());
    }

    // ====================================
    // Rendering
    // ====================================

    /** Emits a gizmo box for every simulated ore; called right before vanilla collects its gizmos. */
    public void render() {
        if (!active || mc.player == null || mc.level == null || oreConfig == null) return;

        int chunkX = mc.player.chunkPosition().x();
        int chunkZ = mc.player.chunkPosition().z();
        for (int range = 0; range <= CHUNK_RANGE; range++) {
            for (int x = -range + chunkX; x <= range + chunkX; x++) {
                renderChunk(x, chunkZ + range - CHUNK_RANGE);
            }
            for (int x = -range + 1 + chunkX; x < range + chunkX; x++) {
                renderChunk(x, chunkZ - range + CHUNK_RANGE + 1);
            }
        }
    }

    private void renderChunk(int x, int z) {
        Map<Ore, Set<BlockPos>> chunk = chunkRenderers.get(ChunkPos.pack(x, z));
        if (chunk == null) return;

        for (Map.Entry<Ore, Set<BlockPos>> oreRenders : chunk.entrySet()) {
            Ore ore = oreRenders.getKey();
            if (!OverlayToggles.isEnabled(ore.type)) continue;
            GizmoStyle style = GizmoStyle.stroke(ore.type.color);
            for (BlockPos pos : oreRenders.getValue()) {
                // Hide boxes whose block has been mined out or exposed.
                if (!mc.level.getBlockState(pos).canOcclude()) continue;
                Gizmos.cuboid(pos, style).setAlwaysOnTop();
            }
        }
    }

    // ====================================
    // Vanilla ore generation
    // ====================================

    private List<BlockPos> generateNormal(ClientLevel world, WorldgenRandom random, BlockPos blockPos, Ore ore) {
        int veinSize = ore.size;
        float f = random.nextFloat() * 3.1415927F;
        float g = (float) veinSize / 8.0F;
        int i = Mth.ceil(((float) veinSize / 16.0F * 2.0F + 1.0F) / 2.0F);
        double d = (double) blockPos.getX() + Math.sin(f) * (double) g;
        double e = (double) blockPos.getX() - Math.sin(f) * (double) g;
        double h = (double) blockPos.getZ() + Math.cos(f) * (double) g;
        double j = (double) blockPos.getZ() - Math.cos(f) * (double) g;
        double l = (blockPos.getY() + random.nextInt(3) - 2);
        double m = (blockPos.getY() + random.nextInt(3) - 2);
        int n = blockPos.getX() - Mth.ceil(g) - i;
        int o = blockPos.getY() - 2 - i;
        int p = blockPos.getZ() - Mth.ceil(g) - i;
        int q = 2 * (Mth.ceil(g) + i);
        int r = 2 * (2 + i);
        for (int s = n; s <= n + q; ++s) {
            for (int t = p; t <= p + q; ++t) {
                if (o <= world.getHeight(Heightmap.Types.MOTION_BLOCKING, s, t)) {
                    return generateVeinPart(world, random, veinSize, d, e, h, j, l, m, n, o, p, q, r, ore);
                }
            }
        }
        return new ArrayList<>();
    }

    private List<BlockPos> generateVeinPart(ClientLevel world, WorldgenRandom random, int veinSize, double startX, double endX, double startZ, double endZ, double startY, double endY, int x, int y, int z, int size, int i, Ore ore) {
        BitSet bitSet = new BitSet(size * i * size);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        double[] ds = new double[veinSize * 4];
        List<BlockPos> poses = new ArrayList<>();
        int n;
        double p;
        double q;
        double r;
        double s;
        for (n = 0; n < veinSize; ++n) {
            float f = (float) n / (float) veinSize;
            p = Mth.lerp(f, startX, endX);
            q = Mth.lerp(f, startY, endY);
            r = Mth.lerp(f, startZ, endZ);
            s = random.nextDouble() * (double) veinSize / 16.0D;
            double m = ((double) (Mth.sin(3.1415927F * f) + 1.0F) * s + 1.0D) / 2.0D;
            ds[n * 4] = p;
            ds[n * 4 + 1] = q;
            ds[n * 4 + 2] = r;
            ds[n * 4 + 3] = m;
        }
        for (n = 0; n < veinSize - 1; ++n) {
            if (!(ds[n * 4 + 3] <= 0.0D)) {
                for (int o = n + 1; o < veinSize; ++o) {
                    if (!(ds[o * 4 + 3] <= 0.0D)) {
                        p = ds[n * 4] - ds[o * 4];
                        q = ds[n * 4 + 1] - ds[o * 4 + 1];
                        r = ds[n * 4 + 2] - ds[o * 4 + 2];
                        s = ds[n * 4 + 3] - ds[o * 4 + 3];
                        if (s * s > p * p + q * q + r * r) {
                            if (s > 0.0D) {
                                ds[o * 4 + 3] = -1.0D;
                            } else {
                                ds[n * 4 + 3] = -1.0D;
                            }
                        }
                    }
                }
            }
        }
        for (n = 0; n < veinSize; ++n) {
            double u = ds[n * 4 + 3];
            if (!(u < 0.0D)) {
                double v = ds[n * 4];
                double w = ds[n * 4 + 1];
                double aa = ds[n * 4 + 2];
                int ab = Math.max(Mth.floor(v - u), x);
                int ac = Math.max(Mth.floor(w - u), y);
                int ad = Math.max(Mth.floor(aa - u), z);
                int ae = Math.max(Mth.floor(v + u), ab);
                int af = Math.max(Mth.floor(w + u), ac);
                int ag = Math.max(Mth.floor(aa + u), ad);
                for (int ah = ab; ah <= ae; ++ah) {
                    double ai = ((double) ah + 0.5D - v) / u;
                    if (ai * ai < 1.0D) {
                        for (int aj = ac; aj <= af; ++aj) {
                            double ak = ((double) aj + 0.5D - w) / u;
                            if (ai * ai + ak * ak < 1.0D) {
                                for (int al = ad; al <= ag; ++al) {
                                    double am = ((double) al + 0.5D - aa) / u;
                                    if (ai * ai + ak * ak + am * am < 1.0D) {
                                        int an = ah - x + (aj - y) * size + (al - z) * size * i;
                                        if (!bitSet.get(an)) {
                                            bitSet.set(an);
                                            mutable.set(ah, aj, al);
                                            if (!world.isOutsideBuildHeight(aj) && canPlaceOre(world, random, mutable, ore)) {
                                                poses.add(mutable.immutable());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return poses;
    }

    private List<BlockPos> generateHidden(ClientLevel world, WorldgenRandom random, BlockPos origin, Ore ore) {
        List<BlockPos> poses = new ArrayList<>();
        int count = random.nextInt(ore.size + 1);
        for (int j = 0; j < count; ++j) {
            int size = Math.min(j, 7);
            int x = randomCoord(random, size) + origin.getX();
            int y = randomCoord(random, size) + origin.getY();
            int z = randomCoord(random, size) + origin.getZ();
            BlockPos pos = new BlockPos(x, y, z);
            if (canPlaceOre(world, random, pos, ore)) {
                poses.add(pos);
            }
        }
        return poses;
    }

    private int randomCoord(WorldgenRandom random, int size) {
        return Math.round((random.nextFloat() - random.nextFloat()) * (float) size);
    }

    // ====================================
    // Vanilla placement decisions
    // ====================================

    /**
     * Mirrors {@code AbstractOreFeature#canPlaceOre}: the block must be a valid
     * replacement target (or already hold the ore, since the real world is
     * generated), then the air-exposure rule decides.
     */
    private boolean canPlaceOre(ClientLevel world, RandomSource random, BlockPos pos, Ore ore) {
        BlockState state = world.getBlockState(pos);
        for (BlockReplacement target : ore.targets) {
            if (target.target().test(state, pos, random) || state.equals(target.state())) {
                return shouldPlace(world, random, pos, ore.discardOnAirChance);
            }
        }
        return false;
    }

    /** Mirrors vanilla's {@code shouldSkipAirCheck} + {@code isAdjacentToAir} logic. */
    private boolean shouldPlace(ClientLevel world, RandomSource random, BlockPos pos, float discardOnAir) {
        if (discardOnAir <= 0F) return true;
        if (discardOnAir < 1F && random.nextFloat() >= discardOnAir) return true;
        return !isAdjacentToAir(world, pos);
    }

    private boolean isAdjacentToAir(ClientLevel world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (world.getBlockState(pos.offset(direction.getUnitVec3i())).isAir()) {
                return true;
            }
        }
        return false;
    }
}
