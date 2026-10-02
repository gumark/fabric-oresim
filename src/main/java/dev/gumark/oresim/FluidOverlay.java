package dev.gumark.oresim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reveals real water and lava through walls.
 *
 * <p>Only <em>hidden</em> fluids are shown: a fluid block is hidden when
 * something opaque sits above it in its column. Ocean and river surfaces are
 * skipped (they are visible anyway), while underground pools, aquifers and
 * lava pockets light up.
 *
 * <p>Each chunk is scanned once and cached; vertical runs of fluid blocks are
 * merged into single boxes to keep the number of drawn boxes small. Scans are
 * amortized over frames so enabling the overlay does not stutter.
 */
public final class FluidOverlay {
    private static final int SCANS_PER_FRAME = 2;

    private record FluidBox(int x, int yMin, int z, int yMax, boolean lava) {
    }

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Map<Long, List<FluidBox>> CACHE = new ConcurrentHashMap<>();
    private static final Deque<Long> QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static ClientLevel cachedLevel;

    private FluidOverlay() {
    }

    public static void onChunkUnload(LevelChunk chunk) {
        CACHE.remove(chunk.getPos().pack());
    }

    /** Draws hidden water/lava boxes around the player. */
    public static void render() {
        if (mc.level == null || mc.player == null) return;
        boolean water = OverlayToggles.isEnabled(OreType.WATER);
        boolean lava = OverlayToggles.isEnabled(OreType.LAVA);
        if (!water && !lava) return;

        if (mc.level != cachedLevel) {
            cachedLevel = mc.level;
            CACHE.clear();
            QUEUE.clear();
            QUEUED.clear();
        }

        GizmoStyle waterStyle = GizmoStyle.stroke(OreType.WATER.color);
        GizmoStyle lavaStyle = GizmoStyle.stroke(OreType.LAVA.color);

        int chunkX = mc.player.chunkPosition().x();
        int chunkZ = mc.player.chunkPosition().z();
        for (int range = 0; range <= OreSim.CHUNK_RANGE; range++) {
            for (int x = -range + chunkX; x <= range + chunkX; x++) {
                renderChunk(x, chunkZ + range - OreSim.CHUNK_RANGE, water, lava, waterStyle, lavaStyle);
            }
            for (int x = -range + 1 + chunkX; x < range + chunkX; x++) {
                renderChunk(x, chunkZ - range + OreSim.CHUNK_RANGE + 1, water, lava, waterStyle, lavaStyle);
            }
        }

        scanQueued();
    }

    private static void renderChunk(int x, int z, boolean water, boolean lava, GizmoStyle waterStyle, GizmoStyle lavaStyle) {
        long key = ChunkPos.pack(x, z);
        List<FluidBox> boxes = CACHE.get(key);
        if (boxes == null) {
            if (!QUEUED.contains(key)) {
                QUEUED.add(key);
                QUEUE.addLast(key);
            }
            return;
        }

        for (FluidBox box : boxes) {
            if (box.lava() ? !lava : !water) continue;
            Gizmos.cuboid(new AABB(box.x(), box.yMin(), box.z(), box.x() + 1, box.yMax() + 1, box.z() + 1),
                box.lava() ? lavaStyle : waterStyle).setAlwaysOnTop();
        }
    }

    private static void scanQueued() {
        for (int i = 0; i < SCANS_PER_FRAME && !QUEUE.isEmpty(); i++) {
            long key = QUEUE.pollFirst();
            QUEUED.remove(key);
            ChunkPos pos = ChunkPos.unpack(key);
            LevelChunk chunk = mc.level.getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
            if (chunk != null) {
                CACHE.put(key, scanChunk(chunk));
            }
        }
    }

    /** Collects hidden fluid runs in one chunk, top-down per column. */
    private static List<FluidBox> scanChunk(LevelChunk chunk) {
        List<FluidBox> boxes = new ArrayList<>();
        ChunkPos chunkPos = chunk.getPos();
        LevelChunkSection[] sections = chunk.getSections();
        int minY = chunk.getMinY();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = (chunkPos.x() << 4) + x;
                int wz = (chunkPos.z() << 4) + z;

                boolean covered = false;
                int runTop = -1;
                boolean runLava = false;

                for (int si = sections.length - 1; si >= 0; si--) {
                    LevelChunkSection section = sections[si];
                    if (section == null || section.hasOnlyAir()) {
                        if (runTop >= 0) {
                            boxes.add(new FluidBox(wx, minY + (si << 4) + 15, wz, runTop, runLava));
                            runTop = -1;
                        }
                        continue;
                    }
                    int sectionBottom = minY + (si << 4);
                    for (int y = sectionBottom + 15; y >= sectionBottom; y--) {
                        BlockState state = section.getBlockState(x, y & 15, z);
                        if (state.isAir()) {
                            if (runTop >= 0) {
                                boxes.add(new FluidBox(wx, y + 1, wz, runTop, runLava));
                                runTop = -1;
                            }
                            continue;
                        }

                        Block block = state.getBlock();
                        boolean isLava = block == Blocks.LAVA;
                        boolean isWater = block == Blocks.WATER;
                        if (isLava || isWater) {
                            if (!covered) continue;
                            if (runTop >= 0 && runLava == isLava) continue;
                            if (runTop >= 0) {
                                boxes.add(new FluidBox(wx, y + 1, wz, runTop, runLava));
                            }
                            runTop = y;
                            runLava = isLava;
                        } else {
                            if (runTop >= 0) {
                                boxes.add(new FluidBox(wx, y + 1, wz, runTop, runLava));
                                runTop = -1;
                            }
                            if (state.canOcclude()) {
                                covered = true;
                            }
                        }
                    }
                }
                if (runTop >= 0) {
                    boxes.add(new FluidBox(wx, minY, wz, runTop, runLava));
                }
            }
        }
        return boxes;
    }
}
