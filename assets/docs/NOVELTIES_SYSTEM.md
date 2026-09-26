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

**Coplanar faces.** Every furniture generator (`gen_furniture_*.py`) ends its `generate` with
`model_depth.py`, which finds faces that z-fight and moves them apart; so do the rebuild
generators (`gen_produce_crates.py`, `gen_furnishings_gameroom.py`, `gen_novelties.py` and
`gen_novelties_seasonal.py` through its catalogue) and the Parks generators. The four hand-made
models no generator writes (the player statue, air hockey table, old record player and ping pong
table) were separated once by the same pass, run over just those files; an edit to one of them
should be checked the same way. A piece is built of
boxes, and two of them often put a face on one plane: a detail laid flush on a body (a clock
face, a door window, a checkout's end panel over its cabinet), or a round part's square and the
same square turned 45 degrees sharing a top. For each set of models a blockstate the generator
wrote draws at once (every combination of its multipart properties), the pass takes faces facing
the same way on planes under 0.2 px apart (`SignFaceDepthTest`'s rule, which a 24-bit depth
buffer holds to about 100 blocks), samples both textures where they overlap, and where both are
opaque and differ moves the smaller face along its normal by growing its box, so nothing else
moves. A face keeps the order it was drawn in (out if it was level or in front, back if it was
a little behind), is placed against every face it overlaps at once so a stack fans out rather
than trading places, stays within the -16..32 a model may hold, and is only moved in a model
the generator itself wrote. Moves are 0.05 to 0.65 px. So: draw a detail flush and let the pass
separate it; do not hand-nudge coordinates to dodge z-fighting, or the pass will measure the
nudge as the design.

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

The bathroom, a commercial restroom's fittings and the laundry follow, drawn by
`gen_furniture_bathroom.py`:

- **Taps.** `IWaterTap` marks a block with a working tap and holds the fill: an empty bucket
  or a glass bottle held to it comes back full (vanilla fill sounds). The kitchen sink base,
  `BlockBathroomVanity`, `BlockBasin` (the pedestal sink and the laundry tub) and the bathtub
  have one. An appliance that uses water standing beside any of them is plumbed
  (`KitchenAppliances.nextToSink` now asks for an `IWaterTap`), so a washing machine beside a
  laundry tub never runs dry, as a dishwasher beside a sink does not.
- `BlockBathroomVanity` is a `BlockKitchenCabinet` on a line of its own (`KitchenLine.VANITY`):
  vanities of one finish join under one stone top with a basin and tap in every block, end
  panels only where the run stops, and never run on into a kitchen. Nine slots under each; its
  top is at a countertop's height, so small pieces rest on it.
- `BlockToilet` sits (Core's seat) when the seat is clicked and flushes when the cistern is: a
  click in the back fifth of the block, or above the seat, plays the synthesised flush and
  swirls water in the bowl. The urinal (`BlockBathroomFixture` with a click sound) flushes on
  any click.
- `BlockBathtub`: two blocks long, placed by its own item only where both are free, broken as
  one; the placed block is the tap end (`head` false), the other is `facing.rotateY()` of it.
  `water` is stored in both and draws the water surface: a water bucket fills it, an empty
  bucket scoops a full one out, an empty hand on the tap end runs the tap (or pulls the plug),
  a bottle fills at the tap, and an empty hand on the head end sits the bather in it, looking
  along the tub to the taps.
- `BlockShower` (a `BlockResidentialTall`, glass, drawn in the translucent layer) and
  `BlockShowerHead` (a wall head at the top of its block) spray while `on` (stored): the block
  schedules its own update every two ticks, and `ShowerSpray` sends one particle packet of
  water (block dust of water, which the client tints and lets fall) from the rose and a splash
  where it lands, and replays the two-second spray loop every forty ticks. No tile entity, and
  nothing ticks while it is off. Only the tray and the glass collide, so a player walks into the
  enclosure through its open side and stands under the rose.
- `BlockFoldingFixture`: the baby changing station folds down on a click and up on the next
  (`open`, stored; its own model and box each way), with the trapdoor sounds. It is meant for
  the block above the floor's, so the bed is at 1.1 m.
- Laundry: `BlockLaundryAppliance` (a `BlockBuiltInAppliance` in no run, drawn in the cutout
  layer so the round door window is a circle on a square) with `LaundryAppliances`: the washing
  machine repairs armour a twenty-fifth a 12 s wash, one wash of water each, until it is whole
  (the elytra is left out, as it is of the dishwasher); the dryer dries wet sponges in 10 s.
  Both windows light while they run. Their tops, the vanity's and the ironing board's are
  counter height in `SurfaceRest`, so the steam iron (a `BlockCounterPiece` that hisses and
  puffs steam on click) stands on the board.
- The rest are plain pieces: `BlockBathroomFixture` (a toilet paper holder, towel rails, the
  radiator, soap and paper towel dispensers, the grab bar, the ironing board), storage (the
  mirror cabinet, wastebaskets and laundry baskets, nine slots each; the wastebasket keeps what
  is put in it rather than voiding it, so nothing is lost by a wrong click), counter pieces (the
  toiletries tray, the toilet brush) and bath mats (`BlockRug` in terry, joining like the rugs).

The living room's extras follow, drawn by `gen_furniture_living.py` (textures and models under
`furniture/living/`):

- **TVs** (`BlockTelevision`): a flat screen on its stand or on the wall, one block or two wide,
  and an old tube TV. Right-click steps the channel through `TvChannel` (off, news, sports,
  nature, colour bars, snow), each an animated 64 px texture the blockstate swaps onto the screen
  (a flat screen shows its 16:9 band, the tube TV crops it to 4:3). The channel lives in a
  `TileEntityTelevision` (the metadata is full), read by `getActualState`; the tile entity's
  baked-model key is the channel, so only a change of channel rebuilds the section. A standing TV
  rests on what is under it (`SurfaceRest`), a wall TV ignores it. A two-block TV keeps the
  channel in both blocks and a click on either changes both. The stand TV two wide is the one
  blockstate written out in full keys (channel x facing x block x rest), since both the block and
  the rest pick its model.
- **Two-block pieces** (`WidePieces`, `BlockResidentialWide`): placed as one by their own item
  (`ItemWidePiece`, which refuses unless both blocks are free), broken as one, `part` 0 where it
  was placed and 1 to the right of someone facing it; drawn whole and cut into the two blocks,
  a picture across both (a screen, the keys, the flames, a canvas) keeping one texture across the
  cut (the generator's spanned UVs). The large TVs, the upright piano, the fireplace and the wide
  wall art.
- **Stereo** (`BlockStereo` + `TileEntityStereo`): right-click with a music disc puts it on the
  turntable and plays it with the jukebox's own world event (1010), so the "Now Playing" line, the
  record volume and the range are vanilla's; right-click again ejects it (the event with 0 stops
  it), and breaking the stereo does both. `record` (stored) draws the disc and lights the display.
  A comparator reads the disc as a jukebox's. Bookshelf speakers and the subwoofer are counter
  pieces.
- **Upright piano** (`BlockUprightPiano`): a click on the keys plays the key under the cursor with
  the note block's harp (pitch 0.5 to 2, two octaves, fifteen white keys, a black key from the
  back half of the keys near one), low notes on the player's left, with a note particle. The
  keyboard's position is `PIANO_KEYS` in the generator. The piano bench (`BlockPianoBench`) faces
  the way its placer looks, so its sitter faces the keys.
