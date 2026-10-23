# Interior Finishes

What finishes a building on the inside, in the **Interior Finishes** tab of `csm_building`: the
ceiling tiles that were migrated here, and the decor track that started with window treatments.
The glazing the window treatments hang against is in `WALL_MATERIALS.md` § Glazing.

| Family | Blocks | Generator |
|---|---|---|
| Ceilings | the popcorn ceiling and ceiling tiles (`BlockPCC`, `BlockCT*`, `BlockDCT*`) | -- |
| Modern ceilings | Tegular Ceiling Tile (White, Light Grey); Fine Fissured and Square Embossed Ceiling Tile; Perforated Metal Ceiling Tile (Silver, White); Pressed Tin Ceiling (Silver, Antique Copper); Wood Slat Ceiling (Light Oak, Walnut); Corrugated Metal Ceiling (Silver, White, Black); Open Cell Ceiling (Black, White); Exposed Ceiling Grid (White, Black) | `gen_ceilings.py` |
| Hanging acoustics | Acoustic Baffle and Acoustic Ceiling Cloud, each in the linear lights' ten finishes (Black, Silver, White, Light Grey, Charcoal, Navy, Sage, Terracotta, Light Oak, Walnut) | `gen_acoustic_ceilings.py` |
| Window treatments | Venetian Blind (White); Roller Shade (White, Grey, Blackout); Vertical Blind (White); Curtain (Beige, Grey, Navy); Sheer Curtain | `gen_window_treatments.py` |
| Wall finishes | Painted Drywall (six colours); Ceramic Wall Tile (White Subway, Green Subway, White Square); Acoustic Wall Panel (Grey, Blue, Charcoal); Beadboard (White); Wood Slat Wall (Oak); Accent Wall Panel (Maple, Walnut, Teal, Blue, Green, Yellow); Corner Guard (Stainless, White Vinyl) | `gen_wall_finishes.py` |
| Wall fittings | Wayfinding Stripe (Blue, Green, Teal, Yellow, Orange, Purple); Room Sign (Charcoal, Blue); Wayfinding Panel; Nurse Station Glass Screen (Clear, Frosted) | `gen_wall_finishes.py` (the panel: `gen_wayfinding.py`) |
| Flooring | Carpet Tile (Grey, Blue, Charcoal); Vinyl Composition Tile (White, Beige); Ceramic Floor Tile (White, Grey); Hardwood Floor (Oak, Walnut); Polished Concrete Floor; Rubber Floor (Studded); a full block of each of the carpet, vinyl, ceramic and rubber floors; and the Polished Concrete, Oak Hardwood and Walnut Hardwood sets | `gen_flooring.py` |

## Modern ceilings

Seventeen ceiling finishes, every one a **full-size block** (`BlockCeilingFinish`, one class
constructed by registry name), so a ceiling is built a layer of blocks as the older tiles are.
The name decides the kind:

