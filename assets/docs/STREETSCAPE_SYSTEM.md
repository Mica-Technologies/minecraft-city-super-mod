# Streetscape

The Streetscape tab (`CsmTabStreetscape`, order 11) in **CSM: Roads & Traffic** holds what stands
on the sidewalk: covers, utility boxes, bollards, hydrants, news racks, mailboxes, parking
meters and EV chargers. The code is in `modules/roads/.../streetscape/`. The player's guide is
`docs/guides/streetscape.md`.

## Scope, and what is left out on purpose

- **No curbs, sidewalks or tactile pads.** The external road mod CSM is usually paired with
  provides those. Covers and bollards come only in styles that mod lacks. Never name that mod or
  its author in code, docs, assets or commits; call it "the external road mod".
- **No bike racks.** Those belong to a separate mod the same team maintains.
- **No real brands.** Mastheads are invented, the concrete sphere is generic, and the collection
  box says "UIA MAIL" (the user's choice) with no emblem.

Moved in when the tab was made (registry names unchanged, so saved worlds keep them): the fire
hydrant from Furnishings, and the Delineator Post, its yellow version and the Zebra Delineator
from Traffic Accessories. The hydrant keeps its six-way rotation, because changing it to four
would turn south-facing hydrants in existing worlds. It draws the same lathed hydrant OBJ as the
NFPA-coloured ones below (red, silver caps), with their box; its class is its own only for that
rotation.

Added beside them for issue #237: post-mounted delineators (`delineator_uchannel_*`, a reflector
plate, or two stacked, in white, yellow or red on a green steel U-channel) and flexible marker
posts (`delineator_flexible_*`, a flat composite post with a band of white, yellow or red
sheeting). They are `BlockWorkZoneDeviceDiagonal`s like the three above -- eight facings, settling
-- and come from `gen_road_markers.py`; see "Mile Markers and Post Markers" in
`TRAFFIC_SIGNS.md`, which also covers the object markers that share their post.

## Settling

Every Streetscape block extends a Core `AbstractBlockRoadSurface*` base
(`ICsmRoadSurfaceAware`, `RoadSurfaceHeight`), like the work zone devices
(`WORK_ZONE_ACCESSORIES.md`). The offset below is applied in `getOffset`, in `getBoundingBox` and
in every TESR, so a box on a sloped or partial-height road block stands on it.

## Multi-block units: `BlockUtilityBox`

Transformers, pedestals, meters, news racks and mailboxes are all `BlockUtilityBox`, sized by a
`UtilityBoxSpec` (up to two cells a side). The **root** (the block placed) draws the whole unit.
Invisible `BlockUtilityBoxPart` cells fill the rest, so the unit collides and can be clicked
anywhere. A part stores nothing: it finds its root by scanning the few cells a root could be in
(`findRoot`), and forwards clicks, picking and breaking to it.

- Placement is **all or nothing**. If any cell is blocked, the root is removed, the item is given
  back and the player is told where it was blocked.
- Breaking **any** cell removes the unit and drops one item. `DEMOLISHING` stops the parts'
  removal from starting a second demolition.
- `mayBreakUnit(world, root, player)` is asked from every cell. Mailboxes use it for owner-only
  breaking. A part also delegates hardness and explosion resistance to its root, since a part
  blown up would take the unit with it.
- Facing north, a wider unit grows to the placing player's right (-x), a deeper one away from them
  (+z), a taller one up. `gen_streetscape_utility.py` writes each spec (unit box, label position)
  from the same elements as the model, so the box and the model cannot disagree. A unit bigger
  than a block gets an `_inventory` model, its elements scaled into one cube.

`BlockUtilityBoxLabelled` adds the editable ID number (`TileEntityUtilityBoxLabel`, GUI 32,
`UtilityBoxLabelPacket`), drawn by a TESR on the face the spec names.

Every pad-mount transformer also wears a HIGH VOLTAGE sticker and, under it, the utility's
"IN CASE OF TROUBLE / CALL" sticker with the black DWP square (the mod's fictional Alto
Department of Water and Power). Both are baked into the model from one 128 px texture
(`hv_stickers`, HIGH VOLTAGE in its top half), with the phone number's place left blank. The
number is the player's: the same GUI has a phone field for any spec with a
`UtilityBoxSpec.Phone`, stored as `ph` (empty means `DEFAULT_PHONE`, `555-DWP-0100`) and sent as
an optional trailing string on the packet. The TESR draws it into the blank in the sticker's ink,
condensed rather than shrunk when it is long. The generator measures the blank off the texture's
layout and writes it into the tab line, and places the stickers' decals 0.2 px proud, where
`model_depth.py` would move them anyway, so the spec's face and the model's agree.

