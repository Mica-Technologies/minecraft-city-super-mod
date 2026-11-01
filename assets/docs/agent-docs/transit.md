# Transit

Fares, stops, shelters, platforms, airport, boards, stations and the Transit demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_transit_fare_vending.py` -- the fare vending machine's model, textures and blockstate
  (Transit): a free-standing machine two blocks tall in the invented CITYLINE livery, drawn from one
  block (so placed machines keep their metadata) with ambient occlusion off, since its upper half
  sits in the air above its block; textures at 4 texels a unit, each face's uv exactly its drawn
  window; `--check`
- `gen_transit_stops.py` -- the bus stops (Transit), built on the road sign system: the four
  invented agencies' double-sided flags with their three route plates, the timetable and route map
  cases and the arrival display, each a length of Roads' sign post with its piece on the front and
  the three shift models every road sign has (`sign_*`, `_setback`, `_back_to_back` at 28.3, which
  `SignShiftModelTest` and `SignFaceDepthTest` check), in Forge sign blockstates. The flag hangs
  off the side of the post, reaching right or left, so it has those three models per side, picked
  by its `hang` property (shift crossed with side), with no `shift` variants (its state mapper
  leaves `shift` out of the model locations); the flag models are shared by the agencies, the
  blockstate filling the plate slots with the plate or a clear texture from `route1`..`route3`. Also the curb plaque; `--check`, `--fragments`
- `gen_transit_shelters.py` -- the bus shelters (Transit): glass and steel, cantilever canopy and
  flat roof, two blocks tall, joining along their length and front to back from actual state
  (`left`, `right`, `ahead`, `behind`). Every part is written once in shelter coordinates (y 0 to
  32) and cut at the block line into a model per half; liveries are child models naming the
  agency's `frame` and `fascia` textures. The glass shelter's back row stops 1.25 px short of its
  left end, the empty frame Signage's shelter ad panel is set against; the collision boxes in
  `BusShelterStyle.java` share its numbers; `--check`, `--fragments`
- `gen_transit_platforms.py` -- the station and platform fit-out (Transit), made to complement
  RCMC's stations rather than repeat them (no platform, edge, speaker, board or track piece):
  tactile paving overlays (domes and bars, two drawings and a turn per block like
  `gen_flooring.py`), the joining platform bench and perch, the help point and emergency point,
  CCTV, the hanging clock (hands by renderer), the clear-bag bin, the stepped number and station
  name signs (a texture per value), hanging wayfinding signs with mirrored arrows on their backs,
  the gap warning and network map, subway tile with agency frieze bands, stacking columns, the
  four-way canopy and the ticket validator; `--check`, `--fragments`.
  `gen_transit_sounds.py` synthesises the help point chime and the validator's tones
- `gen_transit_airport.py` -- the airport terminal pieces (Transit; terminal only, no aircraft
  or airside): check-in desks and bag drop scales that join into one counter, the airline panel
  stepped by metadata, the self check-in kiosk, queue stanchions whose belts reach every
  neighbour, the security lane (X-ray with an animated belt, rollers, divesting table) and trays
  that drop onto it, the gate desk, boarding pass scanner, beam seating on the platform bench's
  seats, the departures and arrivals boards (screen texture only: the rows are the renderer's,
  its layout numbers shared with `TileEntityFlightBoardRenderer`), the baggage carousel (sixteen
  tops by open sides, the plates animated clockwise), carts and the cart rack, the gate sign
  (letter and number cells, 24 textures for 80 gates) and five hanging wayfinding signs with
  generic pictograms; airlines listed as `FlightSchedule.java` lists them; `--check`,
  `--fragments`. `gen_transit_sounds.py` synthesises the kiosk's printer
- `gen_transit_airside.py` -- the airport airside pieces (Transit; no aircraft of any kind),
  importing the terminal's and the platforms' generators: elevated and inset airfield lights, the
  approach light bar, the beacon (eight lens sides, each an animated strip a frame apart, so the
  flash runs round it) and the obstruction-lit wind sock and antenna mast, each with a lit and an
  unlit lens swapped by `lit` and an `_e` companion for the lit one; the four lit airfield signs
  (legend textures swapped by `legend`, the direction sign's two models by `arrow`); the stacking
  airfield mast; the stand sign on the gate sign's cells; ground equipment for Roads'
  `BlockUtilityBox` (two blocks long, drawn whole by the root); and the jet bridge (tunnel, cab,
  rotunda, drive leg, rotunda column) drawn up to a block past its cell, its section numbers
  shared with `BlockJetBridge`'s collision boxes; and the sloped tunnel (`BlockJetBridgeSlope`), the
  level corridor sheared along its length into one OBJ a grade and step; the large bridge (three
  blocks wide and four tall inside: the same elements scaled by `LJ_SX`/`LJ_SY` into OBJ, the
  boxes by `BlockJetBridge.toLarge`); and the turns (`BlockJetBridgeTurn`: corner room, curve and
  U-turn at both sizes), each built once from the tunnel's section, cut at the block lines into
  one OBJ a cell, with `JetBridgeTurnShape.java` (cells, ends, collision boxes) written from the
  same walls after a simulated walk shows every box is reachable; and the helipad
  (`BlockHelipadMarking`: the 5 x 5 H marking drawn whole, cut into cells and each found again as
  a picture and a quarter turn, so a cell's piece is its metadata, with `HelipadLayout.java`
  written from that search for `/csmtransit helipad`; the perimeter stripe and mass and size
  boxes; green, yellow and flood lights on the airfield lights' `helipad` circuit); `--check`
  (the Java too), `--fragments`
- `gen_transit_boards.py` -- the bus departure board and bay display (Transit; bus only, rail
  boards are RCMC's): the monitor and its screen texture (header with a bus pictogram, column
  heads' and rows' bands; every word is `TileEntityBusDepartureBoardRenderer`'s, which shares its
  layout numbers), and the bay display's case, hood and dark LED screen, sized exactly as the
  arrival display's so the two share its panel's lists; both hung on rods where there is no
  wall; `--check`, `--fragments`
- `gen_transit_stations.py` -- the stations (Transit), importing the platforms' generator: the
  entrance kiosk's glass (pane-like arms from a post a block, head rail and kick plate only where
  the wall stops, a lone block drawn as a panel through multipart `OR` rules) and its roof in the
  four agencies' fascias with the name boards (none, SUBWAY, METRO and the ten station names, a
  child model per legend turned to each outside side), the lit globe lamp (octagon tiers, `_e`
  companions, three colours by texture), the painted stair railing and stainless fare line
  railing as vanilla fence multiparts with plates only on straight runs, the service gate's closed
  leaf and the same leaf swung a quarter about its hinge, the sixteen line bullets and the booth
  counter; the legends, lines and globe colours are listed as the Java lists them; `--check`,
  `--fragments`
- `build_transit_demo.py` -- builds the Transit demo world in a flat creative world loaded in a
  dev client, over MCMCP (borrowing `csm_bench.py`'s client; `--client-port`, `--server-port` and
  `--config` aim it at a client other than the dev client): a bus street with every agency's
  stops, flags hung both ways, back to back and set back, every shelter and Signage's ad panel, a
  bus concourse with boards; a subway station with the kiosk, open stair, fare line, booth and an
  open-cut platform with the whole fit-out; an airport terminal and its airside with the jet
  bridge, a stand and four lever-switched light circuits. It checks the Transit tab's
  registration list against what it placed and puts the rest on signed plinths. The landside is
  raised to y 8 so the station's lower level fits above bedrock
