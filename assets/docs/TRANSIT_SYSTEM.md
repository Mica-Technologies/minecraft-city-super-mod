# The Transit Module

CSM: Transit (`csm_transit`, tree `modules/transit`, Java package `transit`) is the optional module
for public transit. Like every module it pins Core to its own exact version and registers nothing
itself. It also **requires Roads & Traffic** (`required-after:csm_roads`), because its bus stop
signs are road signs (below); like Text to Speech on Technology, that is the one other module it
may name. Its creative tab, its creative tab, **Transit** (`tabtransit`, `@CsmTab.Load(order = 27)`), is found by
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
| Bus Stop Flag (CITYLINE, RIVERWAY, VERDANT, EMBERLINE) | `csm:bus_stop_flag_<agency>` | `transit.stop.BlockBusStopFlag` |
| Bus Stop Timetable Case | `csm:bus_stop_timetable_case` | Roads' `trafficsigns.BlockTrafficSign` |
| Bus Stop Route Map Case | `csm:bus_stop_route_map_case` | Roads' `trafficsigns.BlockTrafficSign` |
| Bus Arrival Display | `csm:bus_stop_arrival_display` | `transit.stop.BlockBusArrivalDisplay` |
| Bus Stop Curb Plaque | `csm:bus_stop_curb_plaque` | `transit.stop.BlockBusStopPlaque` |
| Bus Shelter (Glass, Cantilever, Flat Roof; four agencies each) | `csm:bus_shelter_<style>_<agency>` | `transit.shelter.BlockBusShelter` |

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

## Where the module is going

Transit is planned to grow, in order: rail and subway platforms (platform edges with tactile warning strips, tactile paving, platform
furniture), stations (a subway entrance headhouse built to size, ticket validators that use the
fare code, station wayfinding), and working departure boards configured through a screen, drawn by
a baked renderer, with announcements through Text to Speech only when that module is installed.

Two rules hold throughout: every agency, livery and route bullet is invented, never a real transit
brand; and an advertising panel in a shelter is Signage's board, set into the shelter by the
player, since a module may reference only Core and the modules it requires (for Transit, Roads).
