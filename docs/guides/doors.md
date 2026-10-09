# Doors

Two-block doors that pair, lock and close themselves, and garage doors built to the size of the
opening. They are all in the **Building Materials** tab of the **CSM: Building Materials** module,
except the **Job Trailer Door**, which is in the Construction Site tab with the job trailer it fits.

## The doors

| Door | Swings |
|---|---|
| **Interior Door (Oak)**, **Interior Door (White)**, and each with a **Vision Lite** | In |
| **Hollow Metal Door (Grey)** | In |
| **Front Door (White)**, **(Red)**, **(Black)** | In |
| **Back Door (Half Glass)** | In |
| **Exit Door (Push Bar)** | Out |
| **Storefront Door (Dark Bronze)** | Out |
| **Fire Door (Wired Lite)** | Out |

The exit, storefront and fire doors swing **out** because real ones do: an exit door opens in the
direction people escape, which is the only way a push bar can work. On a door that swings toward
you, a push bar would be a handle nobody can pull.

## Hanging a door

Place a door **from outside, looking in**. The way you are looking becomes the door's inside. An
inswing door opens into the room, and an outswing door opens toward you.

- **Sneak while placing** to hang it the other way: an interior door that swings out, or an exit
  door that swings in. A reversed exit door keeps its push bar on the side that is pushed.
- With no door beside it, the **hinge** goes on the side of the opening you clicked.
- **Pairs.** Place a door beside another of the same kind, facing the same way, and the two become a
  pair. The new door hinges on its far side so the latches meet in the middle. If the neighbour is
  shut and hinged on the shared side, it is turned round to match. A pair opens and closes together
  and always swings the same way: the second door takes the first door's swing, sneaking or not.

The **Door Swing Tool** changes the swing of a door that is already hung. Right-click either half
and the door, and its pair, swings the other way. It tells you which way the door swings now. It
only works on a shut door that has stopped moving ("Shut the door and let it stop first"), and not
on a Custom Door, whose movement is set in the Door Workshop.

## Opening and redstone

**Right-click** either half to open or shut it, and its pair with it.

**Redstone** to either half holds the door (and its pair) open while it is powered, and shuts it
when the signal stops. Use a pressure plate for a door that opens as you walk up, or a lever for a
door that stays open.

## The Door Closer

The **Door Closer** is an add-on item that any door can have. No door has one to begin with. Hold
it and right-click a door to fit one.

A door with a closer **shuts itself three seconds after it was last opened** by a player or a
keypad. Every new opening restarts the three seconds, so a door you open twice does not slam on you.
It does not shut while redstone holds it open.

The closer hangs on the **push side** of the door, which is the side the door swings away from. That
is where a parallel-arm closer goes on a real commercial door. Its arm folds and unfolds as the door
moves.

To take the closer off, **sneak-click** the door with an empty hand. The closer goes back into your
inventory. If you break the door, the closer drops with it.

## Keypad locks

A **Door Keypad** linked to a door locks it:

1. Put the keypad on the wall beside the door, on the outside.
2. **Sneak-click** the keypad with an empty hand. Within thirty seconds, **sneak-click** either half
   of the door, also with an empty hand.
3. Right-click the keypad, type a code of **4 to 6 digits**, and press **Set Code**.

From **outside**, the locked door now shows "Locked: use the keypad" when you click it. Type the
code on the keypad and the door opens. From **inside** (the side the door faces) it opens as
usual, so nobody is locked in.

- **Only the player who put up the keypad** can set its code or link it to a different door. Anyone
  can type the code.
- **Five wrong codes** in a row lock that player out of that keypad for thirty seconds.
- **Breaking the keypad unlocks the door.**

!!! note "A lock stops hands, not wiring"

    The lock only stops a click from outside. Redstone still opens a locked door, and so does a
    Garage Door Button or Control Station linked to it. A pressure plate outside a locked door
    makes it an unlocked door.

## Custom doors

The **Custom Door** is a door made from any three blocks: a frame, an upper and a lower material.
You make one at the **Door Workshop**. Put a block in each material slot, set how the door behaves,
and press Make. The workshop can save designs by name, and copy or re-program a custom door you put
in its edit slot.

| Setting | Choices |
|---|---|
| Opens | Swing, Slide, Slide Together, Slide Up, Split |
| Redstone | Normal, Redstone Only, Hand Only, Redstone Locks |
| Sound | Wood, Iron, Heavy, Sliding, Pneumatic, Gate, Trapdoor, Silent |

