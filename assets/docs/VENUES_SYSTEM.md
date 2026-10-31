# CSM: Sports & Venues

Sports venues of every kind: arenas, stadiums, ballparks, football and soccer fields, bowling
alleys, tennis and pickleball courts, tracks and pools. GitHub #324 (a hockey and basketball
arena) started it: the seating bowl first, then the arena's event floor, scoreboard and AV, the
concourse and the exterior, then the other sports and what they share (bleachers, press boxes,
stadium light towers). Mod id `csm_venues`, tree `modules/venues`,
Java package `venues`. It depends on **Core alone**: nothing in it names another module's class.
The section and portal signs are Core's wayfinding panel; sitting is Core's `EntityCsmSeat`.

## Why a module of its own

A sports venue is a building type with its own vocabulary (a bowl, a rink, a diamond, a lane, a
centre-hung board) and a city that has no venues should be able to leave every one of their
blocks out. One module rather than one per sport, since most of what a venue is (seats,
bleachers, scoreboards, concourses, light towers) is shared between sports. Nothing in it is a
street fixture (Roads), furniture you would buy (Furnishings) or a hospital's (Healthcare), and
it has no reason to require any of them: the bowl's stairs are whatever the builder lays (CSM's
Stairs & Railings stairs or vanilla stairs), so the rails and lights read the aisle's geometry
from fixed numbers rather than from a Building block.

## Tabs

| Tab | Order | Holds |
|---|---|---|
| Sports & Venues - Seating & Venue (`tabvenuesseating`) | 34 | what every venue shares: the seating bowl (below), later bleachers, press boxes and venue signs |
| hidden (`CsmTabVenuesHidden`) | −12 | empty so far: multi-block cells and retiring blocks will go here |

Tabs are one per kind of venue, not one per piece, so the module scales to many sports without a
tab for every block family. Planned, in the order they land:

