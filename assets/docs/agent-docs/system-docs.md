# In-depth system documentation

What each doc in `assets/docs/` covers, moved out of the repository `CLAUDE.md` (which keeps a list
of the file names by module). Read the doc itself before changing its subsystem.

- `assets/docs/MODULE_SYSTEM.md` -- Core plus fifteen optional module jars: what each owns, how
  registration still works across jars, the Core service registries, adding a module, the traps
- `assets/docs/BLOCK_AND_ITEM_BASE_CLASSES.md` -- Every abstract class, constructors, rotation, meta encoding, registration
- `assets/docs/FRAMING_SYSTEM.md` -- Stud walls, joists, deck and structural steel: why a wall is
  drawn post-and-arm rather than as a panel, why its post appears only at junctions and run ends,
  why the track is left out between courses, why insulation needed sub-blocks to be obtainable at
  all, why there are no roof trusses, and the traps (a narrow member's UV window, `registerModels`
  and metadata, an `OR` that cannot take a sibling key, plates priced as brackets)
- `assets/docs/WALL_MATERIALS.md` -- Concrete block, brick, stucco, siding, cladding and stone
  veneer: the six generators and the set shape they share, why brick is 32 px, why brick trims are
  blocks and not a state, how each material is told from its nearest neighbour, the name-driven
  Fabricator pricing, the glazing (glass that joins into one framed window, one-way glass), and
  the traps (a name decides a price, fine detail mipmaps away)
- `assets/docs/STAIRS_AND_RAILINGS.md` -- The Stairs & Railings tab: a railing as a cell over
  the stairs (edges stored, the slope read from the stairs: mid, top, foot, level), the nosing
  line every part follows, why both sides of a one-wide flight share a world-relative slope and
  the blockstate mirrors the south and west edges, posts on every cell so a run needs no end
  state, why it is all OBJ, the stairs without uvlock, and the measured cost (about 21 ns a cell)
- `assets/docs/INTERIOR_FINISHES.md` -- The Interior Finishes tab: window blinds, shades and
  curtains (their own block hung against any window, joining into one blind, click to cycle the
  whole blind, redstone to close it, light taken by state), flooring (overlays on any floor and
  full-block sets, why a tile grid is never turned), wall finishes and corner guards, the
  hospital fittings (accent panels, wayfinding stripes, room signs, nurse station screens) and
  their placing table, and where the decor track grows next
- `assets/docs/ADVERTISING_SYSTEM.md` -- The Signage & Advertising boards: a board as one object
  (controller + tagged parts with no tile entity), drawn as one quad across the board, the three
  ad sources (generated, public-domain vintage, server-supplied), rotation and transitions,
  what server ads trust and why, and the traps (polygon offset, mipmap levels, render range)
- `assets/docs/GARAGE_DOORS.md` -- Sectional, roll-up and grille garage doors built to the size
  of the opening: why a door at rest is baked models with no tile entity and only the anchor of a
  moving one has a renderer, the sectional door's path round the bend shared between its OBJ
  ceiling runs and the renderer, the hardware, and the opener and the stackable hanger
- `assets/docs/DOORS.md` -- Twelve two-block doors, pairs, redstone, the Door Closer add-on and
  keypad locks: the state split between the halves, why the open model is written out rather than
  rotated, the swing drawn from the resting models by a client-only tile entity that removes
  itself, which way each door swings and how it is flipped, and why locks are world saved data;
  the hospital, smoke barrier and lead-lined doors, the automatic sliding doors and their
  activation mat; the dark oak doors; the cathedral doors (a leaf two wide built to its opening, one
  block per cell with every cell the same four bits, the pointed arch head and tympanum, why not a
  master and parts); and the metadata table for placing every door by command