## Covers, bollards, hydrants, news racks

- **Covers** (`BlockStreetCover`, `gen_streetscape_covers.py`): flat cutout plates. Textures are
  drawn as the placer sees them and stored turned 180°, so item models add a z-180 display
  rotation or every legend reads upside down in the inventory.
- **Bollards** (`gen_streetscape_bollards.py`): the round ones are OBJ lathes.
  `BlockBollardFlexible` has no collision and `isPassable`.
- **Hydrants** (`gen_streetscape_street_furniture.py`): one lathed OBJ with `#body` and `#cap`
  materials, retextured per NFPA colour scheme by blockstate. The side nozzles are lathes turned
  by proper rotations (`rotated()`), so winding and normals stay right.
- **News racks** (`BlockNewsRack`): `LEFT`/`RIGHT` actual state from the neighbours facing the
  same way. A multipart blockstate draws the end panels only at the ends of a bank. They sit on a
  half-block stand, so they are 1×1×2 units.

## Parking meters

`BlockParkingMeter` (kinds MECHANICAL, DIGITAL, STATION) with `TileEntityParkingMeter`,
`TileEntityParkingMeterRenderer`, `ParkingMeterGui` (GUI 33), `ParkingMeterActionPacket`
(PAY/COLLECT) and `ParkingMeterSettingsPacket`, all through `ParkingPayments`.

- **Real time.** Each space stores its wall-clock expiry, so paid time runs while the chunk is
  unloaded. Nothing ticks: displays compute what's left when they draw. The sync carries the
  server's clock (`sn`), and the client shifts every expiry by the difference, so a display runs
  out when the server says the space has.
- **Redstone.** Weak power 15 while any head is expired (none for a pay station). `refresh()`
  always notifies the neighbours, with no remembered last output, and schedules one block update
  at the next expiry, capped at 400 ticks, because the world keeps one pending update per block.
