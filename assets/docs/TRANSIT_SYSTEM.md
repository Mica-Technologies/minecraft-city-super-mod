# The Transit Module

CSM: Transit (`csm_transit`, tree `modules/transit`, Java package `transit`) is the optional module
for public transit. Like every module it pins Core to its own exact version and registers nothing
itself: its creative tab, **Transit** (`tabtransit`, `@CsmTab.Load(order = 27)`), is found by
Core's tab scan, and every block and item keeps the `csm:` namespace.

Today the module holds the working fare system, which it took over from Technology. What it is to
grow into is at the end of this page.

| Block or item | Registry name | Class |
|---|---|---|
| Fare Vending Machine | `csm:farevend` | `transit.fare.BlockFareVendingMachine` |
| Fare Gate | `csm:fare_gate` | `transit.fare.BlockFareGate` |
| Fare Gate (ADA, 2-Wide) | `csm:fare_gate_ada_2` | `transit.fare.BlockFareGateAda2` |
| Fare Gate (ADA, 3-Wide) | `csm:fare_gate_ada_3` | `transit.fare.BlockFareGateAda3` |
| Fare Ticket (item) | `csm:fareticket` | `transit.fare.ItemFareTicket` |
| Transit Card (item) | `csm:transitcard` | `transit.fare.ItemTransitCard` |

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

## Where the module is going

Transit is planned to grow, in order: bus stops (the stop pole and flag sign, route plates,
timetable cases), glass-and-steel bus shelters that join along their length, rail and subway
platforms (platform edges with tactile warning strips, tactile paving, platform furniture),
stations (a subway entrance headhouse built to size, ticket validators that use the fare code,
station wayfinding), and working departure boards configured through a screen, drawn by a baked
renderer, with announcements through Text to Speech only when that module is installed.

Two rules hold throughout: every agency, livery and route bullet is invented, never a real transit
brand; and an advertising panel in a shelter is Signage's board, set into the shelter by the
player, since a module may reference only Core.
