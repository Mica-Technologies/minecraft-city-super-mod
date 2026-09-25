# The Transit Module

CSM: Transit (`csm_transit`, tree `modules/transit`, Java package `transit`) is the optional module
for public transit. Like every module it pins Core to its own exact version and registers nothing
itself. It also **requires Roads & Traffic** (`required-after:csm_roads`), because its bus stop
signs are road signs (below); like Text to Speech on Technology, that is the one other module it
may name. Its creative tab, **Transit** (`tabtransit`, `@CsmTab.Load(order = 27)`), is found by
Core's tab scan, and every block and item keeps the `csm:` namespace.

The module holds the working fare system, which it took over from Technology, the bus stops and
shelters, the station and platform fit-out, made to complement the stations of RCMC, the
author's train mod, and an airport terminal's pieces. What it is to grow into is at the end of
this page.

| Block or item | Registry name | Class |
|---|---|---|
| Fare Vending Machine | `csm:farevend` | `transit.fare.BlockFareVendingMachine` |
| Fare Gate | `csm:fare_gate` | `transit.fare.BlockFareGate` |
| Fare Gate (ADA, 2-Wide) | `csm:fare_gate_ada_2` | `transit.fare.BlockFareGateAda2` |
| Fare Gate (ADA, 3-Wide) | `csm:fare_gate_ada_3` | `transit.fare.BlockFareGateAda3` |
| Fare Ticket (item) | `csm:fareticket` | `transit.fare.ItemFareTicket` |
| Transit Card (item) | `csm:transitcard` | `transit.fare.ItemTransitCard` |
| Bus Stop Flag (CITYLINE, RIVERWAY, VERDANT, EMBERLINE) | `csm:bus_stop_flag_<agency>` | `transit.stop.BlockBusStopFlag` |
| Bus Stop Timetable Case | `csm:bus_stop_timetable_case` | Roads' `trafficsigns.BlockTrafficSign` |
| Bus Stop Route Map Case | `csm:bus_stop_route_map_case` | Roads' `trafficsigns.BlockTrafficSign` |
| Bus Arrival Display | `csm:bus_stop_arrival_display` | `transit.stop.BlockBusArrivalDisplay` |
| Bus Stop Curb Plaque | `csm:bus_stop_curb_plaque` | `transit.stop.BlockBusStopPlaque` |
| Bus Shelter (Glass, Cantilever, Flat Roof; four agencies each) | `csm:bus_shelter_<style>_<agency>` | `transit.shelter.BlockBusShelter` |
| Station and platform fit-out (29 blocks) | see Station and platform fit-out, below | `transit.platform` |
| Airport terminal pieces (25 blocks) and the Boarding Pass (item) | see Airports, below | `transit.airport` |

---

## The fare gates

