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
| Sports & Venues - Arena (`tabvenuesarena`) | 35 | the event floor (below); later the centre-hung scoreboard, ribbon boards, banners and AV |
| hidden (`CsmTabVenuesHidden`) | −12 | `venue_unit_part`, the invisible cells of the multi-block units |

Tabs are one per kind of venue, not one per piece, so the module scales to many sports without a
tab for every block family. Planned, in the order they land:

| Tab | Holds |
|---|---|
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

## The event floor (Arena tab)

Written by `dev-env-utils/scripts/gen_venues_arena.py` (`--check` holds the Java it writes too,
`--fragments` prints the tab, `--doc` dumps the layout and curve tables as JSON). Everything is
baked models; the only tile entity is the floor marking anchor's (below), and nothing ticks.

### State counts

| Block | Stored (metadata) | Actual | States |
|---|---|---|---|
| `rink_ice` | paint (4) | | 4 |
| `rink_rubber_floor` | | | 1 |
| `rink_ice_line` | kind x axis (6) | | 6 |
| `rink_ice_spot` | facing, kind (16) | | 16 |
| `rink_ice_painted` | | cell (114), rot (4) | 456 |
| `court_maple` | finish x axis (8) | | 8 |
| `court_line` | facing, kind (12) | | 12 |
| `court_floor_anchor` | facing, kind (16) | | 16 |
| `court_painted` | | cell (138), rot (4) | 552 |
| `dasher_board`, `dasher_glass` | facing | end_l, end_r | 16 each |
| `dasher_board_corner`, `dasher_glass_corner` | facing | | 4 each |
| `dasher_{board,glass}_curve_{nw,ne,se,sw}` x 8 | cell (15) | | 15 each, 120 |
| `dasher_bench_gate`, `dasher_penalty_door` | facing, open, right | | 16 each |
| `hockey_goal`, `basketball_stanchion`, `ice_resurfacer`, `ice_plant` | facing | | 4 each |
| `venue_unit_part` (hidden) | toward | | 6 |
| `player_bench`, `penalty_bench`, `scorers_table` | facing | end_l, end_r | 16 each |

1,333 states in all. The painted blocks carry most of them: one state per distinct 32 px cell
picture and turn, each a plain cube with one top texture, so they cost a texture each (250 small
sprites) and no geometry worth counting.

### Floors

Full opaque blocks at the floor level; everything else stands on them. Ice (all the `rink_ice*`
blocks) is as slippery as vanilla ice.

| Block | Metadata |
|---|---|
| `rink_ice` | 0 white (three drawings, turned at random by position), 1 red, 2 blue, 3 crease blue: solid paint for anything the markings below do not draw |
| `rink_rubber_floor` | 0 only: black rubber matting for behind the boards, the benches and the corners outside the curve |
| `rink_ice_line` | `kind * 2 + axis`; kind 0 red centre line (12 in), 1 blue line (12 in), 2 goal line (2 in); axis 0 the line runs along x (east-west), 1 along z. So a line across a rink running north-south is 0 (red), 2 (blue), 4 (goal) |
| `court_maple` | `finish * 2 + axis`; finish 0 natural (three drawings), 1 navy, 2 crimson, 3 grey (an apron); axis as above, the way the boards run: lay a court's boards along its length |
| `court_line` | `facing \| kind << 2`; kind 0 a boundary line along the edge the facing points at, 1 a corner (that edge and the one to its left, looking that way), 2 a centre line across the block; facing S 0, W 1, N 2, E 3 |

### Painted markings: anchors and painted blocks

The circles, the crease and the keys are pictures laid out round an **anchor**
(`rink_ice_spot`, `court_floor_anchor`). An anchor is a floor block showing the middle of its
picture; a **painted block** (`rink_ice_painted`, `court_painted`, metadata 0) shows the piece
of the nearest anchor's picture that falls on it, turned with the anchor. Fill the area round an
anchor with painted blocks, by hand or with `/fill`, and the marking appears; where the picture
has nothing, a painted block is plain ice or maple.

Anchor metadata is `facing | kind << 2`, facing S 0, W 1, N 2, E 3: the picture's *forward*.
A painted cell is found by `i` (cells to the anchor's right, looking forward) and `j` (cells
forward). Only cells on the same level are searched (five cells round on the rink, nine on the
court), nearest ring first; where two pictures could reach a cell, the nearest anchor with
something drawn there wins.

