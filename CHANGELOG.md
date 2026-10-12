# Changelog

All notable changes to **OreSim by gumark** are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.1.0] — 2026-10-09

Minecraft versions: **26.3**, **26.2**, **26.1.2** (Fabric)

Java: 25+

### Added
- **Per-server seed memory** — seeds set with `/setoresimseed <seed>` are
  remembered per server address in `config/oresim-server-seeds.json`.
  Rejoining the same server restores the seed automatically (announced in
  chat); setting it twice is unnecessary. The seed is persisted on
  disconnect and dropped from the session, so it can never carry over to a
  different server. Singleplayer worlds are unaffected — the real seed is
  still read from the world itself.

### Performance
- **Render-path overhaul** — simulated ore positions are bucketed per ore
  type into flat packed-position arrays at simulation time, so each frame
  only walks plain arrays: no per-ore map entries, EnumSet lookups, or
  style allocations (`GizmoStyle` objects are built once at startup).
  Toggles are snapshotted once per frame, disabled types skip their whole
  group, and empty chunks early-out on a cached box count.
- **Fewer duplicate boxes** — overlapping veins from same-type ore
  placements (e.g. the three iron placements) are deduplicated, trimming
  total box count.
- **Simulation micro-cut** — the per-biome ore fallback no longer streams
  the ore map on every placement attempt (a path hit thousands of times
  per chunk).
- **Simulation lag cuts** — simulation bounded to the rendered 5-chunk
  diamond; removed the 3×3-neighbor full biome-palette rescan; chunks are
  evicted from the cache on unload; unsimulated in-range chunks are swept
  when the player crosses a chunk boundary; per-dimension ore list built
  once per reload; per-type gizmo styles cached.

### Fixed
- `/oresim` GUI no longer closes itself immediately (the screen is opened
  on the next tick instead of inside the chat submit path).
- Startup crash from the mixin config listing mixins that did not exist
  for the target Minecraft generation (per-generation mixin lists).

### Changed
- Fluid/water-lava overlay removed entirely — it was the main lag source
  and is not seed-predictable anyway.
- Overlay renders purely from the world seed: every simulated ore is drawn
  through walls even if the real block is visible, and the client world is
  never consulted, so anti-xray fake blocks can neither suppress nor fake
  boxes. (Earlier the overlay tried to hide ores already visible to the
  player; that behavior was replaced.)
- Jars are named with the mod and Minecraft versions, e.g.
  `oresim-by-gumark-1.1.0-mc26.3.jar`.
- Build is parameterized per Minecraft target
  (`gradle build -Pmc=26.2 -Pfapi=0.161.0+26.2`), with a compat layer
  covering the 26.3 and 26.2/26.1.2 API generations; sources and mixin
  configs are split per generation.
- Multi-version support: 26.2 and 26.1.2 jars ship alongside 26.3.

## [1.0.0] — 2026-10-01

Minecraft version: **26.3** (Fabric)

### Added
- Seed-based ore prediction: simulates vanilla ore decoration per chunk
  from the world seed and renders the results as boxes through walls.
- `X` key toggle for the overlay.
- `/setoresimseed <seed>` to set the world seed on multiplayer servers
  (vanilla seed parsing: numeric literal or text hashed like the vanilla
  world-creation field).
- `/oresim` GUI with per-ore-type toggles (coal, iron, gold, redstone,
  diamond, lapis, copper, emerald, quartz, ancient debris).
- Ported the OreSim module from meteor-rejects to a standalone Fabric mod
  for Minecraft 26.3.
