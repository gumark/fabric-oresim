# OreSim by gumark

A standalone Fabric mod for **Minecraft 26.x** (26.1.2 – 26.3) that predicts where ores generated
from the world seed and renders them as colored boxes through walls — no xray
resource pack, no Meteor Client required.

Extracted from the `OreSim` module of
[meteor-rejects](https://github.com/AntiCope/meteor-rejects) and rebuilt as its
own client-side mod.

## Usage

| Action | How |
| --- | --- |
| Toggle the overlay | **X** key (rebindable in Controls) |
| Set the world seed | `/setoresimseed <seed>` |
| Choose what is shown | `/oresim` opens the toggle GUI |

- **Singleplayer:** the seed is detected automatically — just press **X**.
- **Multiplayer:** run `/setoresimseed <seed>` once per world, then press **X**.
  Seeds follow vanilla rules: numeric text is used as a literal seed, any other
  text is hashed (like typing a word into the world creation screen).
- Rendering is **purely seed-driven**: every simulated position gets a box,
  including ores you can already see, and the client world state is never
  consulted. Anti-xray fake blocks can therefore neither suppress nor fake a
  box — the boxes always show what the seed says actually generated.
- The `/oresim` GUI has an on/off toggle for every ore type.

The simulation mirrors vanilla's ore decoration (count, rarity, height range,
vein shape, air-exposure discard) driven by the vanilla placed-feature data for
the current dimension, so the positions match what the game actually generated.

## Requirements

- Minecraft 26.1.2, 26.2, or 26.3 — use the jar matching your game version
  (every jar name ends in `-mc<minecraft-version>`)
- [Fabric Loader](https://fabricmc.net/use/) 0.19+
- [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version

Install the built jar in your `mods` folder alongside Fabric API. Minecraft
1.21.11 and older use a different rendering/worldgen API and are not supported
yet.

## Building

Requires JDK 25.

```bash
gradle build        # or ./gradlew build after generating the wrapper
```

The mod jar is written to
`build/libs/oresim-by-gumark-<version>-mc<minecraft-version>.jar`.

To build for another Minecraft version of the same API generation, override the
target versions (use the matching Fabric API build):

```bash
gradle build -Pmc=26.2 -Pfapi=0.161.0+26.2
gradle build -Pmc=26.1.2 -Pfapi=0.155.3+26.1.2
```

## Notes

- Rendering uses the vanilla gizmo system and is always drawn on top, so ores
  are visible through terrain.
- The overlay covers a 5-chunk taxi-cap radius around the player.
- World edits by players (or anything that changes blocks before the
  simulation sees the chunk) can cause rare false positives. Because rendering
  is seed-only, boxes stay even after the ore itself has been mined out.

## Credits

OreSim was originally part of
[meteor-rejects](https://github.com/AntiCope/meteor-rejects) (the OreSim module
and its worldgen port of vanilla's ore feature). This standalone version is a
port to standalone Fabric for Minecraft 26.3 by gumark.

## License

GPL-3.0-only — see [LICENSE](LICENSE). This is a derivative work of
meteor-rejects, which is also GPL-3.0.
