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
| Sports & Venues - Seating & Venue (`tabvenuesseating`) | 34 | what every venue shares: the seating bowl, aluminium bleachers, the press box, the ticket booth and stadium light towers (below) |
| Sports & Venues - Arena (`tabvenuesarena`) | 35 | the event floor and, over it, the centre-hung scoreboard, ribbon boards, banners and AV (below) |
| Sports & Venues - Concourse (`tabvenuesconcourse`) | 36 | concession stands and their machines, menu boards, the beer kiosk, turnstiles and the ticket scanner, suite fixtures, the team store, and the arena's outside and entry: the marquee board and its pylon, the wayfinding blades and their bracket, the box office window (below) |
| Sports & Venues - Ballpark (`tabvenuesballpark`) | 37 | baseball and softball: the field's surfaces, edges and chalk lines, the marking anchors and painted cells, the mound, plate, bases and rubber, the dugout, nets, the outfield wall, markers and foul pole, the line score and count boards, a pitching machine (below) |
| Sports & Venues - Football & Soccer (`tabvenuesfootball`) | 38 | American football and soccer: the field's turf, paint, surround and end zone colour, the marking anchors and painted cells that draw every line where it really falls, goals, the sideline, end zone lettering and the scoreboards (below) |
| hidden (`CsmTabVenuesHidden`) | −12 | `venue_unit_part`, the invisible cells of the multi-block units |

Tabs are one per kind of venue, not one per piece, so the module scales to many sports without a
tab for every block family. Planned, in the order they land:

| Tab | Holds |
|---|---|
| Bowling | the lane built to length, pinsetter, ball return, scoring monitor, the counter and settees |
| Courts, Track & Pool | tennis and pickleball, volleyball, track and field, swimming, golf and mini-golf, skate park basics |

A piece used by more than one sport (a stadium light tower, a bleacher, a press box) goes in
Seating & Venue rather than in the first sport that needed it. The exterior did not get a tab of
its own: an arena's outside is not a kind of venue, and its entry pieces (the box office, the
blades) sit with the turnstiles in Concourse.

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

## Scoreboards and AV (Arena tab)

Written by `dev-env-utils/scripts/gen_venues_av.py` (`--check` holds `ScoreboardSize.java` too,
`--fragments` prints the tab). Everything a player sees at a distance is baked; three renderers
draw only what changes (the scoreboard's screens, the ribbon board's scrolling text, the banners'
lettering), each within its tile entity's render range, and nothing ticks.

### State counts

| Block | Stored (metadata) | Actual | States |
|---|---|---|---|
| `centre_hung_scoreboard`, `_small` | facing | | 4 each |
| `led_ribbon_board` | facing, pattern (16) | phase (4) | 64 |
| `led_ribbon_board_corner` | facing, pattern (16) | | 16 |
| `led_ribbon_board_text` | facing | phase (4) | 16 |
| `championship_banner`, `_small`, `retired_number_banner`, `_small` | facing | | 4 each, 16 |
| `line_array_speaker`, `broadcast_camera_platform` | facing | | 4 each |
| `venue_catwalk` | | north, east, south, west | 16 |

144 states in all. Thousands of ribbon blocks are 64 states and 16 small models between them.

### Units, hung

The scoreboards, the banners and the speaker cluster are **hung** units
(`BlockVenueUnit(name, true, cells, boxes)`): their item places them against the underside of a
truss or ceiling, the cell clicked under being the unit's top layer, and the master as many cells
below it as the footprint reaches above the master. A `/setblock` names the master, which then
fills its parts as every unit does:

| Block | Footprint | Master | `/setblock` the master at |
|---|---|---|---|
| `centre_hung_scoreboard` | 12 x 8 casing (13 x 9 cells, the edge cells half full), 9 tall: bottom ring, 6-tall screens, top ring, rigging | middle cell of the bottom layer | 9 below the truss's underside block, over the floor's centre |
| `centre_hung_scoreboard_small` | 6 x 4 (7 x 5 cells), 5 tall: half-block rings, 3-tall screens | middle cell of the bottom layer | 5 below |
| `championship_banner`, `retired_number_banner` | 2 wide, 3 tall | the left cell, looking from the north at a north-facing banner, of the middle row | 2 below the truss; the banner reaches to +x at facing north |
| `championship_banner_small`, `retired_number_banner_small` | 1 x 2 | bottom cell | 2 below |
| `line_array_speaker` | 1 x 1, 3 tall | middle cell | 2 below |
| `broadcast_camera_platform` | 2 x 2, 2 high (stands) | front-left (the lens looks north, +x and +z behind) | on the floor |

The scoreboard's facing only turns its long faces: facing north or south puts the 12-wide faces
to the north and south. Its cells are worked out in code from its shell boxes
(`BlockVenueUnit.cellsOf`, the generator's footprint rule): the shell is half a block thick, so
about 480 invisible parts, not the thousand a solid box would claim; the hollow inside stays air.

### The centre-hung scoreboard

The OBJ draws the casing, the corner posts, the rigging (a hoist frame and four bridles up to a
shackle in the top cell), the two LED rings (an animated chase, with an OptiFine `_e`) and the
screens dark, so the board is there at any distance. `TileEntityScoreboardRenderer` draws over
the screens, fullbright, within 160 blocks (capped by the performance mode):

- **Score**: home and away names on bars in the teams' colours, the scores under them in amber,
  the clock in a panel in the middle in yellow, PERIOD and the period under it, SHOTS at the foot.
- **Video**: the idle video (32 frames of an invented intro and a MAKE SOME NOISE meter, 4 frames a
  second) over the top four-fifths, and a strip along the foot: home colour, HOME 2, the period,
  the clock, 1 AWAY, away colour.

A click anywhere on the board opens its editor; a sneak-click with an empty hand starts or stops
the clock (the action bar says which, and at what time); so does a **rising redstone edge at any
of its cells** (a part passes its neighbour changes to the master, `IVenueUnitListener`).

**The clock never ticks.** While it runs the time left is `clk - (worldTime - t0) / 2` tenths,
worked out each frame from the world's time, so every client shows the same tenth. From a minute
up it reads `M:SS`, under it `S.T`. At 0.0 it stops counting; in mode 0 the screens go back to the
video.

**The editor** (GUI id 70): home and away names, scores and shots (with +1 buttons), colours
(click to step, shift-click back), the period, the clock and a period's length (`m:ss`, `ss.t`
or `m:ss.t`), the screens' mode, Start/Stop, Reset (stops it and sets it to the length) and Done.
The clock field is sent only if it was edited, so a running clock is not set back to when the
screen opened. It sends `ScoreboardPacket` (APPLY, TOGGLE, RESET), its strings bounded on decode
and cleaned again by the tile entity.

**NBT** (`/blockdata` on the master):

| Key | Meaning |
|---|---|
| `home`, `away` | team names, up to 12 printable ASCII characters (default HOME, AWAY) |
| `hs`, `as` | scores, 0-999 |
| `hsh`, `ash` | shots, 0-999 |
| `per` | the period, up to 4 characters (`1`, `OT`, `Q4`) |
| `clk` | the clock, tenths of a second: the time left when last started or stopped (12000 = 20:00) |
| `len` | a period's length, tenths; what Reset sets |
| `run` | `1b` while running; set it without `t0` and it starts now |
| `t0` | the world time it started at, written only while running |
| `mode` | 0 score while running and video while stopped (default), 1 always score, 2 always video |
| `hc`, `ac` | team colours, 0-15 (below) |

```
/setblock 13 20 30 csm:centre_hung_scoreboard 2
/blockdata 13 20 30 {home:"WOLVES",away:"BIRDS",hs:3,as:2,hsh:31,ash:24,per:"3",clk:7215,hc:0,ac:9}
/blockdata 13 20 30 {run:1b}
```

**Team colours** (`VenueColours`, the index stored): 0 navy, 1 royal blue, 2 ice blue, 3 teal,
4 green, 5 forest green, 6 gold, 7 orange, 8 red, 9 crimson, 10 maroon, 11 purple, 12 black,
13 silver, 14 white, 15 brown. A team bar light enough to lose white lettering gets dark lettering.

### The LED ribbon board

| Block | Metadata |
|---|---|
| `led_ribbon_board` | bits 0-1 facing (the way the LED face looks), bits 2-3 pattern: 0 navy and ice blue sweep, 1 crimson and gold chevrons, 2 green and white chase, 3 white crawl (LET'S GO) |
| `led_ribbon_board_corner` | as the straight; the facing is the dasher corner's (the band faces between it and the next facing clockwise: east = south-east, a bowl's north-west corner) |
| `led_ribbon_board_text` | facing |

A band is 1 block tall on the **back** of its cell (3 px deep), so it goes in the air cell in
front of a tier's fascia, facing the floor. Placed by hand it looks back at the placer; a
sneak-click with an empty hand steps the pattern. No tile entity, no renderer: one animated
64 px texture holds the four patterns, each four blocks long, and each block shows the quarter its
**phase** (actual state: its position along the band, counted to the right as seen) picks. Every
block of every band keeps time with every other, so straights side by side play one sweep.

**45 degree corners**, drawn as at a bowl's north-west corner: a corner at `(x, z)` meets the
straight along x at `(x + 1, z)`, the straight along z at `(x, z + 1)`, and the next corner at
`(x - 1, z + 1)`. A bowl corner chamfered over `n` cells: straights along the north row up to
`x0 + n`, corners from `(x0 + n - 1, z0)` down the diagonal to `(x0, z0 + n - 1)`, straights down
the west column from `z0 + n`. Corners show phase 0, so a sweep restarts round them. Only the
inside (concave) corner is drawn; a band round a balcony's outside corner is still open.

