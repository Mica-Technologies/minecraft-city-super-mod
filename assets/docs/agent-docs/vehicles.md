# Vehicles

The Immersive Vehicles pack generators and the Vehicles demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_vehicle_parts.py` -- the CSM: Vehicles module's Immersive Vehicles pack (`csmvehicles`):
  the LED lightbar's one OBJ and its four colour schemes' JSON and dark-lens textures, the
  preemption emitter and the siren speaker, their item icons and item models (under
  `assets/mts/models/item/`). Lamps are `&` OBJ objects lit on `EMERLTS`; flash timing is
  `a_b_c_cycle` variables, lit b - 1 ticks of a + b + c; `--check` fails on drift.
  The tree crew's chip bed (its crate and collector/crafter effectors, under the `treecrew/`
  subfolder the pack definition activates with `csm_parks`) is written here too.
  `gen_vehicle_sounds.py [name ...]` synthesises the siren's wail, yelp and hi-lo, the
  chipper's running loop and crunch, the fleet's engine and horn sounds, and the doors'
  (`door_open`, `door_close`, `rollup`), and the reversing alarms (`backup_whitenoise`,
  `backup_beeper`, recreated from measured recordings: burst timing, pitch, band levels).
  `gen_vehicle_fleet.py` writes the fleet: the fire engine, ladder truck (its aerial three
  objects chained by `applyAfter`, deployed by one AERIAL switch), ambulance, police SUV, and the
  public works dump truck, power company bucket truck and rollback tow truck (amber BEACONS,
  never an emergency switch, so they never preempt; DUMP, BOOM and BED deploy them; the tow
  truck's flatbed and wheel lift are real IV hitches), the Metro transit bus (plus the four
  Transit agencies' liveries; DOORS; a TSP emitter switch that starts on, through
  `rendering.initialVariables`, which ADVANCED mode's transit signal priority answers), the civilian pickups (the Halden H-150 XL, XLT and service truck, and the Chevrolay
  Silverline 1500, parody brands, each its own builder after its real truck; trim chrome or
  black by livery) and the utility cap, a part for every pickup's bed slot with a ladder rack
  and a BEACONS beacon, the Subaroo cars (the Impressa hatchback and the WRZ STX, all-wheel
  drive, the STX the fleet's fastest at about 100 mph),
  towing hookups on every vehicle, each vehicle geared to a top-speed ceiling (`ceiling_mph`; `--speeds` prints a model of IV's physics, about 75 mph at the default carSpeedFactor), braked and steered to a figure in g (`brake_g`, `corner_g`, by weight class; `brakingFactor` and `steeringForceFactor` are computed, `--speeds` prints the stops and cornering), 35 liveries (OBJ built
  from boxes on named cells of a 128 px texture, one PNG per livery, lettering in the Life Safety
  pixel font, `lightObjects` written from the same spec as the lamps, item icons projected off
  the boxes) and their own wheels, seat and engines as default parts. Also the tree crew
  (#251): the towed brush chipper (its start lever a click box toggling CHIPPER, its chip box the
  chip bed's own part), the equipment trailer (a mounted `tow_flatbed` deck worked by a click on
  its tie-down) and the self-propelled stump grinder (GRIND lowers the boom; the grinding is
  `StumpGrinderMachines` through Core's `CsmStumpGrinders`, Parks' rules), the trailers on
  `trailer_standard` hookups and a rear `trailer_standard` hitch on the work trucks. Working
  doors on every vehicle (`Vehicle.door`: a region declared before the boxes it takes, which are
  cut at its edges into the door's object; glass into `<door>_window`; click rows that ride the
  door; side doors, hatches, trunk, tailgates, barn doors, roll-up compartments by SCALING,
  hinged utility body and tool box doors, the utility cap's doors), a dome light per cab on the
  `door_*` variables, steering wheels turning on `rudder`, the bus's saloon lights, wipers (an
  INHIBITOR on WIPERS, then a rotation on a flash cycle; raked glass turns about its normal),
  hazards (turn lamps visible on the cycle, lit by the turn signal plus HAZARDS), cab fittings
  (`interior`: a cluster lit on `running_light`, mirror, visors, console; the seat's headrest),
  knuckle booms on the bucket and signal trucks (`knuckle_boom`, LIFT/ELBOW/SWING) and a
  three-section telescopic boom on the boom truck (`csm_boom_truck`, LIFT/SWING/REACH), steered
  by hand from the panel (`BOOM_MANUAL`, an INHIBITOR on BOOM between the preset and the manual
  animations) with a seat riding each bucket (`KNUCKLE_SEAT`, `TELE_SEAT`), the signal
  maintenance truck (`csm_signal_truck`, `arrow_board` on ARROWBOARD), the reversing alarm
  parts on the work trucks (`BACKUP_ALARMS`, a white noise alarm fitted, a beeper to swap in,
  sounding on `engine_reversed_1`), the plow's curved
  moldboard (`plow_moldboard`) lowered by PLOW and clearing snow while down (`csm_snowplow`, an
  invisible IV SNOWPLOW effector on a permanent slot), raked windshields as slanted pieces
  (`raked_windshield`, `Vehicle.slab`/`poly`),
  32-sided wheels with shoulders and a set-back rim face per style (`WHEELS`' style,
  `RIM_RATIO`), wheel openings cut from the body (`Vehicle.wheel_arch`, the police SUV), headlight cones (`LOW_BEAM`, `HIGH_BEAM`,
  `FOG_BEAM` on the lamps' blendable components), and the `csm_car` panel with HIGH BEAM, FOG,
  WIPERS and HAZARDS buttons; `--check`
- `build_vehicles_demo.py` -- builds the Vehicles demo world in a flat creative world loaded in
  a dev client with every module, over MCMCP: a junction of Main St and four-lane Transit Ave on
  far-side mast arms, an ADVANCED controller with a detector-called preempt on each approach and
  detector-called transit priority with queue jump add-ons on Transit Ave, a CITYLINE stop, a
  parking lot holding every livery of every vehicle (emergency, buses and work trucks, and a
  third row of the pickups and Subaroos) plus a chest of the pack's parts, and a showroom east of
  the lot: one of every model with each door clicked open from outside (the click boxes read
  from the vehicle's JSON, and the utility cap's), with signs on what to try. Vehicles are
  placed with their items (an item given NBT spawns without default parts), each from beside
  its bay so the 90-degree placement points it into the bay, filling from the far end;
  `--only roads|lot|vehicles|showroom`; `--instance run-f2eb58` drives the game through the
  MCMCP orchestrator (prefer it to the fixed ports). Built 2026-11-24 as "CSM Vehicles Demo"
