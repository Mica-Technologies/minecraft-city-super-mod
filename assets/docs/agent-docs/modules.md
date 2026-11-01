# Modules

The full module table, moved out of the repository `CLAUDE.md` (which keeps a short one). Design,
service registries, adding a module and the traps: `assets/docs/MODULE_SYSTEM.md`.

| Module tree | Mod id | Display name | Contents |
|---|---|---|---|
| `src/main` | `csm` | CSM: Core | base classes, registration, tabs machinery, config, parts + Fabricator, shared assets |
| `modules/roads` | `csm_roads` | CSM: Roads & Traffic | `trafficsignals`, `trafficaccessories`, `trafficsigns`, `streetscape` (the Streetscape tab: covers, utility boxes, bollards, hydrants, news racks, working mailboxes, parking meters and EV chargers; see `assets/docs/STREETSCAPE_SYSTEM.md`); bridge cables (`trafficaccessories.bridgecable`: stay, main, hanger and rope cables drawn from socket, clevis, saddle and deck anchors, set by `/blockdata`; see `assets/docs/BRIDGE_CABLE_SYSTEM.md`) |
| `modules/lifesafety` | `csm_lifesafety` | CSM: Life Safety | `lifesafety`, `api/firealarm`; four tabs — Fire Alarm & Detection, Exits & Emergency Lighting, Fire Protection, Emergency Services |
| `modules/hvac` | `csm_hvac` | CSM: HVAC | `hvac` |
| `modules/lighting` | `csm_lighting` | CSM: Lighting | `lighting`; its fixtures light the space below them with Core's light-up air (`codeutils/light`), which Venues' stadium light heads use too |
| `modules/powergrid` | `csm_powergrid` | CSM: Utilities | `powergrid`: the Power Grid tab (poles, cross arms, insulators, mounts, the Forge Energy blocks) and the Utilities tab (building service meters and panels; the water system: a water tower built to size, ground storage tanks, the pump station, air release and backflow enclosures, the treatment skid; sewer and stormwater: the lift station's access hatches, control panel and standby generator, sidewalk doors over vaults and utility tunnels in four styles and six sizes, the curb inlet, the outfall headwall, flap gate, wingwalls and riprap, the pond outlet riser and emergency spillway, manhole sections whole and cut away; the gas yard: a regulator station built from a skid, gas pipe, ball valves, a regulator, a turbine meter, a line heater and a vent stack, the odorant tank, and warning signs that hang on a wall or a chain-link fence; telecom: the fibre distribution cabinet, a cell site's cabinets, ice bridge and GPS antenna, a monopole built to height with its antenna arrays, and a small cell's canister antenna and radio for the street poles); was CSM: Power Grid, and kept that mod id and tree so worlds load unchanged; requires Roads (the utility box multi-block); see `assets/docs/UTILITIES_SYSTEM.md` |
| `modules/technology` | `csm_technology` | CSM: Technology | `technology`; three tabs: Technology (AV, phones, the iMac family, the older access points), Computers & Peripherals (gaming rigs, monitors whose screens a click changes, keyboards, mice, desk extras, invented consoles) and Networking & IT, mostly the invented brand Micafi (desktop switches, gateway and wall jacks as desk devices; `technology.network`: cable trays, empty and full of cable, that join into corners and tees and hang lower on threaded rod hangers, access points whose ring a click changes, outdoor radios fitted to Roads' poles and turning eight ways round them, a wall-arm bridge whose head a sneak-click aims; `technology.rack`: 42U racks two blocks tall filled with rack-mount units baked into the chunk mesh, pre-filled racks with no tile entity, the 12U wall cabinet); also the school time and PA set (`technology.school`: Micaplex clocks with renderer hands on game time, clock/speaker panels, PA speakers, the bell schedule controller and hallway bell; see `assets/docs/SCHOOL_TIME_AND_PA.md`); see `assets/docs/TECHNOLOGY_SYSTEM.md` |
| `modules/furnishings` | `csm_furnishings` | CSM: Furniture & Novelties | `furniture`, `novelties`; the Furniture, Residential, Commercial & Office and Market & Store tabs (the last holds the checkout's Verifone MX915, moved here from Technology, and the working shops, `furniture.market.shop`, which take SUM's money or emeralds; see `assets/docs/SHOP_SYSTEM.md`) |
| `modules/building` | `csm_building` | CSM: Building Materials | `buildingmaterials`; five tabs — Building Materials, Structure & Framing, Interior Finishes, Construction Site, and Stairs & Railings (`buildingmaterials.stairs`: railings, guards and wall handrails that follow a flight and its landings, and the stairs they run on; see `assets/docs/STAIRS_AND_RAILINGS.md`) |
| `modules/tts` | `csm_tts` | CSM: Text to Speech | the Redstone TTS block and the MaryTTS engine; requires Technology |
| `modules/signage` | `csm_signage` | CSM: Signage & Advertising | `signage`: ad kiosks, poster boards and billboards (not road signs, which stay in Roads) |
| `modules/parks` | `csm_parks` | CSM: Parks & Greenery | `parks`: street trees built from log and leaves blocks, the Tree Planting Tool, the tree tools (chainsaw, pole trimmer, tree shears, stump grinder), plantings and park amenities, and plantable planters, raised beds and troughs (`parks.planters`: a plant's baked quads merged onto the soil, no TESR; `/csmparks planter fill`); two tabs, Trees & Plants and Parks |
| `modules/transit` | `csm_transit` | CSM: Transit | `transit`: the working fare system (fare gates, the fare vending machine, fare tickets and transit cards, moved here from Technology under their registry names) and the bus stops (`transit.stop`: agency flags with settable route plates, poster cases and the arrival display, all road signs on Roads' sign posts, and the curb plaque), shelters, bus station departure boards and bay displays (`transit.board`: listing the stops around them, paging across a bank, spoken call-outs only through Core's TTS service), and station and platform fit-out made to complement RCMC's stations (`transit.platform`: tactile paving, platform furniture, station signs, tile, columns, canopy, ticket validator), stations (`transit.station`: the subway entrance kiosk built from glass and roof pieces with agency fascias and name boards, the open stair entrance's railing and lit globe lamps, the fare line railing that joins the fare gates, its service gate, line bullets and the agent's booth counter), and airport terminal pieces (`transit.airport`: check-in desks and kiosk, queue stanchions, the security lane, gate desk, boarding pass scanner and seating, working flight information boards, the baggage carousel, carts, gate and wayfinding signs, and the Boarding Pass) and airside pieces (the same package: airfield lights and signs switched a circuit at a time by redstone, the wind sock, beacon and masts, the stand sign, ground equipment on Roads' utility box, a walk-through jet bridge, and a helipad: its marking, laid by `/csmtransit helipad`, and lights; no aircraft); the Transit tab; requires Roads; see `assets/docs/TRANSIT_SYSTEM.md` |
| `modules/healthcare` | `csm_healthcare` | CSM: Healthcare | `healthcare`: the hospital fit-out of GitHub #293: patient rooms and wards (beds as a head and a shared foot, headwalls, monitors, IV poles, cubicle curtains filled from a ceiling track, room furniture, dispensers and carts), a linked nurse call network, the ICU, the ED (its tracking board one of the module's two renderers), the nurses' station (its monitors showing the master panel's calls), surgery support, an operating room set (table, anaesthesia machine, twin surgical light, scrub sink, back table, Mayo stand, kick bucket, supply cabinet, drape trolley), ceiling-hung pendants and lights on a drop column, clinics, dialysis and infusion, imaging (the CT and MRI as a master and invisible parts), the lab, pharmacy and morgue, maternity and the NICU, rehab, the waiting room (a queue with a now-serving display, the other renderer), teaching (a mannequin that lies on a bed or table), the playroom, a baked wayfinding panel that is a drop-in for Transit's airport one, code blue, medical gas, emergency power and infant security; everything settable by `/setblock` and `/blockdata`, wired by `/csmhealthcare`; two tabs, Healthcare - Patient Care and Healthcare - Clinical, and a hidden tab; Core only (the hospital doors and finishes are Building's, the helipad Transit's, the pews and laundry Furnishings'); see `assets/docs/HEALTHCARE_SYSTEM.md` |
| `modules/venues` | `csm_venues` | CSM: Sports & Venues | `venues`: sports venues (GitHub #324: an arena first, then ballparks, football and soccer, bowling, courts, track and pool, golf and a skate park), the seating bowl first (`venues.seating`: stadium seats in ten colours as one tinted set of baked models with no tile entity, 2-deep and 1-deep, folding, rows closing themselves with end standards by actual state; aisle-end seats whose metadata is the row letter and everything else read from the seat beside; companion seat and wheelchair space; aisle step lights; the centre-of-aisle handrail and glass guards following the aisle's nosing line, level, 1 in 1 and 1 in 2, as OBJ; section and portal signs that are Core's wayfinding panel; sitting through Core's `EntityCsmSeat` on click), then the arena's event floor (`venues.arena`: dasher boards and stacked glass, straights closing their runs with end posts by actual state, 45 degree corners and a true radius-8 curve in fifteen OBJ cells a corner, the bench gate and penalty box door opened by a click; rink ice, ice lines and maple court floors set by metadata; the circles, crease and keys as pictures round an anchor block that painted floor blocks find nearby and show their piece of, with no stored state; the hockey goal, a real-size basketball stanchion, the ice resurfacer and ice plant as master-and-parts units (`BlockVenueUnit`); benches and the scorer's table joining into runs), then the arena's scoreboard and AV
(`venues.av`: the centre-hung scoreboard in two sizes, a hung unit whose baked OBJ is the casing,
rings and dark screens and whose renderer draws the score, the clock (worked out from world time,
never ticked, started by click or redstone) and an idle video, edited in a GUI or by NBT; LED
ribbon boards, straights and 45 degree corners whose patterns are one animated texture shown a
quarter a block by position, with no tile entity, and a scrolling-text piece drawing its text
once across the run; championship and retired-number banners tinted from their tile entity;
the line-array speaker, broadcast camera platform and catwalk grating), then the concourse and
premium spaces (`venues.concourse`: concession counters in four finishes joining into runs with an
outside corner, a back counter, the lit concession sign that is Core's wayfinding panel, a baked
3 x 2 digital menu board in four menus, countertop machines (hot dog roller, popcorn, pretzel
warmer, nacho station, soft serve) switched on and off with their moving parts as animated
textures, the beer kiosk unit, a one-way tripod turnstile, free or ticketed, with a ticket
scanner and the Venue Ticket item, the suite bar counter and drink rail, and team store fixtures
tinted in any team's colours from a tile entity; what Furnishings, Life Safety and Transit
already make is reused, not copied), then the exterior and the outdoor pieces every sport shares
(`venues.exterior`: an LED marquee board built to size from one-block cabinets, single-sided on a
wall or double-sided on a clad pylon, its event text and video drawn once across it by its
controller's renderer; gate and section wayfinding blades on Core's wayfinding panel, hung from a
wall bracket; the box office window, a two-block unit joining into a run under lit headers;
`venues.outdoor`: aluminium bleachers, a deck a cell with its seat plank or aisle step above,
rows one or two deep closing themselves with end and back rails by actual state, and their
understructure; press box window and wall modules with outward-leaning glass turning outside
corners; the ticket booth; stadium light towers, a monopole or lattice shaft stacked to height
under an OBJ head of floodlights that lights a grid of the field in front of it with Core's
light-up air (`TileEntityLightHead` on Core's `CsmLightFootprints`, a few cells a tick)), then
baseball and softball (`venues.ballpark`: a full-size diamond
laid with its centre line along the grid, one block a metre, drawn from one geometry,
`ballpark_field.py`: turf in two mowing tones, skin, packing clay and the warning track as
`BlockArenaFloor` floors, turf/clay edges and chalk lines at 45 degrees as standard pieces, the
foul lines on half-integer diagonals so each is a band across a cell's corner; the home plate
area, base cutouts, coach's boxes, on-deck circles, the infield arc in segments and softball's
home area and circle as pictures round marking anchors that painted cells find within seven
blocks, each anchor with a third base side twin drawing mirrored; the pitcher's mound as
heightfield OBJ cells round the rubber's; bases placed off their cell's middle to the diamond;
dugout runs, nets, the outfield wall straight and 45 degree with distance markers drawn by a
renderer, the foul pole; the line score and count boards, their words a renderer's, edited in the
venue text editor), then American football and soccer (`venues.field`: a pro football field and
a 105 x 68 m pitch, one block a metre, from one geometry, `football_field.py`; every line, hash,
number and arc drawn where it really falls in its cell, to the texel, as pictures round a grid of
marking anchors (radius 12) that painted cells find; the mowing tone and the end zone colour are
tints the painted cell takes from the plain turf and end zone beside it, split under each 5 yard
line, so 173 + 81 pictures serve any mowing and colour; goal posts, goals, sideline pieces, end
zone lettering and the football and soccer scoreboards; stadium light heads aimed by an `aim`
NBT offset), then ten-pin bowling (`venues.bowling`: lanes two blocks wide, the centre on the
block edge between a lane's halves, laid end to end to any length, that mark themselves from
their place along the lane (foul line, dots, arrows, range finders, pin deck and pit; the
approach's locator dots) as actual state, with bumpers raised by a sneak-click; lathed pins a
click knocks down; the pinsetter and masking unit for a pair with four lit graphics, the ball
return, settees joining round curved corners, the scorer's console, the overhead scoring
monitors scoring ten frames from score sheet notation, the ball rack, shoe counter, cubbies
and spray), then courts and a track (`venues.lines`: the line art engine, a layout's paint
operations over half-planes and discs, each cell's top cut into convex pieces and baked as quads
with vertex colours, cached by key, so lines and the track's bends fall at their true positions
and radii with no texture per cell; anchors with a tile entity holding the layout, place and
colours, one-state painted cells, plain acrylic and polyurethane in sixteen colours, sand;
`venues.courts`: tennis, pickleball and volleyball nets as runs, the umpire chair, chain-link
fence with windscreen and divider curtain, ball machine, hurdles and other one-block fixtures,
the courtside scoreboard and the lap counter with its bell; the hoop, landing areas, cage,
camera tower and barrier as units; every painted band drawn at least 12.5 cm wide so it stays
continuous at a distance), then a pool, golf and a skate park (`venues.pool`: a third line art
family for the pool floor's lane lines, wall target and touch pad tiles, lane ropes at the
water line from the air above it, numbered starting blocks that place themselves from the lane
number, the pace clock (hands by renderer from world time) and `BlockVenueSign`, the shared text
engine of the pool scoreboard and golf tee sign; `venues.golf`: turf mown in stripes from each
block's position, `BlockEdgeRail` for mini-golf borders and cart path curbs; `venues.skate`:
`BlockSkateElement`, OBJ curves over a staircase of collision boxes); Core only; the Sports & Venues - Seating & Venue tab (shared by every venue), the Arena tab, the Concourse tab, the Ballpark tab, the Football & Soccer tab, the Bowling tab, the Courts, Track & Pool tab, the Golf tab, the Skate Park tab and a hidden tab; see `assets/docs/VENUES_SYSTEM.md` |
| `modules/vehicles` | `csm_vehicles` | CSM: Vehicles | `vehicles`: Immersive Vehicles integration. Its jar carries an IV content pack (pack id `csmvehicles`: an LED lightbar in four colour schemes, a preemption emitter and a siren speaker, all fitting other packs' vehicles by slot type; and, with Parks installed, the tree crew's chip bed for UNU Contractor beds, whose chipper turns dropped logs into Parks' Mulch, a towed brush chipper and a self-propelled stump grinder that grinds by Parks' rules through Core's `CsmStumpGrinders`) and registers `IvPreemptSource`, so any IV vehicle running its emergency lights calls Roads' preempt detectors; requires Roads and Immersive Vehicles (`mts`, the module system's one external dependency); see `assets/docs/VEHICLES_SYSTEM.md` |
