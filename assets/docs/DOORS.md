# Doors

Doors in the **Building Materials** tab of `csm_building`, beside the garage doors: twelve doors in
one class (`BlockBuildingDoor`, constructed by registry name), the **Door Closer** add-on item, and
the **Door Keypad** from the garage doors, which locks a door as well. Every asset comes from
`dev-env-utils/scripts/gen_doors.py` (`--check` fails on drift).

| Door | Registry name |
|---|---|
| Interior Door (Oak / White), and each with a Vision Lite | `door_wood_oak`, `door_wood_oak_lite`, `door_wood_white`, `door_wood_white_lite` |
| Hollow Metal Door (Grey) | `door_metal_grey` |
| Fire Door (Wired Lite) | `door_metal_fire` |
| Exit Door (Push Bar) | `door_metal_exit` |
| Storefront Door (Dark Bronze) | `door_storefront_bronze` |
| Front Door (White / Red / Black) | `door_front_white`, `door_front_red`, `door_front_black` |
| Back Door (Half Glass) | `door_back_halfglass` |

## Using them

- **Placing.** From outside, looking in: the door's facing is the way the player looked, the leaf
  hangs on the outside face of its cell and swings inward, as a vanilla door's does -- except the
  **Exit Door, the Storefront Door and the Fire Door, which swing out** (below). The hinge is
  on the side of the opening clicked, unless there is a door beside it hinged on its far side, in
  which case the new one hinges the other way and the two are a **pair**, which opens and closes
  together.
- **Redstone** holds a door (and its pair) open while it is powered.
- **Door Closer** (item, crafted from two iron ingots, a Sheet Metal and a Fastener Kit): right-click
  a door to fit one. The door then shuts itself three seconds after a player (or a keypad) opens
  it, unless redstone is holding it open, and wears a parallel-arm closer on its **push side** --
  the side it swings away from: the outside of a door that swings in, the inside of one that swings
  out -- with a shoe on the wall above the opening and an arm that folds and unfolds between them
  as the door moves. Sneak-click the door with an empty hand to take it off again. It is an
  add-on rather than a property of some doors because a real closer is -- any door may have one, and
  none does to begin with (the user asked for it to be optional, 2026-09-18).
- **Door Keypad**: linked to a door (sneak-click the keypad, then the door, both with an empty
  hand) it locks the door. From outside it then opens only with the keypad's code; from inside --
  the side the door faces -- it opens as ever. The button and control station link to doors too.
- **Animation** is a client option, `animateDoors` in the CSM configuration, default on. It changes
  only how a door is drawn, so each player's own setting applies and need not match the server's.

## How it works

**Two halves, eight bits.** As a vanilla door, the lower half stores its facing and whether it is
open, the upper half the hinge, whether a closer is fitted, and whether it is swinging; each half
reads the rest from the other as actual state. That is every bit either half has, which is why a
lock is not state: locks live in the world's saved data (`DoorLocks`, keyed by the lower half,
server side only).

**Free at rest.** Open or shut, a door is baked models and nothing else. While it swings -- eight
ticks -- its upper half has a `TileEntityDoorSwing`, which holds only the direction and the tick it
started and never ticks: a scheduled block tick ends the swing, and the tile entity goes with the
state that asked for it. The models draw nothing while a door swings, and the renderer draws both
halves' own closed models turned about the hinge, so handles, bars and a fitted closer's body swing
with the leaf; a closer's shoe stays put and its arm is solved for the angle (below). With
`animateDoors` off it draws them where they are going at once. Every face it draws is shaded as the
world shades a block face, by the way it faces at that moment, so the last frame of a swing is as
light or dark as the baked model that replaces it (the renderer used to draw the leaf unshaded, a
visible step at both ends of every swing).

**The open model is the closed one turned about the hinge**, a quarter turn about the pivot
(0.875, 15.125) px for a left hinge, which carries the leaf from the outside face to lie along the
jamb with its outside face to the room and its latch edge inward. The generator writes it out as
its own model, since a blockstate rotation turns about the block's centre and would put the hinge
in the wrong corner; the renderer turns about the same pivot, so the swing ends on exactly the
model that replaces it. The right-hinged models are the left's mirror.

**Which way a door swings is a kind of door, not state.** The exit, storefront and fire doors
(`OUTSWING`, in `gen_doors.py` and `BlockBuildingDoor`, SHARED) swing **out**, toward the outside,
as real ones do: an exit door opens in the direction of escape, which is what lets a push bar work
at all -- on a door that swings toward you it is a pull handle nobody can pull. Every other door
swings in. No bit is left to make it a choice per door, and a door kind has one right answer.

