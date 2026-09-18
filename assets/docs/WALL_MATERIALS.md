# Wall Materials

What a building is faced in: concrete block, brick, stucco, siding, metal and panel cladding, and
stone veneer. 100 blocks in **CSM: Building Materials** (counted as the tab shows them, without
the double slabs), all shipped by `csm_building`, every one of
them drawn by a generator from a catalogue.

Where `FRAMING_SYSTEM.md` is about the structure before anything closes it in, this is the skin that
closes it. Until this family existed every material in the tab was coloured metal, and there was no
way to face a building in anything.

## What it is in game

| Material | Sets | Trim blocks | Generator |
|---|---|---|---|
| Concrete block | standard, split face, ground face, glazed | | `gen_cmu.py` |
| Brick | red, brown, buff, grey | soldier course, header course, weep holes (each colour) | `gen_masonry.py` |
| Stucco | smooth, sand float, knockdown | | `gen_stucco.py` |
| Siding | fiber cement lap, board and batten, cedar shingle, vinyl | | `gen_siding.py` |
| Cladding | corrugated steel, standing seam, insulated panel, composite panel | | `gen_cladding.py` |
| Stone veneer | ashlar, fieldstone, cast stone | | `gen_stone.py` |

Every set is a block, stairs, slab and fence (`AbstractBlockSetBasic`: five registry names with the
double slab), registered in the order above after the coloured metal sets. **Each material comes in
one colour or finish per profile, on purpose**: the list was cut from eight brick colours to four,
and siding and cladding were given the colour each profile is most often seen in. A colour is cheap
to add later (see below); a creative tab full of near-identical swatches is not cheap to take back.

## Files

| File | Holds |
|---|---|
| `dev-env-utils/scripts/gen_cmu.py` | The CMU sets, and the set blockstates and models every other generator here reuses |
| `dev-env-utils/scripts/gen_masonry.py` | Brick and its trims |
| `dev-env-utils/scripts/gen_stucco.py`, `gen_siding.py`, `gen_cladding.py`, `gen_stone.py` | One material each |
| `modules/building/.../buildingmaterials/BlockSet<Material><Name>.java` | One class per set, named by `gen_cmu.class_name` |
| `…/BlockBrickTrim.java` | Every brick trim block: one class, constructed with its registry name |
| `…/tabs/CsmTabBuildingMaterials.java` | Registration order, which is creative order |
| `src/main/java/…/materials/CsmFabricatorCosts.java` | `buildingMaterialCost`, the pricing described below |

Every generator has the same three modes: no flag writes, `--check` writes nothing and fails on
drift, and `--fragments` prints the lang lines and tab registrations to paste. **Never hand-edit a
file a generator writes**; change its catalogue and re-run it.

## One set shape, six generators

A set's blockstates and models are the same whatever is drawn on it, so they live once, in
`gen_cmu.blockstates` and `gen_cmu.models`. Both take `tex_ref`, the texture path pattern, and
`top_suffix`, the suffix of the top texture. Stucco passes an empty suffix: a coat of plaster looks
the same from every face, so its top points at its side texture rather than shipping a copy.

The shared code is what keeps a slab, a stair and a fence of any material behaving the same, and
it means a fix to the set shape lands on all of them at once. Changing it changes every
material's output, and each generator's `--check` says so.

## Scale and tiling

**The texture size follows from the bond.** A running bond only tiles if a unit's length divides
the texture, so on a power-of-two texture a unit is two or four to one, joint included. At 16 px
the only fit for a masonry unit is 8 × 4, which is exactly the CMU's coursing, so a brick drawn at
16 px would differ from the block beside it only in colour. Brick is 32 px, 16 × 4 units: two
bricks across a block and eight courses up it, about twice real size. True scale is sixteen courses
a block, and three-pixel bricks at that density shimmer. Everything after the CMU is 32 px.

**Courses land on a slab.** Siding's courses are 8 px, four to a block and two to a slab, so a
slab's edge falls on a course line and not halfway up a board. Brick, CMU, cast stone and the
panel cladding divide 16 for the same reason. Ashlar deliberately does not: its courses are 12, 8
and 12, so a slab cuts through a stone, as a sawn stone would.

**Everything wraps.** Anything drawn across a texture's edge is drawn with its coordinates taken
modulo the size, so a brick cut by the edge is one brick with one colour when the texture tiles,
and a stucco mottle or a fieldstone meets itself at the seam.

**Light comes from the upper left**, in every texture in the tab: the top and left edges of
anything raised catch the light, the bottom and right fall into shadow. A texture that breaks the
convention looks inside-out beside the rest.

