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
  hangs on the outside face of its cell and swings inward, as a vanilla door's does. The hinge is
  on the side of the opening clicked, unless there is a door beside it hinged on its far side, in
  which case the new one hinges the other way and the two are a **pair**, which opens and closes
  together.
- **Redstone** holds a door (and its pair) open while it is powered.
- **Door Closer** (item, crafted from two iron ingots, a Sheet Metal and a Fastener Kit): right-click
  a door to fit one. The door then shuts itself three seconds after a player (or a keypad) opens
  it, unless redstone is holding it open, and wears a surface-mounted closer on its inside face with
  its arm up to the header. Sneak-click the door with an empty hand to take it off again. It is an
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
halves' own closed models turned about the hinge, so handles, bars and a fitted closer swing with
the leaf. With `animateDoors` off it draws them where they are going at once.

**The open model is the closed one turned about the hinge**, a quarter turn about the pivot
(0.875, 15.125) px for a left hinge, which carries the leaf from the outside face to lie along the
jamb with its outside face to the room and its latch edge inward. The generator writes it out as
its own model, since a blockstate rotation turns about the block's centre and would put the hinge
in the wrong corner; the renderer turns about the same pivot, so the swing ends on exactly the
model that replaces it. The right-hinged models are the left's mirror.

Glazed doors (the lites, the fire door, storefront, half-glass back door) are on the translucent
layer; the rest are cutout.

## Cost

A storefront door is two glass panes and a Sheet Metal, a hollow metal, fire or exit door two Sheet
Metal and a Fastener Kit, and a wood or residential door three planks. The Door Closer is an item,
so it has a crafting recipe (`recipes/door_closer.json`, only while `csm_building` is loaded)
instead of a Fabricator cost.

## Traps

- **An item that acts on a block uses `onItemUseFirst`.** A right-click is offered to the block
  before the held item's `onItemUse`, and the door took it and opened; the closer item fits itself
  in `onItemUseFirst`, which comes first.
- **"Hollow Metal Door" is a coloured metal set** to a cost rule on the word "metal", which comes
  first in the Building Materials rules. Doors are priced before it, and the garage door fittings
  and keypad, whose names also say "door", are kept out of the door rule.
- **1.12's long array tag cannot be read back** (no getter), so `DoorLocks` stores positions as
  pairs of ints.