An outswing door is the **depth mirror** of an inswing one: its leaf hangs on the *inside* face of
the cell (z 0..1.75 drawn north-inside) and turns outward about (0.875, 0.875), so the open leaf
lies along the hinge jamb inside its own cell exactly where an inswing door's does, and the open
collision box is the same. Only the shut box moves, to the inside face. The one thing that has to
leave the cell is the hardware on the inside face -- the push bar, the inside lever -- which stands
proud of the wall into the room by its own depth while the door is shut, as it does on a real door
hung flush with the wall; open, it is inside the cell. The facing, the placing rule, pairs,
redstone and keypad locks are untouched: the inside is still the side the door faces, and still
the side a locked door lets people out from, which for an exit door is the side with the push bar.

**The closer's arm articulates.** A real closer is three parts: a body on the leaf, a shoe fixed to
the frame, and an arm of two rigid links between them -- the main arm from the body's spindle to an
elbow, the forearm from the elbow to the shoe. The leaf carries the spindle round the hinge; the
shoe does not move; so the elbow can only be where a circle of the main arm's length about the
spindle meets one of the forearm's length about the shoe, on the side the arm folds to.
`DoorCloserArm.solve` works that out for any angle and `gen_doors.closer_joints` for the two it
bakes, from the same constants (`CLOSER_*`, SHARED): shut, the arm lies folded along the door with
its elbow toward the latch, as a parallel arm does; open, it reaches from the body, now in the
opening, out through the top of the opening to the shoe.

- **It is on the push side, for every door.** The leaf opens *within the wall's thickness* and ends
  lying along the jamb, so the face the door swings toward -- where a regular-arm closer would go --
  finishes against the jamb block, and an arm from a body there cannot reach the frame without
  passing through the open leaf. The push face turns into the opening instead, and a parallel-arm
  closer, the common mount on real commercial doors, is what goes there. So an inswing door's
  closer is on its outside (it used to be on the inside, with an arm that swung off with the leaf),
  and an outswing door's -- the exit door's, beside the push bar -- is on its inside. The body and
  shoe stand proud of the wall by a few pixels, as on a real door hung flush with the wall.
- **The models mark which part is which by tint index** (no colour handler is registered for the
  doors, so it tints nothing): the shoe `TINT_FIXED`, which the renderer draws unswung, and the arm
  `TINT_ARM`, which it leaves out and draws itself as bars between the solved points. The body is
  unmarked and swings with the leaf.
- **Both baked poses put each link on a multiple of 22.5 degrees**, the only angles a model element
  can be turned to. That is what fixed the link lengths and the shoe (5.3482 and 5.2133 px, shoe at
  (6.5317, 18.995)): shut the links are at 0 and 157.5, open at 45 and 90. Of the arrangements that
  land on those angles, this is the one that keeps the arm clear of the leaf and the jambs for the
  whole swing and does not pull straight or fold flat. `DoorCloserArmTest` holds the baked models
  to the solver and the arm clear all the way round: change a constant and it fails until the
  generator and the Java agree again.

Glazed doors (the lites, the fire door, storefront, half-glass back door) are on the translucent
layer; the rest are cutout.

## Cost

A storefront door is two glass panes and a Sheet Metal, a hollow metal, fire or exit door two Sheet
Metal and a Fastener Kit, and a wood or residential door three planks. The Door Closer is an item,
so it has a crafting recipe (`recipes/door_closer.json`, only while `csm_building` is loaded)
instead of a Fabricator cost. The Door Workshop is four planks, two Fastener Kits and a Sheet Metal
(a bench with a vice), priced before the door rule catches its name.

## Traps

- **An item that acts on a block uses `onItemUseFirst`.** A right-click is offered to the block
  before the held item's `onItemUse`, and the door took it and opened; the closer item fits itself
  in `onItemUseFirst`, which comes first.
- **"Hollow Metal Door" is a coloured metal set** to a cost rule on the word "metal", which comes
  first in the Building Materials rules. Doors are priced before it, and the garage door fittings
  and keypad, whose names also say "door", are kept out of the door rule.
- **`renderModelBrightnessColor` throws away a baked model's face shading.** It overwrites every
  vertex colour with the brightness it is given, so a model drawn that way in a renderer is lit
  evenly on every face. The swing renderer writes each quad itself and shades it by where it faces.
