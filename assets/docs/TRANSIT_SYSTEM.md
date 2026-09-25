# The Transit Module

CSM: Transit (`csm_transit`, tree `modules/transit`, Java package `transit`) is the optional module
for public transit. Like every module it pins Core to its own exact version and registers nothing
itself: its creative tab, **Transit** (`tabtransit`, `@CsmTab.Load(order = 27)`), is found by
Core's tab scan, and every block and item keeps the `csm:` namespace.

The module holds the working fare system, which it took over from Technology, and the bus stops.
What it is to grow into is at the end of this page.

| Block or item | Registry name | Class |
|---|---|---|
| Fare Vending Machine | `csm:farevend` | `transit.fare.BlockFareVendingMachine` |
| Fare Gate | `csm:fare_gate` | `transit.fare.BlockFareGate` |
| Fare Gate (ADA, 2-Wide) | `csm:fare_gate_ada_2` | `transit.fare.BlockFareGateAda2` |
| Fare Gate (ADA, 3-Wide) | `csm:fare_gate_ada_3` | `transit.fare.BlockFareGateAda3` |
| Fare Ticket (item) | `csm:fareticket` | `transit.fare.ItemFareTicket` |
| Transit Card (item) | `csm:transitcard` | `transit.fare.ItemTransitCard` |
| Bus Stop Pole (round and square; galvanized, teal, navy, green, red) | `csm:bus_stop_pole_<style>` | `transit.stop.BlockBusStopPole` |
| Bus Stop Flag (CITYLINE, RIVERWAY, VERDANT, EMBERLINE) | `csm:bus_stop_flag_<agency>` | `transit.stop.BlockBusStopFlag` |
| Bus Stop Timetable Case | `csm:bus_stop_timetable_case` | `transit.stop.BlockBusStopFitting` |
| Bus Stop Route Map Case | `csm:bus_stop_route_map_case` | `transit.stop.BlockBusStopFitting` |
| Bus Arrival Display | `csm:bus_stop_arrival_display` | `transit.stop.BlockBusArrivalDisplay` |
| Bus Stop Curb Plaque | `csm:bus_stop_curb_plaque` | `transit.stop.BlockBusStopPlaque` |

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
block's chime when that module is not installed. Transit depends on nothing but Core.

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

### A stop is a stack

A stop is a column of `AbstractBlockBusStopStack` blocks, one block each: `BlockBusStopPole` is a
length of pole, and `BlockBusStopFitting` is a length of pole with something clamped to it (the
timetable and route map cases, and the base of the flag and the arrival display). A player builds
a stop the way a real one goes up: a pole or two, then the fittings stacked on top, the flag last.
A typical stop is pole, timetable case, arrival display, flag: four blocks, the flag at 3 to 4 m.

Each block draws its length of pole from three models per pole style: the shaft (no ends), the cap
(a sleeve and a dome, drawn only when nothing of the stack is above, `cap`) and the base (a flange
with four bolts and a collar, only when nothing is below, `base`). A fitting draws the shaft in the
style of the nearest pole below it (or above, for a fitting at the bottom), `pole`, so one timetable
case serves every pole. All three are actual state read by `BusStopStack`; the metadata holds only
the facing. The blockstates are multipart: the pole parts by `pole`, `cap` and `base`, the fitting
by `facing`. Shafts are drawn without end faces, so stacked lengths meet with no seam to fight
over, and the cap and base are wider than the shaft, so none of their faces is coplanar with it.

The pole styles are `BusStopPoleStyle`'s constants. The generator reads them from the Java and
stops if its own list differs, since the blockstates name each style's models.

**Settling.** The whole stack settles onto the surface under its bottom block, by that block's
`RoadSurfaceHeight` offset, and every block of it moves by the same amount (`getOffset`, the
bounding box and both renderers), or the pole would come apart at the first joint. The blocks are
`ICsmRoadSurfaceAware`, so nothing settles onto a stop.

### Flags and route plates

Four invented agencies, each with its own flag layout so they read as four agencies and not one in
four colours: **CITYLINE** (teal and yellow, the fare machine's livery), **RIVERWAY** (navy and
orange), **VERDANT** (green and white) and **EMBERLINE** (red and graphite). A flag stands out
sideways from the pole like a real stop flag and is printed on both faces (the south face uses the
same art uv, which reads the right way round from behind). Each carries the bus pictogram, BUS
STOP, the agency's name and the accessibility symbol, all drawn by the generator in its pixel font
and pixel art.

