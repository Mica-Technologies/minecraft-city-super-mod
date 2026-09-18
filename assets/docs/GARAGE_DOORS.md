# Garage Doors

Garage doors in the **Building Materials** tab of `csm_building`, built to the size of the
opening and animated when they move -- and, the point of the whole design, costing nothing at all
while they are not moving.

| Block | Class | Kind |
|---|---|---|
| Garage Door (White Raised Panel) | `BlockGarageDoor("garage_door_sectional_white")` | sectional |
| Garage Door (Windowed) | `BlockGarageDoor("garage_door_sectional_windowed")` | sectional, windows in the top section |
| Sectional Door (Commercial Steel) | `BlockGarageDoor("garage_door_sectional_commercial")` | sectional |
| Roll-Up Door (Galvanized) | `BlockGarageDoor("garage_door_rollup_galvanized")` | coiling |
| Security Grille | `BlockGarageDoor("garage_door_grille")` | coiling, see-through |
| Garage Door Opener | `BlockGarageDoorOpener` | works the door its rail leads to |
| Garage Door Hanger | `BlockGarageDoorHanger` | holds up an opener or a ceiling track |
| Garage Door Button | `BlockGarageDoorControl("garage_door_button")` | lit wall button: start, stop, reverse |
| Garage Door Control Station | `BlockGarageDoorControl("garage_door_station")` | OPEN / CLOSE / STOP |
| Door Keypad | `BlockGarageDoorControl("garage_door_keypad")` | PIN code, then as the button; also locks a door (DOORS.md) |

Every asset comes from `dev-env-utils/scripts/gen_garage_doors.py` (`--check` fails on drift).

## Building one

Fill an opening with door blocks, placed from outside looking in: the door faces the way the
player looked, and its tracks, springs and hood go on the inside. Blocks of one kind hung the same
way are one door, up to 16 x 16. Right-click any block of it, or give any block of it a redstone
signal, and the whole door opens or closes. Only the rising edge of a signal counts, so a button
opens the door rather than opening it and closing it again at once, and a lever works it on every
flip. As with the blinds, what was powered is remembered only while the world is loaded.

A door hangs just behind the wall, past the opening's inside face (z -2.5..-0.5 of 16 with the
inside to the north, so outside its own block's cell), with its tracks on the wall beside the
opening -- where a real one is. It was first drawn inside the opening, and the bend of the track
over the top then ran into the wall over the opening and the tracks into the jambs. From the street
it reads recessed by the wall's full thickness. A closed door has a one-pixel lip past each side
(into the track) and over the top, so there is no slit where it meets the wall; the lip is not drawn
while the door moves. Its collision box is out there with it -- a box outside its cell still
collides, since entities gather boxes from the blocks around them -- but the box that is clicked is
not: a ray is tested against a block only from where it enters that block's cell, so from inside the
garage a click box out where the panels are is already behind the ray, and the door could be clicked
only from the street. The click box sits on the cell's inside face, just behind the panels.

## Why it costs nothing at rest

A door that is open or closed is baked block models and nothing else: no tile entity, no renderer,
no tick. A street of garage doors draws like a street of walls.

Only while a door moves, or stands stopped part-way, does one block of it, the **anchor** (its
lowest block, furthest anticlockwise), hold a `TileEntityGarageDoor`. That block's `motion` is
`anchor`, the others' `moving`, and `hasTileEntity(state)` is true for the anchor state alone, so
the tile entity comes with the state and goes with it: when the move ends the anchor sets every
block to `open` or `closed`, and `shouldRefresh` drops the tile entity. The renderer draws the whole
moving door from the anchor -- the curtain or every panel, turned to the facing, lit block by block
-- and draws only for the second or three the move takes. Where the door is is one number, its
position (0 closed to 1 open); the tile entity holds the position at the last command, the direction
since (+1, -1, or 0 stopped) and the world tick of that command, and the renderer works out the rest
from the world time, so the server and every client agree without sending positions. The door is
eased by position, not time, so it slows into both ends however often it was stopped on the way. A
stopped door keeps its tile entity and renderer -- it has to be drawn part-way -- but does not tick.

