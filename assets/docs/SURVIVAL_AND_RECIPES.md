# Survival Mode and Crafting

City Super Mod is aimed at creative play, and that has not changed. This document covers the
survival layer added on top of it: how CSM content is obtained without creative mode, how blocks
behave when mined, and what to do when you add a new block.

## The two-tier chain

```
Vanilla ores / ingots  --(crafting table)-->  CSM parts  --(CSM Fabricator)-->  CSM blocks
```

Tier 1 is fourteen crafting parts, made at a vanilla crafting table from vanilla materials.
Tier 2 is every CSM block, made at the CSM Fabricator from those parts.

**CSM adds nothing to world generation.** `CsmWorldGenerator.generate()` is intentionally empty,
and there are no loot tables, villager trades, mob spawns, structures or biome hooks. No CSM block
appears in a world unless a player places it. Nothing in this document changes that.

## Tier 1 — the parts

The parts live in the **CSM: Materials** creative tab and are defined by
`materials/CsmParts.java`. They have no behavior; they exist only as ingredients.

| Part | Made from |
|---|---|
| Sheet Metal | 3 iron ingots in a row → 4 |
| Fastener Kit | 4 iron nuggets → 4 |
| Pole Section | 3 sheet metal in a column → 2 |
| Enclosure Shell | 8 sheet metal around iron bars |
| Wiring Harness | redstone + iron nugget + string → 2 |
| Control Board | gold nuggets + redstone + quartz |
| LED Module | glowstone + redstone over glass panes → 2 |
| Lens Assembly | 2 glass panes + any dye → 2 |
| Sounder Driver | sheet metal + note block + redstone |
| Reflective Sheeting | quartz + white dye → 2 |
| Sign Blank | sheet metal + reflective sheeting → 2 |
| Concrete Mix | gravel + sand + clay ball → 4 |
| Ducting | 4 sheet metal in a ring → 4 |
| Optical Sensor | lens assembly + control board |

Recipes use `forge:ore_shaped` / `forge:ore_shapeless` so vanilla inputs accept ore-dictionary
equivalents and other mods' materials work.

The CSM Fabricator itself is also crafted at a bench, from sheet metal, a control board and an
enclosure shell.

### Regenerating the part assets

Each part has three files that must stay in step: a texture, an item model, and a recipe. One
script owns all three so they cannot drift:

```bash
python3 dev-env-utils/scripts/gen_part_textures.py      # parts: textures + models + recipes
python3 dev-env-utils/scripts/gen_fabricator_textures.py # Fabricator block textures
```

`gen_part_textures.py` validates before it writes: ore dictionary names against the entries Forge
actually registers, shaped pattern/key agreement, and that subtyped vanilla items carry explicit
metadata. That last check exists because a subtyped ingredient without a `data` field makes Forge
**drop the whole recipe** at load time with only a log line — see "Verifying recipes" below.

## Tier 2 — the CSM Fabricator

Right-click the Fabricator to open a searchable picker listing every fabricable block, grouped by
creative tab. Choose a batch size, pick a block, and the parts are taken from your inventory.

### Why there is no crafting recipe per block

This was the original plan and it does not work. The mod has over 1,500 blocks built from fourteen
parts. Grouping blocks by everything a cost table can actually distinguish — creative tab plus base
class — yields only **47 groups**, so 846 of the 852 non-sign blocks would have shared ingredients
with at least one other block. Minecraft resolves identical recipes to whichever loaded first, so
per-block recipes would have produced roughly 47 craftable blocks and 805 permanently uncraftable
ones, while appearing complete.

The Fabricator sidesteps this by selecting its output **explicitly** rather than inferring it from
ingredients. A cost therefore only has to be *fair*, not *unique*. The same reasoning covers the
472 road signs, which cannot have distinguishable grid recipes either.

### How costs are decided

`materials/CsmFabricatorCosts.java` computes cost at runtime from what a block is — its creative
tab, refined by base class. Nothing is generated and nothing needs regenerating.

| Tab | Cost |
|---|---|
| Road Signs | 1 Sign Blank |
| Building Materials | slab 1 / stairs 2 / fence 1 + fastener / set block 2 sheet metal; concrete family 2 Concrete Mix |
| HVAC | Ducting + Sheet Metal |
| Life Safety | sounders: Sounder Driver + Enclosure; activators: Control Board + Sheet Metal; detectors: Optical Sensor + Control Board |
| Lighting | LED Module + Sheet Metal + Wiring Harness |
| Novelties | 2 Sheet Metal + Control Board |
| Gaming | 2 Sheet Metal + Control Board + LED Module |
| Power Grid | Pole Section + Wiring Harness |
| Technology | Control Board + Sheet Metal + Wiring Harness |
| Traffic Accessories | poles: Pole Section + Fastener Kit; otherwise Sheet Metal + Fastener Kit |
| Traffic Signals | sensors: Optical Sensor + Control Board; controller: Enclosure + 2 Control Board + Wiring; heads: LED Module + Lens + Sheet Metal |
| Furniture | Sheet Metal + Fastener Kit |