**Scrolling text**: put `led_ribbon_board_text` at the **left end** (as seen from the floor) of
a straight run. Its renderer draws a band in the background colour over the run (this cell and
the straights to its right facing the same way, up to 256, stopping at a corner, a gap or another
text piece) and the text on it, sliding left and coming round again three blocks after its end;
speed 0 centres it, still. One display list of the text's glyphs is replayed for each copy; two GL
clip planes cut the copies at the run's ends. The run is walked again at most once a second. A
click opens the text editor (GUI id 71): the text, its colour, the band's colour, the speed.

NBT: `txt` (up to 64 characters), `fg`, `bg` (colours), `spd` (0 still, 1 slow 1 block/s, 2
medium 2, 3 fast 4).

```
/setblock 0 12 -6 csm:led_ribbon_board_text 0
/blockdata 0 12 -6 {txt:"WELCOME TO THE ARENA",fg:6,bg:12,spd:2}
```

### Banners

Cloth and trim are baked, tinted (tint 0 the cloth, 1 the trim) from the tile entity by a block
colour handler, so a colour change rebuilds the chunk section (`getBakedModelKey` is the two
colours); the three lines are `TileEntityVenueBannerRenderer`'s, on both faces, lit as the cloth
is, within 128 blocks: the top line, the big line (a year or a number, up to 8), the bottom line
(up to 24), each shrunk to the cloth's width. A click opens the text editor.

NBT: `l1`, `l2`, `l3`, `bc` (cloth), `tc` (trim), `fc` (lettering). Championship banners start
CHAMPIONS / 2026, white on navy with gold trim; retired numbers PLAYER / 00, white on crimson
with white trim.

```
/blockdata 4 26 0 {l1:"CHAMPIONS",l2:"2026",l3:"NATIONAL HOCKEY CUP",bc:0,tc:6,fc:14}
/blockdata 8 26 0 {l1:"SMITH",l2:"19",l3:"1998 - 2012",bc:9,tc:14,fc:14}
```

### Speakers, camera, catwalk

`line_array_speaker`: nine cabinets hung from a fly bar, curling into a J (OBJ). Metadata the
facing (the way the cabinets point). `broadcast_camera_platform`: a 2 x 2 riser with rails on
three sides and a studio camera on a pedestal, its lens to the open front. `venue_catwalk`: bar
grating at the bottom of its cell; a rail (toe board, mid and top rail) on every side with no
catwalk beside it, so a run closes itself. Nothing stored. Venues may not need Roads, so this is
not Roads' truss catwalk.

### Costs

Measured 2026-10-30 in the "Venues Arena Test" world (dev client, all modules, 854 x 480,
render distance 12, the frame cap lifted), from the stands looking at the board and 298 ribbon
blocks (three runs along each side, the north run with its corners and a text run), all in one
session:

| What | How | Cost |
|---|---|---|
| Large scoreboard's screens, video layout | `client_profile_rendering`, 6 s warm-up | 35 us a frame |
| the same, score layout | the same | 29 us a frame |
| the same, frame level | `/csm renderpass skip venueScoreboard`, A/B/A | render work 0.89 / 0.85 / 0.89 ms: about 40 us |
| 298 ribbon blocks (straights and corners) | delete and difference, A/B/A | render work 0.86 / 0.86 / 0.86 ms: below what a frame measure resolves |
| a ribbon text run (21 cells) | profiler | 8.8 us a frame |
| a banner's lettering | profiler | 2.3 to 3.5 us a frame |

So the ribbon board is free at any length (it is chunk geometry and an atlas animation), a board
costs about as much as one dynamic guide sign, and the text and banners are small. Toggles for
A/B: `venueScoreboard`, `venueRibbonText`, `venueBanners`.

### Traps

- **A hung unit's master is not where you clicked.** Placed by hand, the cell under the truss is
  the top layer; by `/setblock` the master is named, which on the large scoreboard is 8 cells
  below the top of its casing. Its rigging cell reaches one more.
- **A unit's screen packet names its master,** which on a scoreboard may be eleven blocks from
  the cell clicked, so reach is to the nearest point of the unit's outline
  (`VenuePackets.canReach`), not to the master.
- **Parts find their master by walking,** up to 64 steps (a scoreboard's far corner is about
  twenty).
- **The video is not a block texture.** It is bound on its own and its frame picked from the time,
  so OptiFine's or VintageFix's animation trimming cannot stop it; nothing names it in a model, so
  the integrity tool lists it as unused.
- **The scoreboard's casing is baked; a baked model is culled with its master's chunk section.**
  The master is the bottom layer's middle cell, so looking up past the board's top edge with that
  section out of view can drop the board for a frame. The renderer is not global, so screens and
  casing go together.
- **A ribbon's sweep restarts at a corner** (corners show phase 0), and only the inside corner is
  drawn.
- **The ribbon text stops at the first cell that is not a straight facing its way**, including a
  corner: put a text piece on each straight that should carry text.
- **The ribbon text's clip planes** are fixed-function GL; a shader pack may ignore them and show
  the copies past the run's ends.
- **A banner's colours are baked** (tint), so changing them rebuilds its chunk section; its words
  are not.
- **The scoreboard GUI sends the clock only if it was edited**, or opening the editor during play
  would set a running clock back to the moment the screen opened.

## The concourse and premium spaces (Concourse tab)

