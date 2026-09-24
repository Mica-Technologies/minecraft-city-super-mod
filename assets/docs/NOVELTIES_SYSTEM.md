# Novelties System

Technical documentation for the novelties subsystem in the City Super Mod.

## Overview

The novelties package provides decorative and interactive blocks -- arcade cabinets, water
dispensers, a hand dryer, a xylophone, a record player, seasonal decorations, figurines, and
game tables. Interactive blocks respond to right-click with sound playback, item conversion,
particle effects, or musical notes.

All novelty code lives in `src/main/java/com/micatechnologies/minecraft/csm/novelties/`.
Blocks are registered across two creative tabs:

- **CsmTabNovelties** (order 5) -- seasonal, collectible, and decorative blocks plus
  interactive utilities (hand dryer, water dispensers, xylophone, record player)
- **CsmTabGaming** (order 13) -- arcade cabinets, game tables (air hockey, ping pong)

Every block in this package extends `AbstractBlockRotatableNSEWUD` (full NSEW+UD rotation).
There are **no tile entities** in the novelties system. All logic is handled through
`onBlockActivated` and (for the hand dryer) `randomDisplayTick`.

## Interactive Blocks

### Arcade Cabinets

Seven arcade cabinet blocks play an attract-mode sound clip on right-click. All use the same
pattern: server-side only (`!world.isRemote`), look up a `FurnishingsSounds` enum value, and
play it at the block position with volume 1.0 and pitch 1.0.

Each cabinet emits light level 1 (the `lightOpacity` constructor parameter) and uses the
`SOLID` render layer.

| Block Class | Registry Name | Sound Enum | Sound Event ID |
|---|---|---|---|
| `BlockACAsteroids` | `acasteroids` | `ASTEROIDS_CABINET` | `csm:asteroids_cabinet` |
| `BlockACBattleZone` | `acbattlezone` | `BZ_CABINET` | `csm:bz_cabinet` |
| `BlockACCentipede` | `accentipede` | `CP_CABINET` | `csm:cp_cabinet` |
| `BlockACGalaga` | `acgalaga` | `GALAGA_CABINET` | `csm:galaga_cabinet` |
| `BlockACMisCmd` | `acmiscmd` | `MISCMD_CABINET` | `csm:miscmd_cabinet` |
| `BlockACPacMan` | `acpacman` | `PACMAN_CABINET` | `csm:pacman_cabinet` |
| `BlockACTempest` | `actempest` | `TEMPEST_CABINET` | `csm:tempest_cabinet` |

### Water Dispensers / Bubblers

Three blocks share identical bottle-filling logic:

| Block Class | Registry Name | Display Name |
|---|---|---|
| `BlockWaterDispenser` | `waterdispenser` | Water Dispenser |
| `BlockWbs` | `wbs` | Water Bubbler (Short) |
| `BlockWbt` | `wbt` | Water Bubbler (Tall) |

**Right-click behavior** (server-side only):

- **Holding a glass bottle:** consumes the bottle, gives the player a water bottle
  (`PotionTypes.WATER`), plays `SoundEvents.ITEM_BOTTLE_FILL` at volume 1.0, pitch 1.0.
  If the player's inventory is full, the water bottle is dropped on the ground.
- **Empty hand / other item:** plays `SoundEvents.ITEM_BOTTLE_FILL` at volume 0.5, pitch 1.2
  (a quieter, higher-pitched "running water" effect with no item exchange).

These blocks use vanilla sounds only -- no custom sound assets.

### Hand Dryer

**Block:** `BlockHd` (registry name `hd`)

**Right-click behavior:**

- **Server side:** plays the custom `csm:handdryer` sound at volume 1.0, pitch 1.0.
- **Client side:** records the block position and current timestamp in a static
  `Map<BlockPos, Long>` (`activeDryers`). This map is `@SideOnly(Side.CLIENT)`.

**Particle effect:** `randomDisplayTick` checks the `activeDryers` map. If the block was
activated within the last 16 seconds (`DRYER_DURATION_MS = 16000`), it spawns
`EnumParticleTypes.CLOUD` particles in a randomized area below the block (Y offset 0.15-0.30)
with a slow downward velocity (0, -0.03, 0). After 16 seconds the entry is removed and
particles stop.

