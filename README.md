# CobbleBeacon

A Fabric mod for Minecraft 1.21.1 that adds three new vanilla-beacon-selectable powers themed
around [Cobblemon](https://modrinth.com/mod/cobblemon), Cobbreeding and Cobbleworkers. The powers
slot directly into the existing beacon tier system, right alongside Speed, Haste, Resistance and
the rest — no new blocks, no new GUI, no new keybinds.

## Powers

| Power | Tier | Effect |
|---|---|---|
| **Shiny Luck** | 1 (alongside Speed/Haste) | Increases the shiny odds of wild Pokémon spawns near the beacon. |
| **Diligence** | 2 (alongside Resistance/Jump Boost) | Speeds up Cobbleworkers pasture jobs (navigation, deposit and area-scan cooldowns, plus each job's own per-completion cooldown). |
| **Fertility** | 3 (alongside Strength) | Speeds up Cobbreeding's egg hatch timer and adds a small bonus chance for hatched eggs to be shiny. |

Like every vanilla beacon power, picking the same one twice at a full level-4 pyramid gives it an
amplified tier II (stronger effect, same power).

## Requirements

- Minecraft **1.21.1**, Fabric Loader **0.17.2+**, [Fabric API](https://modrinth.com/mod/fabric-api)
- **[Cobblemon](https://modrinth.com/mod/cobblemon) 1.7.0+** — required. Shiny Luck depends on it directly.
- **[Cobbleworkers](https://modrinth.com/mod/cobbleworkers)** and **[Cobbreeding](https://modrinth.com/mod/cobbreeding)** — optional. Diligence and Fertility only do
  something if the matching mod is installed; otherwise picking them is simply a no-op (they still
  show up as selectable, since the beacon has no built-in way to hide options per-server).

CobbleBeacon is a **server-side** mod. It doesn't need to be installed on the client for singleplayer
worlds hosted from that same client, but on a dedicated server only the server needs it.

## Configuration

A `cobblebeacon.json` is generated in your config folder on first run:

```json
{
  "shinyLuckRateDivisor": 8.0,
  "eggShinyBonusChance": 0.01,
  "eggHatchSpeedMultiplier": 2.0,
  "workerCooldownMultiplier": 0.5
}
```

- `shinyLuckRateDivisor` — Cobblemon's shiny odds are a "1 in N" rate (default ~1/4096). This
  *divides* that rate, so `4.0` means shinies become roughly 4x as common while the effect is active.
- `eggShinyBonusChance` — flat chance added on top of Cobbreeding's own hatched-egg shiny roll.
- `eggHatchSpeedMultiplier` — multiplies how fast Cobbreeding's egg timer counts down.
- `workerCooldownMultiplier` — multiplies Cobbleworkers' job cooldowns (below 1.0 = faster).

Run `/cobblebeacon reload` (operator only) to re-read the file without restarting the server.

## Commands

Both are operator-only (permission level 2):

- `/cobblebeacon status` — reports whether each power's companion mod is detected and active.
- `/cobblebeacon reload` — reloads `cobblebeacon.json`.

## Building from source

```
./gradlew build
```

The output jar is written to `build/libs/`.

## Third-Party Assets

Most icons in this mod are original artwork. Two are adapted from other projects instead of being
drawn from scratch, credited here per their respective licenses:

- `diligence.png` is adapted from vanilla Minecraft's Haste effect icon (© Mojang/Microsoft).
- `fertility.png` is adapted from [Cobbreeding](https://modrinth.com/mod/cobbreeding)'s egg icon,
  used and modified under Cobbreeding's MIT License.

## Disclaimer

CobbleBeacon is an independent, unofficial addon. It is not affiliated with, endorsed by, or
associated with Mojang/Microsoft, Cobblemon, Cobbleworkers or Cobbreeding.

## License

[MIT](LICENSE)