| Tab | Holds |
|---|---|
| Arena | the event floor (dasher boards and glass, goals, the ice and hardwood palettes, benches, the scorer's table, the ice resurfacer), the centre-hung scoreboard, ribbon boards, banners and AV |
| Concourse | concession counters and appliances, menu boards, turnstiles, suites, the team store, box office and marquee |
| Ballpark | baseball and softball: bases, mound, the infield and warning track palettes, dugouts, backstop, foul poles, outfield wall |
| Football & Soccer | the turf palette and its lines, goal posts and goals, pylons, sideline benches, chains |
| Bowling | the lane built to length, pinsetter, ball return, scoring monitor, the counter and settees |
| Courts, Track & Pool | tennis and pickleball, volleyball, track and field, swimming, golf and mini-golf, skate park basics |

A piece used by more than one sport (a stadium light tower, a bleacher, a press box) goes in
Seating & Venue rather than in the first sport that needed it.

## Performance rules

The bowl is about eighteen thousand seat cells, so:

- **No tile entity, no renderer, no entity per seat.** A seat is plain baked models. Sitting
  spawns Core's `EntityCsmSeat` only when a player clicks, and it goes when they stand.
- **One set of models for every colour.** The ten seat blocks share the same grey models; the
  shell's faces carry tint index 0 and `CsmVenuesClientProxy` registers a block and item colour
  handler that returns the block's colour. An aisle-end block and the companion seat take the
  colour of the seat beside them through the same handler, at no state cost.
- **State counts** (all through `CsmBlockStateContainer`; multipart blockstates, so one model
  location a block):

| Block | Stored (metadata) | Actual | States |
|---|---|---|---|
| `stadium_seat_<colour>` x 10 | facing, up, riser (16) | end_l, end_r | 64 each, 640 |
| `stadium_seat_aisle_end`, `_2` | label (16) | facing, right, up, riser | 512 each, 1,024 |
| `stadium_companion_seat` | facing | | 4 |
| `stadium_wheelchair_space` | facing | | 4 |
| `stadium_aisle_step_light` | facing, high | | 8 |
| `stadium_aisle_handrail` | facing, piece | | 16 |
| `stadium_glass_guard` | facing | | 4 |
| `stadium_glass_guard_slope` | facing, right, high | | 16 |
| `stadium_glass_guard_steep` | facing, right | | 8 |
| `stadium_section_sign`, `stadium_portal_sign` | facing | (wayfinding face, unlisted) | 4 each |

The aisle-end blocks carry the most states because everything but the row letter is read from
the seat beside them; that is what lets one block cover every colour, shape and side with no
tile entity. They are about 0.4% of CSM's states.

## The blocks, and how a generator places them

Every model faces **north**, towards the event floor, and backs onto **+z**. Facing metadata is
`AbstractBlockRotatableNSEW`'s: **S 0, W 1, N 2, E 3** (the way the seat faces). A bowl that
rises to the south has seats facing north, metadata 2.

### Rows

Rows are 1 block up. A seat sits in the air cell on the row's floor block.

- **2-deep row** (1 up x 2 deep, a lower bowl): the front cell is legroom (air on the floor
  block), the back cell holds the seat, `riser` clear. The seat stands on cast floor standards at
  the back of its cell, its back reclined towards the next row's riser.
- **1-deep row** (1 up x 1 deep, a steep upper bowl): every cell holds a seat, `riser` set. The
  standards hang off the riser behind (the next row's floor block), the seat is shallower and
  upright, and the legroom is the front of the same cell.

Placed by hand a seat takes the 1-deep shape when the block in front of it, one down, is not a
solid floor (the row below drops straight away).

### Stadium seat: `stadium_seat_<colour>`

Colours: `charcoal`, `navy`, `ice_blue`, `crimson`, `gold`, `silver`, `black`, `red`, `green`,
`white`.

| Bits | Meaning |
|---|---|
| 0-1 | facing (S 0, W 1, N 2, E 3) |
| 2 | folded up |
| 3 | 1-deep (riser-mounted) shape |

So a north-facing seat is meta **2** (2-deep, down), **6** (2-deep, up), **10** (1-deep,
down), **14** (1-deep, up). Seats facing the same way side by side are a row: each draws the
standard (armrest and cupholder) on its left boundary, and a full end standard with a blank
aluminium plate wherever the cell to its left or right holds no seat of the row. Nothing about
the ends is stored, so a generator places plain seats and the rows close themselves.

A click sits in a seat that is down (or puts down a folded seat); a sneak-click folds it.

### Aisle-end seat: `stadium_seat_aisle_end` (rows A-P), `stadium_seat_aisle_end_2` (Q-Z, AA-FF)

The end seat of a row, its end standard's plate carrying the row letter. **Metadata is the
letter's index within the block**, 0-15:

| Block | 0 | 1 | ... | 15 |
|---|---|---|---|---|
| `stadium_seat_aisle_end` | A | B | ... | P |
| `stadium_seat_aisle_end_2` | Q | R | ... | FF (Q-Z are 0-9, AA-FF 10-15) |

Everything else is read from the `stadium_seat_*` beside it (a seat to its left or right,
facing across the line between them): its facing, its shape, whether it is folded, its colour,
and which side the end standard goes on (away from that seat, towards the aisle). Place it in
the row's end cell instead of a seat, after or with the row; a lone one faces north as a
charcoal 2-deep seat. A sneak-click steps the letter on.

### Companion seat: `stadium_companion_seat`

A loose padded chair beside a wheelchair space. Metadata: facing. Its cushion takes the colour
of a stadium seat beside it.

### Wheelchair space: `stadium_wheelchair_space`

The international symbol of access in a bordered blue square, a decal on the platform's floor
(in the air cell on it). No collision. Metadata: facing.

### Aisle step light: `stadium_aisle_step_light`

A small LED in an aluminium housing on the riser of an aisle step, standing in the air cell
over a tread against the step behind it (+z), with an OptiFine `_e` lens; light level 6, no
collision.

| Bits | Meaning |
|---|---|
| 0-1 | facing: the way the light shines, down the aisle |
| 2 | high: on a full-block riser (y 11-14) rather than a stair's half-step riser (y 3-6) |

In a 2-deep aisle (tread, then stair) the light goes in the air over the tread, low, lighting the
stair's front; in a 1-deep aisle (a stair a row) in the air over each stair, low as well.

### Aisles, and the pieces of a slope

Rails and guards follow the aisle's **nosing line**, so cells side by side make one straight
rail. `StadiumSlope` (ordinals saved):

| Piece | Index | Where | Nosing line in the cell |
|---|---|---|---|
| level | 0 | a landing, cross aisle or tier front | y = 0 |
| steep | 1 | 1 in 1: the air over each stair of an aisle beside 1-deep rows | y = z - 8 |
| shallow_low | 2 | 1 in 2: the air over the tread level with a 2-deep row's legroom | y = z / 2 |
| shallow_high | 3 | 1 in 2: the air over the stair beside the seat, a block above the tread before it | y = z / 2 - 8 |

(z is in pixels up the aisle, 0 at the downhill edge of the cell.) A 2-deep aisle is a tread then
a stair a row: shallow_low over the tread at the row's level, shallow_high over the stair one
block up, repeating. Vanilla stairs and CSM's stairs have the same volumes, so either works.

### Aisle handrail: `stadium_aisle_handrail`

A round stainless centre rail 13 px above the nosing line, a post a cell standing on the tread
under it. Metadata: bits 0-1 the facing (down the aisle), bits 2-3 the piece index above. A
sneak-click steps the piece. Collision: a 2 px wall down the middle of the cell, one block high.

### Glass guards

Frameless glass on an aluminium shoe with a stainless cap; cells join edge to edge with no post.

| Block | Metadata |
|---|---|
| `stadium_glass_guard` (level) | facing, towards the drop; the glass on the cell's front edge |
| `stadium_glass_guard_slope` (1 in 2) | bits 0-1 facing (down the aisle), bit 2 right edge (looking down the aisle), bit 3 high piece |
| `stadium_glass_guard_steep` (1 in 1) | bits 0-1 facing, bit 2 right edge |

A sloped guard's glass stands on the steps under it at the left or right edge of its cell, its
top 15 px above the nosing line: shallow low and high pieces alternate as the handrail's do. The
sloped pieces are OBJ (no JSON element can be turned to 1 in 2), the glass in the translucent
layer.

### Section and portal signs: `stadium_section_sign`, `stadium_portal_sign`

Core's wayfinding panel (`WAYFINDING_PANELS.md`): cells side by side and stacked are one sign,
baked, set in the editor or by NBT. Metadata is the facing, towards the reader. The section sign
defaults to `101` centred in navy on a 1 px plate flat on the wall; the portal sign to SECTIONS /
101 - 104 with an arrow up, lit. Any section number is free text:

```
/setblock x y z csm:stadium_section_sign 2
/blockdata x y z {l1:"214",s:"neutral",m:"wall",al:"c",d:0b,lt:0b}
/blockdata x y z {l1:"SECTIONS",l2:"314 - 318",a:"up",s:"transit"}
```

A two-block-tall section sign gives a number about 0.9 blocks high.

## Brands

None. Team, league and sponsor names are what players type into the signs.

## Traps

- **A model's parent in an item model is `csm:block/...`**, not the blockstate's `csm:...`.
- **The aisle-end block reads only `BlockStadiumSeat`s**, never another aisle-end block, so the
  two never ask each other for their facing in a loop.
- **The seat shell is grey on purpose.** Its colour is the tint; a texture with colour in it
  would be multiplied twice.