A periodic cleanup (1% chance per tick) removes stale entries older than 21 seconds to handle
cases where a dryer block is broken or unloaded while active.

### Xylophone

**Block:** `BlockXylophone` (registry name `xylophone`)

**Right-click behavior** (both client and server):

1. Determines which "bar" the player clicked based on the hit position. The block's facing
   direction selects the axis: `hitZ` for East/West facing, `hitX` for North/South facing.
2. Maps the position to one of 6 bars (indices 0-5) using `(int)(position * 6)`, clamped to
   the valid range.
3. Each bar maps to a note value from the `NOTES` array: `{0, 4, 7, 12, 16, 19}` (a
   pentatonic scale spanning two octaves in semitones: C, E, G, C', E', G').
4. Pitch is calculated as `2^((note - 12) / 12.0)`, producing pitches from 0.5 to ~1.68.
5. Plays `SoundEvents.BLOCK_NOTE_XYLOPHONE` at volume 3.0.
6. On the client, spawns a `NOTE` particle with color derived from the note value
   (`note / 24.0`).

### Old Record Player

**Block:** `BlockOldRecordPlayer` (registry name `oldrecordplayer`)

**Right-click behavior** (server-side only):

- **Normal click:** plays `FurnishingsSounds.OLDRECORDPLAYER` (`csm:oldrecordplayer`) --
  track 1.
- **Sneak + click:** plays `FurnishingsSounds.OLDRECORDPLAYER2` (`csm:oldrecordplayer2`) --
  track 2.

Both tracks play at volume 1.0, pitch 1.0.

## Block Inventory

All novelty blocks extend `AbstractBlockRotatableNSEWUD` unless noted. None connect to
redstone. All use `CUTOUT_MIPPED` render layer except arcade cabinets (`SOLID`).

### Interactive Blocks

| Registry Name | Display Name | Interaction |
|---|---|---|
| `acasteroids` | Asteroids Arcade Cabinet | Sound on click |
| `acbattlezone` | Battlezone Arcade Cabinet | Sound on click |
| `accentipede` | Centipede Arcade Cabinet | Sound on click |
| `acgalaga` | Galaga Arcade Cabinet | Sound on click |
| `acmiscmd` | Missle Command Arcade Cabinet | Sound on click |
| `acpacman` | PAC-MAN Arcade Cabinet | Sound on click |
| `actempest` | Tempest Arcade Cabinet | Sound on click |
| `waterdispenser` | Water Dispenser | Bottle filling |
| `wbs` | Water Bubbler (Short) | Bottle filling |
| `wbt` | Water Bubbler (Tall) | Bottle filling |
| `hd` | Hand Dryer | Sound + particles |
| `xylophone` | Xylophone | Musical notes |
| `oldrecordplayer` | Old Cruddy OwO Record Player | Track selection |

### Decorative Blocks (no interaction)

| Registry Name | Display Name | Creative Tab |
|---|---|---|
| `airhockeytable` | Air Hockey Table | Gaming |
| `coffeecup` | Coffee Cup | Novelties |
| `nutcracker` | Nutcracker | Novelties |
| `picnicbasket` | Picnic Basket | Novelties |
| `pingpongtable` | Ping Pong Table | Gaming |
| `pshawka97` | Player Statue (Akselhok) | Novelties |
| `pspapaginos` | Player Statue (PapaGinos) | Novelties |
| `psthatcrazypandog` | Player Statue (AngelWingsPanda) | Novelties |
| `pumpkins` | Pumpkins | Novelties |
| `scarecrow` | Scarecrow | Novelties |

## Where the Models Come From

These blocks' models are drawn by generators in `dev-env-utils/scripts/`, and
each takes `--check` (the arcade cabinets, statues, air hockey and ping pong tables, record
player and Dream Machine are hand-made models):

| Script | Blocks |
|---|---|
| `gen_produce_crates.py` | The produce crates, the carrot barrel and the large shipping crate |
| `gen_furnishings_gameroom.py` | Wine and beer racks, the beer tap, the wooden barrel, pool table, dartboard, card deck, office chair, radiator, coat rack, tall mirror, restroom signs, hot tub, chains, boarded planks |
| `gen_novelties.py` | Barber pole, birdhouse, doghouse, hand pump, coffee cup, picnic basket, cookies, xylophone, water dispenser, garden flamingo |
| `gen_novelties_seasonal.py` | Wreath, pumpkins, nutcracker, scarecrow, snowman |
| `gen_furnishings_showpieces.py` | Jukebox, grand piano, grandfather clock, swing chair, Christmas tree (OBJ) |

Blocks removed because their models were not our own work are listed, with their old registry
names, in `assets/to-be-added-to-mod/BLOCKS_TO_REVISIT.md`; Core's `CsmRetiredNames` drops
those names from old worlds without a prompt.

## Residential Furniture

The Residential tab (`CsmTabResidential`) is the furniture round-out's living and dining set, all
drawn by `gen_furniture_residential.py`, in the `furniture.residential` package:

- `BlockResidentialFurniture`: single pieces facing the placer; chairs, stools and armchairs sit
  on Core's `EntityCsmSeat` like the park bench (sneak-click does not sit).
- `BlockResidentialRun` (`LEFT`/`RIGHT`, same block and facing), `BlockBookcase` (adds `UP`/`DOWN`
  so a stack shares one top and plinth), `BlockSofa` (also joins a same-fabric corner) and
  `BlockSofaCorner`: runs drawn as one piece, end parts only at the ends. All actual state.
- `BlockDiningTable`: no facing; joins N/E/S/W in world directions into any rectangle, legs only
  at the outer corners.
- `BlockResidentialStorage` (TV stand 9 slots, sideboard 18): `TileEntityResidentialStorage` holds
  an `ItemStackHandler` exposed as a capability (hoppers work), drops on break, gives a comparator
  signal; the screen is served by `NoveltiesGuiProvider`.

The kitchen follows in the same tab, drawn by `gen_furniture_kitchen.py`:

- `BlockKitchenCabinet` (a `BlockResidentialStorage`): base cabinets (two doors, drawer bank,
  door and drawer), islands, wall cabinets and open shelves. Each has a `KitchenLine` (base,
  wall, island) and joins any cabinet of its line and finish, whatever its front, so a drawer
  bank and a sink base share one countertop. `acceptsRun(state, side, runFacing)` is how a
  neighbour asks whether a run continues through a block. `KitchenFront` picks the sound (doors,
  drawers, or an open shelf with no slots and no tile entity).
- `BlockKitchenCorner` turns a base run like the sofa corner (`left`, `front`): both joined is a
  blind corner (countertop over an L of carcass, as a real one is), one joined is that run's end
  with an end panel, alone it is a plain base. Corners accept each other, so two make a U.
- `BlockKitchenSink`: an empty bucket or glass bottle fills at the tap (vanilla fill sounds);
  anything else opens the nine slots under it.
- `BlockKitchenLight` (range hoods, under-cabinet light): stored `lit`, toggled by right-click,
  its lens texture swapped by the blockstate. The chimney hood sits in the wall-cabinet row with
  its canopy dipping below the block; the under-cabinet hood and light hang at the top of the
  block under a wall cabinet.
- `BlockRefrigerator`: two blocks, stored `upper`; placed and broken as one like a door, the
  lower half holds the 27 slots and drops the item. The model is drawn whole and cut into two.
- `BlockChestFreezer` + `TileEntityResidentialFreezer`: 27 slots; once a second a water bottle
  that has sat 30 s becomes ice, a water bucket packed ice, the empty container going to another
  slot (the water waits while there is no room).

The kitchen's appliances, the things on its counters and the tableware follow, drawn by
`gen_furniture_appliances.py`:

- **Working appliances** share one small machine framework in `furniture.appliance`, meant for
  the later phases' washing machine and printer too. An `ApplianceRecipeBook` is a named list of
  `ApplianceRecipe`s (`ApplianceRecipeBook.get("oven").add(...)`; a recipe says what input it
  takes and how many, what it makes, how long it takes, and whether the result goes back into
  the input for another cycle, as a repair does). An `ApplianceSpec` says which book a kind of
  appliance works from and how: a time factor, the input slot's limit, a water tank (a bucket
  fills it, a bottle adds a cycle, a `plumbed` predicate keeps it full), furnace fuel from a third
  slot, a sound while running and one when done, and the light it gives. The block implements
  `IAppliance` (its spec; `RUNNING`, stored, lights its window) and hands the shared work to
  `ApplianceHelper`. `TileEntityAppliance` runs the cycle on the server; hoppers can put into the
  input and fuel slots what they take and take only from the output; a comparator reads the
  slots' fill. `ContainerAppliance` + `GuiAppliance` (GUI id 36, served by
  `NoveltiesGuiProvider`) draw on the vanilla furnace screen, the fuel slot and flame painted
  out for an electric appliance, with a water gauge for one that uses water.