A `null` return means "not fabricable". That covers non-CSM blocks and, importantly, everything in
`CsmTabNone`: `CsmTab` gives hidden and retiring blocks a `null` creative tab, so they drop out of
the picker without needing a separate exclusion list.

### Security model

The Fabricator has no tile entity and no inventory; parts come straight from the player. Everything
the client sends is untrusted, and `CsmFabricateHandler` re-checks all of it server-side: batch size
is clamped, the player must be within ~6 blocks, the block must still be a Fabricator, the target
must resolve and be fabricable, and the cost is looked up server-side rather than taken from the
packet. The packet carries a **registry name, not a list index**, because the picker's ordering is
built client-side and must never be something the server trusts. Parts are consumed only after
every check passes, so a failed attempt cannot destroy materials.

## Mining behavior

CSM blocks use a mod-wide standard: `SoundType.STONE`, harvest with a **pickaxe at level 1 (iron)**,
hardness 2, blast resistance 10. An iron pickaxe or better is required or the block drops nothing.

Traffic signal equipment historically used the material-only constructor and so inherited vanilla
defaults — hardness 0, resistance 0, no harvest tool — meaning roughly 40 blocks broke instantly to
a bare fist and were destroyed by any explosion. Those are now set at the five constructors the
hierarchy bottoms out at, rather than at each leaf, so subclasses cannot reintroduce the defaults:

- `logic/AbstractBlockControllableSignal`
- `logic/AbstractBlockTrafficSignalSensor`
- `logic/AbstractBlockTrafficSignalSensorHZEight`
- `BlockTrafficSignalController` (light opacity 255 — the one genuine opaque full cube)
- `BlockOverheightDetectionSensor`

Light level stays 0 for these because every signal computes emitted light dynamically via
`getLightValue`, which takes precedence over the constructor value.

### Known rough edge

Tile-entity configuration is **not** preserved when a block is broken. Breaking a fire alarm panel
or a signal controller in survival loses its entire programming. This is a pre-existing behavior of
the mod, not something the survival layer introduced, and it is the most likely thing to frustrate a
survival player.

## Adding a new block

Costs are derived at runtime, so **a new block usually needs nothing**: it inherits the cost for its
creative tab automatically and appears in the picker on next launch.

You only need to touch `CsmFabricatorCosts` when:

- you add a **new creative tab** — add a case for it, or its contents silently fall through to the
  generic Sheet Metal + Fastener Kit cost; or
- the block is a new *kind* of equipment whose subsystem default is a poor fit (a new detector type,
  say) — add an `instanceof` branch.

Check the startup log line to confirm coverage:

```
Fabricator coverage: 1426 of 1554 registered blocks are fabricable
```

The remainder should be exactly the `CsmTabNone` blocks, the itemless `*_slab_double` blocks, and
the Fabricator itself. A sudden drop means a tab was added or renamed without pricing its contents.

`dev-env-utils/scripts/csm_block_index.py` prints the same picture statically from the sources, and
is useful for cross-checking the runtime number. It resolves the five different registration forms
the tab files use (class literal, fully qualified class literal, constructor with a registry name,
no-arg constructor, and pre-built instances held as constants on a holder class).

## Verifying recipes

**`./gradlew build` does not verify JSON recipes.** They are parsed when the game starts, so a
broken recipe compiles perfectly and then silently fails to load. Use the dedicated server smoke
test and then grep its log:

```bash
bash .github/scripts/server-smoke-test.sh
grep "Parsing error loading recipe" server-smoke.log
grep "Fabricator coverage" server-smoke.log
```

Startup success alone is **not** sufficient evidence: the first run of the part recipes passed the
smoke test with a dead `part_concrete_mix` recipe in it, because `minecraft:sand` has subtypes and
Forge dropped the recipe rather than failing the boot.

Beware also that a client-only method called from common code compiles fine and dies only on a
dedicated server — `CreativeTabs.getTabLabel()` is `@SideOnly(CLIENT)`, which is why
`CsmTab.getTabId(CreativeTabs)` exists. That failure surfaced as an unrelated "can't pop unfinished
ProgressBar" crash, because the linkage `Error` escaped an `Exception`-only catch and the `finally`
block masked its cause.