- `assets/docs/CONSTRUCTION_SITE.md` -- Frame scaffold, formwork, shoring, rebar and the tower
  crane, site fences, earthworks, logistics and facilities: why the scaffold's look is actual state and its sides three-valued, the guardrail that
  needs its own collision handler, why the crane head draws its jib in Java rather than as blocks
  or baked models, the display list with the slew outside it, how its deck and walkways are made
  solid without blocks, climbing by inserting sections, containers built to size, and
  the UV traps (explicit UVs past 0..16, shift a span a whole block, never clamp it)
- `assets/docs/FIRE_ALARM_SYSTEM.md` -- MovingSound architecture, channel system, sound standards, full inventory
- `assets/docs/EXIT_SIGN_SYSTEM.md` -- The configurable traditional and specialty exit signs:
  setup as tile entity data picked by a multipart blockstate, what each block offers
  (`ExitSignSpec`) and why mains power exists only where there are heads, the face built from
  three cells, why a hung sign's arrow reverses from behind, the heads' glow through the
  emergency lights' renderer, the setup screen, and the traps (ordinals are saved, the generator
  repeats the specs)
- `assets/docs/EMERGENCY_SERVICES.md` -- Life Safety's four tabs and why the fire alarm tab kept
  its id, the fire protection, emergency lighting and fire/police/EMS station families and how
  they are generated, round parts as exact octagons, synthesised sounds, generic emblems, every
  block that does something, why cross-module effects go by redstone, the fire pole's fall, the
  outdoor warning sirens (horn baked at rest, drawn only while sounding, volume from the horn's
  bearing), the connectable standpipe (actual-state runs, floor pass-through, corner elbows off
  the set-back axes), and the traps
- `assets/docs/STREETSCAPE_SYSTEM.md` -- The Streetscape tab in Roads: what is left out and why
  (the external road mod's curbs, bike racks, brands), settling, the utility box multi-block
  (root draws, parts forward, all-or-nothing placing, a break hook every cell asks), covers,
  bollards, hydrants and news racks, parking meters (real-time expiry, the clock skew, the
  scheduled redstone, SUM by reflection and why at server started), mailboxes (doors picked by
  ray trace, the mode in the GUI id, contents never synced, insert-only automation), and the traps
- `assets/docs/SHOP_SYSTEM.md` -- The Market & Store shops: shop shelving, wall shelf, display
  case, cooler and vending machine that sell the real item put in them and may buy it back; why
  they are new blocks rather than a mode of the decorative ones, prices and tills kept per
  currency, the trade order and its refunds, server shops and unlimited stock, who may click and
  break what (and why the tall shops check in `onBlockHarvested`), the counter's empty container,
  the item renderer and its `ShopDisplay` rows from the generator, and the optional SUM economy
  through Core's `codeutils/economy/CsmSumEconomy` (one instance per module, `csm_furnishings`
  for shops, `csm_roads` for meters; emeralds without it)
- `assets/docs/PAWN_SHOP_SYSTEM.md` -- The Market & Store pawn shop: server-run counter, drop-off
  bin and price kiosk that buy any item; its value from `pawn_values.json` and the cheapest
  recipe walk (crafting, smelting, Fabricator), the payout in money or emeralds with carry-over,
  the ledger, resale, the claimed bin's hopper payout, `/csmpawn`, the config and the traps
- `assets/docs/STATION_ALERTING_SYSTEM.md` -- A fire station's alerting: the controller, its
  linked speakers, alert lights, relays and bay clearance lights, the dispatch sequence, and why
  the relay's redstone is how it opens bay doors, strikes the gong and preempts traffic signals
- `assets/docs/TRAFFIC_SIGNAL_SYSTEM.md` -- Controller system, signal phases, pedestrian signals
- `assets/docs/LANE_CONTROL_SYSTEM.md` -- Reversible lanes: the lane control signal, its own
  controller cabinet, groups on a time-of-day schedule, and why the clearance runs one way only