Under the flag hang up to three route plates. `TileEntityBusStopFlag` keeps their numbers (0 for
no plate, 1 to 99; saved as a three-byte array under `r`). Clicking a plate steps its number up and
a sneaking click steps it down, the aisle sign's pattern; a click on the flag itself steps the top
plate. The plate is chosen from the hit's height less the settling, split halfway between plate
middles, and the action bar says which plate now shows what. Which plates are there is actual state
(`route1` to `route3`), so the plates are baked; the numbers are drawn by
`TileEntityBusStopFlagRenderer`, white, on both faces, each number compiled once into a display
list shared by every flag (`CsmSharedDisplayLists`, keyed on the number) and replayed under each
plate's transform. The atlas, colour and depth mask are set outside the lists; the numbers take
the block's own light, since they are printed, not lit. A new number rebuilds the chunk section
only when a plate appears or goes (`getBakedModelKey` is the mask of plates).

The plate middles, the plate's x middle and its two face depths are constants in
`BlockBusStopFlag` (`PLATE_MIDDLE_Y` and the rest) that must match `BULLET_TOPS`, `BULLET_HT`,
`PX0`/`PX1` and `BZ0`/`BZ1` in the generator.

### Cases, the arrival display and the plaque

The **timetable case** and **route map case** are poster cases clamped to the front of the pole
with two bands: a shallow graphite box whose front is the poster, a frame standing proud of it,
and a faint glass sheen printed into the poster. The timetable is invented and the map a generic
diagram (a river, a park, three coloured lines, an interchange and a "you are here" dot) with no
place names.

The **arrival display** (`BlockBusArrivalDisplay`, `TileEntityBusArrivalDisplay`,
`TileEntityBusArrivalDisplayRenderer`) is a small LED panel under a hood. Its housing and dark
screen are baked; the renderer draws three amber dot-matrix lines a page, "12 DOWNTOWN 3 MIN",
fullbright, turning the page every 5 seconds. It lists the routes on its own stop's flag (the
nearest flag up or down the stack), or routes 12 and 40 on a stop with none. Each route's headway
(6 to 16 minutes) and destination are fixed by its number, from a list of generic destinations
(DOWNTOWN, HARBOR, CITY HALL and so on), so a route goes to the same place everywhere; its phase
comes from the world clock and the stop's position, so neighbouring stops differ while every player
at one stop sees the same countdown, a minute each real minute. The next two buses of each route
are listed, soonest first; under a minute reads DUE. The route numbers, destinations and readings
are one shared display list each (99, 16 and 33 at most), already laid out in their column, so a
line is three list calls. The display saves nothing: the routes, the facing and the settling are
looked up once a second (`AbstractTileEntityBusStopFitting.refreshView`), not every frame.

The **curb plaque** (`BlockBusStopPlaque`) is a cast bronze plate reading BUS STOP to the player
who placed it, settling onto the surface below like the Streetscape fixtures. Its texture is
stored turned 180 degrees, because the top face shows it that way to the placer. A plate and not
paint: road markings belong to the external road mod.

A bus stop bench or bin is Parks'; Transit adds none.

### Prices

`TransitFabricatorRules` prices a stop by what it is made of: a pole length is a pole section; a
flag a sign blank and a fastener kit; a case a sign blank and sheet metal; the arrival display an
LED module, a control board and sheet metal; the plaque sheet metal. `audit_fabricator_costs.py`
mirrors the branches.

## Where the module is going

Transit is planned to grow, in order: glass-and-steel bus shelters that join along their length,
rail and subway platforms (platform edges with tactile warning strips, tactile paving, platform
furniture), stations (a subway entrance headhouse built to size, ticket validators that use the
fare code, station wayfinding), and working departure boards configured through a screen, drawn by
a baked renderer, with announcements through Text to Speech only when that module is installed.

Two rules hold throughout: every agency, livery and route bullet is invented, never a real transit
brand; and an advertising panel in a shelter is Signage's board, set into the shelter by the
player, since a module may reference only Core.
