# Sports & Venues

Every Venues generator, the line art engine and the Venues demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_venues_common.py` -- what the Sports & Venues generators share (not run on its own): the Life
  Safety catalogue pointed at `modules/venues` (writing, `--check`, `--fragments`, `model_depth.py`
  over what it writes), the arena palette's swatches, `tinted` and `mirror_x` element helpers, and a
  minimal OBJ writer (polygons, posts, sheared rails and bars, glass planes) for what a JSON element
  cannot turn to
- `gen_venues_seating.py` -- the seating bowl (Sports & Venues): the stadium seat's parts (seat
  down and folded, 2-deep and 1-deep, the boundary standard with armrest and cupholder, end
  standards and plates) as one grey set tinted per colour, the multipart blockstates of the ten
  seats and two aisle-end blocks (a plate model per row letter and side off one 256 px sheet), the
  companion seat, wheelchair marking, step light with its `_e` lens, the handrail and sloped glass
  guards as OBJ on the nosing line of each `StadiumSlope` piece, and the section and portal signs'
  blockstates and item models; `--check`, `--fragments`
- `gen_venues_arena.py` -- the arena's event floor (Sports & Venues): the dasher boards and glass
  (JSON straights and doors, the open pose the closed leaf swung about its hinge; the 45 degree
  corner and the radius-8 curve as OBJ prisms, the curve cut into the fifteen cells its ice face
  crosses), the rink and court floors, every painted marking drawn on a (u, v) plane in metres
  (numpy, supersampled) and cut into deduplicated 32 px cell textures, the hockey goal, the
  stanchion (OBJ, five blocks tall, an animated shot clock), the resurfacer, ice plant, benches
  and the scorer's table (an animated LED front with its `_e`). Writes `ArenaFloorLayouts.java`
  (which cell sits where round each anchor) and `DasherShapes.java` (the curve and corner
  collision boxes) from the same geometry, and the units' footprints into their tab lines;
  `--check` (the Java too), `--fragments`, `--doc`
- `gen_venues_av.py` -- the arena's scoreboard and AV (Sports & Venues): the centre-hung
  scoreboard in two sizes as one OBJ each (casing, corner posts, rigging, the animated LED rings
  and dark screens, cut a block at a time so the textures keep their size) and
  `ScoreboardSize.java` (screens and shell boxes) from the same numbers; the idle video sheet the
  renderer binds on its own (32 frames, not a block texture any model names); the ribbon board's
  one animated 64 px texture (four patterns of four blocks, a block showing its phase's quarter),
  its straight and OBJ 45 degree corner; banners (tinted cloth and trim), the line-array speaker
  (OBJ, cabinets curled in a J), the camera platform and the catwalk multipart; the colour,
  pattern and editor lang; `--check` (the Java too), `--fragments`
- `gen_venues_concourse.py` -- the arena's concourse and premium spaces (Sports & Venues):
  concession counters, corner and back counter (multipart, a model per finish, end panels by
  actual state), the concession sign's icon (the sign is Core's wayfinding panel), the digital
  menu board's four 256 px menus with their food pictograms and prices, the five countertop
  machines as a model on and off (popping, glistening sausages, glowing warmers as animated
  textures), the beer kiosk and the tripod turnstile as OBJ (arms mutually square on a hub
  tilted 54.7 degrees, at rest and indexed 60 degrees), the ticket scanner, the Venue Ticket's
  sprite, the suite bar and drink rail, and the team store fixtures with garments on tint 0 and
  1; unit footprints written into the tab lines from their boxes; `--check`, `--fragments`.
  `gen_venues_sounds.py` synthesises the scanner's accept and refuse tones and the turnstile's
  ratchet (numpy to ffmpeg to OGG)
- `gen_venues_exterior.py` -- the arena's exterior and the outdoor pieces every sport shares
  (Sports & Venues): the marquee cabinet's body and frame edges, single and double-sided
  (multipart by actual state; the screen depths `MARQUEE_*` shared with
  `TileEntityMarqueeRenderer`), the pylon, the blades' icons (the blades are Core's wayfinding
  panel) and their bracket, the box office window with its four lit headers (64 px legends on
  explicit UVs), the bleacher deck, seat plank, aisle step, end and back rails and understructure,
  the press box modules (glass and mullions turned 22.5 degrees, tall elements cut at the block
  lines by `tall()`), and the ticket booth and the light heads (lit and dark) as OBJ; unit
  footprints written into the tab lines from their boxes; `--check`, `--fragments`
- `gen_venues_ballpark.py` -- the Ballpark tab (Sports & Venues), from the field geometry in
  `ballpark_field.py` (a full-size baseball and softball diamond as predicates on world points,
  rastered at 32 px a block with jittered samples so no sample sits on a 45 degree line): every
  cell classified as a plain surface, a standard piece (edge or chalk, matched in each of four
  turns) or a picture; pictures deduplicated (a mirror reuses an existing picture where one is its
  mirror) and owned by anchors (the small markings first, then the home area and cutouts, then
  the arc in greedy segments within a painted cell's seven-block reach); `BallparkLayouts.java`;
  the mound's heightfield OBJ cells; the fixtures, dugout, nets, wall (the 45 degree one a band
  past its cell), pole, scoreboards and machine. `--layout baseball|softball` prints a field's
  blocks (and checks every painted cell shows the drawing through its anchors as the Java finds
  them), `--doc` the anchors and offsets; `--check`, `--fragments`
- `gen_venues_football.py` -- the Football & Soccer tab (Sports & Venues), from the field geometry
  in `football_field.py` (a pro football field and a soccer pitch painted onto a label canvas,
  every edge snapped to the texel, hash marks to every second texel from the centre): every cell
  a plain surface or a picture, pictures stored once per half turn with their tint classes (off
  the field, one tone, the west and east of a stripe edge, the end zone) cut into rectangles of
  one picture texture; anchors on a grid each owning the picture cells nearest it within 12;
  `FieldLayouts.java`; the goal post (gooseneck OBJ at real size), goals and nets, sideline
  pieces, end zone lettering and scoreboards. `--layout football|soccer` prints a field's blocks
  (checked as the Java finds them), `--commands X Y Z [--east] [--zone N]` as an mcfunction's
  lines; `--doc`, `--check`, `--fragments`
- `gen_venues_bowling.py` -- the Bowling tab (Sports & Venues): the lane and approach pictures
  (every board, the foul line, dots, arrows, range finders, pin spots, tail plank, drawn three
  samples a texel where they fall, a 64 px half lane each, the right half the left mirrored),
  the lane, deck and approach geometry and their multipart blockstates by actual `mark`, the
  bumpers, lathed OBJ pins standing and fallen, the pinsetter and masking unit (four invented
  graphics with `_e` companions), the ball return, the settee and its curved corner (OBJ), the
  console, the scoring monitors' casing (its face shared with
  `TileEntityBowlingMonitorRenderer`), ball rack, shoe counter, cubbies and spray.
  `--layout [--lanes N]` prints a whole centre, `--commands X Y Z` as an mcfunction's lines;
  `--check`, `--fragments`. `gen_venues_sounds.py` synthesises the ball's roll and the strike
- `venues_lines.py` -- the courts' and track's line art engine (not run on its own): layouts as
  ordered paint operations over half-planes and discs (a disc drawn as the polygon through its
  crossings of a fine grid, so neighbouring cells meet on the same point), the painter
  `LinePainter.java` follows step for step, the census of plain and painted cells, anchors on a
  lattice within reach of every painted cell, painted numerals, previews, and the Java tables
  (`LineArtMaterials`, the layouts as short literals). `court_layouts.py` and
  `track_layout.py` are the geometry (ITF, pickleball, FIBA, FIVB; the World Athletics 400 m
  track and its field events); `venues_units.py` the fixtures' OBJ and unit helpers. Every
  paint band under `MIN_LINE` (12.5 cm) is widened about its centre as a layout is built
  (`legible()`), since a geometry line under a pixel breaks into dashes at a distance
- `gen_venues_courts.py` -- the Courts, Track & Pool tab's courts (Sports & Venues): the surface
  sprites both families share, the plain, anchor and painted cell blocks, the tennis,
  pickleball and volleyball nets (runs; the volleyball net OBJ), the umpire chair, players'
  bench, chain-link fence with windscreen (Building's look, drawn again) and divider curtain,
  ball machine, the courtside scoreboard (its face shared with `TileEntityCourtBoardRenderer`)
  and the pole-mounted hoop; writes `LineArtMaterials.java`, `CourtLayouts.java` and the
  `CourtSamples` that `LinePainterTest` holds the Java painter to. `--layout NAME --commands X Y Z
  [--rot R]` lays a court with its fixtures; `--preview DIR`, `--doc`, `--check`, `--fragments`
- `gen_venues_track.py` -- the same tab's track and field: the polyurethane surface and track
  cells, `TrackLayouts.java` and `TrackSamples`, hurdles (four heights), starting blocks, the
  shot put toe board (OBJ arc), the finish camera tower, the lap counter, the high jump and pole
  vault landing areas with standards and bars, the discus and hammer cage and the steeplechase
  barrier (OBJ units). `--layout track_400m|long_jump|pole_vault --commands X Y Z [--rot R]`;
  `--preview`, `--doc`, `--check`, `--fragments`. `gen_venues_sounds.py` synthesises the lap
  counter's bell
- `gen_venues_pool.py` -- the same tab's pool (Sports & Venues): tile, deck and gutter, the pool
  floor's line art family (`PoolLayouts.java`, `PoolSamples`: 25 m and 50 m pools and a diving
  well), wall target and touch pad tiles placed by where each lane's middle falls in its block,
  lane ropes, backstroke flags, numbered starting blocks, ladders (OBJ reaching into the water),
  the lifeguard chair, springboards and platform, the pace clock (dial shared with
  `TileEntityPaceClockRenderer`) and the scoreboard (face shared with
  `TileEntityVenueSignRenderer`). `--layout pool_25m|pool_50m|diving_well --commands X Y Z
  [--rot R]` lays a whole pool round real water; `--preview`, `--doc`, `--check`, `--fragments`
- `gen_venues_golf.py` -- the Golf tab: turf in five cuts with stripe textures, the cup,
  flagstick, bunker, tee and yardage markers, the tee sign (text engine), ball washer, bench,
  cart path and curb, the driving range, mini-golf (carpet, cup, border, ramp, OBJ windmill and
  loop, scorecard stand). `--layout green|range|minigolf --commands X Y Z [--rot R]`; `--check`,
  `--fragments`
- `gen_venues_skate.py` -- the Skate Park tab: quarter pipes and the bowl corner as OBJ swept
  and revolved from the transition's circle, their staircase collision boxes written into the
  tab line, kicker, bank and box, grind rail and ledge. `--layout park --commands X Y Z`;
  `--check`, `--fragments`
- `build_venues_demo.py` -- builds the Sports & Venues demo world in a flat creative world named
  "Venues Demo" (refuses any other; `--create` makes one, `--only <area>` rebuilds one) through the
  orchestrator by instance id, after generating the site's chunks (a write never generates
  terrain): a 61 x 26 rink in a small seating bowl with the centre-hung scoreboard and ribbon
  board, a concourse strip with its exterior, a bowling centre, courts, a 25 m pool and diving
  well, golf (green, range, mini-golf), a skate park, the ballpark, the football field and soccer
  pitch with light towers, bleachers and press box, the 400 m track, and a cable-stayed and a
  suspension bridge strung with Roads' bridge cables. Sports fields, courts, the pool, golf, skate
  and bowling are laid from their generators' `--layout --commands` output; the arena, concourse
  and bridges are placed by the script. It checks every Venues tab's registration list against
  what it placed and puts the rest on signed plinths; `--dry-run` works it all out offline