- Electric appliances need no fuel or redstone: they run whenever the input holds something they
  can use (a kitchen is not wired, and a hopper-fed oven should just cook). `KitchenAppliances`
  holds the books and specs: the oven (range and wall oven) cooks any smelting recipe whose
  output is food in 8 s; the microwave the same, one item, in 3 s, and beeps; the air fryer raw
  food (meat, fish, potatoes) in 4 s; the toaster bread into `toast` in 5 s; the blender an apple
  or two melon slices into a `smoothie`; the coffee machine cocoa beans into `coffee` (Speed for
  30 s), a cup of water each; the dishwasher repairs a tool or weapon (not armour) by a
  twenty-fifth of its durability every 12 s wash, keeping it until it is whole. The coffee
  machine and dishwasher are plumbed when they stand beside a kitchen sink base.
- `BlockBuiltInAppliance` (range, wall oven) and `BlockDishwasher` stand in the base run as an
  `IKitchenFitting`: a cabinet beside one carries its run through it, so shows no end panel. The
  dishwasher also carries the countertop of the cabinet beside it (`counter`, actual state:
  granite beside oak, quartz beside walnut and white, its own top alone). The cooktop cabinet is
  a drawer bank with a glass cooktop in its countertop.
- `BlockCounterPiece` (tableware, chopping board, kettle, mixer), `BlockCounterAppliance` and
  `BlockCookieJar` stand on whatever is under them: `rest` (actual state, `SurfaceRest`) picks a
  model drawn that far down -- 1.5 px on a countertop, 4 on a dining table, 9 on a coffee table --
  and the box moves with it. The kettle whistles and steams on right-click, the mixer whirrs. The
  cookie jar holds nine stacks of cookies and nothing else (`TileEntityCookieJar`).