- `motion` and facing are the four metadata bits. Which neighbours are the same door is actual
  state (`ccw`, `cw`, `up`, `down`, in the model's frame), so everything that belongs to the whole
  door is drawn only on the block at that edge of it.
- A command is toggle, open, close or stop. Toggle is a one-button opener's: it starts a door at
  rest, stops a moving one, and reverses a stopped one. Clicking the door, redstone, the opener and
  the wall button toggle; the control station sends open, close and stop.
- Breaking the anchor mid-move finishes the move for the rest of the door at once (a stopped
  door goes to its nearer end). Saving mid-move
  keeps the anchor's tile entity; loaded again, the move is already over and finishes on the first
  tick.
- A closed or moving door keeps the light out (opacity 15, lit by its neighbours so it does not
  draw itself black); an open door and a grille let it through.
- A door is solid unless it is open, including while it moves. An open door can be clicked only
  along the top of its top course, so the opening is clear to reach through.

## The sectional door

Two sections to a block, each a rigid panel hinged at its joints. In motion every section is drawn
as a flat panel between its two edges on the track -- up the opening, round a bend of radius 0.375
block and back along the ceiling -- so the sections fold round the bend the way a real door's do.

An open door's panels lie along the ceiling behind the top course, as far back as the door is tall.
That is up to eight blocks, and a JSON element reaches a block and a half past its cell, so the
ceiling run and the overhead track are **OBJ models, one per door height**, picked by `depth`
(actual state on the top course: how many door blocks are below it, up to 8). Those models and the
renderer use the same path and the same texture mapping (u across the door, v along each row, the
back face mirrored), so the last frame of a move is the model that replaces it. The constants they
share are marked SHARED in the generator and the renderer. A vanilla multipart cannot retexture an
OBJ, so each style's ceiling run has its own OBJ with the texture named in its MTL, and v runs down
the texture as Minecraft's does (no `flip-v`).

The hardware is drawn where it is on a real door, each piece only on the blocks it belongs to:

- vertical tracks on the wall either side of the opening, bending over at the top into the ceiling
  tracks, which run half a block past the open panels;
- the torsion spring assembly on the wall above the opening, standing far enough off it to clear
  the bend: a shaft across the whole door, and at each end a bearing plate beside the opening, a
  cable drum over the door's edge and a spring anchored to a centre bracket;
- while closed, the lift cables down the door's back from the drums to the bottom brackets, and a
  roller bracket at each section joint.

## The roll-up door and the grille

The curtain coils up into the hood hung on the wall's inside face above the opening, running in
guides on the wall either side. In motion the curtain is drawn from its bottom bar up to the top of
the opening, and its slats move with it: the texture is laid by distance up the curtain, not by
height. The hood has end plates, and on its clockwise side a chain wheel with a hand chain down the
wall. The grille is the same door with an open link curtain on the cutout layer.

## The opener and the hanger

The **opener** hangs in the row just above the top of the door, where a sectional door's ceiling
track runs, placed looking at the door. Its rail runs forward over as much air as there is (up to
ten blocks) and ends in a bracket on whatever it meets -- the wall over the door. The length is
actual state (an OBJ per length), so nothing is set up and nothing ticks. Right-click it, or give it
a redstone signal, and it works the first garage door in the three blocks below the rail's end: a
button on the wall wired to the opener is a wall button.

The **hanger** is perforated angle hung from the ceiling, stacked a block at a time so it reaches
down from any ceiling height. Placed on a ceiling it goes against the edge of its cell nearest the
click, or in the middle; placed against a wall, against that wall; stacked, it keeps the position of
the one above. A stack's top block gets a cleat along the ceiling and its bottom block, with nothing
below it, a foot at the height of a sectional door's ceiling track, bolted to the inner side of the
track, which runs just outside the door's end column. So to hold up the back of a track, put a
hanger against the edge of the door's end column, in the cell where the track ends (as many blocks
behind the door as the door is tall, plus one), and stack it up to the ceiling. Over an opener, a
hanger in the middle carries the opener's own strap on up to the ceiling.

## Wall controls

Three controls, one class (`BlockGarageDoorControl`, by registry name), each on a wall facing out
from it: the lit push button inside a garage (it gives off a little light), the commercial OPEN /
CLOSE / STOP station (which button is told by where on it the click landed), and the PIN keypad
outside.

**Linking.** Sneak-click the control, then within thirty seconds sneak-click any block of the door
or its opener, both with an empty hand -- in 1.12 a sneak-click reaches a block only when both hands
are empty. The link lives in a `TileEntityGarageDoorControl`, data only: never ticked, never drawn.
The started link is remembered per player on each side (`GarageDoorLinks`), and the two sides are
kept apart because a singleplayer game runs both in one process: one side finishing the link must
not take it from the other before it has seen the click.

**The keypad.** Clicking it opens a PIN screen (`GuiGarageKeypad`); a right code toggles the door,
a wrong one buzzes. The player who put it up owns it: only they can set the code (4 to 6 digits) or
relink it, and a keypad put up by a command belongs to whoever first sets a code. The code never
leaves the server -- the tile entity's client tag carries only whether a code is set and who owns
it -- and the screen sends what was typed (`GarageKeypadPacket`, fixed size, reach-checked) for the
server to judge. Five wrong codes lock that player out of that keypad for thirty seconds.

## Cost

A door is a Sheet Metal + a Fastener Kit, the grille an iron ingot + a Fastener Kit, the opener an
Enclosure Shell + a Control Board + a Wiring Harness, and a hanger a Fastener Kit. The keypad is an
Enclosure Shell + a Control Board, the control station an Enclosure Shell + a Wiring Harness, and
the button a Wiring Harness.

## Traps

- **A rule on "door" catches every door in its tab.** The Building Materials cost rules match whole
  words in the display name; the opener, the hanger and the controls are matched first because
  their names contain "door" too.
- **A click box must stay inside its cell.** A ray is tested against a block only from where it
  enters the block's cell (see Building one).
- **A GUI button is 20 px tall.** Vanilla's button texture is 20 px; a taller button shows the next
  row of the sheet as a stripe along its bottom.
- **The block index needs a literal.** The doors are constructed by registry name; the opener and
  hanger return theirs from `getBlockRegistryName`, and the guidebook finds all of them.
- **`audit_obj_models.py` reads only the first three vertices of a face.** These OBJs are quads,
  so its see-through warnings on them are the missing half of every quad, not a real hole.
- **A door taller than eight blocks** is handled, but its ceiling run and track stop at eight blocks
  back.
