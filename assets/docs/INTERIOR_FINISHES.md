# Interior Finishes

What finishes a building on the inside, in the **Interior Finishes** tab of `csm_building`: the
ceiling tiles that were migrated here, and the decor track that started with window treatments.
The glazing the window treatments hang against is in `WALL_MATERIALS.md` § Glazing.

| Family | Blocks | Generator |
|---|---|---|
| Ceilings | the popcorn ceiling and ceiling tiles (`BlockPCC`, `BlockCT*`, `BlockDCT*`) | -- |
| Window treatments | Venetian Blind (White); Roller Shade (White, Grey, Blackout); Vertical Blind (White); Curtain (Beige, Grey, Navy); Sheer Curtain | `gen_window_treatments.py` |
| Flooring | Carpet Tile (Grey, Blue, Charcoal); Vinyl Composition Tile (White, Beige); Ceramic Floor Tile (White, Grey); Hardwood Floor (Oak, Walnut); Polished Concrete Floor; Rubber Floor (Studded); and the Polished Concrete, Oak Hardwood and Walnut Hardwood sets | `gen_flooring.py` |

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
(`left` / `right` = false). Left and right are in the blind's own frame -- as seen from inside,
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
- Priced by what they are made of: venetian (aluminium) a Sheet Metal, shades and vertical blinds 2
  paper, curtains 2 paper and a dye.

## Flooring

Two ways to floor a room, both chosen on purpose (2026-09-18):

- **An overlay** (`BlockFloorFinish`, constructed by registry name `floor_<material>_<colour>`) --
  a one-pixel finish laid on top of any floor, as vanilla carpet is. It needs a solid top under it
  and comes up, dropping itself, when that goes. Eleven: carpet tile in three colours, vinyl
  composition tile and ceramic tile in two, oak and walnut hardwood, polished concrete and studded
  rubber.
- **A full-block set** (block, stairs, slab and fence, on `gen_cmu.py`'s blockstates) for the
  finishes that are also a structure: polished concrete, oak and walnut hardwood. A hardwood block
  shows the boards on its top and their edges on its sides.

**No visible repeat.** An overlay's blockstate is a list of models the game picks between by block
position. Carpet tile is laid quarter-turned, and hardwood and polished concrete have two drawings
each (board joints in different places). Hardwood keeps its boards running the way it was laid
(`axis`, the only stored state) and only turns end for end.

**A tile grid is never turned.** Ceramic and vinyl tile draw their joints on two edges of the
texture, so a turned block would double the joint at one seam and lose it at the next; they vary
by a second, differently shaded drawing instead. For the same reason carpet tile draws no seam at
all.

Priced by what they are made of: carpet tile a wool, vinyl tile paper and a dye, ceramic two clay,
hardwood two planks, polished concrete a Concrete Mix, rubber a slime ball.