The bedroom, study and nursery follow, drawn by `gen_furniture_bedroom.py`:

- `BlockResidentialBed` + `BedLayout` (single, double/king, bunk, day bed): beds are slept in
  through Forge's bed hooks (`isBed`, `getBedDirection`, `getBedSpawnPosition`,
  `setBedOccupied`, a no-op: whether a bed is taken is read off the players sleeping in it), so
  night skipping and respawning work as in a vanilla bed. A bed fills two or four blocks; the
  cell is stored with the facing (`part`), placed as one by its own `ItemBlock` (which refuses
  if any cell is taken) and broken as one; only cell 0 drops it. The sleeping position is the
  head cell of the side (or tier) clicked, so a double, king or bunk sleeps two. In the Nether
  or End a bed says so rather than exploding. A day bed sits by day (when sleeping is refused
  for the time) and sleeps at night.
- Vanilla lays a sleeper out only in a `BlockHorizontal`. The server therefore moves a sleeper
  onto the pillow, and `BedSleepClientHandler` (client-only, registered by the module) sets the
  body's render offset each tick: feet 1.8 blocks from the head, at the mattress's height (a top
  bunk's is not a vanilla bed's). The mattress heights in `BedLayout` are the generator's.
- `BlockResidentialTall` generalises the refrigerator's two-block placing and breaking, with
  optional storage: the refrigerator now extends it, as do the dresser with mirror, wardrobe
  (27), vanity (9) and standing mirror (none). `BlockCloset` adds left/right joining.
- The nightstand (9), dresser (27), blanket chest (18), cradle with drawers and changing table
  (9 each) are `BlockResidentialStorage` with drawer or door sounds; the desk joins into runs,
  9 slots a block. Small counter pieces rest on the nightstand, dresser, desk and blanket chest
  (`SurfaceRest` maps them onto the side table, sideboard, table and coffee table heights).
