# OreSim by gumark

A standalone Fabric mod for **Minecraft 26.3** that predicts where ores generated
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
- Boxes are hidden automatically once the ore is mined out or exposed to air.
- The `/oresim` GUI has an on/off toggle for every ore type plus **Water** and
  **Lava**. Water and lava are revealed from the real world (not simulated):
  hidden fluid blocks — underground pools, aquifers, lava pockets — are drawn
  through walls, while surface water (oceans, rivers, lakes) is skipped since
  it is visible anyway.

The simulation mirrors vanilla's ore decoration (count, rarity, height range,
vein shape, air-exposure discard) driven by the vanilla placed-feature data for
the current dimension, so the positions match what the game actually generated.

## Requirements

- Minecraft 26.3
- [Fabric Loader](https://fabricmc.net/use/) 0.19+
- [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3

Install the built jar in your `mods` folder alongside Fabric API.

## Building

Requires JDK 25.

```bash
gradle build        # or ./gradlew build after generating the wrapper
```

The mod jar is written to `build/libs/oresim-by-gumark-<version>.jar`.

## Notes

- Rendering uses the vanilla gizmo system and is always drawn on top, so ores
  are visible through terrain.
- The overlay covers a 5-chunk taxi-cap radius around the player.
- World edits by players (or anything that changes blocks before the
  simulation sees the chunk) can cause rare false positives; the boxes
  disappear when the underlying block is no longer solid.

## Credits

OreSim was originally part of
[meteor-rejects](https://github.com/AntiCope/meteor-rejects) (the OreSim module
and its worldgen port of vanilla's ore feature). This standalone version is a
port to standalone Fabric for Minecraft 26.3 by gumark.

## License

GPL-3.0-only — see [LICENSE](LICENSE). This is a derivative work of
meteor-rejects, which is also GPL-3.0.