**Every seed is explicit.** Python's `hash()` is salted per process, so a texture seeded from it is
different on every run and a `--check` can never pass. The CMU generator was caught by exactly
this on its first run.

## Brick

Running bond, eight courses a block. Every **brick** takes its own shade, not just every pixel:
that is what makes a wall read as brick rather than as a pattern, since every real unit came out
of the kiln a little different.

The three trims keep the plain wall's courses where they have them, at the same positions, so they
lay into a wall in bond:

- **Soldier course**: a band of bricks stood on end across the middle of the block, with two
  courses of running bond above and below.
- **Header course**: seven courses of stretchers over one of headers. Stacked, that is American
  common bond, which is what most real brick walls are laid in; in a single row it is a header
  band. The headers step a quarter brick off the stretchers' joints, and they are drawn **darker**,
  as a brick's end fires. In the stretchers' colour a row of half-length bricks reads only as extra
  joints.
- **Weep holes**: the plain wall with one open head joint a block in its bottom course, where the
  cavity drains above the flashing.

The trims are **blocks, not a state** on the brick. A state is free to render and not free to
obtain: the Fabricator enumerates blocks, not metadata, so a trim carried as a state would be
creative-only, and the set's stairs, slab and fence could not carry it anyway. They differ from
each other in nothing but a name and a texture, so there is one class, `BlockBrickTrim`, built from
its registry name through the same `ThreadLocal` hand-off `BlockRotatableNSEWUDFactory` uses:
`AbstractBlock`'s constructor asks for the registry name before a subclass's fields are assigned.

## Stucco

One warm off-white in three finishes; the finish is the choice, not the colour. A smooth coat
mottles as it cures, a floated coat shows its sand, a knockdown coat is splattered on in blobs and
flattened into pads.

The mottling is value noise on two lattices of **3 and 5 cells**, neither of which divides the
tile. At 4 and 8 their cell edges coincided and the mottling read as a grid of squares. The
knockdown shadow is cast onto the coat **below and right of each pad**, not drawn on the pad's own
edge: drawn on the pad it came out no darker than the thin coat around it, and the pads read as
flat patches of lighter paint.

## Siding

Siding's grain is stronger than any other material's here, but like brick courses and cladding ribs
it holds on every face without any work, because `cube_bottom_top` maps the side texture upright on all four sides and a stairs or fence
blockstate only ever rotates about the vertical, so the texture's up is the wall's up around a
corner.

The vinyl is a **Dutch lap**: the cove along the top of each course is the one thing that tells it
from lap siding at a distance. The cedar shingles took three passes. Narrow, even and each tinted
hard, they read as parquet; what makes them shingles is widths from four to twelve pixels, grain
that varies more along a shingle than between shingles, and a ragged butt line where some hang a
pixel lower.

## Cladding

The profile is **shaded into the texture, not modelled**. The framing's roof deck draws its ribs
as geometry, which is right for a deck seen from below and at its edge; a wall is seen face on,
where shading alone reads as the profile, and a full opaque cube keeps the set's stairs, slab and
fence. What carries over from the deck is that every pitch divides the block, so a run has no
split rib at a seam.

The corrugated uses the framing's galvanised steel colour (`gen_framing.STEEL_BASE`), so a
corrugated wall and a steel stud behind it are the same metal. These sets are `Material.IRON` with
metal sounds, unlike the older colour sets, which are stone.

## Stone veneer

Three kinds of order. **Ashlar** is dressed stone in level courses of unequal height with fine
joints, which is what separates it from brick and block. **Fieldstone** has no courses: it is a
Voronoi partition of the tile measured on a **torus**, so the cells wrap and a wall of it has no
visible repeat, each cell a stone of its own colour, rounded by shading, in recessed mortar. **Cast
stone** is manufactured: ashlar's order without its variation, in units a block wide and half a
block tall. At brick-like proportions it read as buff brick.

## Glazing

Glass that joins into one window, in the same tab: eight kinds -- clear; grey, bronze and blue
tint; one-way; wired; bullet-resistant; frosted (`GlassKind`) -- each as a full block
(`BlockGlazing`) and a pane (`BlockGlazingPane`), sixteen blocks from `gen_glazing.py`. All are one
class per form, constructed by registry name (`glass_<kind>`, `glass_pane_<kind>`).