| Family / kind | Picture | Centred on | Cells to fill (i, j) |
|---|---|---|---|
| ice 0 | end zone face-off: 15 ft circle, hash marks, the L marks, the spot | the anchor's middle | -5..5, -5..5 |
| ice 1 | neutral zone spot | the anchor's middle | the anchor alone |
| ice 2 | centre ice: the red line across, the blue 15 ft circle and blue spot | the anchor's **left edge** (looking forward), its row's middle | -5..4, -5..5 |
| ice 3 | goal line, crease (6 ft radius, 8 ft wide) and the trapezoid behind the goal to the end boards | the anchor's **left edge**; forward is towards centre ice, the end boards 3.5 blocks behind | -5..4, -3..2 |
| court 0, 1, 2 | a basket end: baseline (along the anchor's back edge), sidelines at i = ±7, the key (natural, navy or crimson; its lines white where painted), free throw circle (dashed inside), restricted arc, three-point line (22 ft corners, 23 ft 9 in arc), lane marks | the anchor's middle; forward is into the court | -7..7, 0..8 |
| court 3 | centre court: the half-court line, the 6 ft and 2 ft circles, the sidelines | the anchor's middle | -7..7, -2..2 |

The two rink pictures centred on an edge are what a 26 wide rink needs: its middle is a block
line. `ArenaFloorLayouts.java` lists every non-plain cell `(i, j, cell)`; a generator may fill
just those, or the whole box (a plain painted block only costs its search).

Placed by hand, an anchor faces the way the placer looks; a sneak-click with an empty hand steps
the kind (and says which). The line blocks run the way the placer looks and step their kind the
same way.

### Dasher boards and glass

A board is one block high on the floor's level, 4 px thick on the **ice side** of its cell,
white with a yellow kick plate and a grey cap; the glass is a block of its own stacked on the
board, as many high as wanted (two along the sides, three at the ends). Facing is towards the
ice: a rink running north-south has its west boards facing east (meta 3), east boards west (1),
north boards south (0), south boards north (2).

| Block | Metadata |
|---|---|
| `dasher_board`, `dasher_glass` | facing. A run closes itself: where there is nothing of its layer beside it (or an open door), a straight draws an end post (an aluminium end cap on a board, a stanchion on glass) |
| `dasher_board_corner`, `dasher_glass_corner` | 45 degrees across a cell, mitred into the straights diagonally beside it. Facing: the board faces between that facing and the next clockwise; at a rink's NW corner 3, NE 0, SE 1, SW 2 |
| `dasher_board_curve_<corner>`, `dasher_glass_curve_<corner>` | the cell along the curve, 0-14 (table below); a sneak-click steps it |
| `dasher_bench_gate` | bits 0-1 facing, bit 2 open, bit 3 hinge on the right (looking at the ice from behind the board). Board height; benches have no glass |
| `dasher_penalty_door` | as the gate, with a glass leaf a block higher (the door draws translucent). A glass block above it is a fixed transom |

A click opens or shuts a door; it swings a quarter turn away from the ice. The glass beside the
cell above a penalty door ends with a post, as a frame's stanchion does.

### The curve, and laying a 61 x 26 rink

The corner is a true quarter circle of radius 8 blocks round a centre 8 blocks in from both
boards. Its ice face crosses fifteen cells, all inside the ice rectangle's 8 x 8 corner square;
each draws its stretch (the board's back reaches a quarter block into the cell outside, where
the straight's back does). Drawn as the **north-west** corner, cell `k` sits at offset
`(dx, dz)` from the ice's corner cell (the first ice cell in from the west and north boards):

| k | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| (dx, dz) | 0,7 | 0,6 | 0,5 | 0,4 | 1,4 | 1,3 | 1,2 | 2,2 | 2,1 | 3,1 | 4,1 | 4,0 | 5,0 | 6,0 | 7,0 |

The other corners are the same cells turned: with the ice spanning `x0..x0+25`, `z0..z0+60`,

| Corner block | Cell k at |
|---|---|
| `_nw` | `(x0 + dx, z0 + dz)` |
| `_ne` | `(x0 + 25 - dz, z0 + dx)` |
| `_se` | `(x0 + 25 - dx, z0 + 60 - dz)` |
| `_sw` | `(x0 + dz, z0 + 60 - dx)` |

**Worked example** (the "Venues Arena Test" build, floor at y 3, everything else at y 4): ice
`x 0..25, z 0..60`.

- Floor: `rink_ice` over the rectangle, then `rink_rubber_floor` on every corner-square cell
  whose nearest point to the curve's centre (8, 8 from the corner) is 8 or more blocks away
  (the cells wholly outside the curve); rubber round the outside.
- Straights: west `x -1, z 8..52` meta 3; east `x 26, z 8..52` meta 1; north `z -1, x 8..17`
  meta 0; south `z 61, x 8..17` meta 2. Curves: the fifteen cells of each corner block, meta k.
- Glass: `dasher_glass` (same meta) two high on the sides (y 5, 6), three on the end straights
  (y 5-7); `dasher_glass_curve_<corner>` two high on the curves. The third row on an end
  straight ends in a stanchion beside the curve by itself.
- Benches (east side): no glass over `z 23..28` and `32..37`, gates at each end of each stretch
  (`dasher_bench_gate` meta 1 at the north end, 9 at the south end), and `player_bench` meta 1
  at `x 28`. Penalty boxes (west): `dasher_penalty_door` meta 3 at `z 27` and 11 at `z 33`
  (air above), `penalty_bench` meta 3 at `x -3`, `scorers_table` meta 3 between.
- Lines: red centre row `z 30` (meta 0) outside the centre picture, blue rows `z 22`, `z 38`
  (meta 2), goal lines rows `z 3`, `z 57` (meta 4) on ice outside the goal pictures.
- Anchors (`rink_ice_spot`): centre `(13, 30)` meta 10 (north, kind 2); goals `(12, 3)` meta 12
  (south, kind 3: its left edge, looking south, is the x = 13 line) and `(13, 57)` meta 14
  (north); end zone face-offs `(6, 9)`, `(19, 9)` meta 0 and `(6, 51)`, `(19, 51)` meta 2;
  neutral spots `(6, 24)`, `(19, 24)`, `(6, 36)`, `(19, 36)` meta 4. Then `rink_ice_painted` on
  each picture's cells.
- Goals: `hockey_goal` meta 0 at `(13, 4, 3)` and meta 2 at `(12, 4, 57)` (posts on the goal
  line, 2 blocks wide astride x = 13).

The distances are NHL's to the nearest half block: goal lines 3.5 blocks in from the end boards,
blue lines 22.5 from the ends, end zone spots 6.5 off the middle and 6 out from the goal line.

A 45 degree chamfer approximates the curve if a builder wants fewer pieces: five
`dasher_board_corner` on the line `dx + dz = 4` (cells 0,4 to 4,0) cut the corner as an
octagon, the straights then starting at `dz = 5` and `dx = 5`.

### The court (29 x 15)

`court_maple` along the length, lines navy. With the court on `x c..c+14, z c'..c'+28` running
north-south: `court_floor_anchor` at the middle of each baseline row (`(c + 7, c')` facing south,
`(c + 7, c' + 28)` facing north; kinds 0-2 for the key) and at centre court
(`(c + 7, c' + 14)`, kind 3); `court_painted` over rows 0-8 and 12-16 from each end as the
table above says; `court_line` edge pieces (facing outward) for the sidelines in rows 9-11 and
17-19; natural maple (meta 1) everywhere else, a grey apron (meta 7) round it. Each
`basketball_stanchion` stands in the second row beyond its baseline, on column `c + 7`, facing
the court.

### Units

Drawn whole by their master; `/setblock` the master alone and it fills its footprint with
`venue_unit_part` cells where they are free (cells offset in the model frame, facing north, +x
east, +z away from the front):

| Block | Metadata | Footprint | Where |
|---|---|---|---|
| `hockey_goal` | facing: the mouth | 2 x 2: the master, +x, and the row behind | the master on the goal line row; posts on the line (the middle of the row), 2 blocks wide on +x |
| `basketball_stanchion` | facing: towards the court | 11 cells, 5 high (OBJ: rim at 10 ft, 72 x 42 in glass backboard, a shot clock counting 24 to 0) | the master in the **second** row behind the baseline, on the court's middle column; the backboard's face is 1.22 blocks inside the baseline |
| `ice_resurfacer` | facing: its front | 2 wide x 3 long x 2 high; the master is the left-middle cell | parked |
| `ice_plant` | facing: its front | 2 wide x 1 deep x 2 high | back of house |

The stanchion is real size rather than the two or three blocks first asked for: the rim has to
be 10 ft over a real size court.

### Benches and the scorer's table

`player_bench` (a padded seat, no back), `penalty_bench` (seat and back) and `scorers_table`
(an LED front, an animated texture with an OptiFine `_e`): metadata the facing, the way the
sitter or scorer faces. Side by side they join; a run's ends are drawn by actual state. A click
sits on a bench (Core's seat entity, one a block).

## Brands

None. Team, league and sponsor names are what players type into the signs. The ice resurfacer
is a generic machine with no maker's name; the scorer's table plays an abstract pattern.

## Traps

- **A model's parent in an item model is `csm:block/...`**, not the blockstate's `csm:...`.
- **The aisle-end block reads only `BlockStadiumSeat`s**, never another aisle-end block, so the
  two never ask each other for their facing in a loop.
- **The seat shell is grey on purpose.** Its colour is the tint; a texture with colour in it
  would be multiplied twice.
- **A painted floor block reads its anchor only when its section is drawn.** The client redraws
  only the chunk section a block change is in, so the anchor has a tile entity (no data, no tick)
  whose load, update and removal ask for the cells within ten blocks to be drawn again. Without
  it, painted cells across a section line from a newly placed anchor stay plain.
- **Two rink pictures are centred on a block line** (centre ice and the goal crease), on their
  anchor's left edge looking forward: on a rink of odd width they sit half a block off.
- **A model reaching past its cell is drawn with that cell's chunk section.** The stanchion's
  backboard is four blocks over its master: with the master's section out of view (looking
  straight up at the rim across a section line) the whole stanchion is culled with it.
- **Boards are drawn without ambient occlusion** (the OBJ corners and curves through
  `"custom": {"ambient": false}`), or the floor darkens their white face from the foot up.
- **A unit's footprint is written into its tab line** by the generator from its boxes; a box
  reaching less than 6 px into a cell does not claim it, and has no collision there.
- **A JSON element may not reach 32 exactly plus an epsilon.** The octagon helpers stagger
  their four boxes by 0.004 px, so a round part ending at 32 fails to load; end it at 31.9.
