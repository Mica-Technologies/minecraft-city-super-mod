# Parks & Greenery

Trees, plantings, amenities, tools and the Parks demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_trees.py` -- the Parks & Greenery tree kit: bark and leaf-cluster textures (an autumn set
  drawn with its summer sibling's seed, and the fruit trees' fruiting sets the same way with the
  fruit dotted on), palm crown sheets (the Joshua tree's rosette and the banana among them),
  moss, the log and leaves placeholder models and blockstates, and the lang for woods, leaves and planting presets. Logs and leaves are
  drawn in Java from their connections; the catalogue only appends, so existing textures never
  change. `--fragments` prints the tab lines; `--check` fails on drift
- `gen_park_plantings.py` -- the street-tree accessories and plantings (grates, pit fence, stakes,
  pole-fitted hanging baskets, hedges, shrubs, grasses, flower beds, ground covers, planters,
  raised beds and the joining troughs, each planter's tab line carrying its soil height and plant
  scale), the regional plants, herbs and garden plants, cacti and succulents (the stacking
  saguaro, `BlockParkSaguaro`) and garden flowers with their nursery pots, and the nursery,
  garden centre and farm pieces (terracotta pots, urn, bowl, half barrel, window box, the joining
  nursery bench, potting bench, stacked pots, seedling flats, compost bin, wheelbarrow, crop rows,
  trellis) from one catalogue whose tab lines carry each block's size; `--check`, `--fragments`
- `gen_park_amenities.py` -- the Parks tab: benches and picnic tables (end frames only at a run's
  ends), bins, playground, pergola, fountains with animated water, irrigation; borrows
  gen_park_plantings.py's helpers; `--check`, `--fragments`
- `gen_park_legacy_amenities.py` -- the five Parks amenities that kept their old ids: both swing
  sets (OBJ, 2.78 m to the beam), the teeter totter, the slatted trash can and the low-poly bird
  bath; writes no lang or tab lines; `--check`
- `gen_parks_tools.py` -- the tree tools' item sprites and models (the chainsaw and the stump
  grinder each with a running sprite) and the brush pile a felled tree leaves; `--check`.
  `gen_parks_tool_sounds.py` synthesises the chainsaw's pull, start, idle and cut sounds and the
  stump grinder's start, idle and grind (numpy to ffmpeg to OGG) and writes the Parks module's
  `sounds.json`
- `build_parks_demo.py` -- builds the Parks & Greenery demo world in a flat creative world loaded
  in the dev client, over MCMCP (borrowing `csm_bench.py`'s client): a street of leaning trees, a
  park with every amenity, an arboretum of every planting preset with signs, and the tree kit on
  plinths. Every tree is planted with the real Tree Planting Tool, aimed with `client_look` (a
  teleport's yaw and pitch can leave the camera pointing anywhere, and a click then plants there)