- **Owner settings** (rate, minutes per block, maximum, collect or sink, a station's spaces) are
  per meter, clamped to the caps in Core's `CsmConfig` `parking` category.
- **SUM.** `ParkingPaymentSum` reaches SUM's economy by **reflection**, through Core's shared
  bridge `codeutils/economy/CsmSumEconomy` (the shops use the same one; see `SHOP_SYSTEM.md`).
  CSM never depends on SUM and must not. The handle is acquired in `FMLServerStartedEvent`, not
  `FMLServerStartingEvent`: SUM loads after CSM and installs its provider in its own starting
  handler. The server needs `csm_roads=wallet_read,wallet_write` in SUM's
  `economy_integration.allowedMods`.
  Without SUM, or when refused, meters take emeralds, which drop when a meter is broken.

## Parking garage gates and ticket machines (#302)

`gen_parking_garage.py` writes them; the code is in `streetscape/`.

- **Gate arm** (`BlockParkingGate`, `parking_gate_arm`): a cabinet whose striped arm closes the
  lane to its right as a driver facing it sees it (`armDirection`: the facing turned
  anticlockwise). The arm is `TileEntityParkingGateRenderer`'s, a few dozen coloured quads with
  the world's light, pivoting on the cabinet's side; it swings up in 1.5 s and down in 2 s.
  Stored: the facing and `open` (bit 2), the server's answer, which the client swings toward, as
  the railroad crossing gate's arm does. Saved on the tile entity: `md` the mode, `al` the arm
  length (3 to 6 blocks). A click with an empty hand cycles the length, a sneak-click the mode:
  - **Automatic**: lifts for anything in the lane (the arm's cells and a block either side, two
    high): a player, a mob, a vehicle; holds 3 s after it clears.
  - **Ticket**: lifts only for a ticket issued or paid at a machine beside it, for 10 s, and stays
    up while the car is in the lane.
  - **Redstone only.** Redstone lifts the arm in every mode.
  The server decides every five ticks; nothing else runs.
- **Ticket dispenser** (`parking_ticket_dispenser`) and **exit pay kiosk**
  (`parking_exit_kiosk`), both `BlockParkingMachine`, with a head at a car window's height. The
  dispenser hands out a `parking_ticket` stamped with the real time (`t`, epoch ms, as the meters
  keep real time) and lifts the nearest gate within four blocks: a machine pairs with the gate
  beside it with no linking. The kiosk takes a ticket, charges for the hours since it was issued
  (rounded up, at least one, at most 24; an unstamped ticket is 24) at its hourly rate, in
  emeralds or, with SUM's economy, as much money (`ParkingPaymentSum`, as the meters pay), and
  lifts its gate. Its owner (who placed it) or an op sneak-clicks with an empty hand to change
  the rate: 1, 2, 3, 5, 10, free. Nothing is collected for the owner (the meters' collect mode is
  not carried over).

## EV charging (#315)

`gen_ev_charging.py` writes the chargers and the stall markings; the signs are road signs, from
`gen_gap_signs.py` (below). No brand: the chargers say EV and show a plug.

- **Pedestal** (`ev_charger_pedestal`, `ev_charger_pedestal_dual`) and **DC fast**
  (`ev_charger_dc_fast`) chargers are `BlockEvCharger`, a `BlockUtilityBox` two cells tall like
  the parking meters: the root draws the charger, an invisible part fills the cell above. The
  pedestal is a 1.6 m column with its connector holstered on the side and the cable looped to its
  foot (the dual one has a second port, face and light on the back); the DC fast charger is a
  1.9 m cabinet with a screen, card reader and a heavy connector on each side.
- **Wall mount** (`ev_charger_wall`, `BlockEvChargerWall`): one block standing in the cell in
  front of the wall, its back on the cell's far face as the Life Safety wall devices are drawn.
  Placed against a wall it faces the player, so it hangs on the face clicked. It does not settle.
- **Status light** (`EvChargerStatus`, the `status` property): available (green), charging
  (blue), fault (red). Redstone picks between the first two: powered is charging. Read at the
  root, the bottom cell, for the two-tall chargers. A right-click with an empty hand sets a fault
  and another clears it (back to what the redstone says); while it is set redstone changes
  nothing. The blockstate swaps the lens texture; every lens and the DC screen have an OptiFine
  `_e` companion. No tile entity, nothing ticks: the state is worked out on a neighbour change
  and on placing.
- **Not done:** an "occupied" mode from an Immersive Vehicles car parked in the stall. Roads must
  not depend on Vehicles, and seeing a parked car means scanning for entities; a vehicle module
  hook or a detector giving redstone would be the way, as the preempt detector does it
  (`VEHICLES_SYSTEM.md`).
- **Stall markings** are `BlockStreetCover`s, one upward face like the covers: `ev_stall_paint`
  (green paint, a block at a time to fill a stall), `ev_stall_marking_symbol` (the D9-11b pump
  stencilled white on green) and `ev_stall_marking_ev_only` (EV ONLY on green). Textures are
  stored turned 180 degrees, as the covers' are.
- **Signs** (`CsmTabRoadSigns`, on the road sign posts): `signevcharging` (D9-11b, the EV pump,
  from the public-domain Commons redraw of FHWA's drawing in `artwork/mutcd_d9_11b.svg`, since
  the 2004 book predates it), `signevchargingplaque` (D9-11bP ELECTRIC VEHICLE CHARGING, the
  official interim drawing), `signevchargingarrowleft` / `signevchargingarrowright` (wayfinding:
  the pump and an arrow, white on blue, for lots and garages) and `signevchargingonly` (EV
  CHARGING ONLY, green on white under the symbol, R7 style; there is no federal design).
- **Pricing:** a charger is a control board, sheet metal and a wiring harness (DC fast: two
  sheet metal, two harnesses); the markings are a reflective sheeting.

### Placing by command

Metadata is `facing | status << 2`, facing as every NSEW block stores it (0 south, 1 west,
2 north, 3 east: the way the front faces), status 0 available, 1 charging, 2 fault. A two-tall
charger needs its part placed in the cell above, as every utility box unit placed by command
does; nothing carries NBT.

| Block | Cells | Metadata | Notes |
|---|---|---|---|
| `csm:ev_charger_pedestal` | root + `csm:utility_box_part` above | `facing + 4 * status` | connector on the viewer's right |
| `csm:ev_charger_pedestal_dual` | root + part above | `facing + 4 * status` | ports both sides, face front and back |
| `csm:ev_charger_dc_fast` | root + part above | `facing + 4 * status` | |
| `csm:ev_charger_wall` | one | `facing + 4 * status` | in the cell in front of the wall, facing away from it |
| `csm:ev_stall_paint`, `csm:ev_stall_marking_symbol`, `csm:ev_stall_marking_ev_only` | one | facing (0 to 3) | the legend reads upright from the side the front faces |

For example `/setblock ~ ~ ~ csm:ev_charger_pedestal 6` (facing north, charging) then
`/setblock ~ ~1 ~ csm:utility_box_part`. Redstone and clicks change the status afterwards.

## Mailboxes

`BlockMailbox` (`BlockMailboxCurbside` adds the flag), `TileEntityMailbox`, `ContainerMailbox`,
`GuiMailbox`, `MailboxActionPacket` (CLAIM/ASSIGN/FREE), from `gen_streetscape_mailboxes.py`.

- **Compartments.** The tab line gives each compartment's door as a rectangle on the front face
  (facing north, pixels, y from the root's floor), its slot count and its name. A click opens the
  door nearest the point looked at. It is found by ray-tracing from the player's eyes, because a
  click on a part cell arrives relative to that cell. The generator writes the rectangles from
  the same layout it draws the door decal from, so the numbers on the doors are the doors the
  Java picks.
- **Owners.** The placer owns every compartment, and hands one out by name (online players, or
  names in the profile cache only; any other name would make the cache ask Mojang on the server
  thread) or frees it for the next player to claim. Only the owner or an operator takes out; the
  placer or an operator manages and breaks.
- **The screen's mode travels in the GUI id**: `34 | compartment << 8 | mode << 16`, with modes
  FREE (claim button), POST (one slot) and OPEN (the compartment's slots). The server decides
  it, so the two containers cannot disagree about their slots. `canInteractWith` re-checks
  ownership every tick, so a compartment handed away closes on its old owner.
- **Privacy.** The client's tile entity is sent owners and one mail bit per compartment, never
  contents (`getUpdateTag` writes without items). An OPEN container on the client is backed by an
  empty stand-in handler of the right size, and the container sync fills it for that one player.
- **Automation.** A one-compartment box exposes an insert-only `IItemHandler` (posting goes
  through `post`, so the owner is told). Extraction always returns empty. Multi-compartment
  boxes expose nothing, since they couldn't tell which compartment an item was for.
- **Flag.** The curbside `FLAG` is actual state from the first mail bit. `getBakedModelKey`
  returns that bit, so a sync rebuilds the chunk only when the flag changes.

## Pricing

`StreetscapeFabricatorRules` (mirrored in `audit_fabricator_costs.py`): digital meters and
stations are a control board and sheet metal, the pay-by-phone sign and storm drain marker a sign
blank, transformers two sheet metal plus a wiring harness and concrete mix, the concrete sphere
two concrete mix, cluster mailboxes two sheet metal and a fastener kit, and covers iron (vault
lids, valve boxes and the cleanout add concrete mix). Everything else falls to the generic cost.
Core's mounting-hardware rule, which prices anything whose last word is a mount noun such as
"cover" as a bracket, exempts this tab.

## Traps

- **An item handler keeps the stack it is given.** `ItemStackHandler.insertItem` stores the
  caller's own stack object when the slot was empty. Post a copy, or the poster's stack and the
  compartment share one object.
- **A post slot needs a resend outside the click.** The server syncs slots while handling a click
  with "quantity only" set, which holds slot updates back, and afterwards its record already says
  empty. The client keeps showing the posted item. `ContainerMailbox.detectAndSendChanges` resends
  the slot once the flag is clear.
- **An item model's `parent` needs `csm:block/...`.** A blockstate's model path gets `block/`
  added; an item model's does not.
- **Close the ring between stacked octagons.** A wider piece on a narrower one must cap its own
  end, and a box body under an inset lid needs its `up` face. Otherwise the model is see-through
  there.
- **Block textures must be square.** A 16×64 face is read as an animation strip with no `.mcmeta`
  and drawn as the missing texture.
- **A single odd texel wrapped once round a sphere becomes a streak.** The concrete sphere's
  texture is a plain mottle.
- **Check the log for model errors only after the world has loaded.** They are logged after
  "Sound engine started".
- **Testing as a non-operator in singleplayer:** set `allowCommands` to 0 in the world's
  `level.dat`, and put it back afterwards.