- `assets/docs/LIGHTING_SYSTEM.md` -- 4-state on/off control, light-up air projection (the block and
  its projection are Core's, `codeutils/light`, so the stadium light heads share it), AbstractBrightLight, the decorative pendant/sconce family and its 3-material OBJ finish/lens pattern, the tall building (aviation obstruction) beacons, flashed bright by a renderer
- `assets/docs/UTILITIES_SYSTEM.md` -- The Utilities module (was Power Grid; mod id `csm_powergrid`
  kept): why only the display name changed, why it requires Roads, why two tabs (Power Grid and
  Utilities); the Forge Energy integration and the utility pole pieces; the building service
  meters; the water system: a tower stacked to size (legs, riser, bracing placed along a square
  panel's diagonals, the pedestal column) under a tank placed whole, a tank as a grid of
  three-block tiles that find their place by counting their neighbours (one registry name a tank,
  nothing stored per tile), its OBJ lathe cut at the tile faces, the collision map written from
  the same profile, the balcony, the name band, ground tanks stacked a layer at a time, the pump
  station's pipe runs, fittings and pumps; sewer and stormwater: the double-leaf hatch placed as
  a pair and set in the ground, the redstone-lit control panel and click-started generator on
  Roads' utility box, the curb inlet at the external road mod's curb height, the headwall with
  its pipe and flap gate, wingwalls one slope over two blocks, riprap drawn by position, the
  outlet riser and manhole sections stacking like poles, the manhole climbed on its steps and cut
  away for a side view; the gas yard and telecom: the water pipe carrying a service (gas joins
  only gas), the skid set into the ground with pipe stands under the run, the line heater's gas
  through its coil header, signs that hang on a chain-link fence's mesh, the fibre cabinet's
  doors, the ice bridge on tower-column stanchions, the monopole stacked with its antenna array
  as an `IColumnJoint` (three OBJ sectors 120 degrees apart), the small cell as a post-top fixture
  with a collar sized to the pole; what was cut and the traps; the module's final weight
  (133 blocks, 1,793 states), the demo world, and what the module could grow into
- `assets/docs/TRAFFIC_SIGNS.md` -- Forge blockstate format, dynamic properties, 472-sign system,
  how large a sign face texture may be (85.3 texels a block of plate; `SignTextureSizeTest` fails
  the build on a larger one),
  the mile markers whose number is baked into the chunk mesh from the tile entity (no TESR), the
  object markers and post delineators,
  the three shift models and where a back-to-back plate has to sit (`SignShiftModelTest` fails the
  build on a shift entry that does not move), and why the metal behind a sign's art is recessed
  (`SignFaceDepthTest` fails the build on two faces too close to tell apart at a distance)
- `assets/docs/DYNAMIC_GUIDE_SIGN_SYSTEM.md` -- Highway guide signs: panel/row/element data model, TESR, FHWA legend font, sign atlas
- `assets/docs/DYNAMIC_STREET_SIGN_SYSTEM.md` -- Street name blades: fixed-slot data model, hanging vs flat mount, double-sided rendering, civic logo atlas rows
- `assets/docs/SPAN_WIRE_SYSTEM.md` -- Wire-span signal mounting: the catenary solver, why mounts
  go below the cable, the three different ways a payload hangs, box span tether clearance
- `assets/docs/BRIDGE_CABLE_SYSTEM.md` -- Bridge cables (#323): stay, main, hanger and rope
  cables an anchor (socket, clevis, saddle, deck anchor) owns and draws to another anchor or any
  block; the NBT a bridge generator writes (one cable per `/blockdata`, added or replaced by
  target, saved as offsets) with examples, the parabola main cable and the span wire's catenary
  for rope, hangers dropped every N blocks to whatever is below, guide pipes drawn at the cable's
  angle, one display list per anchor as a global renderer with a whole-cable box, the traps
- `assets/docs/MAST_ARM_CURVE_SYSTEM.md` -- Realistically scaled signal mast arm upsweeps: why they are multi-block, the parabolic sweep, oblique end clipping
- `assets/docs/WORK_ZONE_ACCESSORIES.md` -- Cones, drums, channelizers, barricades and the arrow board: how a device settles onto the road below it with no dependency on whatever built that road, barricade runs and their mounted signs, the animated arrow board
- `assets/docs/GUARDRAIL_SYSTEM.md` -- Four rail families, their end treatments, the W-to-thrie transition and the crash cushion: why a run joins on the RAIL rather than on block identity, why the slope is read off where the rails actually are rather than off block positions, and why the end treatments are chiral
- `assets/docs/CONCRETE_POLE_SYSTEM.md` -- The concrete poles (round and octagon, thick and thin)
  in the traffic pole family: one stackable block that shows plinth, cap, tenon (under an
  `ICsmPostTopFixture`) or seamless joint per end, why each end model carries half the shaft,
  the concrete and octagon accessories and why their bodies are redrawn, what was left out
  (retiring families, the pole base), and the legacy `rcp*`/`ocp*` pieces retiring into it
- `assets/docs/TRAFFIC_POLE_BASES.md` -- The bolted bases the painted thin, thick and impact
  resistant poles draw at their foot (square, squircle, slim, pedestal, none): the one `foot`
  property that carries the base and the reach-down extension, why upright poles are always
  drawn facing up, the metadata that leaves existing poles untouched, the OBJ bases, and the
  impact resistant pole (full block width, no mount stubs)
- `assets/docs/PEDESTAL_POLE_SYSTEM.md` -- The pedestrian pedestal pole: one stackable block that decides base, cap or seamless joint per end from its neighbours, why the end properties are named in model space, and the six-style finial block a pole wears on top, cycled with the Street Light Configuration Tool
- `assets/docs/RAILROAD_CROSSING_SYSTEM.md` -- The grade crossing: crossbuck and signs, the
  redstone-driven flasher mast (wig-wag in the texture, bell from the tile entity) and the gate
  whose arm is a renderer swinging at a real gate's pace; why redstone and not a controller
- `assets/docs/PARKS_GREENERY_SYSTEM.md` -- Street trees built from log and leaves blocks: logs
  whose connections (including the 12 edge and 8 corner diagonals that make a stepped lean read as one trunk)
  travel in an extended state to a baked model, leaves drawn as sheets on open faces with tufts past them,
  palm crowns, the Tree Planting Tool and its six generator shapes (street clearance, one volume
  check, presets appended by ordinal), the plantings and amenities (why nothing shares a trunk's
  cell, bench runs, the irrigation controller and sprinklers), the plantable planters and joining
  troughs, and the traps
- `assets/docs/SCHOOL_TIME_AND_PA.md` -- The school time and PA set in Technology: clocks whose
  hands a renderer draws from game time (dials supplied per block), the two-block clock/speaker
  panels whose speaker half is an ordinary TTS-linkable speaker, the PA speakers, and the bell
  schedule controller (game-time periods, linked devices, a minute check, no chunk loads)
- `assets/docs/TECHNOLOGY_SYSTEM.md` -- The Technology module's 2026-10 expansion: its three
  tabs, the Micafi brand rule, desk devices at the bottom of their block (so they work on any
  full block), click-cycled screens and lights in metadata (append-only ordinals, the state
  counts), the 42U rack (2/3 px a U, two blocks split like a tall wardrobe, rows joined by actual
  state, the tile entity's `u` byte array, units baked into the chunk mesh through an unlisted
  property and `CsmBakedModelWrappers` with a quad cache, no renderer, doors in the glass layer,
  the full 128 px unit sheet, the wall cabinet's depth limit, removal at the height clicked), the
  pre-filled racks (no tile entity, the same unit boxes, `RackUnitBoxesTest`), the measured cost,
  pole fitting, the tray joining rule, Fabricator words from the display name and the
  `MOUNT_NOUNS` trap, the round-face and `model_depth.py` trap, the demo world, Phase 7 in
  progress and what was left out
- `assets/docs/TRANSIT_SYSTEM.md` -- The Transit module: the fare gates (ticket, card and exit
  sensing, the gate's states, operator modes), the fare vending machine and its purchases, the
  ticket and stored-trip card, why the move from Technology kept every registry name, GUI id and
  asset path; the bus stops (road signs on Roads' sign posts, why there is no pole family, flags
  hung off the side of the post and double-sided, why `hang` rather than `shift` picks the flag's
  model and the state mapper that leaves `shift` out, the invented agencies, route plates set by
  clicking and drawn from shared lists, the arrival display's made-up but steady countdown); the
  shelters (joining both ways, the roof light, the slot for Signage's ad panel); the
  station and platform fit-out made to complement RCMC (tactile paving, furniture, signs, tile,
  columns, canopy, validator, and keeping clear of a train at the platform edge); the airport
  terminal (terminal only, the metal detector left to Life Safety, a flight schedule made from
  the world's time of day, the lit boards and their shared lists and pages across a bank, the
  kiosk's boarding pass and the gate scanner, the carousel's sixteen tops, trays that drop onto
  the lane); the bus departure boards (stops found, not linked, and numbered as bays; one
  timetable shared with the arrival display; banks rather than build-to-size; announcements only
  through Core's TTS service; the measured cost); the stations (a kiosk from glass and roof
  pieces on the container's pattern rather than the job trailer's, the open stair and its globe
  lamps, the fare line railing that finds the fare gates above the floor, the service gate as a
  vanilla fence gate, line bullets, and why the line diagram is RCMC's); the airside (airfield
  lights switched a circuit at a time, the beacon turning by texture, signs, masts, ground
  equipment on Roads' utility box, a jet bridge that draws and collides past its cell, and a
  helipad laid by `/csmtransit helipad`); the demo world and its builder; and what was left out
  and what the module could grow into
- `assets/docs/VEHICLES_SYSTEM.md` -- The Vehicles module and emergency vehicle preemption: the
  preempt detector in Roads and Core's preempt source service, why emergency lights are found by
  custom variable name, how IV finds a pack in a module jar (and the dev client's pack-only jar),
  the parts and their flash timing, IV as the module system's one external dependency, and the
  traps (a vehicle is placed at 90 degrees to its placer, IV polls the mouse itself)
- `assets/docs/VENUES_SYSTEM.md` -- The Sports & Venues module (#324: arenas, stadiums, ballparks,
  fields, courts): why Core only, the tabs as they grow per sport, the performance rules (no tile entity per seat, one tinted model set for ten
  colours), the state counts, how 2-deep and 1-deep rows are built, and every seating block's
  registry name, metadata and NBT for a bowl generator: seats, aisle-end letters, the slope pieces
  rails and guards follow, the step light, and the section and portal signs; the arena's event
  floor: the floors, the painted markings round their anchors, laying a 61 x 26 rink with true
  radius-8 curves (a worked example), the court, the units' footprints and the traps; the
  scoreboard and AV: hung units and where their master goes, the scoreboard's screens, clock,
  editor and NBT, the ribbon board's phase, corners and scrolling text, banners, the measured
  costs; the concourse: the existing blocks (Furnishings, Life Safety, Transit) an arena builder
  should use instead, every concourse block's metadata and NBT, the one-way turnstile and the
  ticket, the team colours, and the traps; the exterior and shared outdoor pieces: the marquee
  built to size and where its controller goes, its NBT and measured cost, the blades on Core's
  wayfinding panel, the box office window, how bleacher rows are laid, the press box clockwise
  round its corners, the booth, the light towers and the field they light (a grid of Core's
  light-up air a few cells a tick, who removes which cell, the measured cost); the
  ballpark: the one-block-a-metre scale and the diamond laid along the grid, why the foul lines
  sit on half-integer diagonals, the surfaces and standard pieces, the anchors and painted
  cells (and why the arc is three anchors a side: a painted cell's reach against the render
  cache's), the mound, every fixture's metadata and NBT, the scoreboards' NBT, a worked baseball
  and softball diamond (where each base, plate, mound, line, box and anchor goes), costs and traps;
  football and soccer: the two fields' dimensions and frames, how the sub-block lines work
  (pictures to the texel, the mowing tone and end zone colour as tints split under the yard
  lines), the anchors, worked layouts and the fixtures they place, the goals, the sideline, the
  end zone lettering's and the scoreboards' NBT, aiming light heads (`aim`) and a field lit by
  six towers, the state counts (1,196), costs and traps; bowling: the two-block lane and its
  scale, lanes and approaches that mark themselves from their place along the lane, every
  block's metadata and NBT, the scoring monitor's notation and NBT, a worked eight-lane centre,
  the state counts (392), measured costs and traps; courts and track: the line art engine (why
  geometry rather than a picture a cell, circles as their grid-crossing polygons so a bend's
  lines are continuous at their true radii, the convex partition, the cached quads and their
  vertex colours), anchors and painted cells with their NBT, every court's dimensions and
  defaults, the nets, fences and fixtures, the 400 m track's markings (staggers, relay zones,
  hurdle marks, lane numbers) and field events, the equipment, worked layouts, state and atlas
  costs and traps, and why every painted line is drawn at least 12.5 cm wide; the pool (a
  deck-level pool round real water, why nothing stands in the water, lanes a quarter block off
  the grid and the start-end rule that lets a starting block place itself, every block's
  metadata and NBT, the scoreboard's lines, worked 25 m and 50 m pools and a diving well), golf
  (mown stripes from position, the edge rail, the tee sign, the range and mini-golf, worked
  layouts) and the skate park (OBJ curves, staircase collision a player can walk up, a worked
  park), with their state counts (477), atlas, costs and traps
- `assets/docs/WAYFINDING_PANELS.md` -- The wayfinding panels (#307): the airport, hospital and
  generic `wayfinding_panel` as one baked engine in Core, cells joining into a panel up to 16 x 6,
  each cell's place worked out on the server, the mounts (auto, wall, hung, post, ceiling) and
  drop, the panel scale, four lines with icons either side, schemes and custom colours, the two
  sheets, the NBT every generator writes, the editor, and the traps
- `assets/docs/HEALTHCARE_SYSTEM.md` -- The Healthcare module (#293, the Alto hospitals): why it
  depends on Core only and what its neighbours make instead, the two tabs, the performance rules
  (two renderers, animated screens, nothing ticks, event-driven links in saved data), the bed as a
  head and a shared foot, the curtain filled from its track, ceiling-hung heads on a drop column,
  the CT and MRI as a master and parts, joining runs, nurse call, the ED tracking board, the
  queue, code blue, the wayfinding panel's drop-in parity with Transit's, the mannequin's rests,
  the commands, the full per-block metadata and NBT table the hospital generators use, the
  traps, the measured weight, the demo world, the station monitors showing calls and the
  operating room set
- `assets/docs/HVAC_SYSTEM.md` -- Rooms that hold heat: the thermal simulation (flood-filled
  spaces split into regions, walls/openings/ground/neighbours, implicit step), model-based
  modulating control, vent throw and the thermostat trim, why a partly unloaded room freezes, the
  server-sent HUD, `/csmhvac` and the test lab, why the old offset engine was replaced, and the
  lightweight mode (`hvac.lightweightMode`, AUTO by default past 2,000 loaded devices: no rooms,
  thermostats read their setpoint, units run a steady 30%), with what the full simulation cost on
  the live server; exhaust grilles and fans and room pressure (#314)
- `assets/docs/SURVIVAL_AND_RECIPES.md` -- Crafting parts, the CSM Fabricator, mining behavior, why there is no per-block recipe
- `assets/docs/PERFORMANCE_AND_SECURITY.md` -- Where frame time and memory actually go (client frame time is
  most of it; the server tick is small except for work that scales with a city -- HVAC rooms at the
  edge of view or arriving with their chunks, controllers loading their sensors' chunks -- measured
  at city scale, with the fire alarm cleared), the block atlas budget (`atlas_budget.py`), how to
  measure without fooling yourself, the rules render and
  tick code follow, NBT short keys, and the conventions every network packet follows