Written by `dev-env-utils/scripts/gen_venues_concourse.py` (`--check`, `--fragments`); the
scanner and turnstile sounds by `gen_venues_sounds.py` (synthesised, no `--check`). Everything
is baked models: no renderer, nothing ticks. The only tile entities are the concession sign's
(Core's wayfinding panel) and the team store fixtures' two colours.

### Use these existing blocks

A concourse shares a lot with a supermarket, a kitchen, a bar and a station, and CSM already has
those. Venues requires none of their modules, so nothing here is copied from them: build with
these where the module is installed.

| Need | Block (registry name) | Module |
|---|---|---|
| Fountain drinks | `fountain_machine_red`, `fountain_machine_blue` | Furnishings (Market & Store) |
| Bottled drinks, beer | `reach_in_cooler_black`, `_white`; `ice_cream_case_white`; `ice_merchandiser_*` | Furnishings (Market & Store) |
| Hot food, rotisserie | `hot_food_case_steel`, `rotisserie_oven_steel` | Furnishings (Market & Store) |
| Registers and card readers on the counter | `pos_terminal_black`, `card_terminal_stand_black`, `cash_register_brass` | Furnishings (Market & Store) |
| Cups and lids, condiments | `cup_counter_grey`, `_walnut`; `coffee_bar_grey` | Furnishings (Market & Store) |
| Draught beer at a bar | `beertap`, `beerrack` | Furnishings (Furniture) |
| Suite and bar stools | `bar_stool_oak`, `_walnut`, `_white` (seat at 12 px: right for the drink rail and the suite bar) | Furnishings (Residential) |
| Suite and concourse TVs | `wall_tv_black`, `large_wall_tv_black`, `xl_wall_tv_black`, `giant_wall_tv_black` (the sports channel; the food and drinks menu channels are a restaurant's) | Furnishings (Residential) |
| Team store rails, tables, mannequins | `clothing_rail_chrome`, `round_rack_chrome`, `apparel_table_oak`, `_white`, `mannequin_black`, `_white`, `dress_form_*`, `security_gate_grey`, `shop_*` (working shops) | Furnishings (Market & Store) |
| Entry security | `metal_detector` (Walk-Through Metal Detector) | Life Safety (Emergency Services) |
| Bag check | `airport_xray_scanner`, `airport_security_roller`, `airport_security_trays` | Transit |
| Queue lines | `airport_queue_stanchion_black`, `_blue` | Transit |
| Restrooms | toilets, urinals, `trough_sink_*`, hand dryers, toilet partitions | Furnishings (Residential) |

Not built for that reason: a security arch (Life Safety's), coolers, a fountain machine,
registers, stools and TVs. What the stadium has and those do not is below.

### State counts

| Block | Stored (metadata) | Actual | States |
|---|---|---|---|
| `concession_counter` | facing, finish (16) | end_l, end_r | 64 |
| `concession_counter_corner` | facing, finish (16) | | 16 |
| `concession_back_counter`, `suite_bar_counter`, `suite_drink_rail` | facing | end_l, end_r | 16 each, 48 |
| `concession_sign` | facing | (wayfinding face, unlisted) | 4 |
| `concession_menu_board` | facing, layout (16) | | 16 |
| `hot_dog_roller_grill`, `popcorn_machine`, `pretzel_warmer`, `nacho_cheese_dispenser`, `soft_serve_machine` | facing, on | | 8 each, 40 |
| `beer_kiosk` | facing | | 4 |
| `stadium_turnstile` | facing, ticketed, open | | 16 |
| `stadium_ticket_scanner` | facing, result (3) | | 12 |
| `jersey_wall_rack` | facing, numbered | | 8 |
| `hat_wall`, `team_apparel_table`, `jersey_mannequin` | facing | (colours in the tile entity) | 4 each, 12 |

240 states. Facing is always bits 0-1, **S 0, W 1, N 2, E 3**, towards the customer, reader or
shopper; every model's back is at +z.

### Placing (metadata and NBT)

| Block | Metadata | Notes |
|---|---|---|
| `concession_counter` | `facing \| finish << 2`; finish 0 stainless, 1 navy, 2 crimson, 3 black | A block-high counter (top at 16 px, so a terminal set on it stands at its cell's floor); the front panel faces the customers, an open shelf behind. Side by side they join; a run's ends draw end panels. A sneak-click with an empty hand steps the finish |
| `concession_counter_corner` | as the counter | The **outside** corner: fronts on its facing and the side a quarter turn to its left (looking that way). At a stand's north-west corner it faces north; the north run joins it on its east side, the west run (facing west) on its south side, and neither draws an end panel there. No inside corner |
| `concession_back_counter` | facing (towards the server, its doors) | Stainless cabinets, worktop, a 4 px backsplash rising into the cell above. Joins as the counter does |
| `concession_sign` | facing | Core's wayfinding panel (`WAYFINDING_PANELS.md`): cells side by side and stacked are one sign, lit by default. Presets: HOT DOGS (red), BEER (black and gold), PIZZA (orange), SNACKS (blue), GRILL (black), SWEETS (pink), DRINKS (teal), CONCESSIONS, TEAM STORE. A price list is four lines typed in its editor. `/blockdata x y z {l1:"PIZZA",s:"laboratory",al:"c",m:"wall"}` |
| `concession_menu_board` | `facing \| layout << 2`; 0 hot dogs and grill, 1 beer and drinks, 2 pizza and snacks, 3 sweets | A unit 3 wide x 2 tall on the back of its cells (against the wall). The master is the **bottom middle** cell; `/setblock` it and it fills its five parts. A sneak-click steps the menu |
| `hot_dog_roller_grill`, `popcorn_machine`, `pretzel_warmer`, `nacho_cheese_dispenser`, `soft_serve_machine` | `facing \| on << 2` | Countertop machines standing at their cell's floor: put them on a counter. A click switches one on or off (light 6, 10, 8, 6 and 4 on). On, the sausages glisten, the kettle pops, the warmer and the cheese pump glow, the soft-serve display reads ready |
| `beer_kiosk` | facing | A unit 2 wide x 1 deep x 3 tall: a counter with four taps on a T-bar, a DRAFTS price board behind, a lit COLD BEER canopy on two posts. Its other cells are at +x of the master, which is the viewer's **right** cell looking at its front. A click pours (a sound) |
| `stadium_turnstile` | `facing \| ticketed << 2 \| open << 3` | People pass from its front (the end with the green arrow, the way it faces) to its back. Free (bit 2 clear): walking in from the front the arms index a third of a turn and let you through; from the back the arm is a barrier. Ticketed: locked both ways (red cross) until a ticket is scanned on it or at a scanner within two blocks, then open one way for 3 s or until the person is through. Leave bit 3 clear: the server sets and clears it. A creative player's sneak-click with an empty hand switches free and ticketed |
| `stadium_ticket_scanner` | `facing \| result << 2`; leave result 0 | A click with an unscanned Venue Ticket opens the nearest shut ticketed turnstile within two blocks on its level (green ring, a chirp) and marks the ticket scanned; a stub or no ticket is refused (red ring, a buzz) |
| `suite_bar_counter` | facing (towards the guests) | Walnut front, stone bar top (16 px), LED under the lip, a brass foot rail, the server's work surface behind. Joins |
| `suite_drink_rail` | facing (towards the event floor) | An oak ledge at 16 px reaching 8.5 px back over the guests' side, on a slim panel, with a foot bar. Put bar stools in the cells behind it. Joins |
| `jersey_wall_rack` | `facing \| numbered << 2` | On the wall behind it: a waterfall arm with three jerseys, the front one numbered 19 when bit 2 is set (placed by hand: numbered, or blank if placed while sneaking) |
| `hat_wall`, `team_apparel_table`, `jersey_mannequin` | facing | Nine caps on slatwall (on the wall behind); folded tees in stacks on a nesting table; a mannequin in a jersey with the number on its back |

**Team colours.** The four team store fixtures share one grey set of models: the garments carry
tint 0 (the team colour) and tint 1 (the trim: cuffs, hem stripes, collar, numbers, a cap's
brim and logo). Their tile entity (`TileEntityTeamColour`, `tileentityvenueteamcolour`) keeps
`c` and `t`, `VenueColours` indices (0 navy ... 14 white; the scoreboard's list), default navy
and white. A click with an empty hand steps the team colour, a sneak-click the trim, and the
action bar names it. `/blockdata x y z {c:9,t:6}` is crimson and gold.

**Venue Ticket** (`venue_ticket`, item): ADMIT ONE, stacks to 16. Crafted four at a time from
paper and any dye (Core's recipes, conditional on `csm_venues`). Scanning splits one ticket off
as a stub (NBT `scanned:1b`); a creative player's tickets are not used up.

### Costs

Every concourse block is chunk geometry and atlas textures. The four menu pictures are the
largest textures (256 px each, with `_e` copies for OptiFine's emissive layer: about 1 MB of
atlas); the popping and sausage animations are 16 px strips. The turnstile's
`open` change and the scanner's ring are one block update each, scheduled, never a ticking tile
entity. A team store fixture's colour change rebuilds its chunk section, once.

### Traps

- **The counter tops are a block high on purpose.** Furnishings' countertop pieces read only
  Furnishings' own counters for their drop (`SurfaceRest`); on anything else they stand at the
  floor of their cell. A 16 px top is where they then sit, and so do this tab's machines.
- **The corner joins only an outside corner.** Two straights meeting at an inside corner each
  draw an end panel; build an L stand round its outside.
- **The turnstile's one way is collision by where you stand.** The arm's box is added only for an
  entity whose middle is past the cell's middle and that does not already overlap the arm (or
  always, while locked). The overlap test is not optional: the server sends a player back when a
  move ends inside a collision box they were not inside before, so an arm that appeared the tick
  their middle crossed it stopped every player dead in the lane (found in game). A mob or item has
  no "from", so a locked turnstile stops everything and a free one stops only what is behind it.
- **Who closes the turnstile:** a scheduled update that reschedules itself every 5 ticks while a
  living entity is in the cell. Do not set bit 3 in a `/setblock`: without the scheduled update
  it stays open until something walks through.
- **The menu board's master is in the middle**, unlike the kiosk's (an end cell). A `/setblock`
  names the master; its parts are `venue_unit_part`.
- **The machines are translucent-layer blocks** (glass and sneeze guards), so they sort with the
  world's other translucent faces. A concourse of hundreds of them is still small.

## The exterior and the shared outdoor pieces

Written by `dev-env-utils/scripts/gen_venues_exterior.py` (`--check`, `--fragments`). The arena's
outside and entry (the marquee, the blades, the box office) are in the Concourse tab, after the
concourse; the pieces every outdoor venue shares (bleachers, the press box, the ticket booth, the
stadium light towers) in Seating & Venue, after the bowl. Everything is baked models; the one
renderer is the marquee's, one per board. Nothing ticks: the light heads' field lighting (below)
is placed by Core a few cells a tick when a head is switched, loaded or random-ticked.

### State counts

| Block | Stored (metadata) | Actual | States |
|---|---|---|---|
| `marquee_board`, `marquee_board_controller` | facing, double | left, right, up, down | 128 each, 256 |
| `marquee_pylon` | facing | up, down | 16 |
| `venue_gate_blade`, `venue_section_blade` | facing | (wayfinding face, unlisted) | 4 each, 8 |
| `venue_blade_bracket` | facing | plate, tip | 16 |
| `box_office_window` | facing, legend (4) | end_l, end_r | 64 |
| `aluminium_bleacher`, `aluminium_bleacher_aisle` | facing, seat | end_l, end_r, back | 64 each, 128 |
| `aluminium_bleacher_frame` | facing | post_l, post_r | 16 |
| `press_box_module` | facing, wall, corner | end_l, end_r | 64 |
| `ticket_booth` | facing | | 4 |
| `stadium_light_pole`, `stadium_light_lattice` | facing | base | 8 each, 16 |
| `stadium_light_head`, `_small` | facing, lit | (tile entity: the field's light-up air, the last redstone seen) | 8 each, 16 |

604 states. The aisle block carries the seat property it never sets (it is the same class), so
half its 64 are never reached.

### Placing (metadata and NBT)

Facing is bits 0-1, **S 0, W 1, N 2, E 3**: towards the reader, the customer or the field; every
model's back is at +z.

| Block | Metadata | Notes |
|---|---|---|
| `marquee_board` | `facing \| double << 2` | One cabinet cell of a board built to any size up to 48 x 24. Single-sided (bit 2 clear) the cabinet is on the back of its cell, its screen 7 px out from the wall; double-sided it is centred in its cell with a screen on each face, for a pylon. Cells of the same facing and sidedness side by side and stacked are one board, framed round the outside only. By hand: faces the placer, double-sided if sneaking |
| `marquee_board_controller` | as the board | The board's **bottom-left cell as the front's reader sees it**: the board is as wide as the cells running to the reader's right of it, as tall as the cells running up from it. A click opens the editor (three lines, the mode, the text and background colours) |
| `marquee_pylon` | facing | A 10 px deep clad slab centred in its cell, under a double-sided board; a plinth where no pylon is below, a cap where none is above. Stack it and set it side by side |
| `venue_gate_blade`, `venue_section_blade` | facing (along the wall, for a projecting blade) | Core's wayfinding panel, lit, double-sided and hung by default: GATE / A, and SECTIONS / 101 - 112 with an arrow up. Presets: gates A-F, BOX OFFICE, WILL CALL, TEAM STORE, ENTRY; section ranges, SUITES / CLUB LEVEL, ALL GATES, ACCESSIBLE SEATING |
| `venue_blade_bracket` | facing: the way the arm reaches out from the wall | Put it in the cell over a blade's top cell, so the blade's rods reach it; a blade two cells out takes two brackets in a line. The cell against the wall draws the plate and knee brace, the outer end a cap. By hand, clicked on a wall it reaches away from it |
| `box_office_window` | `facing \| legend << 2`; legend 0 BOX OFFICE, 1 WILL CALL, 2 TICKETS, 3 blank | A unit two blocks tall set into a wall: the master is the lower block (panel and counter ledge with the deal tray), the part above it the glass with a speaking grille under a lit header (light 7 unless blank). Windows side by side are a run with end jambs only at its ends. A sneak-click with an empty hand steps the header. For one header across a run, set the windows blank and hang a `concession_sign` (presets BOX OFFICE, WILL CALL, TICKETS) above |
| `aluminium_bleacher` | `facing \| seat << 2` | The row's deck filling its cell (planks over a closed riser); with bit 2 its seat plank in the cell above, at the back, half a block over the deck. Bit 2 clear is a plain deck: the front cell of a 2-deep row |
| `aluminium_bleacher_aisle` | facing | A deck with a half step at the back of the cell above, the aisle's tread between rows. It continues a row, so rows beside it stay open |
| `aluminium_bleacher_frame` | facing | The understructure: fill every cell under the rows down to the ground. A ledger under each riser, and a post with a diagonal brace every third cell and at each end |
| `press_box_module` | `facing \| wall << 2 \| corner << 3` | A unit two blocks tall on the front edge of its cells (the master below). Window: siding to desk height, a writing ledge inside, tinted glass leaning out, a fascia; wall: siding up to the fascia. A corner also walls its left side looking the way it faces: build the box clockwise, each corner facing the run that leaves it (north-west corner faces north, north-east east, south-east south, south-west west). A slab roof on top closes it. By hand: a window, a wall if sneaking |
| `ticket_booth` | facing: its window | A unit 2 x 2 x 3 (OBJ): the master is the front cell on the customer's right; the booth reaches a block to the customer's left and a block back. Light 7 (its TICKETS band) |
| `stadium_light_pole`, `stadium_light_lattice` | facing (the head's; the monopole's step bolts are on its back) | Stack as tall as wanted; the bottom cell stands on a concrete pier |
| `stadium_light_head`, `_small` | `facing \| lit << 2` | A unit on top of the shaft: the master is the bottom middle cell, in the cell over the shaft's top; the head is 5 wide and 3 tall (20 floodlights) or 3 wide and 2 tall (6). Facing is the way the floodlights aim, tipped 20 degrees down, and the way its footprint of field lighting lies (below). A click with an empty hand switches it, and so does redstone at the master, on the signal's edges; by hand it is lit and aims at the placer |

**Bleacher rows** rise a block at a time, as the bowl's do. With the bleacher facing north and
its front row at `z`: a **2-deep row** is a plain deck (meta 2) at `z` and a seat block (meta 6)
at `z + 1`, the next row starting a block up at `z + 2`; a **1-deep row** is one line of seat
blocks, each row a block up and a block back. The aisle column takes `aluminium_bleacher_aisle`
in the seat cells (a plain deck in a 2-deep row's front cell). Fill the cells under every row
with frame blocks. The planks are the steps a bleacher is climbed by (the plank is 0.5 block
over the deck, the next deck 1); a click with an empty hand on a seat block's deck sits on its
plank (Core's seat entity).

**Marquee NBT** (`/blockdata` on the controller):

| Key | Meaning |
|---|---|
| `l1`, `l2`, `l3` | the lines: printable ASCII, 32, 32 and 48 characters; the first biggest, an empty line leaves no gap |
| `mode` | 0 text and video in turn, eight seconds each (default), 1 text only, 2 video only |
| `fg`, `bg` | text and background colours, `VenueColours` indices (the scoreboard's list) |

```
/fill -36 8 51 -23 12 51 csm:marquee_board 0
/setblock -36 8 51 csm:marquee_board_controller 0
/blockdata -36 8 51 {l1:"TONIGHT 7PM",l2:"WOLVES VS BIRDS",l3:"TICKETS AT THE BOX OFFICE",fg:6,bg:0}
```

That is the "Venues Arena Test" entry's board: 14 x 5 on the south face of the wall at `z 50`,
facing south, so its reader looks north and the controller is its west end. The plaza's board
on a pylon is double-sided (meta 4): `marquee_pylon` meta 0 at `x -37..-32, y 4..6`, then
`marquee_board` meta 4 over `x -38..-31, y 7..9` with the controller at `(-38, 7, 60)`.

The other placements in that world: `box_office_window` meta 4, 0, 0, 8 at `x -40..-37, y 4,
z 50` (WILL CALL, two BOX OFFICE, TICKETS); a gate blade meta 3 at `(-19, 8..9, 51)` under a
`venue_blade_bracket` meta 0 at `(-19, 10, 51)`, and a two-cell section blade meta 3 at
`(-17, 9, 51..52)` under two brackets; and the field corner south of the arena, a bleacher of
two 2-deep and four 1-deep rows facing north at `x -6..8, z 100..107` with its aisle at `x 1`,
the press box on a platform behind it, a `ticket_booth` meta 2 at `(12, 4, 100)`, and a monopole
at `(-10, 4..23, 104)` and a lattice tower at `(14, 4..23, 106)`, each with a
`stadium_light_head` meta 6 on top.

### The marquee's picture

`TileEntityMarqueeRenderer` draws the board's picture once, as one quad (and the text's one
display list) across the board inside the frame's lip, fullbright, with a second face for a
double-sided board, within 192 blocks plus the board's size (capped by the performance mode).
The **text** is the three lines centred between two thin rules on the background colour, each
line shrunk to fit across; the **video** is the centre-hung scoreboard's idle video
(`textures/blocks/venues/av/scoreboard_video.png`, 32 frames of 128 x 64), cropped to the
board's shape from its middle. In the cycle the switch is on world time, so every board in the
world turns over together. The controller finds the board's size by walking its bottom row and
left column, at most once a second.

### Field lighting

A lit stadium light head lights the ground in front of its tower with Core's light-up air
(`csm:lightupair`, the invisible light-15 block Lighting's fixtures project; see
`LIGHTING_SYSTEM.md`, "Light-Up Air in Core"). The head's own light 15 and its glowing lenses
only light the top of the tower.

**The footprint** is a square grid of columns, a column every 3 blocks, lying along the head's
facing:

| Head | Grid | Centre | Reaches (blocks out from the tower) | Light-up air |
|---|---|---|---|---|
| `stadium_light_head` | 9 x 9 | 16 out | 4 to 28, 24 across | 81 |
| `stadium_light_head_small` | 5 x 5 | 9 out | 3 to 15, 12 across | 25 |

Each column gets one light-up air in the air cell on the first surface below the head's bottom
row (up to 64 blocks down), so a stand roof, a net or the turf under it is lit, whichever comes
first. A column that already holds a light-up air, or has no surface, is skipped. Light 15 every
3 blocks leaves no cell of the grid darker than 13; past its edge the light falls away over about
12 blocks. Towers beside a field facing across it light it best (the ballpark below: four towers,
two each side, light the infield).

**Placing.** `TileEntityLightHead` (on the master, `tileentityvenuelighthead`) says where the
columns are and keeps the cells the head placed (`lpa`, offsets from the master) and the redstone
it last saw (`pw`). Core's `CsmLightFootprints` does the work at the end of each server tick:
the waiting heads round robin, a column or a cell at a time, at most 16 light changes, 512 steps
and 3 ms a tick, so many towers switched together come up (or go out) over a few seconds rather
than in one stalled tick. It never loads a chunk: a column in an unloaded one is skipped and
filled at the next heal; a placed cell in one stays on the head's list until it can be removed.
A head asks again when it is switched (click, or redstone at the master on the signal's edges),
when its chunk loads (the tile entity's `onLoad`; worlds with heads placed before the tile
entity existed get one from Core's backfill and light up as they load), when it is placed
(`onBlockAdded`, a tick late) and on random ticks, the same heal Lighting's fixtures have.

**Aiming.** By default a head's grid lies square to its facing, centred straight ahead. The
master's tile entity takes an `aim` (NBT, two whole numbers, blocks east and south of the head):
the grid's centre moves there and the grid turns to face along the line from the head to it, still
`n x n` columns every 3 blocks, each rounded to the nearest block. `aim:[I;0,0]` is straight
ahead again; each part is clamped to 96.

```
/blockdata 146 24 70 {aim:[I;13,18]}
/blockdata 146 24 70 {aim:[13,18]}
```

(Either form reads.) Re-aiming gives up the cells outside the new grid, through Core's
`CsmLightFootprints.reshaped` (handed on, left to a fixture or removed, as a broken head's are),
keeps the ones still in it and fills the new columns. The head's model still turns only four ways:
give it the facing nearest its aim. Two heads can be stacked on one tower, the second's master
three over the first's. "Lighting a field: aimed heads" under Football and soccer lights a whole
football field with six towers.

**Ownership.** The light-up air stores no owner, so a head only ever removes cells it placed.
Giving a cell up (switched off or broken), it hands it to another loaded, lit head whose own
column scan meets that cell (overlapping grids on the same 3-block lattice), leaves it to a lit
Lighting fixture above that relies on it (`ICsmLightupAirSource`: a street light in a footprint
keeps its light when the head goes off), and otherwise removes it. A street light turning off
removes the first light-up air in its column whoever placed it (Lighting's rule, unchanged); a
head that loses a cell that way fills it again at its next heal. A head broken while some of its
cells are in unloaded chunks leaves those behind.

**Cost**, measured 2026-10-31 in "Venues Arena Test" (dev client, all modules), switching eight
large heads together by redstone (648 cells), `/csm lightfootprints` and `server_tick_stats`:

| What | Ticks | Worst tick spent on it | Median server tick meanwhile |
|---|---|---|---|
| on (648 placed) | 118 (about 6 s) | 5.9 ms | 0.4 ms (p95 4.7) |
| off (648 removed) | 212 (about 11 s) | 6.3 ms | 3.5 ms |
| the switch itself (eight masters' own light 15) | 1 | 33-35 ms | |

Removing costs more than placing (darkening relights a wider volume). Before the time budget
was checked every step, a tick could take 16 changes and 27 ms. Nothing else is spent: an idle
lit head is a tile entity that does not tick and a random tick that rescans its 81 columns about
once a minute. The client relights each change too. `/csm lightfootprints [reset]` reports the
heads loaded, the work waiting and the worst tick.

### Costs

Measured 2026-10-31 in "Venues Arena Test" (dev client, all modules, 854 x 480, render
distance 12, frame cap lifted), standing in the plaza with both boards in view, the arena's
scoreboards behind them:

| What | How | Cost |
|---|---|---|
| a 14 x 5 single-sided marquee, video | `client_profile_rendering`, 6 s warm-up | 5.2 us a frame |
| an 8 x 3 double-sided marquee, text | the same | 7.0 us a frame |
| both, frame level | `/csm renderpass skip venueMarquee`, A/B/A | render work 1.02 / 1.02 / 1.00 ms: below what a frame measure resolves |

A board costs a quad or two and one list call whatever its size, a fifth of a centre-hung
scoreboard. The bleachers, press box, booth, poles and heads are chunk geometry: a head is the
heaviest model (its OBJ is a few hundred quads), drawn once per tower. Toggle for A/B:
`venueMarquee`.

### Traps

- **An unaimed head's footprint is square to its facing**, so it lights the field straight ahead
  of it. A corner tower needs an `aim` to light the middle.
- **A footprint light's chunk unloading does not invalidate its tile entity**, and its list is
  saved with the chunk: work done for it afterwards left light-up air nobody owned (seen when a
  build command loaded the towers' chunks only for its own run). Core drops such a light's work;
  it asks again when it loads.
- **Switching a head is itself a light update.** The master's own light 15 at the top of a
  20-block tower relights a large volume: about 4.5 ms of server tick a head, at once, whatever
  the footprint does. That was so before the footprint; eight heads on one redstone line cost
  about 36 ms in the tick they switch.
- **The marquee's controller is the bottom-left as the front's reader sees it**, which on a
  board facing south is its west end and facing north its east end. A controller anywhere else
  draws a picture on the cells to its right and above it only.
- **A ragged board** (a cell outside the controller's row and column rectangle) is a dark
  cabinet with its own frame; only the rectangle shows the picture.
- **The gate blade's letter is fitted with GATE**: Core's panel scales every line together to
  the widest, so on a one-block blade the letter is about a third of a block. A two-wide blade
  gives a letter twice the size.
- **The press box and box office window are units two blocks tall**, the master below: a
  `/setblock` of the master fills the part above if it is free, and the item will not place one
  under a block. A press box corner's return wall has no collision above the master.
- **The press box glass leans out past its cell** (to z -6.5 at the fascia), so a block directly
  in front of a module's upper half shows through it.
- **The bleacher's plank and rails reach into the cell above** (collision up to 1.9 blocks), as
  a fence's does; a block placed in that cell overlaps them.
- **The booth's TICKETS band and the box office headers are 64 px textures with the legend in
  their top rows**, mapped by explicit UVs (`SIGN_ROWS`, `HEADER_UV`); change the drawing and the
  mapping together.

## The ballpark (Ballpark tab)

Baseball and softball. Written by `dev-env-utils/scripts/gen_venues_ballpark.py` (`--check`
holds `BallparkLayouts.java` too; `--fragments` prints the tab; `--layout baseball|softball`
prints a whole field's blocks as JSON, home plate's cell at (0, 0) and the field to the north;
`--doc` dumps the anchors, the base offsets and the block census). The field itself is one
geometry, `dev-env-utils/scripts/ballpark_field.py` (`--analyse`, `--plan`, `--count` for a
developer): every line, circle and region of a full-size diamond as predicates on world points.
Everything a field cell shows is cut from that one drawing, so a plain block, a standard piece
and a painted picture can never disagree where they meet.

### The scale, and how a diamond lies on the grid

**One block is a metre.** A 90 ft base path is 27.43 blocks, a 60 ft one 18.29. A diamond is laid
with its **centre line along the grid**: home plate at the south, the pitcher and second base
due north of it, centre field beyond. The base paths and the foul lines therefore run at 45
degrees, which is why the field needs diagonal pieces at all (a diamond laid with its base
paths along the grid would put the centre line on the diagonal instead, and the outfield wall
too).

In the canonical frame (x east, z south, blocks; home plate's cell is (0, 0)) home plate's back
point is at **(0.5, 1.0)**: the middle of the south edge of its cell. The foul lines are then
`x + z = 1.5` (first base) and `x - z = -0.5` (third base). Both are half-integers: a 45 degree
chalk line crosses every cell it meets through two mid-edges, so it is a band across one corner
of each cell, two cells a step, and never narrows to a point at a block corner. Surface edges at
45 degrees are on whole diagonals instead (`x + z = 0`, `x - z = 37`, ...), so they split a cell
corner to corner.

### State counts

| Block | Stored (metadata) | Actual | States |
|---|---|---|---|
| `ballpark_turf` | facing, kind (3) | | 12 |
| `ballpark_clay`, `ballpark_field_edge`, `ballpark_track_edge`, `ballpark_chalk_turf`, `ballpark_chalk_clay` | facing, kind (4) | | 16 each, 80 |
| `ballpark_marking_anchor`, `_third`, `softball_marking_anchor`, `_third` | facing, kind (4) | | 16 each, 64 |
| `ballpark_arc_anchor`, `_third` | facing, kind (3) | | 12 each, 24 |
| `ballpark_painted` | | cell (329), rot (4) | 1,316 |
| `pitchers_mound` | facing | | 4 |
| `pitchers_mound_slope` | | cell (38), rot (4) | 152 |
| `home_plate` | facing, variant (2) | | 8 |
| `baseball_base`, `softball_base` | facing, variant (4) | | 16 each, 32 |
| `pitchers_rubber`, `dugout_helmet_rack`, `dugout_bat_rack`, `dugout_water_cooler`, `ballpark_net_roof`, `pitching_machine` | facing | | 4 each, 24 |
| `dugout_bench`, `dugout_rail`, `dugout_canopy` | facing | end_l, end_r | 16 each, 48 |
| `ballpark_net` | facing, pole | top, end_l, end_r | 64 |
| `ballpark_net_diagonal` | facing, pole | top | 16 |
| `outfield_wall`, `outfield_wall_diagonal` | facing | top | 8 each, 16 |
| `outfield_wall_marker` | facing, diagonal | top | 16 |
| `foul_pole` | facing, wing | base, cap | 32 |
| `ballpark_scoreboard`, `ballpark_count_board` | facing | | 4 each, 8 |

1,916 states, of which the painted cells are 1,316: one per distinct 32 px picture (214 of
them, plus 115 mirrored copies for the third base side) and turn, each a plain cube with one top
texture. That is the arena's painted floor's pattern, at about twice the court's size; the
pictures cost 214 small sprites and no geometry worth counting.

### Field surfaces and standard pieces

All are `BlockArenaFloor` with `Orient.FACING`: **metadata `facing | kind << 2`**, facing S 0,
W 1, N 2, E 3. A piece is drawn facing north; the facing turns it. By hand it faces the way the
placer looks, and a sneak-click with an empty hand steps the kind. The surfaces' grain has no
direction, so a turned or mirrored cell meets its neighbours unseen.

| Block | Kinds | Facing north draws |
|---|---|---|
| `ballpark_turf` | 0 light stripe, 1 dark stripe, 2 split | 2: dark on the north-west half, light on the south-east (diagonal mowing) |
| `ballpark_clay` | 0 infield skin, 1 dragged skin, 2 packing clay (mound, plate), 3 warning track | 1: drag grooves north-south |
| `ballpark_field_edge` (turf and skin) | 0 diagonal, 1 half, 2 quarter, 3 quarter inverted | 0: turf north-west of the diagonal from the south-west corner to the north-east one; 1: turf on the north half; 2: turf in the north quarter (the triangle to the cell's middle); 3: skin there, turf elsewhere |
| `ballpark_track_edge` (turf and warning track) | 0 diagonal light, 1 diagonal dark, 2 half light, 3 half dark | as the field edge's 0 and 1 |
| `ballpark_chalk_turf` | 0 band light, 1 band dark, 2 middle light, 3 middle dark | band: a chalk line across the north-west corner (`u + v = 0.5`); middle: a line down the middle, north-south |
| `ballpark_chalk_clay` | 0 band on skin, 1 band on track, 2 middle on skin, 3 middle on track | as above |

So a foul line is a staircase of band pieces, alternately cutting the corner on one side and the
other: in the canonical field, first base line cells with `cx + cz = 0` take facing south (meta
0) and those with `cx + cz = 1` facing north (2); on the third base line, `cz - cx = 0` facing
west (1) and `cz - cx = 1` facing east (3). `--layout` lists every one.

### Painted markings: anchors and painted cells

What is not a plain block or a standard piece is a picture laid out round an **anchor**, as on
the arena's floor: a `ballpark_painted` cell finds the nearest anchor within **7 cells** each way
on its level that draws something at its cell and shows that piece, turned with the anchor.
There are three anchor blocks, each with a **third base side twin** (`_third`) that draws the
same kinds **mirrored**:

| Block | Kind 0 | 1 | 2 | 3 |
|---|---|---|---|---|
| `ballpark_marking_anchor` (`_third`) | baseball home plate area | base cutout (first; third on the twin) | coach's box (first; third on the twin) | on-deck circle |
| `ballpark_arc_anchor` (`_third`) | infield arc, centre | infield arc, middle | infield arc, end | |
| `softball_marking_anchor` (`_third`) | softball home plate area | pitcher's circle | softball infield arc, centre | softball infield arc, end |

Anchor metadata is `facing | kind << 2`; facing is the field's (towards centre field), so on a
field laid north every anchor is meta 2, 6, 10 or 14. The home area and the cutouts own every
cell of their circle's square, plain ones included; the arc segments their arc's cells and a
band a cell wide beside it; the coach's box, on-deck circle and pitcher's circle just the cells
their chalk touches, which is what lets them travel to any field. Fill a picture's cells with
`ballpark_painted` (or fill a little wider: a cell no anchor draws is plain skin).

The anchor's tile entity is the arena anchor's (no data, no tick): it asks for the painted cells
within ten blocks to be drawn again when it is loaded, changed or removed.

### The pitcher's mound

`pitchers_mound` is the rubber's cell; `pitchers_mound_slope` cells within three blocks of it
find it and show their cell of the mound, a heightfield OBJ: the 18 ft circle of packing clay
in the infield grass, the 5 ft x 34 in table 10 in up (4 px) with the rubber set in it, the
table's front sloping towards home, the rest rounding down to the circle. Each cell's collision
rises to its highest point (a player stands on the table at 0.31 of a block). Mound metadata is
the field's facing (2 on a field laid north); the slope stores nothing. The mound's middle is
59 ft from home's point and the rubber's front 60 ft 6 in, so the rubber's cell is **(0, -18)**
and the mound's 38 cells are x -3..3, z -20..-15 round it (`--layout` lists them).

### Fixtures

| Block | Metadata | Notes |
|---|---|---|
| `home_plate` | `facing \| variant << 2`; facing the field's | The 17 in pentagon on a black rubber edge. Variant 0: its point on the cell's back (south) edge, the field's home; 1: centred (a bullpen or a cage). Stands in the air cell over the home area anchor |
| `baseball_base` | `facing \| variant << 2` | A 15 in bag, turned 45 degrees to the base paths. Variant 0 first, 1 second, 2 third base: placed off the cell's middle to where the 90 ft diamond puts the bag (first (10.0, 9.6) px in its cell, second (8, 3.3), third (6.0, 9.6), facing north); 3 centred and square to the cell |
| `softball_base` | as above | The 60 ft diamond's bags: first (2.6, 1.1), second (8, 2.2), third (13.4, 1.1); first is the double base, its orange half in foul ground |
| `pitchers_rubber` | facing | 24 x 6 in, flush; the mound has its own, this is for a flat practice area |
| `dugout_bench`, `dugout_rail`, `dugout_canopy` | facing (towards the field) | Runs: side by side they join, ends drawn by actual state. The bench is sat on with a click; the rail is a padded top rail to lean on; the canopy a roof deck at the top of its cell with a padded fascia at the front |
| `dugout_helmet_rack`, `dugout_bat_rack`, `dugout_water_cooler` | facing | Against the wall behind (+z). Decor |
| `ballpark_net` | `facing \| pole << 2` | A net down the middle of its cell, across its facing. Side by side and stacked it is one net; poles stand at a run's ends and wherever bit 2 is set (the panel's left edge, looking the way it faces); the top panel draws the head cable. Backstops, netting over a dugout, batting cage walls |
| `ballpark_net_diagonal` | `facing \| pole << 2` | Across its cell corner to corner, the dasher corner's convention (facing north looks north-east) |
| `ballpark_net_roof` | facing | A batting cage's roof, at the top of its cell; no collision |
| `outfield_wall` | facing (towards the field) | A wall block with dark green padding; stack two for an 8 ft wall. The top block draws the yellow home run line |
| `outfield_wall_diagonal` | facing | Across its cell: facing south looks south-west (a right field wall), facing east south-east (left field). Its back is a smooth band, a little past its cell |
| `outfield_wall_marker` | `facing \| diagonal << 2` | A wall block with a painted distance: NBT `txt` (up to five characters, default 400), `fc` (colour, `VenueColours`, white); a click opens the editor. The number is about 1.4 blocks tall, centred on the block's top edge: use it as the lower block of a two-high wall |
| `foul_pole` | `facing \| wing << 2` | Stack it; a footing where nothing of the pole is below, a cap at the top. Bit 2 puts the yellow mesh wing, a block and a half long, on the facing side: set it on the blocks above the wall, the wing reaching into fair territory |
| `ballpark_scoreboard` | facing (towards the reader) | A unit 12 x 5 on two legs 3 tall; the master is the left leg's foot, looking at the board (the board reaches one block to the master's left and ten to its right) |
| `ballpark_count_board` | facing | A unit 3 x 2 for a wall or fence; the master is the bottom left cell, looking at it |
| `pitching_machine` | facing (the way it throws) | Decor |

### The scoreboards

The line score: the inning numbers (the last nine played if there are more), each team's name on
a bar of its colour with its runs by inning in squares and **R** (summed from the innings),
**H** and **E** in yellow, a lamp beside the batting team, and along the foot the ball (green),
strike and out (red) lamps and the last pitch's speed. The count board: the three rows of lamps
and the speed. The casing is baked; `TileEntityBallparkScoreboardRenderer` draws the squares and
lamps (a few coloured quads) and every glyph (one display list a board, compiled when a value
changes), fullbright, within 192 blocks.

A click opens the venue text editor (`GuiVenueText`, which lays out more than three lines with
their captions beside them): the teams, each team's runs by inning as numbers between spaces
(`X` for a home half not played, `-` for an empty square), `H E H E`, `B S O`, the speed, and
buttons for the two colours and who is batting. The count board's editor has the count and the
speed. NBT on the master:

| Key | Meaning |
|---|---|
| `away`, `home` | up to 10 characters (AWAY, HOME) |
| `ai`, `hi` | runs by inning, up to twelve: `"0 1 0 0 2"` |
| `ah`, `ae`, `hh`, `he` | hits and errors, 0-99 |
| `b`, `s`, `o` | balls 0-3, strikes and outs 0-2 |
| `mph` | 0-199, 0 shows nothing |
| `ac`, `hc` | colours (`VenueColours`; navy, crimson) |
| `bat` | 0 away, 1 home |

```
/blockdata 5 4 172 {away:"VISITORS",ai:"0 1 0 0 2 0 0",hi:"1 0 0 3 0 0",ah:7,ae:1,hh:9,b:2,s:1,o:1,mph:94,bat:1}
```

### A worked diamond: where everything goes

Relative to home plate's cell (0, 0), the field laid north (the field's facing, meta 2 on every
anchor). The ground is one layer; fixtures stand in the layer above. `--layout` gives every cell.

| What | Baseball (90 ft) | Softball (60 ft, 43 ft pitching) |
|---|---|---|
| Home plate area anchor | (0, 0) `ballpark_marking_anchor` 2, `home_plate` 2 above | (0, 0) `softball_marking_anchor` 2, `home_plate` 2 above |
| The home area | the 13 ft dirt circle, cells -4..4 each way | all skin; the boxes, cells -5..5 |
| Batter's boxes | 4 x 6 ft, 6 in off the plate, in the home picture | 3 x 7 ft |
| Catcher's box | 43 in wide, 8 ft back | 8 ft 5 in wide, 10 ft back |
| Foul lines | from the batter's box's front (1.73 m along the line) to the wall; band pieces on `x + z` 0/1 and `z - x` 0/1 | the same, from 1.95 m, out to the fence |
| First base | `baseball_base` 2 at (19, -19); cutout anchor `ballpark_marking_anchor` 6 there | `softball_base` 2 at (13, -12) |
| Second base | `baseball_base` 6 at (0, -38) | `softball_base` 6 at (0, -25) |
| Third base | `baseball_base` 10 at (-19, -19); cutout anchor `ballpark_marking_anchor_third` 6 there | `softball_base` 10 at (-13, -12) |
| Base paths | a 2.1 m skin strip on `x + z` 0..3 and `x - z` -2..1, edges `ballpark_field_edge` 0 | inside the skin |
| Infield grass | `x + z <= 0`, `x - z >= 1`, `x - z <= 37`, `x + z >= -36`, less the circles | none |
| Pitcher | `pitchers_mound` 2 at (0, -18), `pitchers_mound_slope` round it | pitcher's circle anchor `softball_marking_anchor` 6 at (0, -13), plate painted |
| Infield arc (skin to grass) | 95 ft from the rubber's front; `ballpark_arc_anchor` 2, 6, 10 at (7, -46), (22, -36), (27, -28) and the `_third` twins at (-7, -46), (-22, -36), (-27, -28) | 60 ft from the plate's front; `softball_marking_anchor` 10, 14 at (7, -30), (17, -20), twins at (-7, -30), (-17, -20) |
| Coach's boxes | `ballpark_marking_anchor` 10 at (22, -12), `_third` 10 at (-22, -12) | the same blocks at (16, -4), (-16, -4) |
| On-deck circles | `ballpark_marking_anchor` 14 at (12, 0) and (-12, 0) | the same |
| Warning track | 5 blocks inside the wall and along the sides (`x >= 67`, `x <= -67`), behind home from `z >= 10` | |
| Outfield wall | two high: straight at x 72 (z -77..-66) and x -72, 45 degree from (71, -78) to (28, -121) and its mirror, straight at z -122 (x -27..27): 330 ft at the lines, 375 in the alleys, 400 to centre | (none laid; a 200-220 ft fence the same way) |
| Markers | `outfield_wall_marker` 330 at (72, -74) and (-72, -74), 375 diagonal at (44, -105) and (-44, -105), 400 at (0, -122) | |
| Foul poles | at (73, -73) and (-73, -73), behind the wall, 18 high, the wing on the blocks over the wall | |

The cut-off softball picture uses baseball's coach's box: its box sits seven cells nearer home
and one further out than baseball's, so its anchor lands on the same fraction of a cell.

In "Venues Arena Test" the baseball field's home plate cell is at **(0, 3, 300)** (ground y 3),
the field running north; round it: the backstop (a padded wall and nets four high at z 313,
returns at x -11 and 11, the count board on it), dugouts at x 17..20 and -20..-17 (z 296..305),
a bleacher of four rows at x 23..26, the bullpen in right field foul ground (plate at (50, 4,
270), its mound at (50, 3, 252)), a batting cage at x -51..-47, z 269..282 with a pitching
machine, and the line score at (5, 4, 172) facing home over the centre field wall. Four light
towers light the infield, each 20 shafts high under a `stadium_light_head`: monopoles at
`(-30, 4, 292)` facing east (head meta 7) and `(30, 4, 292)` facing west (meta 5), lattice towers
at `(-36, 4, 262)` meta 7 and `(36, 4, 262)` meta 5; the outfield is left dark.

### Costs

The field is chunk geometry and atlas sprites: 214 pictures, 38 mound textures and about 25
plain and piece textures at 32 px (about 1.1 MB of atlas), a cube each (the mound cells an OBJ of
64 quads). A painted cell reads at most 225 blocks to find its anchor, and only when its chunk
section is built; a field has about 250 of them. The renderers are the line score and count
boards (a list call and a few dozen quads each) and the distance markers (a list call each);
toggle `venueBallpark` for A/B.

### Traps

- **A painted cell's reach is seven blocks, the render cache's sixteen.** A picture farther from
  its anchor would read air in the chunk cache a section is built from, so the infield arc, 29 m
  from the mound, is three anchors a side rather than one centre.
- **Pictures are drawn in the light mowing tone.** Stripe the outfield from three blocks past
  the arc's top, or the light grass in the arc's cells shows against a dark stripe.
- **The bases are placed off their cell's middle** to the 90 ft (or 60 ft) diamond laid as above,
  turned 45 degrees: on any other layout use variant 3 (centred, square).
- **The chalk bands depend on the half-integer foul lines.** Lay home plate's point on a cell's
  edge middle as above, or the foul lines will not fall on the band pieces.
- **The mound's cells are full blocks with their top above them** (up to 5 px): a block placed
  on a mound cell sits on the cell's top face, not on the mound.
- **The 45 degree wall's back reaches past its cell**, square to its face at the corners, so a
  run is smooth front and back; the straight wall meets it at the corner with a small notch at
  the back.
- **The scoreboard's text is in front of its squares by 0.03 px** towards the reader; flip the
  sign and the numbers vanish behind the squares.

## Football and soccer (Football & Soccer tab)

American football and soccer at full size, one block a metre. Written by
`dev-env-utils/scripts/gen_venues_football.py` (`--check` holds `FieldLayouts.java` too;
`--fragments` prints the tab; `--layout football|soccer` prints a whole field's blocks as JSON,
and with `--commands X Y Z [--east] [--zone N]` as `/fill` and `/setblock` lines for an
mcfunction; `--doc` dumps the anchors and the block census). The fields themselves are one
geometry, `dev-env-utils/scripts/football_field.py` (`--analyse`), which borrows
`ballpark_field.py`'s resolution and picture key: every line, number, arrow and arc painted onto a
label canvas three samples a texel, and every cell's picture cut from it.

### The two fields

| | Football (pro) | Soccer |
|---|---|---|
| Size | 120 x 53 1/3 yd (109.7 x 48.8 m), a 6 ft white border round it | 105 x 68 m, inside the lines |
| Centre (canonical frame) | the middle of cell (0, 0): the 50 yard line runs north-south through column 0, the field's length east-west | (0.5, 0.0): the halfway line through column 0, the touchlines on the block edges z = -34 and 34, the goal lines on x = -52 and 53 |
| Lines | 4 in (drawn 4 texels), goal lines 8 in, in the end zone | 12 cm |
| Markings | yard lines every 5 yd; hash marks every yard 70 ft 9 in from the sidelines (2 ft long); 2 ft yard ticks 8 in inside the sidelines; numbers every 10 yd, 6 ft x 4 ft, each row read from the sideline nearest it (bottoms 7 yd in from it, tops 9 yd, towards the middle), with direction arrows beside the tops pointing to the nearer goal; the 2 yard marks; the end zones in the end zone colour; the midfield logo area left plain | halfway line, centre circle (9.15 m) and spot, penalty areas (16.5 x 40.32 m), goal areas (5.5 x 18.32 m), penalty spots (11 m) and arcs, corner arcs (1 m), to the Laws of the Game |
| Mowing | 5 yd stripes from goal line to goal line, each edge under a yard line; end zones light | 5 m stripes across the pitch on block edges, the middle one centred on the halfway line, the goal line and halfway columns light |
| Extent laid | cells x -57..58, z -26..27 | cells x -54..55, z -36..36 |

Both are 180 degree symmetric about their centre, which is what lets one goal post model and one
soccer goal model serve both ends.

### How the sub-block lines work

Five yards are 4.572 m, so a football field's lines do not fall on block edges; a soccer pitch's
.5 and .16 m offsets do not either. Rather than line blocks with an offset kind (which would need a
block, an offset and an axis for every combination of line, hash and number edge in a cell), every
cell that is not one plain surface is a **picture**: its 32 x 32 coverage of turf, white paint and
end zone, drawn where everything really falls, snapped only to the texel (1/32 of a block, 3 cm).
A cell can hold a yard line and a hash a yard away, a line and the edge of a number, the corner of
the border: the census simply finds each distinct picture. Hash marks and yard ticks are drawn
with their centres on every second texel from the field's centre (a texel out at most), which
halves their pictures and keeps the far side's the near side's turned.

The pictures are drawn **once, in the light tone**. A picture's top is cut into rectangles by
tint class, each a window of the one texture, and the tints come from the blocks beside it
(`FieldTint`):

| Tint index | Part | Colour |
|---|---|---|
| none | off the field: the border, the surround | as drawn |
| 0 | on the field, one tone | the nearest `field_turf` 0 or 1 along the stripe (the picture's north-south axis turned with the cell, up to 24 cells each way): light as drawn, dark a multiplier |
| 1, 2 | on the field in a cell a stripe edge crosses (under a 5 yard line): the parts west and east of the edge, in the picture's frame | the tone found along the stripe in the column beside the cell on that side |
| 3 | the end zone | the colour of a `football_end_zone` among the cell's eight neighbours (grass if none) |

So the white lines in a dark stripe are a little darker, as painted grass is, a picture serves
both stripes and any of the sixteen end zone colours, and the cell under a yard line takes each
stripe's tone on its own side of the line. Pictures that are a half turn of one another are stored
once (the turn is in the anchor's table): **173 football pictures and 81 soccer pictures**.
Pictures are only stored unturned or turned 180 degrees, never a quarter, so a stored picture's
stripe edges always run along its own north-south axis, the axis its tint looks along.

### Anchors and painted cells

As on the ballpark, a **painted cell** (`football_painted`, `soccer_painted`, metadata 0) looks
round itself, nearest ring first, for a **marking anchor** of its sport on the same level and shows
the picture the anchor's table gives at its offset, turned with the anchor. Each field has
**15 anchors on a regular grid**, each owning the picture cells nearest it within **12 cells**
(the tables are disjoint, so only the owner draws a cell); every anchor is a kind of its own:

| Block | Kinds | Metadata |
|---|---|---|
| `football_marking_anchor` | 0-7 | bit 0: the field runs north-south (the anchor's forward east rather than north); bits 1-3 the kind |
| `football_marking_anchor_2` | 8-14 | as above, kind - 8 |
| `soccer_marking_anchor`, `soccer_marking_anchor_2` | 0-7, 8-14 | as above |

`--layout` places them; a sneak-click with an empty hand steps the kind (the action bar says
which). The anchor's tile entity is the arena anchor's, its redraw reach widened to 13 for these.

### Plain blocks

| Block | Metadata |
|---|---|
| `field_turf` | 0 light stripe, 1 dark stripe (a tint), 2 white paint (the football border), 3 the surround: the same light turf, but a painted cell never takes its tone from it, so a touchline's cells take the field's stripe rather than the grass beyond. Lay it outside the lines |
| `football_end_zone` | the end zone's colour, a venue colour 0-15 (navy 0, the scoreboard's list) |

A sneak-click with an empty hand steps either.

### Worked layouts

```
py -3 dev-env-utils/scripts/gen_venues_football.py --layout football --commands 200 3 100 > football.mcfunction
py -3 dev-env-utils/scripts/gen_venues_football.py --layout soccer --commands 200 3 250 > soccer.mcfunction
```

(The centre cell's position, at the ground's level; `--east` lays the field north-south, the
anchors' bit 0 set and every fixture's facing turned; `--zone 9` makes the end zones crimson.)
Copy them to `<save>/data/functions/csm/`, `/reload`, raise `maxCommandChainLength` above the
football field's 2,308 commands, and `/function csm:football` with the chunks loaded.
The football field is 1,950 painted cells, 3,550 `field_turf`, 749 end zone cells and 15
anchors; the soccer pitch 736 painted cells and 7,279 turf. The fixtures it places, in the
canonical frame (centre cell (0, 0), one block over the ground):

| What | Football | Soccer |
|---|---|---|
| Goals | `football_goal_post` 3 at (-57, 0) and 1 at (57, 0) | `soccer_goal` 3 at (-52, 0) and 1 at (52, -1) |
| Corners | `end_zone_pylon` at the four corners of each end zone, outside the sidelines | `soccer_corner_flag` 3 / 1 at (-52, -34), (52, -34), (-52, 33), (52, 33) |
| Sideline | `football_team_bench` 2, x -17..-5 and 6..18 at z 32, a heater at (-18, 32) and a fan at (19, 32) | `soccer_substitutes_shelter` 2, x -14..-5 and 6..15 at z 37; `fourth_official_board` 2 at (0, 36) |
| Chain crew | north sideline, z -27: stakes 0 on the home 35 and 45, the chain 1 between, the down box 8 (third down) | |
| End zone | `football_end_zone_lettering` 3 HOME at (-50, 0), 1 AWAY at (50, 0) | |

In "Venues Arena Test" the football field's centre cell is (200, 3, 100) and the soccer pitch's
(200, 3, 250), each with its scoreboard behind the east end (`football_scoreboard` 1 at
(262, 4, 95), `soccer_scoreboard` 1 at (262, 4, 245)), and the football field is lit by six
lattice towers, two heads each (below).

### Goals

| Block | Metadata | Notes |
|---|---|---|
| `football_goal_post` | facing: towards the field | A unit, OBJ: the base post (padded to 6 ft) 6 ft behind the uprights' plane, rising and curving forward as a gooseneck to the crossbar's middle; crossbar 10 ft up, 18 ft 6 in between the uprights' insides, uprights 35 ft over the bar (13.7 m to the top) with wind ribbons. The master is the base post's cell; on the canonical field it is cell x -57 (the base 0.81 into it), which puts the uprights' inside on the end line's inner edge. Parts: the post, the gooseneck, the crossbar and both uprights' columns (32 cells) |
| `soccer_goal` | facing: towards the pitch | A unit, OBJ: 12 cm posts and bar, 7.32 x 2.44 m inside, the posts on the goal line (the line's 12 cm), the net's roof 1 m back and its foot 2 m back on a ground frame. Its middle is the master cell's west edge in its own frame, a block edge: on the canonical pitch the west goal's master is (-52, 0) facing east, the east goal's (52, -1) facing west |
| `soccer_goal_small` | facing | A portable small-sided goal, 12 x 6 ft, centred on its master, the posts at its front edge |

The nets are a 32 px mesh texture in the cutout layer, cut into a quad a block so it repeats
(an OBJ's uv may not leave 0..1).

### The sideline

| Block | Metadata | Notes |
|---|---|---|
| `end_zone_pylon` | facing | 4 in x 18 in orange foam; no collision |
| `football_team_bench` | facing (the way the players face) | A run, as the dugout bench: aluminium planks and a backrest; sat on with a click |
| `sideline_heater` | facing | A propane cylinder, mast, burner and reflector, 1.9 blocks tall |
| `sideline_fan` | facing | A misting drum fan on a wheeled stand |
| `football_down_box` | `facing \| (down - 1) << 2` | The down on a box on a pole; a sneak-click steps the down |
| `football_chain_stake` | facing | A pole with a red head |
| `football_chain` | facing: the chain runs along its axis | Lying on the ground, no collision |
| `soccer_corner_flag` | facing: the way the flag flies | A 1.5 m post and a red and yellow flag; no collision |
| `soccer_substitutes_shelter` | facing | A run two blocks tall: seats against a back panel under a clear roof, clear end panels where the run stops; sat on with a click |
| `fourth_official_board` | facing | The board on a stand, +3 lit; decor |
| `football_end_zone_lettering` | facing: towards the field | A marker in the air cell over the end zone's middle; its renderer paints the words flat across the end zone, the letters' bottoms towards the goal line (read from the field of play, as most real end zones are; the yard numbers instead read from their nearer sideline), lit as the air over the field (the stadium lights light them). No model in the world |

**End zone lettering NBT**: `txt` (24 characters, HOME), `h` the letters' height in blocks (1-9,
5), `w` the widest they may run (4-48, 40; they shrink to fit), `fc` their colour, `oc` an
outline's (16 none). A click opens the venue text editor.

```
/setblock 150 4 100 csm:football_end_zone_lettering 3
/blockdata 150 4 100 {txt:"WOLVES",h:6,fc:14,oc:6}
```

### The scoreboards

`football_scoreboard`, `soccer_scoreboard`: the ballpark line score's casing (12 x 5 on two legs
3 tall, the master the left leg's foot, looking at the board; metadata the facing, towards the
reader), its own layout: each team's name on a bar of its colour and its score in amber, the clock
in a panel in the middle with QTR or HALF under it; football adds three time-out lamps under each
score, a possession lamp beside the team with the ball and DOWN, TO GO and BALL ON along the foot;
soccer adds ADDED TIME +n. A click opens the venue text editor (the teams, the score, the period,
the clock, down/distance/ball on or the added minutes, time-outs, colours, possession); a
sneak-click with an empty hand starts or stops the clock, as does a rising redstone edge at any
of its cells. The clock never ticks: football's counts down, soccer's up, from the world's time, as
the centre-hung board's does. A running clock is not set from the editor (stop it first).

| Key | Meaning |
|---|---|
| `home`, `away` | up to 10 characters |
| `hs`, `as` | scores, 0-999 |
| `per` | the quarter or half, 3 characters |
| `clk` | tenths: football's time left (default 9000, 15:00), soccer's time played (0) |
| `run`, `t0` | running (set without `t0` it starts now), the world time it started |
| `dn`, `dist`, `ball` | football: the down 0-4 (0 none), the distance (5 characters: `10`, `GOAL`), the ball on (6: `35`, `OWN 20`) |
| `hto`, `ato`, `pos` | football: time-outs left 0-3, possession 0 none 1 home 2 away |
| `add` | soccer: added minutes 0-30 |
| `hc`, `ac` | team colours (navy, crimson) |

```
/blockdata 262 4 95 {home:"WOLVES",away:"BIRDS",hs:21,as:17,per:"3",clk:4235,dn:3,dist:"7",ball:"35",hto:2,ato:3,pos:1}
/blockdata 262 4 245 {home:"CITY",away:"ROVERS",hs:2,as:1,per:"2",clk:43800,add:4,hc:4,ac:12,run:1b}
```

### Lighting a field: aimed heads

A stadium light head's footprint can be aimed (Field lighting, above: `aim`). The football field
in "Venues Arena Test" is lit by six `stadium_light_lattice` towers 20 high, three each side 30
blocks out from the middle of the sidelines' borders, each with two `stadium_light_head`s, the
second stacked on the first (its master three over the first's, on its top row):

| Tower | Heads (meta) | `aim` of the lower head, the upper |
|---|---|---|
| (146, 4, 70), (200, 4, 70), (254, 4, 70) | 4 (facing south, lit) | (13, 18), (40, 30); (-13, 18), (14, 18); (-12, 18), (-40, 30) |
| (146, 4, 131), (200, 4, 131), (254, 4, 131) | 6 (facing north, lit) | the same with z negated |

The corner towers' lower heads light their end of the field diagonally, their upper heads the
middle from the corner; the middle towers split their half between two heads. Twelve heads light
the whole field and its end zones: 940 light-up air for about 6,600 square metres, placed over
127 server ticks (about six seconds), the worst tick spending 5.7 ms on it (`/csm
lightfootprints`, re-aiming all twelve at once).

### State counts

| Block | Stored (metadata) | Actual | States |
|---|---|---|---|
| `field_turf` | kind (4) | | 4 |
| `football_end_zone` | colour (16) | | 16 |
| `football_marking_anchor`, `soccer_marking_anchor` | east, kind (8) | | 16 each, 32 |
| `football_marking_anchor_2`, `soccer_marking_anchor_2` | east, kind (7) | | 14 each, 28 |
| `football_painted` | | cell (173), rot (4) | 692 |
| `soccer_painted` | | cell (81), rot (4) | 324 |
| `football_goal_post`, `soccer_goal`, `soccer_goal_small` | facing | | 4 each, 12 |
| `end_zone_pylon`, `football_chain_stake`, `football_chain`, `sideline_heater`, `sideline_fan`, `soccer_corner_flag`, `fourth_official_board` | facing | | 4 each, 28 |
| `football_down_box` | facing, down (4) | | 16 |
| `football_team_bench`, `soccer_substitutes_shelter` | facing | end_l, end_r | 16 each, 32 |
| `football_end_zone_lettering` | facing | | 4 |
| `football_scoreboard`, `soccer_scoreboard` | facing | | 4 each, 8 |

**1,196 states**, of which the painted cells are 1,016: fewer than the ballpark's 1,316 for two
whole fields, because the tone and the end zone colour are tints (drawn into the pictures they
would have about tripled them) and a half turn is free. The pictures cost 254 small sprites
(32 px, about 1 MB of atlas) and a few quads a cell.

### Costs

The fields are chunk geometry: a plain cell is a cube, a painted cell a soil body and one to
three tinted top faces. A painted cell reads at most 625 blocks to find its anchor (radius 12;
most find it within six rings, about 170 reads), and up to 49 for each tinted part's tone (most
find turf a block or two away), only when its chunk section is built. The renderers are the two
scoreboards (a list call, a few dozen quads and the clock's few glyphs a frame) and the end zone
lettering (two to nine copies of a short string a frame); toggle `venueField` for A/B. The aimed
heads cost what unaimed heads do (Field lighting).

### Traps

- **Lay the surround as `field_turf` 3, not 0.** A touchline or sideline cell looks along the
  stripe both ways for its tone; light turf off the field would be found as soon as the stripe.
- **A painted cell's tone is read only when its section is built.** Changing a stripe's turf
  redraws its own section and the cells next to it; a painted cell further along the stripe in
  another section keeps its old tone until that section is built again.
- **The football field's centre is a cell's middle, the soccer pitch's a cell's west edge.**
  `--layout` gives every cell; the goals' masters follow from it (the soccer goals are not mirror
  images on the grid: (-52, 0) and (52, -1)).
- **The goal post is drawn by its master**, 13.7 blocks up and 1.8 back: with the master's chunk
  section out of view (looking straight up at the uprights from beside the base across a section
  line) the whole post is culled with it, as the basketball stanchion is.
- **The heads' models still turn only four ways.** Aim a head whose facing is nearest the line
  to its target; the light follows `aim`, the floodlights' look does not.
- **Stacked heads overlap**: two heads on one tower placing light-up air in the same column share
  it (the first placed keeps it, and hands it over when it goes dark).
- **A sprite must be square** (the fourth official's board once showed the missing texture: its
  32 x 16 face read as a broken animation).

## Brands

None. Team, league and sponsor names are what players type into the signs, the scoreboard and
the banners (HOME and AWAY by default). The idle video is an invented intro and a noise meter;
the ribbon's crawl reads LET'S GO. The concourse's signs say what is sold (HOT DOGS,
COLD BEER, DRAFTS, SOFT SERVE), never whose; the box office headers say BOX OFFICE, WILL CALL
and TICKETS, and the marquee shows what is typed into it. The ice resurfacer
is a generic machine with no maker's name; the scorer's table plays an abstract pattern. The
ballpark's line score says AWAY and HOME until typed into; its helmets, bats, water cooler and
pitching machine carry no maker's name. The football and soccer scoreboards say HOME and AWAY,
the end zones HOME and AWAY until typed into; the goals, heater, fan and boards carry no maker's
name.

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