- **Clocks** (`TileEntityResidentialClockRenderer`): the digital clock's red HH:MM (24-hour, the
  world's time) is a string drawn fullbright on its display, made again only when the minute
  changes; the wall clock's two hands are two untextured quads. Where the digital clock stands is
  read from the world at most once a second. Both stop drawing past 32 blocks.
- **Ceiling fan** (`BlockCeilingFan`): runs on redstone -- powered, the light is on and the
  blades turn; unpowered, both are off (the last power is a `TileEntityPowerMemory`'s, as the
  metadata holds facing, light and fan). Between changes a click steps off, fan, fan and light,
  light. While it runs the blockstate leaves the blades out and `TileEntityCeilingFanRenderer`
  draws the very same baked quads turning (found as the quads the still fan has and the running
  one does not); a few dozen quads a fan, only while it runs.
- **Fireplace** (`BlockFireplace`): flint and steel lights it, an empty hand lights it or puts it
  out, and a change of redstone power does either; lit, the animated flame and the glowing embers
  are swapped in, it gives light 13, sends up flame and smoke particles and now and then crackles.
  The fire is drawn only: it sets nothing alight.
- **Lamps and candles**: `BlockKitchenLight` and `BlockCounterLight` (and so the kitchen and
  office lights, the floor and table lamps and `BlockCandle`) now follow redstone
  (`LampSwitching`: `powered`, stored, and only a change of power switches them). Candles puff a
  wisp of smoke; their flame is drawn.
- **Light switch** (`BlockLightSwitch`): a lever on the wall (it powers what it is on and beside).
  It can also be linked to one block (`SwitchLinks`, `ItemLightSwitch`): with the switch in hand,
  right-click a block that switches with redstone (this module's `ISwitchable` blocks, Core's
  powered blocks, vanilla's lamp, doors, trapdoors, gates, pistons, dispensers, note blocks, TNT,
  powered rails, hoppers, wire, repeaters and comparators) to link it, sneak-right-click the air
  or any other block to clear it, then place the switch anywhere within 32 blocks in the same
  dimension (`TileEntityLightSwitch` keeps the link; a sneak-right-click with an empty hand says
  what it is linked to). Switched on, it puts a hidden relay (`BlockSwitchRelay`, in the hidden
  tab: invisible, no box, not breakable by a player, replaceable by a placed block) in a free
  cell beside the linked block, which powers that block alone, weakly and strongly, as a lever
  its wall; switched off or broken, it takes the relay away. A relay removes itself when the
  block it powers goes, or when a neighbour changes and its switch is gone, off or linked
  elsewhere. Nothing ticks. That is what lets it drive a vanilla redstone lamp or door as well as
  this module's lamps: every block reads power from its neighbours.
- **Doorbell** (`BlockDoorbell`): pressed, it rings a synthesised two-note chime and gives a
  one-second redstone pulse, as a stone button does.
- The rest: photo frames and wall art (`BlockLivingDecor`, and the wide art on
  `BlockResidentialWide`), house plants in pots (counter pieces, cutout; a hanging plant), door
  mats (`BlockRug` in coir and rubber), and the storage crate (a `BlockResidentialStorage` of 27
  slots).

The tab's last section, outdoor and backyard, is drawn by `gen_furniture_outdoor.py` (textures
and models under `furniture/outdoor/`), its new classes in the `furniture.outdoor` package. None
of it repeats the Parks module (benches, picnic tables, bins, playground, fountains, planters):
this is a house's own patio and garden.

- **Patio.** The patio table is a `BlockDiningTable` with a slatted top (teak or powder-coated
  black), joining into any rectangle; chairs and Adirondack chairs sit on Core's seat. The sun
  lounger (`BlockSunLounger`) is two blocks long on `WidePieces`, its backrest raised at the end
  it was placed from; a click on either block sits you just past the backrest's hinge, looking
  to the foot. The outdoor sofa and corner are `BlockSofa` and `BlockSofaCorner` with their own
  teak-framed models, joining as the living room's do; outdoor rugs are `BlockRug`s.
- **Umbrellas.** `BlockPatioUmbrella` is a `BlockResidentialTall` on a weighted base, its square
  canopy 2.25 m across with its edge at 2.05 m; `BlockTableUmbrella` is one block set on a patio
  table, its pole drawn down through the table top to a base on the ground and its canopy at the
  same height. A click opens or furls either (`open`, stored; the free-standing one's in both
  blocks). A canopy panel at 22.5 degrees is laid as strips that narrow toward the top, since a
  JSON element cannot be both tilted and turned: from above and below they read as one pyramid.
- **Grills** (`BlockOutdoorGrill`, a `BlockBuiltInAppliance` in no run, `OutdoorAppliances`):
  both cook from the oven's recipe book on the appliance framework and sizzle while they cook,
  the seam under the lid glowing and smoke rising from the vent. The gas grill needs nothing but
  food (6 s); the charcoal kettle grill burns furnace fuel from its third slot (8 s).
