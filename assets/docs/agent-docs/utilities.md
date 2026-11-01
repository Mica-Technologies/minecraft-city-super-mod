# Utilities

Meters, water, sewer, gas and telecom, sidewalk doors and the Utilities demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_utilities_meters.py` -- the Utilities tab's building service meters (Utilities module):
  electric meters (digital and analog, on a ringless socket), the blank meter socket, the meter
  bank that joins side by side (`BlockUtilityRun`, end flanges only at its ends), the service
  disconnect, the main breaker panel whose door a click opens (`BlockUtilityPanel`), the service
  entrance switchboard (Roads' `BlockUtilityBox`, 1 x 1 x 2), gas meters single and in a bank
  (one header, capped at one end and fed by the riser and regulator at the other), the water
  meter setter and four utility room labels. A picture on a round part is one square plane with
  the octagon cut out of its texture's corners; the meter faces, the gas index and the water
  register are animated textures, so nothing ticks. Parts shared between blocks (the meter, the
  gas meter body and outlet) are one model file each. Text writes keep each file's line
  endings; `--check`, `--fragments`
- `gen_utilities_water.py` -- the Utilities tab's water system (Utilities module), importing the
  meters generator's catalogue and element helpers: the tower's stacking pieces as JSON (legs and
  riser with pier or cap, the tie rod corner to corner of a block that X-braces a square panel,
  struts, the caged ladder, the 16-sided pedestal column section as eight turned rectangles);
  the tanks (two multi-leg sizes with their balcony, two pedestal sizes, two ground tanks stacked
  a layer at a time) as one 16-gon OBJ lathe per shape, clipped at the faces of each three-block
  tile and written for one tile of each quarter-turn orbit only, the blockstate turning it to the
  rest; the band tiles' shells and a decal per town name off one 128 px sheet; and
  `TankShapes.java`, every tank's cell map voxelised from the same profile, so collision and
  drawing cannot disagree. Also the pump station (the pipe that joins like the standpipe, inline
  valves, flow meter and air release valve, the pumps and pressure tank on Roads' utility box
  with pipe nozzles, the control panel with its animated HMI), the air release enclosure and
  vault, the backflow hot box, and the chemical feed skid and chlorine cylinder scale. OBJ v runs
  down the texture (a multipart OBJ takes no flip-v); `--check`, `--fragments`, `--report` (the
  tanks' OBJ quads)
- `gen_utilities_sewer.py` -- the Utilities tab's sewer and stormwater pieces (Utilities
  module), importing the meters generator's catalogue and the water generator's OBJ polygons and
  lathes: the access hatches (one leaf a block, collar, frame, the leaf shut or stood on its
  hinge, the wet well's safety grate), the lift station control panel (beacon and flood light
  swapped by `on`) and standby generator (display and rain cap swapped by `on`) on Roads' utility
  box, the curb inlet (end walls and Streetscape's STORM lid only at a run's end), the headwall
  (coping and end faces by neighbour; its face round the pipe one convex polygon per edge of the
  16-gon hole, the pipe and flap gate OBJ lathes turned to run north), the wingwall (one slope
  over two OBJ wedges), riprap (two stone drawings, four turns, picked by position), the outlet
  riser and spillway, and the manhole rings, base and cone as OBJ lathes, the cutaways their back
  half with the cut faces added. Reuses the water system's textures; `--check`, `--fragments`,
  `--report` (OBJ quads)
- `gen_utilities_gas_telecom.py` -- the Utilities tab's gas yard and telecom pieces (Utilities
  module), three catalogues (`utilities/gas`, `utilities/signs`, `utilities/telecom`) on the
  meters generator's catalogue and the water generator's helpers: the gas pipe (welded, no end
  face at a block face; fittings capped there), the ball valve, regulator and turbine meter (the
  index an animated texture), the vent stack's top, the skid (beam only where it stops, a pipe
  stand turned for either axis), the line heater and odorant tank on Roads' utility box; the six
  signs from one 128 sheet, each a wall plate and a fence plate reaching back to the mesh; the
  fibre cabinet (doors shut or swung out), the cell cabinets on one body, the ice bridge (ends and
  post by neighbour) and its stanchion, the GPS antenna, the monopole section and the antenna
  array (its three sectors an OBJ of boxes turned 120 degrees), and the small cell's canister over
  three collars and its radio over three strap sets. `--check`, `--fragments`, `--report` (OBJ
  quads)
- `gen_utilities_sidewalk_doors.py` -- the Utilities tab's sidewalk doors (#310,
  `BlockSidewalkDoor`): four styles (aluminium, black steel and galvanized double leaves, a
  single painted leaf on gas struts) in six sizes, one block a cell, each cell's part of the door
  from its neighbours (`left`, `right`, `front`, `back`). The edge (collar and frame) and the shut
  plate with its seam are JSON parts turned by the multipart; the standing leaves (up to three
  blocks tall) and their struts are OBJ cut into one-block slices; `--check`, `--fragments`,
  `--report`
- `build_utilities_demo.py` -- builds the Utilities demo world in a flat creative world loaded in
  a dev client, over MCMCP, in the Transit demo's style (`--client-port`, `--server-port`,
  `--config`, and `--only` to rebuild one area): a street of buildings with their meters and
  panels; a lattice and a pedestal water tower in both sizes with their bands set differently,
  the ground tanks and the pump station; a storm street with curb inlets, the lift station
  compound (a hatch open, the panel on a lever, the generator running), an outfall into a pond
  with its riser and spillway, a manhole cut away beside a trench; the gas regulator yard, the
  cell site and small cells; a short Power Grid pole line and an overview board. Tanks are
  placed with their items (their placement writes the parts), everything else directly. It
  checks the Utilities tab's registration list against what it placed and puts the rest on
  signed plinths. The ground is raised to y 12 for the wet well, vault, pond and trench
