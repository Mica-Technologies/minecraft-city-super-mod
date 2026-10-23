# Wayfinding Panels

Three blocks, one engine. `airport_wayfinding_panel` (Transit, "Large Hanging Sign"),
`hospital_wayfinding_panel` (Healthcare) and `wayfinding_panel` (Building, Interior Finishes,
GitHub #307) are Core's wayfinding panel (`codeutils/wayfinding/`) with their own default sign,
preset list and tab. Their metadata (the facing, S 0 W 1 N 2 E 3, towards the reader) and their
tile entity NBT are one, so a generator writes any of the three by changing the name.

Until #307 the airport panel was drawn by a renderer every frame and the hospital panel was
baked; the hospital panel's engine was grown into the shared one and the airport panel's renderer,
layout, editor and packet were removed.

## What a panel is

Cells of one block facing the same way, side by side and stacked, are one panel, up to 16 x 6
(`WayfindingPanels`). The controller is the bottom-left cell as the reader sees it; the panel's
width is how far the controller's row runs to the reader's right, its height how far its column
runs up. A cell outside that rectangle (a ragged build) is a plain slab. Every cell holds the
sign: an edit writes them all, a cell placed fresh against a panel takes its neighbour's, so
whichever cell becomes the controller already has it.

Each cell's **place** (`pc`, `pr`, `pw`, `ph`) is worked out on the server whenever a cell is
added or removed (`WayfindingPanels.refresh`, a flood over the plane of the panel) and synced,
so a cell's baked model draws its own share of the sign without looking at the rest of the panel.
The airport panel kept no place; a cell read without one is queued and placed at the end of the
tick it loads in (`WayfindingPanels.Ticker`), since walking a panel while its chunk loads would
load chunks.

## Drawing

Baked, never rendered. The blockstate's world model is Core's empty
`block/wayfinding/wayfinding_panel_base`; `WayfindingBakedModel` (a `CsmBakedModelWrappers`
wrapper on every block that is an `AbstractBlockWayfindingPanel`) adds the cell's share of:

- the **slab**: its front and back in the frame colour (the background darkened) and its four
  edges, shaded by hand, only where they fall in the cell;
- the **sign** (`WayfindingLayout`, client-free and tested): background, border, pictogram, arrow,
  each line's icons and glyphs, each a rectangle in the panel's space with an affine map onto one
  of two 512 px sheets, clipped to the cell; the back is the same layout with its arrows mirrored;
- the **rods** (hung) up to the first block above, or the **posts** (on posts) down to the first
  block below, within 24 blocks, two of them or one for about every four blocks of width (one
  post under a panel no wider than 1.5).

A cell on the panel's outer edge clips only on its inner sides, so a panel scaled past its cells,
or lowered below them by its drop, is drawn whole by its edge cells. Everything is drawn unshaded
with its colour in its vertices; quads are cached by facing and `WayfindingFace` (the sign, the
place, the resolved mount and the supports), so identical signs bake once. Nothing ticks and
nothing runs per frame. A lit panel gives light 8.

The **hitbox** is the cell's share of the slab, worked out from the same `WayfindingFace`
geometry as the drawing, so the two cannot disagree. A cell none of the panel reaches (a 0.5
scale panel on a big build) gets a one-pixel sliver so it can still be broken.

## The sheets

Both are drawn by `dev-env-utils/scripts/gen_wayfinding.py`, which also writes
`WayfindingSheet.java` (do not edit it by hand; `--check` fails on drift):

- `textures/blocks/wayfinding/wayfinding_glyphs.png`: the printable ASCII glyphs of the committed
  highway font (Series D) at a 32 px cap height, white on clear, and a white swatch every flat
  colour is sampled from.
- `textures/blocks/wayfinding/wayfinding_icons.png`: 64 px cells, 8 x 8, each icon an alpha mask
  (white art on clear). The two arrows are cells 0 and 1; the baked model turns them. An icon on
  a tile is a square in the pictogram colour with the art in the background colour on it; a plain
  icon is the art in the legend colour. 46 icons, every id the airport and hospital panels ever
  saved among them, 18 left free. No red cross anywhere: first aid is a kit.

`WayfindingBakedModel.register` puts both on the atlas at the stitch.

## The sign and its NBT

`WayfindingSign` is one immutable value. Every key is optional.

| Key | Meaning |
|---|---|
| `l1`..`l4` | the lines: printable ASCII, 32 characters, trimmed |
| `ts`, `ts1`..`ts4` | text scale: the legend's, and each line's times it, 0.5..1 |
| `p` | the panel's pictogram (an icon id) at the end away from the arrow |
| `a` | the panel's arrow: `none`, `left`, `right`, `up`, `down`, `up_left`, ... |
| `s` | the colour scheme (`corporate`, `neutral`, `transit`, `hospital`, `airport`, `metro`, `exit`, `info`, the hospital departments) |
| `d` | double-sided (byte, default 1; never on a wall) |
| `n` | line count: 0 (default) shows the lines with text, the old rule; 1 to 4 shows exactly that many, an empty one a gap |
| `al` | alignment: `l` (default), `c`, `r` |
| `il1`..`il4`, `ir1`..`ir4` | an icon left and right of each line: any icon id, or `arrow_<direction>` |
| `is` | icon style: `tile` (default) or `plain` |
| `m` | mount: `auto` (default), `wall`, `hung`, `post`, `ceiling` |
| `rd` | drop, sixteenths 0..16: down from the top when hung, up from the bottom on posts |
| `sc` | panel scale 0.25..2 (the editor steps 0.5, 0.75, 1, 1.25, 1.5, 2) |
| `bg`, `fg` | custom background and legend colours, `#RRGGBB` (or a number); the legend colour also tints tiles |
| `bd` | border 0..2, in the legend colour |
| `lt` | lit (byte, default 1) |
| `pc`, `pr`, `pw`, `ph` | the cell's place: the server's, never write them |

The old keys are always written (a line only when it has text), the new ones only when they
differ from their default, so a sign using none of them writes exactly what it did before #307
(`WayfindingSignNbtTest` holds both panels' old NBT to that, byte for byte). A compound is a sign
only if it has `p` or any key added for #307: a cell never edited writes no sign and shows its
block's default. Within a sign a missing key is empty, none or off, except the scheme (the
block's default), `d` and `lt` (on). A `/setblock` with NBT or a `/blockdata` on any cell is the
whole panel's.

Every enum is saved by id, never by ordinal, and an unknown id reads as the enum's default, so
the lists may be reordered and grown.

## Mounts

| Mount | Where the panel is drawn |
|---|---|
| `auto` | `wall` when the controller has a solid face behind it, else `hung`. What every panel placed before #307 reads as, which is the #307 fix: a panel built against a wall no longer hangs rods off it |
| `wall` | a 1 px plate against the block behind (`z` 15..16), one-sided, no supports, centred up its cells |
| `hung` | the slab in the middle of the cell (`z` 6..10) on rods; its top at the cells' top less the drop |
| `post` | on posts; its bottom at the cells' bottom plus the drop |
| `ceiling` | its top flush with the cells' top, no rods |

The scale shrinks or grows the panel about its anchor: centred across the cells, and up them by
the mount as above.

## Laying out a face

The proportions are the two old panels' (`WayfindingLayout`): margins of the frame plus 0.08 of
the height; the pictogram a square the height less its margins at the end away from the arrow,
the arrow 0.8 of it at the end it points to; capitals 0.45 of the height for one line, 0.28 for
two, 0.17 for three, 0.12 for four, every line scaled down together to fit; a long legend first
takes room from the pictogram and arrow, down to 0.55 of their size. A line's text scale makes it
smaller about the middle it has at full size. A line's icons are 1.4 of its capitals square, 0.4
of them from the text, and count in its width when the lines are fitted; the line sits left,
centred or right between the pictogram and the arrow. A border is inset 0.035 of the height,
0.025 or 0.045 thick, and moves the margins in; the capitals shrink with the room it takes.

## Editing

An empty-handed click opens the editor (`WayfindingPanelGui`, GUI id 46, Core's
`WayfindingGuiProvider`); a sneak-click turns the arrow. The editor has two pages: **Text** (the
four lines, each line's size, and its left and right icons picked from a grid of the real icons)
and **Style and mount** (pictogram, arrow, scheme, icon style, mount, drop, panel scale, border,
both sides, lit, the legend's text size, the block's presets, and the custom colours as hex
fields). Every change is sent at once on Core's channel (`WayfindingPanelPacket`, the sign as its
own NBT), and the server reads it as it reads a `/blockdata`: cleaned, clamped, checked for reach
and edit rights. A preset rewrites the words (lines, line icons, pictogram, scheme, its arrow if
it has one) and drops custom colours, keeping the mount, scale and the rest.

## Traps

- **The tile entities keep their names.** Each block has its own tile entity class
  (`tileentitywayfindingpanel`, `tileentityhospitalwayfindingpanel`,
  `tileentitywayfindingpanelgeneric`): one class cannot be registered under two names, and the
  names are in saved worlds.
- **The ceiling is read when the chunk section is built.** A rod or post's length, and the auto
  mount's wall, come from the world as the cell's section is meshed. A block placed next to a
  cell rebuilds its section, so a wall placed behind a panel is seen at once; a ceiling placed far
  above, in another section, is seen at the cell's next rebuild.
- **Never look up a tile entity that might be created while light spreads.** `getLightValue`
  reads the cell's lit flag through the chunk with `CHECK`, never `World.getTileEntity`, which
  creates one.
- **The registry name goes in the tab line.** `csm_block_index.py` resolves a block from the
  string in `initTabBlock(new X("name"))`; a constructor that takes no name left the hospital
  panel out of the reference pages until #307.