A fare gate is one block with a tile entity (`TileEntityFareGate`, `csm:tileentityfaregate`). The
two ADA gates are the same class with a wider lane: the model, the collision box and the exit
sensing box reach past the placed cell sideways (the iMac's trick), rather than placing a
multi-block. Its item (`ItemBlockFareGate`) places the gate one cell above the block aimed at, so
the gate's cell sits at chest height, where a player looking straight ahead clicks it, and the
cabinet is drawn down into the floor-level cell.

**Direction.** `FACING` points at the gate's outside, the side the player placed it from.

- **Entering** takes fare media, from the outside. Right-clicking with a Fare Ticket uses it up;
  with a Transit Card, one trip comes off the card's balance, and a card with none left is
  refused with a low note. Either opens the gate as `OPEN_ENTRY` (green arrow).
- **Leaving** is free. Every five ticks the tile entity looks for players in the cells behind the
  gate, and one who has just arrived there opens it as `OPEN_EXIT`, which shows the outside a red
  X, so nobody tries to come in against the flow. A player has to leave that box before they can
  open the gate again.

An open gate closes itself after 60 ticks (3 s). The `state` property is a `GateState`: `CLOSED`,
`OPEN_ENTRY`, `OPEN_EXIT`, or `CLOSED_LOCKED` (held shut by an operator, shown with the red X). An
open gate gives off a little light.

**Operator modes.** Sneak and right-click with an empty hand to open the gate's operator screen
(`FareGateConfigGui`, GUI id 17): **Normal**, **Always Open** (held in `OPEN_ENTRY`, no fare
needed) or **Always Closed (Maintenance)** (held in `CLOSED_LOCKED`). The choice goes to the
server as a `FareGateOpModePacket`, which the server takes only from a player within six blocks
of the gate, and is saved in the tile entity as `opMode` (the enum's ordinal).

## The fare vending machine and the fare media

The machine itself is drawn by `gen_transit_fare_vending.py`: a free-standing ticket machine two
blocks tall in the invented CITYLINE livery (teal, a yellow accent), with a lit TICKETS header, a
hooded touchscreen, keypad and tap pad, card, cash and coin slots and a ticket tray in relief. It is
one block whose model reaches into the space above (so every machine already placed keeps its
metadata); the model turns ambient occlusion off because that upper half was being shaded by the
lower block's neighbours, and the block gives light 6 for the screen and header. Its bounding box
follows the cabinet. The GUI is unchanged.

Right-clicking the vending machine opens `FareVendingGui` (GUI id 16). It sells, for emeralds
(`FareVendingPurchase`): a single-use ticket for one emerald; a new card with 1, 2, 5, 10 or 25
trips, at one emerald a trip; and reloads of +1, +5 or +25 trips onto the card in the player's
main hand. The choice goes to the server as a `FareVendingPurchasePacket`; the handler checks the
player is within six blocks of a real vending machine, that they can pay, and for a reload that
they hold a card, builds the item before taking the emeralds, and drops the item at the player's
feet if their inventory is full.

A purchase plays the checkout card terminal's approval beep, `csm:verifone_mx915`. That sound
belongs to the Furniture & Novelties module, so the handler looks it up by name and plays a note
block's chime when that module is not installed. Transit requires Roads, and nothing else but
Core.

The **Fare Ticket** is a plain single-use item. The **Transit Card** stacks to one, because every
card carries its own balance (`trips` in its NBT) and its tooltip shows it.

## Moved from Technology, nothing renamed

The fare system shipped in Technology until this module existed. The move kept everything a world
or a server depends on:

- the registry names of all four blocks and both items, and the tile entity's name, so placed
  gates and machines, their tile entity data and the tickets and cards in inventories load as they
  were;
- the GUI ids, 16 and 17 (GUI ids are global across the mod), now answered by
  `TransitGuiProvider`;
- every asset path. The blockstates, models and textures moved into the Transit tree with
  `git mv` but still live under `models/block/technology/shared_models/` and
  `textures/blocks/technology/`, because those paths are what the JSON names. Do not "tidy" them.

What did change: the classes' package (`technology` to `transit.fare`), the creative tab
(Technology to Transit, so these six entries left the Technology tab and appear in the new tab
instead), and the network channel. The two fare packets are registered on Transit's own channel
(`csm_transit`), and Technology's channel keeps its first two packets with their discriminators.

The Fabricator prices the Transit tab through `TransitFabricatorRules`, which gives the fare
equipment the price it had in Technology: a control board, sheet metal and a wiring harness.
`audit_fabricator_costs.py` mirrors that rule.

The textures come from `dev-env-utils/generate_fare_gate_textures.py` and
`generate_fare_item_textures.py`, which find the Transit tree through `csm_layout.owner_of`.

## Bus stops

Everything on a stop is drawn by `gen_transit_stops.py` and lives in `transit.stop`.

### A stop is a signed post

A bus stop is built on the road sign system, not beside it. The flag, the arrival display and the
two poster cases are **road signs**: subclasses of Roads' `BlockTrafficSign` (and so of
`AbstractBlockSign`), each one block of Roads' sign post with its piece on the front of the post,
exactly as every road sign's model is a length of post with a plate on it. A player builds a stop
the way a signed post goes up:

1. **Sign posts** (`signpost`, in Roads' Road Signs tab) from the ground, one or two.
2. **The poster cases and the arrival display**, stacked on the posts in any order.
3. **The flag** on top. It takes the facing of the sign below it, as every road sign does, so only
   the bottom post needs turning.

A typical stop is post, timetable case, arrival display, flag: four blocks, the flag's top at about
4.3 m. Because every piece is a road sign, the sign system gives it everything a road sign has, with
no code of Transit's own:

- **Eight facings**, and the facing passed up from the sign or post below.
- **The extension post** (`downward`) onto a slab or down through a guardrail.
- **Setback** (`shift=setback`): in front of a signal arm or hung from a span wire, the piece and
  its post move 12.5 back, into line with the arm's hardware, like any sign on a signal pole.
- **Back to back** (`shift=backtoback`): two flags (or a flag and any road sign) facing opposite
  ways, one on the post and one hanging in the block behind it, share the one post: the hanging one
  is drawn on the far side of the post. `AbstractBlockSign.getShouldBackToBack` picks which moves.
- **Span wires**: a flag hangs from a span like any sign.

Nothing settles onto the road any more: a road sign stands where its post is, and a slab or a
guardrail below is what the extension post is for.

### The models, and the rules they follow

Each piece has the three shift models every road sign has, written by the generator from one
drawing: `sign_<piece>` as drawn (the piece on the front of the post, the post at z 0.5 to 3.5),
`_setback` (all of it 12.5 back) and `_back_to_back` (the piece alone, no post, 28.3 back). The
post is the same five bars every road sign's model carries, so a stop's pieces and the sign posts
between them read as one post. The blockstates are Forge format, like the road signs': facing by
`transform`, `downward` by the `sign_pole` submodel, `shift` by model.

Two differences from Roads' own signs, both deliberate:

- **Back to back is 28.3, not 28.5.** The road signs' convention puts a shifted plate's front at
  28.5, level with the end of the partner's post, and paints the art on a sliver a hundredth of a
  unit in front of it. Here the whole piece stops 0.2 short, so its face stands the depth test's
  gap clear of the post end and needs no sliver. It still pairs with every road sign: the
  partner's post is where it always is.
- **The post's end caps each paint one texel.** The five bars' ends are coplanar by design, and
  `model_depth.separate` would otherwise stair-step them past the block into the next post.

`SignShiftModelTest` and `SignFaceDepthTest` (Roads' tests, run in the one suite) hold these models
to the same rules as Roads' own: they take a blockstate whose default model is under
`csm:transit/stops/sign_` as a road sign, as well as one under `csm:trafficsigns/`.

### Flags and route plates

Four invented agencies, each with its own flag layout so they read as four agencies and not one in
four colours: **CITYLINE** (teal and yellow, the fare machine's livery), **RIVERWAY** (navy and
orange), **VERDANT** (green and white) and **EMBERLINE** (red and graphite). Each carries the bus
pictogram, BUS STOP, the agency's name and the accessibility symbol, all drawn by the generator in
its pixel font and pixel art. The flag is a 10 x 13 plate printed on both faces (the back uses the
same art uv, which reads the right way round from behind); it stands on the top of its block and
above it, as the road signs' tall plates do, so the route plates can hang under it inside the
block, where a click reaches them.

Under the flag hang up to three route plates, 10 x 2.5, each in the agency's colour with a white
rim and a small bus. `TileEntityBusStopFlag` keeps their numbers (0 for no plate, 1 to 99; saved as
a three-byte array under `r`). Clicking a plate steps its number up and a sneaking click steps it
down, the aisle sign's pattern; a click on the flag itself steps the top plate. The plate is chosen
from the hit's height, split halfway between plate middles, and the action bar says which plate now
shows what.

Which plates are there is actual state (`route1` to `route3`), and it does not pick a model: the
four agencies share one flag model per shift, whose plate faces paint texture slots `p1` to `p3`,
and the blockstate fills a slot with the agency's plate while the plate is there and with a clear
texture while it is not. So the plates are baked in all three shift models without a model for
every combination. A new number rebuilds the chunk section only when a plate appears or goes
(`getBakedModelKey` is the mask of plates).

The numbers are drawn by `TileEntityBusStopFlagRenderer`, white, on both faces, each number compiled
once into a display list shared by every flag (`CsmSharedDisplayLists`, keyed on the number) and
replayed under each plate's transform. The renderer works in the route marker sign's frame (the
facing's turn plus a half turn, the reader in front with +x to the right), at the depth of the
shift the sign system has put the flag in (`BusStopSigns.SHIFT_Z`), and multiplies the white by
the plate's diffuse shade (0.8, or 0.6 facing due east or west), which a baked face carries and a
renderer does not. The facing and the shift are looked up once a second
(`AbstractTileEntityBusStopSign.refreshView`), not every frame.

The plate middles, the numbers' place on a plate and the plate faces' depth are constants in
`BlockBusStopFlag` (`PLATE_MIDDLE_Y` and the rest) that must match `PLATE_TOPS`, `PLATE_HT`,
`PX0`/`PX1` and `PZ0`/`PZ1` in the generator.

### Cases, the arrival display and the plaque

The **timetable case** and **route map case** are poster cases on the front of the post with two
bands round it: a shallow graphite box whose front is the poster, a frame standing proud of it,
and a faint glass sheen printed into the poster. The timetable is invented and the map a generic
diagram (a river, a park, three coloured lines, an interchange and a "you are here" dot) with no
place names. They are plain `BlockTrafficSign`s with their own models.

The **arrival display** (`BlockBusArrivalDisplay`, `TileEntityBusArrivalDisplay`,
`TileEntityBusArrivalDisplayRenderer`) is a small LED panel under a hood, clamped to the front of
the post. Its housing and dark screen are baked; the renderer draws three amber dot-matrix lines a
page, "12 DOWNTOWN 3 MIN", fullbright, turning the page every 5 seconds, on the screen at the depth
of the display's shift. It lists the routes on its own stop's flag -- the nearest bus stop flag up
or down its post, walking through the signs and posts of the column (`BusStopSigns.flagNear`) --
or routes 12 and 40 on a post with none. Each route's headway (6 to 16 minutes) and destination are
fixed by its number, from a list of generic destinations (DOWNTOWN, HARBOR, CITY HALL and so on),
so a route goes to the same place everywhere; its phase comes from the world clock and the stop's
position, so neighbouring stops differ while every player at one stop sees the same countdown, a
minute each real minute. The next two buses of each route are listed, soonest first; under a minute
reads DUE. The route numbers, destinations and readings are one shared display list each (99, 16
and 33 at most), already laid out in their column, so a line is three list calls. The display saves
nothing: the routes, the facing and the shift are looked up once a second, not every frame.

The **curb plaque** (`BlockBusStopPlaque`) is a cast bronze plate reading BUS STOP to the player
who placed it, settling onto the surface below like the Streetscape fixtures. Its texture is
stored turned 180 degrees, because the top face shows it that way to the placer. A plate and not
paint: road markings belong to the external road mod.

A bus stop bench or bin is Parks'; Transit adds none.

### Prices

`TransitFabricatorRules` prices a stop by what it is made of: a flag a sign blank and a fastener
kit; a case a sign blank and sheet metal; the arrival display an LED module, a control board and
sheet metal; the plaque sheet metal. The sign posts are Roads', priced by Roads.
`audit_fabricator_costs.py` mirrors the branches.

### Why there is no bus stop pole

The first bus stops had a pole family of their own: six pole blocks (round and square, galvanized
and painted) that the flag, cases and display stacked on, settling onto the road. They were
removed before any release, because a transit sign is a road sign: a stop built on Roads' sign
posts stands next to a signal arm, hangs from a span wire, pairs back to back with a street name
blade or a NO PARKING sign and matches every other post on the street, and a second pole family
could do none of that without copying the sign system. The registry names of the flags, cases and
display were kept.

## Shelters

Twelve blocks, `bus_shelter_<style>_<agency>`: three styles (`BusShelterStyle`) in the four
agencies' liveries, all one class, `BlockBusShelter`, and all drawn by `gen_transit_shelters.py`.

| Style | What it is | Layer |
|---|---|---|
| `glass` | glass back and end walls with a dotted frit band at eye level, steel posts, a roof with the agency's name on its fascia, a timber bench, and an empty frame at one end for an ad panel | translucent |
| `cantilever` | a canopy on one column a block, curving down at the back, open front and ends, a perforated steel screen and a bench | cutout |
| `flat` | a thin flat roof with a slim name fascia on slim corner posts, and a lean rail | cutout |

### One piece, two blocks

A shelter is placed as a door is: into its block and the one above (`upper`), both broken
together, only the lower half dropping the item. It faces the player who places it; its open
front is that side.

### Joining

Shelters of the same block facing the same way join both ways, from actual state only:

- **Along their length** (`left`, `right`: the same block continues on the sitter's left or
  right). End walls, end posts and the roof's end overhang are drawn only where the run stops; the
  back wall, bench and fascia run on through, and a post stands at every joint along the back. Any
  number in a row read as one shelter.
- **Front to back** (`ahead`, `behind`: the same block continues in front or behind). The back
  wall and bench are drawn only in the back row, the fascia and front posts only along the front,
  and the end walls run through, so two rows make a shelter two blocks deep (or three, or more).

Every part is written once in "shelter coordinates" (y from 0 to 32 over both halves) and cut at
the block line, so a post or a glass pane is one piece in the generator and two models in the
game. The collision boxes (`BusShelterStyle.collision`) are the walls, bench, rail, posts and roof
of each half, from the same numbers. The selection box is the whole block, so the end of a shelter
is easy to click when setting an ad panel against it.

### The roof light

A lens under the roof, lit when the shelter is placed (or matching the shelter it joins). It is
switched as the Residential lamps are: a click with an empty hand, or a change of redstone power at
either half -- on when power comes, off when it goes. The lower half remembers the power it last
saw (`powered`), the upper holds `lit`, each in the bit above `upper`. Either switches every upper
half of the shelter the block belongs to, up to 64 blocks, so one daylight sensor or switch lights
a long shelter. A lit upper half gives light 8. A click holding anything is left to the item, so
an ad panel (or any block) can be placed against a shelter.

### The ad panel slot

The glass shelter keeps the sitter's left end of its back row for an advertising panel: no glass,
but an empty steel frame -- two posts and a rail top and bottom -- set in 1.25 px from the block
edge, with the back wall, bench and roof stopping at it too. The panel itself is Signage's **Bus
Shelter Ad Panel** (`ad_shelter_panel`, `ADVERTISING_SYSTEM.md`), which the player sets against
that frame from outside the shelter: a 1 x 2 backlit lightbox in the next block, its back lip
reaching the pixel into this one that the shelter leaves clear, one ad facing along the pavement
and one facing into the shelter. Transit and Signage never refer to each other; they meet only in
the world, so a shelter without Signage installed simply has an empty frame.

### Liveries

The geometry is drawn once per style in CITYLINE's colours; every other agency's model of a part
that shows livery is a child model naming its own `frame` and `fascia` textures (`fascia_slim` on
the flat roof). The fascias carry the agency's name and the stripe its flag uses: CITYLINE teal
with yellow, RIVERWAY navy with orange end bars, VERDANT green with white lines, EMBERLINE red on a
graphite frame.

### Prices

`TransitFabricatorRules`: a glass shelter is a pole section, two sheet metal, four glass panes and
an LED module; the cantilever a pole section, two sheet metal and an LED module; the flat roof a
pole section, one sheet metal and an LED module. `audit_fabricator_costs.py` mirrors them.

## Station and platform fit-out

Twenty-nine blocks in `transit.platform`, all drawn by `gen_transit_platforms.py`, with three
sounds from `gen_transit_sounds.py`.

### Made to complement RCMC

The set is designed to fit out stations for **RCMC** (Rails & Coasters: Minecraft), the author's
own train mod, and never to repeat it. RCMC already has the station itself: its **Station
Platform** and **Platform Edge** blocks (decking laid at exactly a metro car's floor height, the
edge carrying its own tactile strip), the Station Line Map sign, the Arrival Board, the Station
Speaker, the operator panel and line desk, and spline track with catenary, switches and
signalling. So Transit has no platform or edge block, no speaker, no arrival board for platforms
(its own departure boards, to come, are for bus stops and station concourses), and nothing laid
along the track: RCMC's track is splines, not blocks, and a block-aligned third-rail cover or
buffer stop would clash with it. What is here is the fit-out an RCMC station lacks.

**Clearance.** A metro car in RCMC is 3.8 blocks wide (1.9 either side of the track's centre)
and its roof stands about four blocks above the platform surface. Keep furniture (benches, bins,
help points, validators, columns) at least a block back from the platform edge, hang signs and
clocks over the platform rather than the track, and keep a canopy over the platform: nothing here
checks, so a sign hung over the track will be driven through. The tactile paving is the one piece
meant for the edge itself: a one-pixel overlay, below any car.

| Block | Registry name | Class |
|---|---|---|
| Tactile Paving (Warning or Guidance; Yellow or Grey) | `csm:tactile_warning_yellow` and the other three | `BlockTactilePaving` |
| Platform Bench | `csm:platform_bench` | `BlockPlatformBench` |
| Platform Perch | `csm:platform_perch` | `BlockPlatformRun` |
| Help Point | `csm:platform_help_point` | `BlockPlatformHelpPoint` |
| Station Emergency Point | `csm:station_emergency_point` | `BlockPlatformHelpPoint` |
| CCTV Dome Camera, CCTV Camera (Wall Bracket) | `csm:platform_cctv_dome`, `csm:platform_cctv_camera` | `BlockPlatformFixture` |
| Platform Clock | `csm:platform_clock` | `BlockPlatformClock` |
| Platform Litter Bin (Clear Bag) | `csm:platform_litter_bin` | `BlockPlatformFixture` |
| Platform Number Sign | `csm:platform_number_sign` | `BlockPlatformNumberSign` |
| Hanging Sign (To Trains, Exit, Line Bullets) | `csm:platform_sign_to_trains`, `_exit`, `_lines` | `BlockPlatformFixture` |
| Gap Warning Sign | `csm:platform_gap_sign` | `BlockPlatformFixture` |
| Station Name Sign | `csm:station_name_sign` | `BlockStationNameSign` |
| Network Map Board | `csm:station_network_map` | `BlockPlatformFixture` |
| Station Wall Tile (White, and a Band in each agency's colour) | `csm:station_tile_white`, `csm:station_tile_band_<agency>` | `BlockStationTile` |
| Platform Column (Tiled, Steel) | `csm:platform_column_tile`, `csm:platform_column_steel` | `BlockPlatformColumn` |
| Platform Column (Number Band) | `csm:platform_column_number` | `BlockPlatformColumnNumber` |
| Platform Canopy | `csm:platform_canopy` | `BlockPlatformCanopy` |
| Ticket Validator | `csm:platform_validator` | `BlockPlatformValidator` |

### Tactile paving

One-pixel overlays laid on any solid floor, as vanilla carpet is, RCMC's platform decking
included: truncated warning domes and directional guidance bars, in yellow and in grey with
stainless studs. The class follows Building's floor finish, copied rather than shared (a module
names only Core and the modules it requires): it stores only `axis`, the way the player faced,
and comes up when its floor goes.

A floor of it shows no repeat. Each texture is drawn twice, every dome or bar a little different,
and the blockstate picks one of the two drawings and a turn per block position. The domes sit
centred in 2 px cells on a square grid, so a quarter turn leaves the grid where it was; the bars
run the way they were laid and turn only end for end, their segments on a 32-texel period that
divides the texture, so a run of blocks has no seam.

### Furniture

Every piece faces the player who places it; a wall piece has the wall behind it.

- **Bench** (`BlockPlatformBench`): two perforated steel seats a block on a beam on one
  pedestal, with armrests between and at the ends. Benches side by side facing the same way join
  (`left`, `right`, actual state): the beam runs through, an armrest stands at every joint, and
  the end armrests are drawn only at the bench's ends. A click sits the player in the nearer seat
  through Core's `EntityCsmSeat`, one person a seat. It is a platform bench, not Parks' timber
  park bench.
- **Perch** (`BlockPlatformRun`): a lean rail, a padded bar tilted forward over a stainless rail
  on one post a block, joining the same way, capped only at its ends.
- **Help point** (`BlockPlatformHelpPoint`): a column two blocks tall under a lit HELP POINT
  header (light 4), with a green information button over a speaker grille and a guarded red
  emergency button. A click plays `help_point_chime`, three struck bars rising, and says on the
  action bar who is answering; a click on the front below 12.6 px (`HELP_SPLIT_Y`) is the
  emergency button, and its chime is pitched lower. Both buttons are in the column's lower block
  on purpose: a click reaches only the block the player's aim passes through, so anything drawn
  in the space above (the header) cannot be clicked. The **emergency point** is a red wall cabinet
  (a lit EMERGENCY header, a phone handset, an extinguisher behind glass) whose every click is an
  emergency call. Nothing is sent anywhere: a help point is a place, not a service.
- **CCTV**: a dome on a short pipe from the ceiling, and a bullet camera on a wall bracket tilted
  down the platform. Decoration.
- **Clock** (`BlockPlatformClock`, `TileEntityPlatformClock`,
  `TileEntityPlatformClockRenderer`): a double-faced clock hung from the ceiling, its drum and
  dials baked, its hands the renderer's -- the world's time on both dials, four untextured quads
  a frame, the Residential wall clock's approach. The dials stand the same distance either side
  of the block's middle, so the back dial is the front one turned half round.
- **Litter bin**: the clear-bag bin stations use, a steel hoop on a post holding a see-through
  bag (translucent layer) under a lid ring.

### Signs

- **Platform number sign** (`BlockPlatformNumberSign`): PLATFORM over a big number, hung on two
  rods, printed on both faces. A click steps the number 1 to 20 and round, a sneaking click back;
  the action bar says the new number. The number is kept by `TileEntityPlatformSign` and read as
  actual state (`number`), and the blockstate swaps the face texture: the aisle sign's pattern,
  shared with the column band and the name sign through `PlatformSigns`.
- **Hanging wayfinding signs**: TO TRAINS and EXIT with an arrow, and a strip of line bullets
  (1, 4, 7 and E, in the four agencies' colours). The back face is read mirrored so it reads the
  right way round, and a sign with an arrow has its own back art with the arrow reversed, so from
  either side it points the same way in the world.
- **Gap warning**: a yellow wall sign, CAUTION, a figure stepping from the platform into the car,
  STEP OVER THE GAP. The wording is invented, as every phrase here is.
- **Station name sign** (`BlockStationNameSign`): a porcelain-enamel panel, white on navy, set on
  the wall. A click steps through ten invented names (Alder Park, Civic Square, Foundry Row,
  Harbor Lights, Kestrel Hill, Lantern Quay, Millstone, Orchard End, Saxton Cross, Willow Bend),
  kept in the same order in `BlockStationNameSign.NAMES` and the generator's `STATION_NAMES`.
- **Network map board**: a framed wall map of a generic network, four lines in the agencies'
  colours over a river and a park, interchanges, a "you are here" dot and a legend of bullets,
  with no place names: the city is whatever the player builds.

### Architecture

- **Station wall tile** (`BlockStationTile`): full blocks of glazed subway tile in running bond,
  plain white, or with a frieze course in CITYLINE teal, RIVERWAY orange, VERDANT green or
  EMBERLINE red between two dark liners, on the sides at the same height on every block, so a
  row is one unbroken band.
- **Columns** (`BlockPlatformColumn`): tiled square or painted steel octagon, one block of shaft
  at a time. Stacked, they read as one column: `up` and `down` (actual state) draw the plinth
  only at the foot and the capital only at the head. The **number band column**
  (`BlockPlatformColumnNumber`) is the tiled column with a navy band carrying PLATFORM and its
  number on all four faces, stepped by clicking like the number sign; its band is one model per
  number (children of band 1 naming their texture), picked by the multipart blockstate.
- **Canopy** (`BlockPlatformCanopy`): a steel roof one block at a time, set on columns (the deck
  lies at the foot of its block, so it rests on the column below). Canopies join on all four
  sides, the fascia drawn only round the outside (world sides, actual state); a light strip on the
  white soffit runs along `axis`, the way the player faced, and gives light 11.

### The ticket validator

`BlockPlatformValidator` is a tap post for a platform or a station without gates. Held to it, a
Fare Ticket is used up and a Transit Card gives up one trip, exactly as at a fare gate (the same
items and `ItemTransitCard.consumeTrip`); the screen shows a green tick and `validator_accept`
beeps, or a red cross and `validator_deny` sounds for a card with no trips or anything else, and
the action bar says why. The screen (`light`: 0 idle, 1 accepted, 2 refused, stored in the two
bits above the facing) goes back to idle after 30 ticks by a scheduled tick; lit, it gives light
5. Validating opens nothing: pairing a validator with a gate or RCMC's doors is the builder's
business.

### Prices

`TransitFabricatorRules`: tactile paving and wall tile a concrete mix; the help and emergency
points a control board, a sounder driver and sheet metal; CCTV an optical sensor and sheet metal;
the validator a control board, an LED module and sheet metal; the clock a control board and sheet
metal; the canopy two sheet metal and an LED module; a column a pole section and a concrete mix;
the signs a sign blank and a fastener kit; the bench, perch and bin two sheet metal and a
fastener kit. `audit_fabricator_costs.py` mirrors the branches.

## Airports

Twenty-five blocks and one item in `transit.airport`, all drawn by `gen_transit_airport.py`, with
one sound (`kiosk_print`) from `gen_transit_sounds.py`. Every airline, flight number and city is
invented, and every pictogram generic (a plane, a suitcase, a bus, a taxi).

### Terminal only

These are the pieces inside a terminal: check-in, the queue, security, the gate, the boards,
baggage claim and carts. There are no aircraft, and nothing airside: jet bridges, ground equipment
and airfield lights are for later. Two things a terminal needs come from other modules rather
than being drawn again:

- **The walk-through metal detector** is Life Safety's (`metal_detector`, in Emergency Services),
  which already works: it alarms and lights red on a player carrying metal. An airport one was
  built and cut before release, because it was the same block under the same name.
- **Duty-free and cafe fit-out** is Furnishings' Market & Store tab (shelving, coolers, the
  checkout, the counters).

Transit cannot name either module (it may reference only Core and Roads), so these meet only in
the world: an install without Life Safety simply has no detector.

| Block | Registry name | Class |
|---|---|---|
| Check-In Desk | `csm:airport_checkin_desk` | `BlockCheckinDesk` |
| Bag Drop Scale | `csm:airport_checkin_scale` | `BlockAirportCounter` |
| Gate Desk | `csm:airport_gate_desk` | `BlockAirportCounter` |
| Self Check-In Kiosk | `csm:airport_self_checkin_kiosk` | `BlockSelfCheckinKiosk` |
| Queue Stanchion (Black Belt, Blue Belt) | `csm:airport_queue_stanchion_black`, `_blue` | `BlockQueueStanchion` |
| Security X-Ray Scanner | `csm:airport_xray_scanner` | `BlockSecurityLine` |
| Security Roller Conveyor, Divesting Table | `csm:airport_security_roller`, `csm:airport_divest_table` | `BlockSecurityLine` |
| Security Trays (Stack), Security Tray (With Belongings) | `csm:airport_security_trays`, `csm:airport_security_tray_items` | `BlockSecurityTray` |
| Boarding Pass Scanner | `csm:airport_boarding_pass_scanner` | `BlockBoardingPassScanner` |
| Airport Seating (Black, Blue) | `csm:airport_seating_black`, `_blue` | `transit.platform.BlockPlatformBench` |
| Flight Information Board (Departures, Arrivals) | `csm:airport_flight_board_departures`, `_arrivals` | `BlockFlightBoard` |
| Baggage Carousel | `csm:airport_baggage_carousel` | `BlockBaggageCarousel` |
| Gate Sign | `csm:airport_gate_sign` | `BlockGateSign` |
| Airport Sign (Gates, Arrivals, Check-In, Baggage Claim, Ground Transport) | `csm:airport_sign_<name>` | `transit.platform.BlockPlatformFixture` |
| Luggage Cart | `csm:airport_luggage_cart` | `transit.platform.BlockPlatformFixture` |
| Luggage Cart Rack | `csm:airport_cart_rack` | `transit.platform.BlockPlatformRun` |
| Boarding Pass (item) | `csm:boarding_pass` | `ItemBoardingPass` |

Every model faces north as the platform fit-out's do: a counter's customer side, a sign's front
and a monitor's screen look north, a wall piece has its wall at z = 16, and every piece faces the
player who places it. The element helpers are `gen_transit_platforms.py`'s.

### The schedule

`FlightSchedule` is the whole airport's timetable, and it is a function of the world's clock and
nothing else: it saves nothing, sends nothing, and every board, kiosk and scanner in the world
agrees. Each list (departures, arrivals) has a flight every 20 minutes of the day, 72 a day, the
same flights every day as an airline timetable repeats. A flight is made from its slot's place in
the day by a hash: its airline (one of four: KESTREL AIR, LANTERN AIRWAYS, WILLOWJET, SAXTON
AIRLINES), number, city (sixteen invented, PORT VESTA to ST AUBREN), gate (A1 to D20), its time a
few minutes into its slot, and now and then a 30-minute delay (about one in nine) or a
cancellation (about one in thirty). A slot is counted from the world's first 6:00 as a `long`, so a
boarding pass names its flight by slot for as long as anyone keeps it.

The schedule runs on the **world's time of day**, the one the platform clock shows, so the boards
agree with the clocks and the sun. That makes an airport busy: a game hour is 50 real seconds, so
a flight leaves every 17 seconds and the rows roll up the board about that often. The statuses
follow from the minutes left: BOARDING under 45, FINAL CALL under 20, GATE CLOSED under 10, then
DEPARTED for ten minutes; arrivals are ON TIME, LANDING under 15, LANDED for twenty. A world with
the daylight cycle off stops the boards, as it stops the clocks.

### The flight information boards

`BlockFlightBoard` is a slim landscape monitor against the back of its block: on a wall, or on two
rods to the block above when there is no wall behind it (`hung`, actual state; not under another
board). Its bezel and the screen's texture (the DEPARTURES or ARRIVALS header and the row bands)
are baked. `TileEntityFlightBoardRenderer` draws the rest, a board being one quad and at most
sixteen list calls a frame:

- **The screen, lit.** The same texture again, a hair in front, fullbright, one quad from the
  block atlas, so a board glows at night like a screen rather than going dark with the room.
  Beyond the renderer's range (48 blocks) only the baked screen shows.
- **The text**, all display lists shared by every board (`CsmSharedDisplayLists`), laid out in
  their columns in the font's units when compiled: a row's white part (time, flight, city, gate)
  keyed by the flight's place in the day (at most 144 lists over both boards), each remark in its
  colour (nine), the column heads (two) and the clock in the header (one per minute of the day).
  Following "Display lists: one texture, no cached state", the lists hold geometry only; the font
  atlas, the colours, the depth mask and the fullbright lightmap are set outside them.
- **Pages.** Boards of the same kind facing the same way side by side and stacked are a bank, read
  like text: left to right along a row, then the next row down. Each board lists seven flights
  from its page, the number of boards to its reader's left plus the whole rows above it, so a bank
  of six lists the next 42 flights. `TileEntityFlightBoard` looks the page and facing up once a
  second, up to eight boards each way.

The screen's place and the bands (`SCREEN_*`, `HEADER_H`, `COLHEAD_H`, `ROW_PITCH`, `ROWS` and the
256 x 148 window) are in both the generator and the renderer: change one, change both.

### Check-in and the kiosk

- **Check-in desk** (`BlockCheckinDesk`): a customer ledge over a laminate front and a stainless
  kick plate, the agent's work top, monitor and bag-tag printer behind, and a lit airline panel on
  a post. A click with an empty hand steps the airline, a sneaking click back; the airline is kept
  in the two metadata bits above the facing, so the desk has no tile entity.
- **Bag drop scale** (`BlockAirportCounter`, family `checkin`): a low weighing belt between two
  stainless guards, running into a rubber curtain under a hood, its weight read out on the
  customer's side. Desks and scales facing the same way join into one counter (`left`, `right`):
  a desk draws its end panel only where the run stops, so desk, scale, desk reads as one.
- **Gate desk** (`BlockAirportCounter`, family `gate`): the same counter in charcoal with a lit
  strip and a boarding pass reader set in the ledge; gate desks join with each other only.
- **Self check-in kiosk** (`BlockSelfCheckinKiosk`): a pedestal with a tilted touchscreen over the
  card and passport readers and the pass slot, under a lit CHECK-IN header on a stem. A click
  prints a **Boarding Pass** for one of the next six departures at least 45 minutes off and not
  cancelled, with a seat, and plays `kiosk_print`. The screen and slots are in the lower block
  on purpose: a click reaches only the block the aim passes through, so the header above cannot be
  clicked (the help point's lesson).

The **Boarding Pass** (`ItemBoardingPass`) carries only its flight's slot, a seat and whether it
has been used (`s`, `seat`, `b`); the tooltip works the flight number, city, time and gate out of
`FlightSchedule`, so a pass always agrees with the boards. One taken from the creative tab has no
flight and says where passes come from.

### The queue

`BlockQueueStanchion`: a weighted base, a chrome post and a belt cassette. Its belt reaches out to
every stanchion beside it (world sides, actual state), whatever the colour of the other's belt, so
a row of stanchions is one barrier. A queue is rows of stanchions with a block of floor between
them, each row stopping a block short where the lane turns. The belts collide up to a fence's
height, so a queue cannot be jumped. The belt's faces map the whole texture top to bottom onto
their 1.8 units, which is how the blue belt keeps its two white edges. There are no diagonal
belts: a belt runs only to the four sides.

### Security

The X-ray unit, roller conveyor and divesting table (`BlockSecurityLine`) all have their tops at
12 sixteenths (`LANE_Y`), so a lane of them reads as one. Pieces side by side join whichever way
each faces, as long as the lane runs along the same axis, and the rollers and the table draw their
end plates only where the lane stops. The **X-ray unit** is a housing with a tunnel through it,
lead curtains at both mouths, an animated belt (`xray_belt`, four frames), a generic radiation
sticker and an operator's monitor on top; it collides to its housing's top (22 sixteenths).

**Security trays** (`BlockSecurityTray`), a stack or one tray with a laptop, a jacket and shoes,
stand on the floor, and set on a roller or the divesting table they drop onto its top (`low`,
actual state): the model and the box reach four sixteenths down into the lane's block, which is
where a player means them to be. Not onto the X-ray, whose housing stands over its belt
(`BlockSecurityLine.hasOpenTop`).

### The gate

- **Boarding pass scanner** (`BlockBoardingPassScanner`): the ticket validator's idea for a plane.
  Held to it, a Boarding Pass whose flight has neither departed nor been cancelled, and that has
  not been used, is marked boarded; the screen shows a green tick and the validator's accept tone
  sounds. Otherwise a red cross, the refusal tone, and the action bar says why. It does not check
  the gate: which gate a scanner stands at is the builder's business, and the schedule has no way
  to know.
- **Gate sign** (`BlockGateSign`): GATE over the letter (black on yellow) and number (yellow on
  charcoal), hung on rods, on both faces. A click steps the number 1 to 20, a sneaking click steps
  the letter A to D. The gate is one value 1 to 80 in the platform signs' `TileEntityPlatformSign`,
  read as two actual-state properties, `letter` and `number`, each of which swaps the texture of
  its own cell, so 24 textures make every gate. The back's cells are laid out mirrored, so it reads
  the same from behind.
- **Airport seating**: beam seating with upholstered seats, chrome armrests and a T leg, black or
  blue. It is `BlockPlatformBench` with its own model: the seats are in the platform bench's places
  (`SEATS`), so the bench's sitting code works unchanged, and seats side by side join the same way.

### Baggage claim

`BlockBaggageCarousel` is one block of carousel that joins on all four sides (world sides, actual
state) into a loop of any size; a 2 x N rectangle is the usual carousel. Which of its sides are
open picks its top from sixteen rules in the blockstate: one open side is a straight run of plates
(an animated texture, `carousel_belt`, eight frames) turned so the plates move **clockwise round
the loop** seen from above; two open sides that meet are a corner whose plates fan round the
inner corner; no open side is the middle island of a loop three or more wide; anything else (the
ends of a one-wide run, a lone block) is a round end plate. An open side gets the stainless skirt
and its rubber bumper. The carousel stores nothing, so it needs no facing: the plates' direction
follows from where the loop's edge is.

### Carts and wayfinding

- **Luggage cart**: a ribbed bed on four small wheels, a nose stop and a ladder frame up to a red
  grip. **Cart rack** (`BlockPlatformRun`): nested carts, three a block, between two blue guide
  rails, running along the player's left and right; a hoop at one end of a row and a post with
  the CARTS sign at the other.
- **Wayfinding signs**: hanging panels 24 units wide (reaching four past each side of their
  block), yellow capitals on charcoal with a black pictogram on a yellow square and an arrow:
  GATES (a plane taking off), ARRIVALS (a plane landing), CHECK-IN (a figure at a desk), BAGGAGE
  CLAIM (a case over a belt) and GROUND TRANSPORT (a bus over a taxi). Each has its own back art
  with the pictogram and the arrow swapped over, so from either side the arrow points the same way
  in the world; turning the sign round points it the other way.

### Traps

- **The airlines are named in two places.** `FlightSchedule.AIRLINES` and the generator's
  `AIRLINES` must list the same airlines in the same order: the desk's `airline` metadata indexes
  both.
- **A property value must be lower case.** The gate sign's letters are `a` to `d` in the
  blockstate, because a property's values are checked against `[a-z0-9_]+`; `BlockGateSign.Letter`
  returns its name in lower case for the same reason.
- **Do not add a second metal detector.** Life Safety has one; a Transit one clashed on its sound
  event name (`metal_detector_alarm`, which `CsmSoundsTest` holds to one module) before it was cut.
- **Keep a renderer's face clear of the model's.** The baked screen stands 0.4 proud of the
  bezel (`SCREEN_Z` 14.6) on purpose: at 0.1, `model_depth.separate` treated the two as a clash and
  moved the screen forward, past the depth the renderer draws at, and the whole lit screen and its
  text vanished behind the baked one with no error. A face a renderer draws on must be more than
  0.2 from any other face of its model.
- **The boards follow the world's time of day, not total time.** `/time set` jumps the boards with
  the clocks, which is right; total world time would put them out of step with every clock in the
  mod.

### Prices

`TransitFabricatorRules.airport`: the desks two sheet metal, a control board and a fastener kit;
the scale a sheet metal, a control board and a wiring harness; the kiosk a control board, an LED
module and sheet metal; the X-ray an enclosure shell, a control board, an optical sensor and a
wiring harness; the pass scanner a control board, an optical sensor and sheet metal; a board an LED
module, a control board and sheet metal; the carousel two sheet metal, a wiring harness and a
fastener kit; a stanchion a pole section and a fastener kit; trays and the cart a sheet metal and a
fastener kit; the signs a sign blank and a fastener kit; the rollers, divesting table, seating and
cart rack two sheet metal and a fastener kit. `audit_fabricator_costs.py` mirrors the branches.

## Where the module is going

Transit is planned to grow, in order: stations (a subway entrance headhouse built to size, and
more station wayfinding), an airport's airside pieces (a jet bridge, ground equipment, stand signs
and airfield lights switched by redstone), and working departure boards configured through a screen, drawn by a
baked renderer, with announcements through Text to Speech only when that module is installed --
for bus stops and station concourses, since RCMC's arrival boards already serve its platforms.

Two rules hold throughout: every agency, livery and route bullet is invented, never a real transit
brand; and an advertising panel in a shelter is Signage's board, set into the shelter by the
player, since a module may reference only Core and the modules it requires (for Transit, Roads).
