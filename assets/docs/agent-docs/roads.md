# Roads & Traffic: signals, poles and streetscape

Signals, poles and mast arms, the European signal parts, enforcement cameras, sign trusses and
bridge cables, the Streetscape tab, parking, the rail crossing, and the European signals demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_enforcement_cameras.py` -- the red light cameras, speed cameras and flash unit
  (`BlockEnforcementCamera`), in white and black, all hung on the mod's own traffic poles: the
  pole-top units wear a slip-fitter collar that fits every pole width, and the side-arm units
  hang beside the pole with the arm running sideways into it and the device turned to face the
  road, so each is written for both hands and all three pole fits (the `arm` and `polefit`
  actual-state properties, resolved from whichever neighbour is a pole). Also the per-model
  inventory fits, projected rather than guessed; `--check` fails on drift
- `gen_streetscape_covers.py` -- the Streetscape tab's covers (`BlockStreetCover`): manholes,
  utility vault lids, valve boxes, drainage grates and the storm drain marker, each with a
  "(Rusted)" twin where it is iron. A cover is one upward face with a 64 px cutout texture, so a
  round cover is round; textures are drawn as the placer sees them and stored turned 180 (the
  item models turn the icon back). Borrows `life_safety_gen_common.py`'s catalogue and font;
  `--check`, `--fragments`
- `gen_streetscape_utility.py` -- the Streetscape tab's utility boxes (`BlockUtilityBox`,
  `BlockUtilityBoxLabelled`): pad-mount transformers in four sizes, metal and plastic telecom
  pedestals, the low telecom enclosure, buried utility markers, with "(Rusted)" twins of the
  metal ones. A unit up to 2x2x2 is drawn whole by its root; invisible `utility_box_part` blocks
  fill its other cells. The unit box and ID-number decal position in each tab line are measured
  from the model's own elements, and a big unit gets a second, shrunken inventory model;
  `--check`, `--fragments`
- `gen_streetscape_meters.py` -- the parking meters (`BlockParkingMeter`: mechanical and digital,
  one or two heads, the multi-space pay station) and the pay-by-phone sign, on the utility box
  multi-block code. Each head's window is written into its tab line so the meter renderer draws
  exactly on it. Payment is emeralds, or money through the optional SUM economy (reached by
  reflection in `ParkingPaymentSum`, never a build dependency); `--check`, `--fragments`
- `gen_parking_garage.py` -- the parking garage pieces (#302): the gate arm cabinet
  (`BlockParkingGate`, its arm `TileEntityParkingGateRenderer`'s), the ticket dispenser and exit
  pay kiosk (`BlockParkingMachine`) and the Parking Ticket sprite; `--check`, `--fragments`.
  `gen_ev_charging.py` writes the EV chargers (#315: pedestal, wall mount, DC fast; status
  light by blockstate) and the EV stall markings the same way
- `gen_streetscape_bollards.py` -- the bollard styles external road mods lack: cast-iron
  decorative, stainless, crash-rated, pipe with a cover sleeve, flexible delineators
  (`BlockBollardFlexible`, no collision) and the red concrete sphere, a lathed OBJ written through
  the Life Safety catalogue's `extra` files; `--check`, `--fragments`
- `gen_streetscape_street_furniture.py` -- dry-barrel fire hydrants (one lathed OBJ with #body
  and #cap materials, six NFPA colour schemes by retexture), the wall hydrant, the sidewalk
  standpipe, and the news racks (`BlockNewsRack`: a multipart blockstate draws end panels only at
  the ends of a bank); `--check`, `--fragments`
- `gen_streetscape_mailboxes.py` -- the mailboxes (`BlockMailbox`, `BlockMailboxCurbside`): the
  UIA MAIL collection box, cluster box units, curbside boxes whose flag is actual state, and the
  wall bank. Each door's rectangle goes into the tab line from the same layout the front decal
  is drawn from, since a click opens the door nearest the point looked at; `--check`,
  `--fragments`
- `gen_rail_crossing.py` -- the railroad crossing hardware's assets: the flasher's wig-wag lens
  strip with its `_e` companion, the hardware swatch, the flasher and gate JSON models and the
  four blockstates. `gen_rail_crossing_sounds.py` synthesises the crossing bell (numpy →
  ffmpeg → OGG)
- `gen_euro_lenses.py` -- the European signal lens tiles (diffused ball and masked arrow, off and
  on, in green, amber and red, and the masked standing and walking pedestrian figures) that
  `ImageTilerTool` appends to the lens atlas at cells 64-79, plus the pedestrian heads' opaque
  inventory lenses; `--check` fails on drift, `--sheet` writes a contact sheet
- `gen_euro_push_button.py` -- the European push buttons (pedestrian and bicycle): the model,
  drawn at full height and scaled to 75% about the pole face, its front textures (zebra chevron,
  pointing hand, walker or bicycle, signal mark, drawn at their true aspect and squeezed square),
  the ribbed sides, caps and the request lamp off and lit, and both blockstates (the lamp is the
  `requested` actual state); prints the bounding box the block class must match; `--check`
- `gen_euro_button_sounds.py` -- the European push button's click and walk tick, cleaned from a
  field recording passed on the command line (high-pass, spectral subtraction against the
  recording's quiet tail, the click cut out): the click is the locator and the press, the walk is
  the click every 100 ms (Polara's rate) in a file exactly one 20-tick cycle long, so it loops with
  no pause. No `--check`: listen, then commit the OGGs
- `gen_pv_lens_atlas.py` -- the programmable-visibility lens atlas (`lights/atlas_pv.png`) from the
  light atlas: an edge-preserving smoothing that removes the LED dot texture but keeps legends and
  the lens rim crisp and never pushes colour past the disc's alpha; `--check` fails on drift
- `build_euro_signals_demo.py` -- builds the European signals demo world in a flat creative world
  named "Euro Signals Demo" (refuses any other) in the dev client: a gallery of every European
  head on a NORMAL controller with red+amber and the green flash both on, a German junction
  (NORMAL, red+amber, steady red pedestrian clearance), a Mexican one (ADVANCED, US heads, green
  flash, flashing green pedestrian clearance) and an Austrian one (ADVANCED, both). Every
  controller is programmed by NBT; it drives the game through the orchestrator
  (`--instance run-f2eb58`), never a game port
- `gen_sign_truss.py` -- the overhead sign trusses (Roads, `BlockSignTruss`: 1x1 and 2x2
  galvanized box trusses, laced on every face, end frames and base plates from actual state, each
  2x2 block a quarter of the section; the low-profile `BlockSignTrussLight` span and its twin-post
  frames) and the catwalk (`BlockTrussCatwalk`): textures, part models and multipart
  blockstates; see "Overhead Sign Trusses" in `DYNAMIC_GUIDE_SIGN_SYSTEM.md`;
  `--check`, `--fragments`
- `gen_bridge_cables.py` -- the bridge cable anchors (Roads, `BlockBridgeCableAnchor`: socket,
  clevis, saddle, deck anchor) drawn in the sign trusses' textures, their blockstates (six
  facings for the wall-mounted two, four for the others), the Bridge Cable Tool's sprite and
  model, and the four languages' lang lines; the cable is the renderer's, and each anchor's
  attach point and hitbox are shared with `BridgeCableAnchorKind.java`; `--check`, `--fragments`
- `gen_mast_arm_curves.py` -- the realistically scaled mast arm upsweeps: sweeps a tapered
  parabolic tube, splits it across the block cells it passes through, and emits one OBJ per
  cell plus all 30 blockstates AND the Java enum holding the cell layout, so the placement code
  cannot disagree with the geometry it was split on. It has no `--check`: running it rewrites
  the tree, so `git status` afterwards is the check
- `gen_concrete_poles.py` -- the concrete traffic poles (round and octagon, thick and thin):
  each end's model carrying its own half of the shaft (plinth, cap, tenon, plain), the four
  blockstates, the concrete accessories (silver blockstates retextured, their 16-gon body
  redrawn with UVs that suit concrete, and again as an octagon for the straight vertical
  sections) and the `concrete_light_pole` texture itself; `--check` fails on drift
- `gen_preempt_detector.py` -- the preempt detector's model and blockstate: a small black optical
  head (octagon body, lens in a bezel, cable connector, stepped sun-shield fin) on a mount stub
  reaching down to a thin traffic pole in the cell below, so it sits on top of a mast arm; and on
  the same stub the arm-mounted confirmation lights (white and blue PAR lamps in a yoke, a red 360 degree
  dome), whose lens boxes `BlockPreemptConfirmationLight` must match; and the span wire versions
  of all four (`<name>_span`, #321): the device without its stub, hung on a collar and pipe from
  the top of the cell under a span wire mount; and the pole-side versions (`<name>_pole`, #338):
  the device on a bracket and clamp band reaching back into a vertical pole's cell; `--check`
- `gen_pedestal_pole.py` -- the pedestal (pedestrian) traffic pole: lathes the tube, domed cap,
  tapered base with its access door and the clamp bracket as OBJ, and emits the five
  blockstates plus lang/tab fragments; `--check` fails if the tree has drifted from the script
- `gen_pole_finials.py` -- the six cast finials a pedestal pole wears on top (ball, acorn,
  urn, spire, flat cap), each one lathe sharing the collar that sleeves over the pole below,
  plus the blockstate; borrows the pedestal pole's lathe rather than copying it, and the urn
  is cut by a fluted lathe that takes its normals from the surface; `--check` fails on drift
- `gen_pole_fit_models.py` -- the `_thin` and `_pedestal` copies of every side-mounted arm that
  clamps to a pole (light mounts, NOV tapered masts, SCE light mounts), with
  the pole-end plate moved back to the thinner pole's skin, and the `polefit` variant block in
  each arm's blockstate that swaps them in. Its catalogue is the one list of which arms adapt
  to the pole behind them (`ICsmPoleFitted` / `CsmPoleFit`); `--check` fails on drift