- **Fire pits** (`BlockFirePit`, stone ring or steel bowl) light as the fireplace does: flint and
  steel, an empty hand, or a change of redstone power (`LampSwitching.POWERED`, stored; so a
  light switch works one), with the living room's animated flame and embers swapped in, light
  14, flames, smoke and the quiet crackle. The fire is only drawn. **The chimney stack**
  (`BlockChimney`, a fire pit that gives no light) stacks, drawing its crown and two pots only
  on the top block (`up`, actual state); a click or a change of power on any block lights the
  whole stack, and smoke rises from the pots.
- **The cooler** is a `BlockResidentialStorage` of 18 slots with the refrigerator's seal sounds.
- **Garden.** `BlockPicketFence` (Core's `AbstractBlockFence`) and `BlockPicketGate` (vanilla's
  `BlockFenceGate`, registered as the mod's blocks are) behave as vanilla's: the fence joins
  wooden fences, gates and solid faces, the gate opens away from whoever clicks and on redstone.
  The gate is one picket leaf on its hinges, and the open model is the closed one swung a
  quarter about the hinge (`swing()` in the generator, which carries a turned element's axis
  round with it). `BlockSteppingStones` lies a sixteenth thick like a rug; its blockstate lists
  three layouts each turned four ways, and the game picks one by position, so a path does not
  repeat. `BlockStringLights` is a `BlockResidentialRun` swagged across the top of the block with
  three bulbs, a hook only where the run stops, no collision, light 8 while lit; a click or a
  change of power to any block switches the whole run.
- **Bouncing** (`IBouncy`, `Bounce`). The trampoline (`BlockTrampoline`, a `BlockDiningTable`
  that joins into any rectangle, pad and legs only round the outside), the bounce castle's floor
  and the diving board cancel fall damage and send what lands back up with 0.85 of its speed
  (from `onLanded`, as slime does; sneaking lands dead). A jump off one adds to the bounce it was
  made on (`LivingJumpEvent`, registered by `CsmFurnishings.preInit`, both sides since a
  player's movement is the client's), so jumping each time you land builds up to about six
  blocks; the diving board adds more from its front half, toward the tip. A fall of more than a
  block boings, quietly.
- **The bounce castle** (`BlockBounceCastle`) is three blocks square and two tall, placed and
  broken as one. Its item puts the root in the middle of the floor, one block beyond where the
  player clicked so the doorway faces them, and only where all eighteen blocks are free and
  nobody is in them. The root draws the whole castle (a JSON element may reach -16..32, which is
  just three blocks) and sixteen invisible `BlockBounceCastlePart`s (the hidden tab) fill the
  rest: each stores only its place round the root (`index`, 0 to 15, in the world, not turned),
  reads the castle's facing from the root, and takes its floor, walls and turret boxes from
  `BounceCastleLayout`, whose numbers are the generator's `CASTLE`. The block over the root is
  left empty for bouncing; breaking any part breaks the castle, which drops itself.
- The rest: `BlockDivingBoard` (its board runs out past the block's front), the kiddie pool (sat
  in, on Core's seat), float rings, pet beds, bowls and a litter box (`BlockLivingDecor`), a cat
  tree (`BlockResidentialTall`), the hose reel (`BlockHoseReel`, an `IWaterTap`: a bucket or a
  bottle held to its tap fills, and a water appliance beside it is plumbed) and the outdoor wall
  light (`BlockKitchenLight`, a gooseneck barn light; the Lighting module already has carriage
  lantern sconces).

Every storage block implements `IResidentialStorage` (slots, where the slots are, open and close
sounds), shares `ResidentialStorageHelper` and the one storage screen, and sounds its doors when
the first player opens it and the last closes it.

Finishes are separate blocks (`_oak`, `_walnut`, `_white`; fabrics `_charcoal`, `_navy`,
`_oatmeal`, `_red`; appliances `_stainless`, `_white`; bathroom fittings `_white` porcelain or
`_chrome`). Countertops: dark granite on oak, light
quartz on walnut and white. The plan for the rest of the round-out (bedroom, kitchen, bath, office,
outdoor, working appliances) aims at parity with other furniture mods.

## Commercial & Office

The Commercial & Office tab (`CsmTabCommercialOffice`, `tabcommercialoffice`) holds the furniture
of offices, schools and studios, drawn by `gen_furniture_office.py` (textures and models under
`furniture/office/`), in the `furniture.office` package and on the Residential classes:

- **Desks.** Office desks and desks with a drawer pedestal are `BlockKitchenCabinet`s on a line of
  their own, `KitchenLine.DESK`: desks of one laminate join into one desktop, a T-leg only where
  the run stops (the pedestal holds 18 slots with drawer sounds, the plain desk none). The L-desk
  corner is a `BlockKitchenCorner` on the same line (its new constructor takes the line, box,
  slots and front): with runs on both open sides it is the L, a modesty panel along both backs
  and a leg in the back corner. The reception desk is `KitchenLine.RECEPTION`: a transaction
  counter a block high over the front panel, the work surface behind at desk height and a drawer
  bank (9 slots) facing the receptionist, side panels only at the ends. The teacher's desk is a
  double-pedestal `BlockResidentialStorage` (18). Desks, pedestals, corners and the teacher's desk
  are a table's height in `SurfaceRest` (TABLE), so the things on a desk rest on them; the
  reception counter is FLOOR (a block high).
- The filing cabinet is a `BlockResidentialTall` (1.3 m, four drawers, 27 slots, drawer sounds).
  The office shelving is a `BlockBookcase` whose part models are the living room's bookcase with
  binders for books. The conference table is a `BlockDiningTable` with its own top and steel legs.
- **Cubicle panels** (`BlockCubiclePanel`, full and half height, three fabrics) join like a fence
  but thin: each side is `panel` (towards another panel or a solid wall), `end` (the panel runs to
  the block's edge and stops in an end post) or `none`, all actual state. A post stands in the
  middle only where two sides at right angles are `panel` (the multipart asks with an `OR`), so a
  straight run has none and posts appear only at ends and corners; a run's last block is carried
  to its edge. A lone panel stands across the way it was placed (`along_x`, stored). A full panel
  with a panel on it drops its top cap (`up`), so a half on a full reads as one 1.5 m panel.
  Right-clicking a panel's face with an empty hand hangs a shelf on it (`shelf`, stored: the
  world side), again takes it down.
- **Seating** on Core's `EntityCsmSeat`: the task, guest, conference and gaming chairs
  (`BlockResidentialFurniture`) and the waiting-room bench (`BlockResidentialRun`, a seat in every
  block on one beam, legs and arms at the ends); the school desk and chair sits too, facing its
  desk top.
- **Boards**: the whiteboard, chalkboard and cork board are `BlockResidentialRun`s that join into
  one long board, the frame's ends only where it stops and the markers or chalk (a `tools` part)
  on the tray at its right-hand end. The projector screen and the streamer's green screen are
  `BlockFoldingFixture`s (click to let the screen down or pull it up). Lockers are `BlockCloset`s
  (two blocks tall, joining into a row, 9 slots each) with the locker door sounds.
- **On the desk**: `BlockCounterPiece`s (the computer tower, desk phone (beeps), fax (whirs),
  pen holder, paper tray) and `BlockCounterLight`, a counter piece that switches on and off on a
  click (`lit`, stored; the `glow` texture swapped, and light given while on): the desktop
  computer's and the retro computer's screens, the laptop, the desk lamp. The ring light is a
  `BlockKitchenLight`; the studio camera a plain fixture.
- **The copier** (`BlockBuiltInAppliance` with `OfficeAppliances.COPIER`) copies written books on
  the appliance framework. The framework gained a *supply* slot for it: `ApplianceSpec.supply`
  names what the third slot takes (here a book and quill, with a hint shown over the empty slot)
  and each cycle uses one up; and `ApplianceRecipe.consumesInput()` lets a recipe keep its input.
  So the original stays in the input, each copy (the crafting table's rules: an original makes a
  copy of the original, that a copy of a copy, which cannot be copied) goes to the output and uses
  a book and quill, 5 s each. A written book does not stack, so it makes one copy and waits for it
  to be taken; a hopper under it takes each as it comes.

Laminates are separate blocks: `_white` (white laminate on a white frame), `_grey` (a light grey
laminate on a dark frame) and `_walnut`; seating in `_charcoal`, `_navy` and `_red`; cubicle
panels in `_charcoal`, `_navy` and `_oatmeal`. The tab is priced by `OfficeFabricatorRules`
(registered by `CsmFurnishings.preInit`, mirrored in `audit_fabricator_costs.py`).

## Market & Store

The Market & Store tab (`CsmTabMarketStore`, `tabmarketstore`, order 26) holds a grocery store and
a shop floor, drawn by `gen_furniture_market.py` (textures and models under `furniture/market/`),
in the `furniture.market` package and on the Residential classes. The produce crates moved to it
from the Furniture tab, and the Verifone MX915 from the Technology module (below); both kept their
classes' registry names, so placed ones load as they were.

- **Stock is art, not contents.** What is on a shelf or behind glass is part of the model: each
  kind of stock is a 32 px sheet of four product columns (cans in tiers, bottles, cereal boxes,
  crisp bags, jugs, spray bottles, kitchen roll, cartons, shampoo, ice cream tubs, frozen meals,
  milk, yoghurt, cheese, sweets, loaves and pastries) drawn standing on the sheet's bottom at
  their true height, and a second sheet of their tops. A shelf's `stock_row` lays groups four
  pixels wide across it, each group one column as tall as its product, its front set a little in
  or out so the groups read apart. The packaging is invented: colours and shapes, no names.
  Cutout texels (a bottle's neck, the gap between cans) show the shelf behind.
- **Refrigerated displays.** `BlockDisplayCooler` (a `BlockCloset`: two blocks tall, joining into
  a line, 27 slots in the lower half, the refrigerator's door sounds) is the glass-door reach-in
  cooler and freezer and the open multideck dairy case. A cooler's mullion is drawn half in each
  block, so a line of doors has one between each pair; its door is a translucent pane, so the
  block draws in the translucent layer. `BlockDisplayCase` (a `BlockResidentialStorage` run,
  27 slots) is the island freezer (sliding glass lids, stock lying flat, double-sided), the ice
  cream dipping cabinet and the deli case (curved glass: three facets at 0, 22.5 and 45 degrees,
  since an element turns only by 22.5, and a flat top back to a light canopy over the server's
  side; the end glass steps back with the curve) and the bakery case (straight glass on an oak
  base, two tiers). All are lit when placed (`LIT`, light 10): a sneaking click with an empty
  hand switches them, and so does a change of redstone power (`LampSwitching`; they are
  `ISwitchable`, so a linked light switch works them). Either switches the display and the
  displays joined to it in its line, up to five (`DisplayLine.REACH`, taken alternately from each
  side, so a switch at the end of a row works the next four and one in the middle two either
  way); a longer row takes a second switch, and the cap keeps one change of power from walking an
  arbitrarily long row. Only the switched display records the power, so the others switching sets
  off nothing further. The one-block case keeps `LIT` and
  `POWERED` in its metadata; the two-block one keeps `LIT` in both halves and remembers the power
  in its lower half's `TileEntityDisplayCase` (a `TileEntityResidentialStorage` with one more
  flag), its metadata being full. The generator writes each part that has a lamp or a header
  sign twice, lit and unlit (`<part>_on`, `<part>_off`), and the multipart picks by `lit`.
- **Gondola shelving** (`BlockGondola`, a `BlockBookcase` in the cutout layer): a pegboard back at
  +Z, a shelf at the half with a price strip on its edge, stock on it and below it; the base deck
  only at the bottom of a stack, the top cap only at its head, uprights and end panels only at a
  run's ends. It joins and stacks with any gondola, whatever its stock, so canned goods beside
  cereal over snacks is one run; two runs set back to back make an island gondola. Stocked with
  canned goods, cereal, snacks, bottled drinks, household, health and beauty, or empty. A stock
  group's side on the block's edge is left off: an end panel covers it there, and the next
  block's stock continues it anywhere else.
- **Produce.** `BlockMarketRun` is a `BlockResidentialStorage` run that joins any block of its
  *group*, not only itself. The produce stand (group `produce_stand`) is oak with its bed tilted
  22.5 degrees towards the shopper, the produce crates' own beds (`furniture/produce/bed_*`) on
  it, and a price card on its lip: apples, oranges, lettuce, tomatoes, potatoes and bananas side
  by side are one stand. The bulk bins (translucent: clear fronts) hold nuts and grains or sweets;
  the produce scale hangs from the block above (`BlockBathroomFixture`).
- **Checkout.** The belt counter, the scanner counter and the bagging end are one group,
  `checkout`, so they join into one lane with end panels only where it stops. The belt is an
  animated 32 px texture (four frames, two ticks each, a ridge every eighth of a metre moving a
  texel a frame); the scanner counter's flatbed and tower (its window facing the cashier) beep on
  click (`SCANNER_BEEP`); the bagging end is lower, with a rack of bags, and holds 9 slots.
  `SurfaceRest` knows them: the belt and scanner counters are a countertop's height, the bagging
  end a table's, so counter pieces stand on them. `BlockCashRegister` (a `BlockCounterPiece` with
  a nine-slot drawer, `IResidentialStorage`) is the POS terminal and the old brass register; the
  drawer opens with a synthesised bell and slide (`REGISTER_DRAWER`) and closes with the drawer
  sound. The receipt printer (the copier's sound, faster), the card terminal on its swivel stand
  (the card beep) and the bagging carousel are counter pieces. The self-checkout
  (`BlockMarketTall`, two blocks, its screen and lane light giving light 7 from the upper half)
  beeps on click. The customer service desk is a `BlockKitchenCabinet` on the office's
  `KitchenLine.RECEPTION`, drawn from the reception desk's own parts in store colours with a
  CUSTOMER SERVICE sign on the block at the run's customer-left end. The candy rack is a wire
  rack of chocolate displays on stepped shelves.
- **The Verifone MX915** (`furniture.market.BlockVerifoneMx915`, `vf915`) moved here from the
  Technology module: the class, its blockstate, its hand-made model and texture (still at
  `models/block/technology/shared_models/verifone_mx_915.json` and
  `textures/blocks/technology/verifone_mx_915.png`, now in this module's tree) and its lang line.
  Its sound, a recording of unknown origin, is replaced by a synthesised two-beep approval under
  the same key (`verifone_mx915`, now `FurnishingsSounds`). Lying flat it now rests on what is
  under it (`REST`, actual state): the generator writes its model dropped to each surface height
  and the blockstate that picks one. The Transit module's fare vending machine still plays the
  same beep on a purchase: it finds `csm:verifone_mx915` by name and plays a note block's chime if
  this module is not installed.
- **Store fixtures**: shopping carts in three colours (wire mesh as a 32 px cutout), the cart
  corral (group `cart_corral`: rails along the run, a closed hoop with a CART RETURN sign at its
  left end, open at its right), shopping basket stacks, the security gate pedestal (chirps on
  click), the magazine rack, and the bottle return machine (`BlockMarketTall`, decorative). The
  aisle sign (`BlockAisleSign`) hangs from the ceiling with its number on both faces: a click
  steps it 1 to 16 and round, a sneaking click steps back; the number lives in a
  `TileEntityAisleSign` (the metadata holds the facing) whose baked-model key is the number, and
  the blockstate swaps the face texture for it.

The tab is priced by `MarketFabricatorRules` (registered by `CsmFurnishings.preInit`, mirrored in
`audit_fabricator_costs.py`): displays are sheet steel and glass with their controls and lights,
gondolas steel, stands and counters timber (the belt with a wiring harness for its motor, the
scanner with an optical sensor), terminals electronics, carts and racks iron; the moved crates
and the Verifone cost what they did in their old tabs.

### Fresh departments

The butcher, fishmonger, bakery, florist and coffee station, drawn by the same generator on the
same classes, so a store's fresh side is built from pieces that already behave like the rest of
the tab.

- **Butcher and seafood cases** (`butcher_case_*`, `seafood_case_*`, white and black) are the
  deli case's section, the same base, curved glass and canopy (`SERVICE_SEGS`), with different
  things on show: trays of steaks, chops, mince, chicken, sausages, ribs, lamb and a roast split
  by the green plastic parsley butchers lay between trays (`meat_trays`, one 32 px sheet of eight
  trays), or an ice bed tilted 22.5 degrees towards the shopper with fish, fillets, shrimp, crab
  and mussels on it (`seafood_ice`). Because the section is the same, `BlockDisplayCase` takes a
  group (`"service_case"`): the deli, butcher and seafood cases join one another in any order and
  one light switch works across them. The deli joined only itself before and now joins the
  group. Each block carries a scale on its top, its display read from both sides.
- **Bakery.** The bread rack (`BlockMarketRun`, group `bread_rack`, oak and walnut) is three
  shelves stepping back with a lip each, bagged sliced bread and crusty loaves (the `bread`
  stock) on the outer two and the bakery case's stock on the middle, under a BREAD sign. The
  self-serve pastry case (translucent, group `pastry_case`) has two tiers of muffins, cinnamon
  rolls, cookies, bagels (`pastry`) and doughnuts behind acrylic doors. The hot food case is a
  `BlockDisplayCase` of its own group on the bakery case's straight glass: rotisserie chickens
  in clear clamshells (translucent texels) and foil trays of hot food (`hot_food`) under amber
  heat lamps and a lit HOT FOOD header, switched as the other displays are.
- **The rotisserie oven, the commercial coffee brewer and the fountain drink machine** are
  `BlockCounterAppliance`s on the kitchen's machine framework with specs in `StoreAppliances`.
  The rotisserie roasts raw meat and fish from the air fryer's recipe book, eight at a time and
  half as slow again; the brewer uses the coffee machine's book but takes 32 and its tank lasts
  sixteen cups; the fountain pours a `fountain_drink` (an `ItemResidentialFood` drink, its own
  16 px sprite in `items/market/`, registered in this tab) from sugar and water. The brewer and
  the fountain are plumbed beside a sink like the kitchen's machines. `RUNNING` retextures their
  `glow` (the rotisserie's window, the brewer's lamp, the fountain's LED strip), which is the
  kitchen appliances' pattern.
- **Floral.** The flower bucket stand (`BlockBathroomFixture`, cutout, black metal or oak) holds
  three tiers of galvanised buckets; the floral cooler is a `BlockDisplayCooler`, the reach-in's
  frame (`reach_in_frame()`) over three shelves of buckets, a front row and a back row raised on
  a step, under a lit FRESH FLOWERS header. Each bunch is a crossed pair of planes at 45
  degrees showing a quadrant of `flowers_a` or `flowers_b` (roses, tulips, sunflowers, daisies,
  carnations, lilies, irises, a bouquet wrapped in kraft paper); buckets are octagons.
- **Coffee station.** The self-serve coffee bar (a condiment caddy and a COFFEE sign on its back
  panel) and the cup and lid counter (cups in three sizes, a lid organiser, straws, a bin flap)
  are one group, `drink_station`, so they join into one station with end panels only at its
  ends; both hold nine slots behind their doors. `SurfaceRest` puts the coffee bar at a
  countertop's height, so the brewer, the fountain and the rotisserie stand on it; the cup
  counter's top is full and is not stood on.

The traps:

- **A shared section is a contract.** The deli, butcher and seafood cases meet because their
  bases, glass and canopies are drawn from the same numbers; change one case's section and the
  group still joins it, with a step where they meet. Change `SERVICE_SEGS` for all three or give
  the odd one a group of its own.
- **Nothing a counter piece carries may reach past z 12.5.** The coffee bar's caddy and back
  panel start there, and a piece resting on it is drawn in the block above; the rotisserie was
  first drawn to z 14 and sat in the caddy.
- **Keep a two-block model's rounds off the half line.** `split_tall` cuts an element at y 16 but
  hands a turned element whole to the half its middle is in, so an octagon (two of its four
  rectangles are turned) straddling the line splits into a sliver above and whole rectangles
  below. The floral cooler's back row stands on a 1.75 px step, not 2, so its buckets on the
  middle shelf end at y 15.75.
- **Stock must fit under the shelf above it.** The bread rack's lower shelves have 4.25 px under
  the next shelf's back, so the `bread` sheet's tallest column is 4 px.
- **The rotisserie's glass is painted.** A counter appliance draws in the solid layer, so its
  door cannot be seen through; the spits of chickens are in the window texture, lit and unlit,
  as the oven's window is.
- **The ice bed's sides are open.** It is a turned slab; at a run's end the wedge under it shows
  through the end glass. The end glass is close to it and the ice is pale, so it was left.
- **Atlas.** The departments added 27 sprites, 16 to 32 px except the two headers (64 px, lit
  and unlit: a sprite must be square and FRESH FLOWERS is 52 texels wide in the pixel font),
  and reuse the bakery stock, the reach-in's frame textures, the kitchen's lamps and the
  residential woods; the block atlas went from 95.1% to 95.2% of 8192 x 4096.

### Front of the store

The pharmacy, the tobacco case and the lottery behind the register, the kiosks inside the door
and the merchandisers outside it, drawn by the same generator on classes the tab already had:
nothing here needed a new Java class or a new property.

- **Pharmacy.** The drop-off and pick-up counters (`pharmacy_dropoff_*`, `pharmacy_pickup_*`,
  teal and walnut) are `BlockKitchenCabinet`s on the office's `KitchenLine.RECEPTION`, drawn
  from the reception desk's own parts, as the customer service desk is, with one more part on
  every block: a sign on a post at the back of the transaction top reading PRESCRIPTION DROP OFF
  or PICK UP to both sides. Cabinets join by line and finish, so the drop-off and pick-up
  counters of a finish make one counter in any order. The shelf wall behind (`BlockMarketRun`,
  group `pharmacy_shelf`) is three open shelves of generic cartons and pill bottles (the
  `pharmacy` and `pharmacy_b` stock) on a back panel, with no top or base of its own, so blocks
  stacked on one another are one wall; its end panels come only at a run's ends. The PHARMACY
  and PRIVATE CONSULTATION signs hang from the ceiling like the aisle sign, printed both sides.
  **No red cross anywhere:** it is a protected emblem, so the pharmacy has an Rx and a
  two-tone capsule.
- **Tobacco and lottery.** The tobacco case (`BlockMarketRun`, group `tobacco_case`, black and
  walnut, 9 slots, translucent) hangs on the wall behind the register: its model fills the back
  seven pixels of its block, three rows of plain packs in colour bands behind two sliding
  smoked-glass doors. WE CHECK ID, 21 AND OVER is on the frieze of the run's end on the
  shopper's left. The lottery is an invented one, LUCKY CITY LOTTERY: the ticket dispenser (a
  translucent `BlockCounterPiece`) shows four scratch games (a seven, a clover, a star, a coin)
  hanging from their slots behind a clear front, and the terminal has a play-slip reader, the
  clerk's screen tilted towards the back and a jackpot display on a post; it prints (the
  copier's sound, faster) on click. Both rest on counters.
- **Outdoor merchandisers.** The ice merchandiser (`BlockMarketRun`, group
  `ice_merchandiser`, white and blue, 27 slots behind the refrigerator's door sounds) is an
  insulated chest 1.25 m high whose door reads CITY ICE (invented). It is not lit: real ones
  mostly are not, and a lit one would need the display cases' `LIT` and `POWERED`. The propane
  exchange cage (group `propane_cage`, cutout) holds two tiers of cylinders, each an octagon
  with its collar and valve, behind grey mesh; its mullions are drawn half in each block, as the
  reach-in's are, so a run of cages has a door each. The firewood rack (group `firewood_rack`)
  holds eight wrapped bundles on a black steel rack.
- **Kiosks** (`BlockMarketTall`, two blocks, the upper half giving light 7): a coin counting
  kiosk with a coin tray on a ledge, a photo printing kiosk with a tilted touchscreen over its
  print tray, and a movie rental kiosk with four invented films' posters and a disc slot. None
  wears a brand, and none looks like one: generic headers (COIN COUNTER, PHOTO PRINTS, MOVIES),
  colours chosen away from the real kiosks'. CSM has no currency item, so the coin counter
  counts nothing; the kiosks beep or print on click and are otherwise decorative.
- **Pricing** (`MarketFabricatorRules` and its mirror): the pharmacy counters as the service
  desk with a sign blank, the shelf wall as a gondola, the signs a sign blank each, the tobacco
  case timber and glass, the dispenser glass and paper, the terminal electronics and paper, the
  ice chest the island freezer without its glass, the cage and the rack iron (the rack with two
  logs), the kiosks sheet steel with a control board and LEDs.

The traps:

- **A sign is a band of a shared sheet, and its panel must be 4:1.** Every sign of these
  families (twelve of them) is one of the four bands, 64 x 16 texels, of the three 64 px
  sheets in `SIGNS` (`store_signs_a` to `_c`), picked by the model's UV. A panel of any other
  shape stretches its letters; `sign_panel` and `sign_print` take the band's number, so a new
  sign takes a free band or a new sheet, never a re-order of the existing bands, which would
  change the models that point at them.
- **A run's left and right are the block's own.** `LEFT` and `RIGHT` are seen from behind the
  block, looking out of its front, so the shopper sees them swapped: the tobacco case's sign is
  on its `right` end part to stand on the shopper's left. (The cart corral's "left end" is on
  the shopper's right.)
- **A walnut pharmacy counter joins the office's walnut reception desk.** Cabinets join by
  line and finish, and both are `RECEPTION` in walnut. They share the parts, so the join is
  clean; the teal counters join only one another.
- **A piece on a pharmacy counter meets its sign post.** `SurfaceRest` puts a counter piece on
  the transaction top (the block above, at `FLOOR`), where the post stands at z 4.1 to 4.9.
- **The cage's colours are chosen for contrast.** White cylinders behind white mesh vanished
  in the first preview; the cage is grey and its mesh (`mesh_cage`) a wire every 12 cm, so the
  cylinders read through it. The back row has no collars, being hidden by the front one.
- **Atlas.** The families added 25 sprites, 16 to 64 px (the three sign sheets are the only
  64 px ones), and reuse the store's plastics, steels, glass, woods and price strip; the block
  atlas went from 95.2% to 95.3% of 8192 x 4096.
- **Memory.** `/csm memstats`: the 18 blocks have 264 states, 99 model locations (one per
  multipart block, 28 for each counter piece's rests) and about 5,300 baked quads, 1.0 MB in
  all. The two heaviest are the lottery dispenser (1,232 quads over its seven rests) and the
  propane cage (1,167 quads: twelve cylinders of octagons); every other block is under 80 KB.

### Merchandising and fixtures

The things a store puts on its floor between the fixtures: end caps and pallets, signs and tags,
bins and seasonal tables, and a small apparel corner. Three new classes (the department sign,
its tile entity and the fitting room) and one new property (the gondola's tags); everything else
is on the classes the tab already had.

- **End caps** (`end_cap_*`, stocked the six ways the gondola is) close the end of a gondola run:
  placed against a run's end and facing out, a pegboard back against the run and deeper shelves
  (stock about 11 px deep against the gondola's 8) with the gondola's price strips, a base deck and a
  WEEKLY SPECIAL header on posts over the head of a stack. They are `BlockGondola`s drawn
  differently, so they join and stack with one another exactly as gondolas do (two side by side
  close an island's end as one end cap; a header stands over each), and a run turned their way
  joins them. They carry the tags below, a yellow price tag rather than the red one.
- **Shelf tags and talkers.** A click with an empty hand on a gondola or an end cap hangs red
  SALE tags (yellow price tags on an end cap) under its price strips, a second click adds a shelf
  talker (a starburst card on a clear arm standing square to the shelf, out into the aisle, so a
  shopper walking the aisle sees it), a third takes both off; a sneaking click steps back.
  `BlockGondola.TAGS` (`ShelfTags`: `none`, `sale`, `talker`) is stored in the two bits above the
  facing, and the multipart draws the tags on the shelf strip and on the deck strip or, stacked,
  the bottom strip. The cards, tags and the standing signs' cards share one 64 px sheet,
  `store_cards`, quartered.
- **Pallet stacks** (`pallet_stack_drinks`, `_water`, `_paper`, `BlockBathroomFixture` in the new
  `FixtureMaterial.WOOD`) are a timber pallet and a load in courses (printed cases, trays of
  bottles, bulk kitchen roll) under a stretch wrap's sheen. One model, retextured. Placed on one
  another they are a double-stacked pallet.
- **Bargain bins.** The wire bargain bin on castors and the cardboard SPECIAL BUY dump bin are
  heaped with odds and ends (`jumble`, one 32 px texture used for the heap and the four things
  poking out of it at 22.5 degrees).
- **Seasonal display tables** (`seasonal_table_summer`, `_harvest`, `_winter`, `BlockMarketRun`,
  group `seasonal_table`) are a skirted table and a riser of generic seasonal goods: beach balls,
  sun cream, pails and sandals; pumpkins, gourds, candles and cider; gifts, candles and tins. The
  season's sign stands over the run's end on the shopper's left. One model, retextured: the skirt,
  the goods' sheet and the sign's sheet.
- **The checkout's candy strip** (`checkout_candy_*`, grey and walnut) is a checkout counter of
  group `checkout`, so it joins the belt, scanner and bagging counters into one lane; its top is
  plain and its customer side carries a wire strip of gum, mints and bars on three ledges.
- **Department signs** (`department_sign_black`, `BlockDepartmentSign`) hang from the ceiling like
  the aisle sign but are two metres wide, reaching half a block past each side of their block:
  PRODUCE, BAKERY, DELI, MEAT & SEAFOOD, PHARMACY, FLORAL, DAIRY, FROZEN, CHECKOUT and CUSTOMER
  SERVICE, each in its colour with its pictograms, printed both sides. A click steps to the next
  department, a sneaking click back; the department is kept in a `TileEntityDepartmentSign` (the
  metadata holds the facing), read by `getActualState` as `DEPARTMENT` (`StoreDepartment`), and
  the blockstate picks the band's model and the sheet for it. PHARMACY is the pharmacy sign's own
  band.
- **Standing sale signs**: a SALE card (2 FOR 5.00) and a HOT DEAL card (9.99) in a frame on a
  floor stand, both sides. Every price in the tab is invented.
- **Apparel.** The rolling clothes rail (`BlockMarketRun`, group `clothing_rail`) is a top rail
  with garments along it and the castor frames only at a run's ends; the round rack is a ring of
  eight bars on crossed arms and a pole with a garment at the middle of each side. A garment is a
  plane square to its rail, cut out to its shape on its broad faces (eight garments on one 32 px
  sheet: a tee, a shirt, a dress, a jacket, jeans, a hoodie, a skirt and a blouse on hangers) and
  mapped to the garment's middle column on its edges, so its edge follows its length. The folded
  clothes table (group `apparel_table`, oak and white) holds stacks of folded tees on top and
  jeans below. The mannequin and the dress form are `BlockMarketTall`s: a figure 1.85 m tall on a
  rod, dressed in a top and trousers (gloss white or black), and a padded torso on a pole with a
  turned cap.
- **Fitting rooms** (`BlockFittingRoom`, a `BlockCloset`, white with a charcoal curtain or walnut
  with a red one) are booths two blocks tall with a mirror, a bench and hooks, joining into a row:
  each booth draws the partition on its own left, so a row has one wall between each pair, and
  the booth at the row's right end also draws its right-hand wall and the FITTING ROOMS sign on
  the fascia. A click on either half draws the curtain across or back to its side, with a
  synthesised rattle of rings (`CURTAIN_SLIDE`); `OPEN` is stored in both halves, in the bit
  above the half. Only the walls and a drawn curtain collide, so an open booth can be walked into.
- **Pricing** (`MarketFabricatorRules` and its mirror): end caps as the gondola with a sign blank,
  pallets as planks and paper, the wire bin as iron and a sign blank, the dump bin as paper, the
  seasonal table as timber and cloth with a sign blank, the candy strip as the belt counter less
  its motor with an iron rack, department signs as two sign blanks, the standing signs as one on
  a pole, rails and racks as iron and cloth, tables as timber and cloth, the mannequin as sheet
  steel and cloth, the dress form as cloth and timber, the fitting room as timber, cloth and a
  mirror's glass.

The traps:

- **A retextured model fixes the column heights.** The seasonal tables are one model, so every
  season's sheet draws its four columns at the same heights (`SEASONAL_H`); a shorter drawing
  leaves its box's top face floating over the product. And a table is seen from above, so its
  goods have opaque tops: the bottle and can tops the shelves use (caps on a transparent sheet)
  read as a floating lattice there.
- **The sign's band is in the model, so a retexture cannot change it.** The three seasons' signs
  are band 3 of three sheets (`store_signs_d` to `_f`), so the one table model takes any of them
  by naming the sheet. The department sign needs a model per band and names the sheet in the
  department's variant; with PHARMACY reused there are seven sign sheets in all.
- **`TAGS` and the department are stored ordinals.** `ShelfTags` is in the gondola's metadata and
  `StoreDepartment` in the tile entity: append, never re-order.
- **Only an empty hand hangs tags.** A gondola's click with anything in the hand is left alone,
  or a block could not be placed against the shelving.
- **The tags' particle is the shelf's.** A part's particle texture is added to every finish that
  lacks it; giving the tag parts the cards sheet as their particle rewrote all 49 existing gondola
  finish models for nothing.
- **A pallet's boards have no bottom faces.** A pallet stacked on a load sits on its top; drawn,
  its boards' undersides would fight the load's top.
- **The candy strip is in the cutout layer.** Stock columns leave a transparent texel between
  products; in the solid layer, as the rest of the lane is, they would draw black.
- **A diagonal garment is a square one turned.** An element turns only about its own origin by
  22.5-degree steps, so the round rack's four diagonal garments are the north and south ones
  turned 45 degrees about the pole.
- **The department sign reaches past its block.** Its panel is -8 to 24 px, drawn from one block;
  its box (the part that can be clicked) is only the middle block's.
- **Atlas.** The families added 25 sprites, 16 to 64 px (four sign sheets and the cards sheet are
  the 64 px ones), and reuse the product sheets, the pallets' slats, the residential fabrics and
  woods, the store's plastics and steels; the block atlas went from 95.3% to 95.4% of
  8192 x 4096.
- **Memory.** `/csm memstats`: the 29 new blocks have 1,512 states, 120 model locations (one per
  multipart block, 40 for the department sign's facings and departments, eight for each tall
  piece) and about 6,800 baked quads, 1.7 MB in all; the heaviest are the end caps (the canned
  one 151 KB) and the white fitting room (148 KB). `tags` took each gondola from 64 states to
  192, still one model location each. The department is the one property memstats calls
  texture-only (bands on different sheets share a model), as the aisle sign's number is.

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
| `csm:toilet_flush` | `TOILET_FLUSH` | toilet (cistern click), urinal | synthesised |
| `csm:shower_spray` | `SHOWER_SPRAY` | shower and shower head while on (a 2 s loop replayed every 40 ticks), the bath's tap | synthesised |
| `csm:washing_machine_run`, `csm:dryer_tumble` | `WASHING_MACHINE_RUN`, `DRYER_TUMBLE` | while running | synthesised |
| `csm:iron_steam` | `IRON_STEAM` | steam iron click | synthesised |
| `csm:printer_run` | `PRINTER_RUN` | the copier while it copies (a 2 s loop every 40 ticks), the fax's click | synthesised |
| `csm:doorbell_chime` | `DOORBELL_CHIME` | doorbell | synthesised: two struck chime bars, ding and dong |
| `csm:fireplace_crackle` | `FIREPLACE_CRACKLE` | a lit fireplace, now and then from its display tick, quietly | synthesised |
| `csm:grill_sizzle` | `GRILL_SIZZLE` | the gas and charcoal grills while they cook (a 2 s loop every 40 ticks) | synthesised |
| `csm:trampoline_boing` | `TRAMPOLINE_BOING` | a fall of more than a block onto the trampoline, the bounce castle or the diving board, quietly | synthesised |
| `csm:locker_door_open`, `csm:locker_door_close` | `LOCKER_DOOR_OPEN`, `LOCKER_DOOR_CLOSE` | lockers | synthesised (replacing the unused sounds of unknown origin the first version shipped under these names) |
| `csm:verifone_mx915` | `VERIFONE_MX915` | the Verifone MX915 and the card terminal on its stand (click); the Transit module's fare vending machine, by name | synthesised: two piezo beeps, the second higher (replacing a recording of unknown origin that came with the terminal from Technology) |
| `csm:scanner_beep` | `SCANNER_BEEP` | the checkout scanner counter and the self-checkout (click) | synthesised |
| `csm:register_drawer` | `REGISTER_DRAWER` | the POS terminal and the cash register opening | synthesised: key, bell, drawer run and stop |
| `csm:curtain_slide` | `CURTAIN_SLIDE` | the fitting room's curtain drawn or opened (click) | synthesised: rings rattling along the rod over the cloth's swish |

The xylophone, water dispensers/bubblers and the kitchen sink use only vanilla sounds (filling an
appliance's tank with a bucket or bottle does too)
(`SoundEvents.BLOCK_NOTE_XYLOPHONE`, `SoundEvents.ITEM_BOTTLE_FILL`, `ITEM_BUCKET_FILL`).