- The desk chair, rocking chair and vanity stool sit on `EntityCsmSeat`; the crib is decorative.
- `BlockRug`: a sixteenth thick, joins on all four sides; the bound border and its corners are
  drawn only on open sides.

Every storage block implements `IResidentialStorage` (slots, where the slots are, open and close
sounds), shares `ResidentialStorageHelper` and the one storage screen, and sounds its doors when
the first player opens it and the last closes it.

Finishes are separate blocks (`_oak`, `_walnut`, `_white`; fabrics `_charcoal`, `_navy`,
`_oatmeal`, `_red`; appliances `_stainless`, `_white`). Countertops: dark granite on oak, light
quartz on walnut and white. The plan for the rest of the round-out (bedroom, kitchen, bath, office,
outdoor, working appliances) aims at parity with other furniture mods.

## Sound Assets

All custom sounds are declared in `FurnishingsSounds.java` (handed to Core's registrar by `CsmFurnishings.preInit`) and defined in `sounds.json`. Every
entry uses `"stream": false` (loaded into memory, not streamed).

| Sound Event ID | FurnishingsSounds Enum | Used By | OGG File |
|---|---|---|---|
| `csm:asteroids_cabinet` | `ASTEROIDS_CABINET` | BlockACAsteroids | `asteroids_cabinet.ogg` |
| `csm:bz_cabinet` | `BZ_CABINET` | BlockACBattleZone | `bz_cabinet.ogg` |
| `csm:cp_cabinet` | `CP_CABINET` | BlockACCentipede | `cp_cabinet.ogg` |
| `csm:galaga_cabinet` | `GALAGA_CABINET` | BlockACGalaga | `galaga_cabinet.ogg` |
| `csm:miscmd_cabinet` | `MISCMD_CABINET` | BlockACMisCmd | `miscmd_cabinet.ogg` |
| `csm:pacman_cabinet` | `PACMAN_CABINET` | BlockACPacMan | `pacman_cabinet.ogg` |
| `csm:tempest_cabinet` | `TEMPEST_CABINET` | BlockACTempest | `tempest_cabinet.ogg` |
| `csm:handdryer` | `HANDDRYER` | BlockHd | `handdryer.ogg` |
| `csm:oldrecordplayer` | `OLDRECORDPLAYER` | BlockOldRecordPlayer | `oldrecordplayer.ogg` |
| `csm:oldrecordplayer2` | `OLDRECORDPLAYER2` | BlockOldRecordPlayer | `oldrecordplayer2.ogg` |
| `csm:cabinet_open`, `csm:cabinet_close` | `CABINET_OPEN`, `CABINET_CLOSE` | kitchen cabinets with doors | synthesised by `gen_furniture_sounds.py` |
| `csm:drawer_open`, `csm:drawer_close` | `DRAWER_OPEN`, `DRAWER_CLOSE` | the drawer bank | synthesised |
| `csm:fridge_open`, `csm:fridge_close` | `FRIDGE_OPEN`, `FRIDGE_CLOSE` | refrigerator, chest freezer | synthesised |
| `csm:appliance_beep`, `csm:oven_timer`, `csm:toaster_pop` | `APPLIANCE_BEEP`, `OVEN_TIMER`, `TOASTER_POP` | done: microwave, air fryer, dishwasher; ovens; toaster | synthesised |
| `csm:blender_whirr`, `csm:coffee_gurgle`, `csm:dishwasher_hum` | `BLENDER_WHIRR`, `COFFEE_GURGLE`, `DISHWASHER_HUM` | while running (the mixer's click too) | synthesised |
| `csm:kettle_whistle`, `csm:jar_lid` | `KETTLE_WHISTLE`, `JAR_LID` | kettle click, cookie jar | synthesised |

The xylophone, water dispensers/bubblers and the kitchen sink use only vanilla sounds (filling an
appliance's tank with a bucket or bottle does too)
(`SoundEvents.BLOCK_NOTE_XYLOPHONE`, `SoundEvents.ITEM_BOTTLE_FILL`, `ITEM_BUCKET_FILL`).