- **Plain** (tegular, fissured, embossed, perforated, pressed tin): vanilla `cube` with the tile's
  64 px face below, a plain edge on the four sides (the tile's body with no grid, the white wall
  angle along the bottom), so the edge of a ceiling does not repeat the face, and plain
  fibreboard above (`ceiling_tile_back`; a metal tile's own metal). Relief is shaded into the
  face, not modelled: the tegular tile's reveal, the tin's bevels and boss. A tile's T-bar is
  drawn flat white, two texels on each edge, so neighbours make a one-pixel bar. **The tegular
  reveal is shaded the same on all four sides**: a light from one side turned the wrong way on
  every turned tile, and the user read it as untidy edges.
- **Directional** (`_slat_`, `_corrugated_`): stores the `axis` its slats or ribs run along (the
  placer's look; metadata bit 0) and the blockstate turns the model a quarter for x. Every slat
  (16 texels) and rib (4 texels, `gen_cladding.py`'s corrugated profile recoloured) divides the
  block, so a field lines up across blocks in either direction.
- **Grids** (`_open_cell_`, `_grid_`): elements at the bottom of the block, cutout, open to the
  plenum above: the open cell's blades every 4 px, 4 px deep, half a blade on each edge so
  neighbours make one; the exposed T-bar's flange and web round the block's edges only. Still a
  full block to stand on and select; not opaque, so the blocks above draw.

The Fabricator prices the slats as planks (the `slat` word) and the metal ones (`corrugated`,
`perforated`, `tin`, `grid`, `cell` on a `ceiling_` name) as sheet metal; the tiles keep the old
tiles' cost.

## Hanging acoustics

Baffles and clouds for an open ceiling, in the ten finishes of Lighting's linear LED lights and
drawn with the very same Core textures (`blocks/finishes/finish_<name>`, written by
`gen_linear_lights.py`), so a fin and the light under it match. Both hang on the lights' aircraft
cable texture.

- **Baffle** (`BlockAcousticBaffle`): a vertical fin 2 px thick and 10 px deep, 2 px below the
  ceiling. Stores the `axis` it runs along (the placer's look); actual state joins it with the
  baffles along its run (`front`, north or east, and `back`), drawing the felt edge only where
  the run stops, and hangs a cable at a run's ends and every fourth block (`cable`).
- **Cloud** (`BlockAcousticCloud`): a flat panel 1 px thick, 4 px below the ceiling. Clouds side
  by side are one panel with no joining state: their touching edges lie between the panels'
  faces where nothing can see them. A cable at the cloud's north-west block and every third
  block each way from it, so a cloud of any size hangs from at least one.

Neither drops further than its own block; for a deeper drop, hang the lights lower (their
`linear_light_hanger`) and keep the acoustics at the ceiling, as in an open office.

## Window treatments

Nine blocks, one class: `BlockWindowTreatment`, constructed by registry name
(`blind_venetian_*`, `shade_roller_*`, `blind_vertical_*`, `curtain_*`).

**A blind is its own thin block, hung in the cell on the room side of a window**, against whatever
it was placed on: our glazing, vanilla glass, or any opening. That was chosen over making it a
state of the glass: it keeps the glazing's state count down, and a blind works with any window.
Its facing is the way to the window, taken from the face it was placed against (placed on a floor
or ceiling, the way the player faces).

**Blinds of one kind hung the same way join into one blind.** Everything that belongs to the
whole blind rather than to one block is drawn only where it belongs: the headrail, cassette or
rod along the top course (`up` = false), a bottom rail along the bottom (`down` = false), a
raised venetian's slat stack on the top course, a half-lowered roller shade's bar on the bottom
course, a drawn vertical blind's vanes and an open curtain's bunched fabric at the ends of the run
(`left` / `right` = false), and a curtain rod's wall brackets and finials at the two ends of its
top course. Left and right are in the blind's own frame -- as seen from inside,
looking at the window -- so they turn with it.

| Kind | States (`state`, stored) | Light taken |
|---|---|---|
| Venetian | 0 open, 1 tilted, 2 closed, 3 raised | closed a little, tilted less |
| Roller | 0 down, 1 half, 2 up | down a little; the blackout shade all of it (half: about half) |
| Vertical | 0 open, 1 closed, 2 drawn aside | closed a little |
| Curtain | 0 closed, 1 open | closed a little; the sheer one barely any |

- **Right-click cycles the whole joined blind** (a flood fill over the blocks that join, capped at
  512). A new blind comes out closed.
- **Redstone closes it and taking the signal away opens it**, as a motorised shade does, acting
  only when the signal at a block changes -- so a blind closed by hand is not opened by a neighbour
  being placed. There is no metadata bit left for the power (facing and state fill all four), so
  what was powered is remembered in memory while the world is loaded; after a restart a blind left
  closed by a signal that has since gone stays closed until clicked.
- **Light** comes from the stored state (`getLightOpacity`), so it updates when the state is set.
  A blind draws itself with its neighbours' light (`getUseNeighborBrightness`), or a closed blackout
  shade, which has none of its own, would draw black.
- No collision: a blind hangs, and a player can reach past it to the window.
- The sheer curtain is on the translucent layer; the rest are cutout.
- The textures are even: the lines that make slats and pleats run the whole way across, so a big
  blind shows no repeat.
- **Anything repeated down a blind has a pitch that divides the block.** Venetian slats are 16/11 px
  apart, eleven to a block, and vertical vanes 2 px. At a pitch that does not divide sixteen (the
  slats were 1.5 px) the gap across every block seam differs from the rest, and a tall blind shows
  a band at each block (issue #219).
- Priced by what they are made of: venetian (aluminium) a Sheet Metal, shades and vertical blinds 2
  paper, curtains 2 paper and a dye.

## Flooring

Three ways to floor a room, the first two chosen on purpose (2026-09-18), the third added for
issue #245:

- **An overlay** (`BlockFloorFinish`, constructed by registry name `floor_<material>_<colour>`) --
  a one-pixel finish laid on top of any floor, as vanilla carpet is. It needs a solid top under it
  and comes up, dropping itself, when that goes. Eleven: carpet tile in three colours, vinyl
  composition tile and ceramic tile in two, oak and walnut hardwood, polished concrete and studded
  rubber.
- **A full-block set** (block, stairs, slab and fence, on `gen_cmu.py`'s blockstates) for the
  finishes that are also a structure: polished concrete, oak and walnut hardwood. A hardwood block
  shows the boards on its top and their edges on its sides.
- **A full block** (`BlockFloorFinishBlock`, `<overlay>_block`) of each of the eight overlays with
  no set: carpet tile, vinyl and ceramic tile, rubber. It is there so a floor can be built at full
  block height and furniture, which is drawn to stand on a full block, sits on it rather than a
  pixel into it. The overlay's own textures on every face (nothing new in the atlas), and no
  state at all: its one variant is the overlay's list of turns and drawings, picked by position.
  No stairs, slab or fence; the sets are for finishes that are also a structure. Carpet and
  rubber are cloth to the game (soft, by hand, wool's hardness), tile is rock (a pickaxe, stone's
  hardness).

**No visible repeat.** An overlay's blockstate is a list of models the game picks between by block
position. Carpet tile is laid quarter-turned, and hardwood and polished concrete have two drawings
each (board joints in different places). Hardwood keeps its boards running the way it was laid
(`axis`, the only stored state) and only turns end for end.

**A tile grid is never turned.** Ceramic and vinyl tile draw their joints on two edges of the
texture, so a turned block would double the joint at one seam and lose it at the next; they vary
by a second, differently shaded drawing instead. For the same reason carpet tile draws no seam at
all.

Priced by what they are made of: carpet tile a wool, vinyl tile paper and a dye, ceramic two clay,
hardwood two planks, polished concrete a Concrete Mix, rubber a slime ball. A full block is its
overlay's cost and a Concrete Mix, the screed it is laid on.

## Wall finishes

A thin panel hung on the face of any wall, as a blind hangs against a window (`BlockWallFinish`,
constructed by registry name `wall_<kind>_<colour>`): painted drywall in six colours, ceramic wall
tile, fabric acoustic panels, beadboard and a wood slat wall. Chosen over full-block sets
(2026-09-18) so a finish goes on whatever the wall is built of.

- **Facing** is the way to the wall, from the face it was placed against, and the only stored
  state.
- **Finishes of one kind on the same wall join**, and trim is drawn only where it belongs: a
  tile's bullnose cap and a beadboard's chair rail on the top course of the run (so a two-block
  run of tile under paint is a wainscot), an edge trim at the run's ends, and an acoustic panel's
  frame round the outside of the whole panel. Paint has no trim. Left and right are as seen from
  the room, facing the wall, as the blinds' are.
- **Inside corners.** A cell holds one finish, so the cell in a corner hangs on one wall and the
  run on the other wall would stop a block short of it. So when the corner cell's side is a wall
  (a solid face) and the cell beside it along that wall holds the same finish facing it, the
  corner cell also draws that run's last stretch (`left` / `right` = `corner`). The side panel
  is butted against the face of the corner cell's own panel, not run through it, and both runs
  count the corner as a join, so neither draws an edge trim where it turns.
- **A different finish in the corner** cannot draw the other run, since a block draws only its
  own finish. So the other run's last cell reaches in instead (`abut`): when the cell past its
  end holds a different wall finish hung on the wall that ends the run, and the run's own wall
  goes on behind that cell, it carries its panel (and cap or frame) on into the corner cell,
  against its own wall, and draws no edge trim there -- as a wainscot meets a different finish in
  a real corner. It stops 2 px off the far wall, clear of every kind's trims (beadboard's chair
  rail is the deepest), or 0.25 px against paint (`abut_thin`), which would otherwise leave a
  strip of bare wall. The corner's own finish keeps its edge trim, and where it is thinner than 2
  px there is a small step, never a shared face or a gap onto the wall.
- `left` and `right` are one five-valued property each (`end`, `join`, `corner`, `abut`,
  `abut_thin`), all actual state, so facing is still all that is stored: 400 states a block.
  Outside corners need nothing: the two runs are in different cells and meet only along the
  corner's edge, which is what the corner guard covers.
- **No two faces share a plane** in any state, since that is z-fighting: the textures flicker
  through each other (#218: a slat wall's felt end face and its edge trim's, at every run end).
  A face some part always covers is left off -- a trimmed kind's panel has no end or top faces --
  and a part that would overlap another only in some states is cut, each piece drawn only in the
  states it shows in: the strip of panel under an edge trim is drawn only where the run joins,
  and the edge trim stops under the top course's trim, which owns the corner.
- **Corner guards** (`BlockCornerGuard`, stainless or white vinyl) sit on the wall face beside an
  outside corner: the half of the face that was clicked picks the edge, one flange lies on the
  face and the other wraps round onto the wall's end face, outside the guard's own cell. A guard
  placed on another takes its facing and edge, so a stack is one guard.
- **Harvesting.** Drywall and vinyl are clay to the game, which comes off by hand; rock would
  need a pickaxe to drop anything. Tile is rock and takes a pickaxe, and so does stainless (any pickaxe, wood up).

Priced by material: drywall paper and a dye, wall tile two clay (it is ceramic), acoustic panels a
wool, beadboard and slat wall a plank, a stainless guard an iron ingot and a vinyl one paper and a
dye.

### Accent wall panels

`wall_panel_<colour>` (`BlockWallFinish`, kind `panel`): high-pressure laminate panels 0.75 px off
the wall, wood-look (maple, walnut) or in a hospital colour (teal, blue, green, yellow), with the
reveal between panels down one edge of every block and a thin anodised aluminium trim along the
top course and the ends of the run (a trimmed kind, as tile is: `TRIM["panel"]`, one shared
`wall_panel_trim` texture). They join, turn inside corners and abut other finishes exactly as the
other finishes do, so an accent wall behind a nurses' station or a bed is a run of them.

## Wall fittings

Added for a hospital build (2026-10), on the same generator.

- **Wayfinding stripe** (`BlockWallStripe`, `wall_stripe_<colour>`, six colours): a 2 px
  department colour band, 0.25 px off a bare wall, at one of four heights. Stripes of one colour
  at one height on one wall meet end to end with no trim, so a run reads as one band down a
  corridor, and in an inside corner the corner cell draws the other wall's band on its side too
  (`left` / `right`, actual state), as the finishes' corners do. Placed beside or on another
  stripe, a stripe takes its height. No collision. A cell holds one block, so a stripe goes on a
  bare wall (the hospital's walls are white blocks), not on a painted drywall finish.
- **Room sign** (`BlockRoomPlate`, `room_plate_charcoal`, `room_plate_blue`): a plate beside a door
  with the room's number over its name, white on the plate, and a row of braille dots. The legend
  is a tile entity's (`TileEntityRoomPlate`: a number and a name index), drawn by the plate's
  **baked model** from one 128 x 64 glyph sheet (`RoomPlateBakedModel`, `RoomPlateGlyphs`,
  laid out as the mile markers' legends are): the quads are baked into the chunk once per number
  and name, so a plate costs nothing a frame, and the tile entity is never ticked. Why not
  metadata or a renderer: a number and a name are tens of thousands of combinations, past the
  bits left after the facing and past any sane state count, and a renderer would draw every plate
  in a hospital every frame; a data-only tile entity is a few bytes a plate. A click cycles the
  name and a sneak-click steps the number, for building by hand.
- **Wayfinding panel** (`BlockWayfindingPanelGeneric`, `wayfinding_panel`, GitHub #307): Core's
  wayfinding panel without airport or hospital theming, for stairwell floor signs ("L3 / STAIR
  A"), office room plates, museum galleries and corridors. It starts unlit in the corporate
  aluminium scheme and, on its `auto` mount, sits flat on the wall it is placed against. Cells join
  into a sign up to 16 x 6, and it takes every option the airport and hospital panels do (mounts,
  scale, four lines with icons, colours, border): see
  [WAYFINDING_PANELS.md](WAYFINDING_PANELS.md). Placed by command like a room sign, by
  `/setblock` with its NBT.
- **Nurse station glass screen** (`BlockNurseScreen`, `nurse_screen_clear`,
  `nurse_screen_frosted`): a glass panel in an aluminium shoe, 11.75 px high, standing on the far
  edge of the counter it is placed on (the edge the player looked toward). Screens of a kind side
  by side join into one run, glass and shoe running on and a post only at each end. Glass in the
  translucent pass, shoe and posts in the cutout pass (`ICsmGlassFronted`); the frosted screen is
  frosted below a clear band at the top.

## Placing by command

Every block here is placed and set up with `/setblock` and `/blockdata` alone. Facing is a
horizontal index, **S 0, W 1, N 2, E 3**.

| Block | Metadata | NBT | States |
|---|---|---|---|
| `wall_<kind>_<colour>` (finishes and accent panels) | facing: the way to the wall | -- | 400 |
| `corner_guard_*` | bits 0-1 facing (the way to the wall), bit 2 (4) the right-hand edge | -- | 8 |
| `wall_stripe_<colour>` | bits 0-1 facing (the way to the wall), bits 2-3 height: 0 at 2..4 px, 1 at 6..8, 2 at 10..12, 3 at 13..15 | -- | 64 |
| `room_plate_<colour>` | facing: the way to the wall | `num` (int, 0-9999, -1 for none), `name` (0 none, 1 EXAM, 2 PATIENT, 3 SOILED UTILITY, 4 CLEAN UTILITY, 5 NOURISHMENT, 6 STAFF ONLY, 7 MED ROOM, 8 STORAGE) | 4 |
| `nurse_screen_<kind>` | facing: the side of its cell it stands on (the way to the customer side) | -- | 16 |

For example, a room sign on the north wall of a corridor (the wall to the north of the cell):
`/setblock ~ ~ ~ csm:room_plate_blue 2` then `/blockdata ~ ~ ~ {num:214,name:2}`. A new plate,
set with no NBT, reads 101 EXAM. A stripe on the east wall at height 1: metadata `3 | 1 << 2` =
`7`. The joins (`left`, `right`, `up`, `down`) are all actual state, never set.