- **Glass of one kind joins with no seams; a thin dark bronze frame runs only around the outside
  of the whole window.** A block draws no face against the same glass (`shouldSideBeRendered`)
  and a frame rail only along an edge where both faces are outside, the rule the site containers
  use. A pane's sides are three-valued (`Side`): `none` (no pane that way), `edge` (it runs to the
  block's edge and meets a wall or different glass -- framed there) or `glass` (it runs on into the
  same glass -- no frame); frames go along the top where no pane of the same glass is above and
  the bottom where none is below, and a mullion stands where the pane turns, branches or ends
  free. Different kinds are different windows, so a frame runs between them.
- **One-way glass needs nothing but two textures.** It is dark on its outside face and clear on the
  inside one, and Minecraft never draws the back of a face, so from outside only the dark face is
  seen and from inside only the clear one. The dark face is fully opaque: at 90% it still showed
  the inside faintly, and one-way glass that can be seen through is not one-way. The outside is the way the placer was looking (they
  are taken to be standing inside). A pane's facing is turned a quarter in its actual state if it
  runs along the pane.
- **A pane's arm is drawn twice, running east and running west, never turned 180 degrees**:
  turning it would move the dark face to the other side of the glass on half of every pane (the
  privacy-screen trap from `CONSTRUCTION_SITE.md`). The blockstate turns those two onto the north
  and south arms by the facing.
- **Frames stand a quarter pixel proud of the glass**, so an opaque frame and a translucent face
  never share a plane.
- **The glass textures are one flat tint with a little noise.** A streak or a glint would repeat on
  every block of a big window and read as a pattern. Wired glass is 32 px so its mesh can be half
  a pixel wide.
- Everything is on the translucent layer. The glass drops itself, unlike vanilla glass.
  Bullet-resistant glass is hardness 25 and blast resistance 2000; the rest break like glass.
- A pane's inventory icon is a flat item texture (the glass in its frame), from
  `models/item`; a block's is the block with its full frame.

## Pricing

`CsmFabricatorCosts.buildingMaterialCost` decides by **whole words in the English display name**,
in this order:

| Rule | Matches | Cost |
|---|---|---|
| metal | the coloured metal sets | Sheet Metal + that colour's dye (scaled for slab, stairs, fence) |
| cladding | all four cladding profiles | 2 Sheet Metal + Fastener Kit |
| veneer, not cast | ashlar, fieldstone | cobblestone + Concrete Mix |
| siding + cement | fiber cement lap | Concrete Mix + paper |
| siding + vinyl | vinyl | 2 paper + any dye |
| siding | board and batten, cedar shingle | 2 planks |
| anything else | CMU, brick and its trims, stucco, cast stone | Concrete Mix + clay ball |

`dev-env-utils/scripts/audit_fabricator_costs.py` mirrors these rules. Change one and change the
other, then check the result **per block** with `--grep <registry prefix>`; a summary count cannot
show a block priced wrong, only one not priced at all.

## Adding a colour or a finish

1. Add an entry to the generator's catalogue with its own explicit seed, and add it to `ORDER`.
2. Add the set class: copy a sibling, change the class name (`gen_cmu.class_name` of the registry
   name) and the registry name.
3. Run the generator, then `--fragments`, and paste the lang lines (the lang files are sorted, C
   order, CRLF) and the tab line after its siblings.
4. Check the display name against the pricing table: a name decides the price.
5. `python dev-env-utils/scripts/gen_wiki_reference.py`, build, and look at it in game.

## Traps

**A name decides a price.** Pricing reads the English display name, so renaming a block in
`en_us.lang` can move it to a different rule. The cladding is named to avoid the word "metal",
which would take it into the colour-set rule and charge each block a dye.

**Fine detail mipmaps away.** The corrugations read up close and turned to flat grey a few blocks
off. Check every new texture **from a distance** as well as up close; the fix there was contrast,
not pitch, since the pitch was right for real corrugated at this scale.

**Texture-only changes still need a rebuild.** `runClient` puts each module's built jar on the game
classpath, not its `src` tree (`modules.gradle`), so a regenerated PNG does not appear in a running
client, reload or not, until the jar is rebuilt. Never build while a dev client is running; quit it
first.

**Near-identical materials must differ in more than colour.** Brick at 16 px would have been the
CMU in red, cast stone at brick proportions read as buff brick, and vinyl without its cove would be
lap siding in beige. Before drawing a new material, ask what shape tells it from its nearest
neighbour in the tab, and draw that first.

## Related

- `assets/docs/FRAMING_SYSTEM.md` — the structure these materials close in
- `assets/docs/SURVIVAL_AND_RECIPES.md` — the Fabricator and how costs are derived
- `assets/docs/BLOCK_AND_ITEM_BASE_CLASSES.md` — `AbstractBlockSetBasic` and the other base classes
- `assets/docs/MODULE_SYSTEM.md` — how a module's tabs and assets reach Core