- **A closer on the pull face cannot work here** (see the closer's arm): the open leaf lies along
  the jamb inside the wall, and the pull face ends against the jamb block.
- **1.12's long array tag cannot be read back** (no getter), so `DoorLocks` stores positions as
  pairs of ints.

## Custom doors

`BlockCustomDoor` (`custom_door`) is a door made of any three blocks -- a frame, an upper and a lower
material -- with its own movement, sound, speed, auto-close, redstone mode and proximity sensor, all
in `CustomDoorSettings`. They are made in the **Door Workshop** (below); the plain custom door in the
tab (oak) is what the Fabricator makes.
Modelled on another mod's door factory, which the user asked for; implemented from scratch and
built to be **free at rest**, where that one draws every custom door every frame.

- **Settings** live in a data-only `TileEntityCustomDoor` on the lower half. No renderer is
  registered for it, so it costs nothing a frame: a chunk lists only tile entities with one.
- **Drawn baked.** `CustomDoorBakedModel` reads the settings through the block's extended state and
  faces the leaf from `CustomDoorGeometry` (a frame of stiles and rails round a set-in panel) with
  each material's sprite -- its model's north face, so a log shows its bark. Quads are cached per
  combination (settings, half, facing, hinge, open, paired, render pass): a street of identical
  doors bakes once. Each material draws in the pass its own block draws in, so glass is translucent.
  Materials are any plain-model block with no tile entity (`CustomDoorMaterials`).
- **Moving doors** are drawn by `CustomDoorRenderer` from `RenderWorldLastEvent`, only the doors in
  `CustomDoorMotion` (put there by a block event the server sends when a door moves), from the same
  cached quads under the movement's transform; the baked model draws nothing for a door while it is
  there. Not a TESR, which would be visited for every door every frame.
- **Movements.** Swing (as the fixed doors: the leaf on the outside face, turned about the hinge
  pivot); Slide (toward the hinge, into the wall -- a pair parts in the middle); Slide Together (a
  pair both to the left, the second stacked behind); Slide Up; Split (upper up, lower down). A door
  that slides, lifts or splits stands in the **middle of the wall's thickness**, as a glass pane does
  (the user's suggestion): it travels along the middle of the wall and ends wholly inside the block
  it slides into. It stops half a pixel short of a whole block, so its edge shows in the jamb like a
  pocket door's rather than lying in the jamb's plane, where the two faces would fight.
- An open door that has slid away is walked through freely; a strip at the jamb it went into can be
  clicked to shut it.
- **Redstone modes:** normal, redstone only, hand only, and redstone lock (a signal shuts and locks
  it). Auto-close is a number of ticks. The closer item does not fit a custom door.
- **Proximity sensor.** A door with the sensor on looks every 5 ticks through its own scheduled
  block tick, which re-schedules itself -- no ticking tile entity, and a door without a sensor
  schedules nothing. It is armed when the door is placed, when its settings change (the workshop,
  or `/blockdata`) and when its chunk loads; a pending tick is saved with the chunk anyway. It sees
  players only (a sensor that opened for mobs would let them in), 2.5 blocks out either side of
  the opening, two blocks high; a pair watches both its openings, or one leaf would shut the pair
  on someone in the other. It shuts once nobody has been there for the auto-close time, or a
  second if that is off, unless redstone is holding it open; a redstone lock stops it opening.
- **Keypad locks** work as on the fixed doors (a custom door is a `BlockBuildingDoor`): link a
  keypad by sneak-clicking it and then the door, and the door is locked in `DoorLocks` from the
  outside. With a sensor, a locked door opens only for someone on the inside -- free to leave,
  the code to come in, as an access-controlled automatic door -- but once open **anyone** in the
  zone holds it open, so it does not shut on the person who has just keyed it open.

### The Door Workshop

`BlockDoorWorkshop` (`door_workshop`, a bench block) opens `GuiDoorWorkshop` (GUI id 27, the one
screen in the module with a server-side container). Its `TileEntityDoorWorkshop` holds five slots
(frame, upper, lower, edit, output), the **design** on the screen, and up to twelve **saved
designs**; it is never ticked or drawn.

- **The design is a whole door**: three materials and the behaviour. A block put in a material slot
  becomes the design's material and stays so when taken out, so an empty slot shows the block the
  next door still needs, faded (a ghost). What Make builds is the slot's block, or the design's
  where the slot is empty. In survival every slot must hold its block and one of each is used up; a
  player in creative uses nothing and may make from the ghosts. Shift-click Make for a stack.
- **Set** gives the doors in the edit slot the screen's behaviour and keeps their materials;
  **Copy** takes the whole design from the door in the edit slot. A placed door's design comes back
  the same way: pick block in creative, or break it (the drop keeps its settings).
- **Saved designs** are named on the workshop and shared by whoever uses it; Save replaces a design
  of the same name.
- **The preview** is the door as Make would build it, turning slowly, shut a second, opening at its
  own speed, open a second, closing -- a pair for Slide Together and Split. It is drawn with
  `CustomDoorRenderer.drawDoor`, the same call and the same cached quads as a moving door in the
  world, so the two cannot disagree, and only while the screen is open. It is clipped to its
  window (scissor), so a sliding panel goes out of sight as into its pocket.
- **Nothing is trusted from the client.** `DoorWorkshopPacket` carries only the behaviour, an action,
  a design index and a name (cut to 24 printable characters); the handler checks reach and that the
  player's open container is this workshop's, takes materials from the slots, and asks the player's
  own game mode for creative.
- **The screen is 236 high** with 18-high buttons (`ShortButton` draws the texture's bottom edge
  rather than cutting it off), so it fits a 240-high window: a small window at GUI scale 2.

