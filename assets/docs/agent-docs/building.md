# Building Materials

Wall materials, glazing, finishes, doors, stairs and railings, ceilings, framing and the construction site, and the ceilings demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_cmu.py` -- the four concrete masonry sets: the coursed textures, and the five
  blockstates and six models each set needs. The bond is drawn at 8 x 4 px, two units across
  a block and four courses up it, because a real 8 x 16 in unit does not divide sixteen
  pixels; `--check` fails on drift
- `gen_masonry.py` -- the four brick colours: each a block/stairs/slab/fence set, reusing
  `gen_cmu.py`'s blockstates and models, plus soldier, header and weep-hole trim blocks
  (`BlockBrickTrim`, one class constructed by name). Drawn at 32 px, 16 x 4 units and eight
  courses a block, because at 16 px the only bond that tiles is the CMU's own; `--check` fails
  on drift
- `gen_stucco.py` -- the three stucco finishes (smooth, sand float, knockdown), each a set on
  `gen_cmu.py`'s blockstates with one 32 px texture on every face. The noise wraps at the tile
  edge and its lattices (3 and 5 cells) deliberately do not divide the tile, or the mottling
  reads as a grid; `--check` fails on drift
- `gen_siding.py` -- the four siding profiles (fiber cement lap, board and batten, cedar shingle,
  vinyl), one colour each, as sets on `gen_cmu.py`'s blockstates. Courses are 8 px on a 32 px
  texture so a slab's edge lands on a course line; `--check` fails on drift
- `gen_screen_blocks.py` -- the breeze blocks (`BlockBreezeBlock`, four patterns in three
  colours: the pierced face, a darker web drawn on seven slices inside the block so a hole has
  walls, and the plain edge) and the slatted timber screens (`BlockSlatScreen`, teak, oak, walnut:
  slats on one edge of the cell, rails capping each run by actual state); `--check`,
  `--fragments`
- `gen_cladding.py` -- the four metal and panel cladding profiles (corrugated, standing seam,
  insulated panel, composite panel). The profile is shaded into a 32 px texture rather than
  modelled, with every rib pitch dividing the block; `--check` fails on drift
- `gen_stone.py` -- the three stone veneers (ashlar, fieldstone, cast stone). Fieldstone is a
  Voronoi partition measured on a torus, so its stones wrap across block seams; `--check` fails
  on drift
- `gen_window_treatments.py` -- window blinds, shades and curtains (`BlockWindowTreatment`): the
  parts that belong to the whole blind (headrail, bottom rail, bunched curtain) drawn only on the
  course or end they belong to; `--check` fails on drift
- `gen_flooring.py` -- the floor finishes: one-pixel overlays (`BlockFloorFinish`, carpet, vinyl
  and ceramic tile, hardwood, polished concrete, rubber) whose blockstates pick a turn or a second
  drawing per block position so a floor shows no repeat, full blocks of the other finishes
  (`BlockFloorFinishBlock`, the same textures and picks), and the polished concrete and hardwood
  full-block sets; `--check` fails on drift
- `gen_stairs_railings.py` -- the Stairs & Railings tab (`BlockStairRailing`, `BlockStairFlight`):
  27 railings in eight styles (picket, round-rail picket, two-line pipe, cable, glass, wall
  handrail, square and turned balusters) and six stairs. Every railing part is built from one
  nosing line per shape (level, mid, top, foot, each both ways) as OBJ, a set and MTL per finish,
  with a JSON icon from the level cell; the stairs keep vanilla's volumes and rotations without
  uvlock so the nosing stays at each tread's front; `--check`, `--fragments`, `--report`
- `gen_ceilings.py` -- the modern ceiling finishes (`BlockCeilingFinish`, full blocks): tegular,
  fissured and embossed tiles, perforated metal and pressed tin as `cube_all` textures with the
  relief shaded in; wood slats and corrugated metal storing the axis they run along; the open cell
  and exposed T-bar grid as cutout elements at the bottom of the block; `--check`, `--fragments`
- `gen_acoustic_ceilings.py` -- the hanging acoustic baffles (`BlockAcousticBaffle`, joining
  along their axis, edges only at run ends) and clouds (`BlockAcousticCloud`) in the linear
  lights' ten finishes, drawn with the same Core textures (imports `gen_linear_lights.py`);
  `--check`, `--fragments`
- `gen_wall_finishes.py` -- wall finishes hung on any wall (`BlockWallFinish`: paint, ceramic
  tile, acoustic panels, beadboard, slat wall) with caps, edge trims and frames drawn only at the
  edges of a joined run, and the corner guards (`BlockCornerGuard`) that wrap an outside corner
  into the next cell; `--check` fails on drift
- `gen_doors.py` -- the doors: two halves per door, left and right hinge, and the open pose written
  as the closed model turned a quarter about the hinge pivot the swing renderer shares; the door
  closer's parts and item; and, through `gen_cathedral_doors.py`, the cathedral doors (a leaf's
  cells, the arch head stepped a pixel row at a time, geometry parents shared by the three
  variants), the tympana and the carved panelling; `--check` fails on drift
- `gen_garage_doors.py` -- the garage doors, opener and hanger: textures, the part models drawn
  on the edge of the door they belong to, the sectional door's ceiling runs and tracks and the
  opener's rail as OBJ per length, and the multipart blockstates; `--check` fails on drift
- `gen_glazing.py` -- the glazing: eight kinds of glass (clear, three tints, one-way, wired,
  bullet-resistant, frosted) as blocks and panes that join into one window with a frame only
  around its outside. One-way glass is two textures on two faces, since the back of a face is
  never drawn; a pane arm is drawn east and west, never turned 180; `--check` fails on drift
- `gen_scaffold.py` -- the frame scaffold: its textures, part models and the multipart blockstate
  that picks frames, braces, deck, jacks and guardrails from the neighbours, plus the three add-on
  items' icons. Stored: the frame axis and the add-ons (ladder frame, netting, casters), one bit
  each; everything else is actual state, so the look can change here without touching a placed
  block. Sides are three-valued (scaffold/open/rail) to hold the state count at 5,184; `--check`
  fails on drift
- `gen_formwork.py` -- the construction site's formwork, shoring and rebar: wall and column forms,
  the stack-aware post shore, and rebar mat, dowels, column cage and bundle. Reuses gen_scaffold's
  element helpers so every face has fitted UVs; `--check` fails on drift
- `build_ceilings_demo.py` -- builds the modern ceilings demo in a flat creative world named
  "Ceilings Demo" (refuses any other; `--create` makes one) through the orchestrator by instance
  id: a drop-ceiling corridor with a section of each ceiling finish and every troffer in its tiles
  (a plenum with pipe and duct over the open grids), an open office of baffles over hung linear
  runs with clouds, ducts and a pipe rack, and a gallery of every light, baffle, cloud, duct and
  pipe finish. Sets blocks only, and checks every block the five ceiling generators register was
  placed
- `gen_crane.py` -- the tower crane mast in three liveries: 3D corner chords, and the lacing drawn
  into a cutout texture on a plane per face -- a 1x1 face's chord-to-chord diagonal is not an angle
  an element can be turned to, and a texture diagonal can be any angle and meets the chord at the
  cell edge; `--check` fails on drift
- `gen_earthworks.py` -- the trench plate (edge bar only on open sides, so plates side by side are
  one plate), the stacking trench box, and soil/gravel/sand stockpiles in eight layers like snow;
  `--check` fails on drift
- `gen_logistics.py` -- the construction site's loads: brick, block, drywall and bagged concrete
  pallets, lumber stack, insulation rolls, pipe and conduit bundles, wire spool (one axial class,
  `BlockSiteAxialProp`); round things are a square plus the same square turned 45; `--check`
- `gen_facilities.py` -- build-to-size shipping containers and roll-off dumpsters (four colours,
  `BlockSiteShell`: walls only on outside faces, rails only on outside edges), the job trailer's
  wall and window (`BlockJobTrailer`: built hollow, each face inside or outside by whether its
  space has trailer above and below; its door is a `gen_doors.py` door), plus the portable toilet,
  gang box and concrete washout (`BlockSiteFacingProp`); `--check` fails on drift
- `gen_fencing.py` -- the site and chain-link fences (one class, `BlockSiteFence`): temporary
  fence with and without privacy screen, silt fence, stacking chain-link in two finishes and its
  barbed-wire top. The mesh is a zero-thickness cutout plane, a panel is drawn once running east
  and turned by the multipart blockstate; `--check` fails on drift
- `gen_framing.py` -- every asset the framing family ships: the textures, the shared geometry, and
  each of the 32 blocks' models and blockstate, from one catalogue. `--check` fails on drift,
  `--fragments` prints the lang and tab-registration lines. It was made to reproduce the
  hand-built first block byte-identically before any other block entered its catalogue, which is
  the only way to know a generator describes what was actually looked at in game
