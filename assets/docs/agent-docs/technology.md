# Technology

Computers, networking, racks, wireless, the school set and the Technology demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_technology_common.py` -- what the Technology generators share (not run on its own): the
  Life Safety catalogue pointed at `modules/technology` (writing, `--check`, `--fragments`,
  `model_depth.py` over everything written), the `Pic` face picture drawn in a face's own units
  and squeezed into a square texture, the fitted-UV element helper `E`, the shared swatches, the
  animated strip helper, the Micafi mark (an invented brand's own pixel logo), and `emissive`,
  which gives every texture with lit LEDs an OptiFine `_e` companion from the same drawing
- `gen_technology_computers.py` -- the Computers & Peripherals tab: gaming rigs (a painted,
  animated glass side rather than a see-through one), monitors (`BlockDeskScreenFactory`: the
  desktop, game, code and idle screens as 64 px animated textures picked by the blockstate; the
  ultrawide's wings turned 22.5 degrees), keyboards from one 256 px shelf-packed layout sheet
  (`BANDS`, each layout at its true aspect), mice, pads, desk extras and the invented consoles,
  their controllers and a VR headset as item sprites. Everything sits at the bottom of its block;
  creative order by registry prefix (`_GROUPS`); `--check`, `--fragments`
- `gen_technology_networking.py` -- the Networking & IT tab's desktop gear: Micafi switches with
  animated port LEDs, the gateway, Control Hub, the Corvane modem, PoE injector, wall jacks and
  the floor box, and the wire basket and ladder cable trays (`BlockCableTray`: middle, arm and
  rail parts drawn once and turned by a multipart, lying or hung on rods, a charcoal item icon
  for the black ladder; full twins carrying a bundle of mostly black and blue cable in the
  middle and arm parts, so it runs on and turns with the tray) and the threaded rod hanger (`BlockCableTrayHanger`) that stacks under a
  ceiling so a hung tray sits lower, a rod on each side where the tray beneath has a rail;
  `--check`, `--fragments`
- `gen_technology_rack.py` -- the racks: the 42U open frame and cabinets (two cells, side panels
  only at a row's ends), the 12U wall cabinet, the fourteen rack-mount units' item models and
  their one animated 128 px unit sheet (full: the next unit needs a second sheet), the seven
  pre-filled racks' models from the same unit boxes as `RackUnitBoxes` (which
  `RackUnitBoxesTest` holds them to), `RackUnitType.java` and `RackFrame.java` (units only ever
  appended: their ordinals are saved), and the units' recipes in Core's recipes folder, each
  conditional on `csm_technology`; `--check`, `--fragments`
- `gen_technology_wireless.py` -- the Micafi access points and outdoor radios: ceiling APs (up or
  down from the face clicked), wall and in-wall APs, the outdoor mesh AP, the Beam bridge on its
  wall arm and on a pole clamp, the Station CPE, the sector antenna, two dishes and the Link 60;
  the ring and link lights as swapped textures, round faces as a picture cut to the prism's
  outline that is the part's only end face (16-gons from `round_z` for the dishes and the Beam),
  and the thin and pedestal pole fits through `gen_pole_fit_models.fitted`; the four diagonal
  facings of every pole device (a Forge `transform` carrying the whole model 45 degrees round the
  pole's axis, read through `BlockMicafiDevice.PoleFitted`'s state mapper), and the wall-arm
  Beam split into arm and a head submodel the `aim` variants turn about its ball joint;
  `--check`, `--fragments`
- `gen_technology_school.py` -- the Technology tab's school set: the Micaplex clocks (dial
  textures; each block's dial centres, radii and planes written into its tab line from the
  numbers the model is drawn with, so `TileEntitySchoolClockRenderer` draws the hands exactly on
  the face), the clock/speaker panels (two halves), the PA speakers, the bell schedule controller
  and the hallway bell; `--check`, `--fragments`. `gen_technology_school_sounds.py` synthesises
  the bell, tone and chime
- `build_technology_demo.py` -- builds the Technology demo world in a flat creative world named
  "Technology Demo" (refuses any other) through the orchestrator by instance id (default
  `run-f2eb58`), in a dev client with every module: an office of workstations on full-block desks
  carrying the whole Computers tab and a gaming corner; a server room with two rows of fillable
  cabinets filled through the tile entity's `u` byte array, an open frame, two rows of pre-filled
  racks, hung basket and ladder trays making a corner and a tee, ceiling APs; a network closet
  with the wall cabinet and desktop gear; and Roads poles on the roof carrying every outdoor radio,
  aimed at a second building. It checks both new tabs' registration lists against what it placed
  and puts the rest on signed plinths. Sets blocks and runs commands only