A custom door can also close itself after a time you choose, and open when a player comes near. A
keypad locks it the same way. A Door Closer does not fit a custom door, and the Door Swing Tool does
not change it.

## Garage doors

| Block | What it is |
|---|---|
| **Garage Door (White Raised Panel)** | Sectional door |
| **Garage Door (Windowed)** | Sectional door with windows in the top section |
| **Sectional Door (Commercial Steel)** | Sectional door |
| **Roll-Up Door (Galvanized)** | Coiling door that rolls up into a hood |
| **Security Grille** | Coiling, see-through grille |
| **Garage Door Opener** | Works the door its rail leads to |
| **Garage Door Hanger** | Holds up an opener or the back of a ceiling track |
| **Garage Door Button**, **Garage Door Control Station**, **Door Keypad** | Wall controls |

### Building one

**Fill the opening** with door blocks, placed from outside looking in. Blocks of the same kind that
face the same way join into **one door**, up to 256 blocks. The tracks, springs and hood go on the
inside. The **Security Grille** goes the other way round, because a grille is fitted and worked
from inside the shop: its hood and guides go on your side. **Sneak while placing** to put the
hardware on the other side, for any kind. A block you add joins the door whichever side you place
it from.

A sectional door folds round a bend at the top of the opening and lies along the ceiling when open,
as far back as the door is tall. The ceiling run and track are drawn up to eight blocks back. A
door taller than that still works.

### Working one

**Right-click** any block of the door, or give any block of it a **redstone** signal, and the whole
door moves. Only the moment a signal turns on counts. A button opens the door rather than opening
it and shutting it again, and a lever works it on every flip.

Like a one-button opener, a click starts a door that is at rest, stops a door that is moving, and
reverses a door that is stopped part-way.

A closed or moving door keeps light out. An open door, and the Security Grille, let it through.

### The opener and the hanger

Hang the **Garage Door Opener** in the row just above the top of the door, placed while looking at
the door. Its rail runs toward the door over as much air as there is, up to ten blocks, and ends in
a bracket on the wall above the door. Right-click the opener, or give it a redstone signal, and it
works the garage door at the end of its rail. A button wired to the opener works as a wall button.

The **Garage Door Hanger** is steel angle hung from the ceiling. Stack it a block at a time to reach
down from any ceiling height. Placed on a ceiling, it goes against the edge of the block nearest
where you clicked, or in the middle if you click the middle. Placed against a wall, it goes against
that wall. Blocks stacked under it keep its position.

- **Over an opener**, put a hanger in the middle, above the opener, and stack it up to the ceiling.
- **At the back of a sectional door's track**, put a hanger against the edge of the door's end
  column, where the track ends, and stack it up to the ceiling. Its foot bolts to the track.

### Wall controls

| Control | What it does |
|---|---|
| **Garage Door Button** | A lit push button: start, stop, reverse, like clicking the door |
| **Garage Door Control Station** | Three buttons, top to bottom: OPEN, CLOSE, STOP |
| **Door Keypad** | Type the code and the door moves, like the button |

**Link a control** the same way as a keypad lock: sneak-click the control, then within thirty
seconds sneak-click any block of the garage door, or its opener, both with an empty hand. A
control that is not linked says "Not linked: sneak-click this, then a door". The button and the
station link to the two-block doors too.

A keypad on a garage door **does not lock it**. It adds a way in, but a click on the door or a
redstone signal still works it.

## Turning the animation off

Doors swing when they open and shut. If you would rather the two-block doors and custom doors snap
open and shut, set `animateDoors` to `false` in the `general` category of `config/csm.cfg`; garage
doors still roll. It is on by default. It
changes only how doors are drawn, so it is each player's own setting and does not need to match the
server. See [Configuration](../getting-started/configuration.md#animatedoors).

## In survival

Every door and garage door block is made at the CSM Fabricator. The **Door Closer** and the **Door
Swing Tool** are items, so they have crafting recipes instead: the closer from two iron ingots, a
Sheet Metal and a Fastener Kit, the swing tool from an iron ingot and a Fastener Kit. See
[Survival & Crafting](survival-and-crafting.md).

## See also

- [Building Materials](../reference/building-materials.md): every door, garage door and control in
  the reference.
- [Construction Site](../reference/construction-site.md): the Job Trailer Door.
