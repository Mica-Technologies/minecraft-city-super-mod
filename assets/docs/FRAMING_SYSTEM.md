# Framing System

The structure a building is made of before anything closes it in: stud walls, the joists and deck
that span between them, and the structural steel that carries the lot. 32 blocks in **CSM:
Structure & Framing**, all shipped by `csm_building`, all generated from one catalogue.

Nothing here is a finished surface. The point of the family is that a building can be shown
part-built — studs open, bays visible, a wall half laid out — which is a thing no other city mod
does and which the whole design is bent toward.

## What it is in game

| Group | Blocks | What they do |
|---|---|---|
| Steel stud framing | 7 | Galvanised C-studs in track: plain, narrow, braced, door and window rough openings, the floor track a wall is set out with, and a hollow metal door frame |
| Wood framing | 7 | The same seven shapes in lumber, with fire blocking in place of the metal frame |
| Horizontal structure | 8 | Wood joist, I-joist, open-web bar joist, joist girder, roof deck, composite deck, a 45° rafter and a ceiling joist |
| Structural steel | 10 | Column, beam, base plate, bolted connection and X-brace, each in red oxide primer and galvanized |

The three insulable steel walls and the four insulable wood walls each offer three insulation
variants, so the tab holds **46 creative entries for 32 blocks**.

Walls join each other. A run may change from a plain bay to a door opening to a braced bay, and
from steel to wood, without ever stopping being one wall.

## Files

| Path | Role |
|------|------|
| `dev-env-utils/scripts/gen_framing.py` | generates **everything**: textures, shared geometry, every block's models and its blockstate. `--check` fails on drift, `--fragments` prints the lang and tab lines |
| `modules/building/.../buildingmaterials/BlockFramingWall.java` | the wall: post-and-arm, neighbour-aware, 768 states |
| `…/FramingJoins.java` | what counts as a neighbour, and on which of the six sides |
| `…/ICsmFramingMember.java` | the marker, and the one hook that can refuse a join |
| `…/FramingInsulation.java`, `…/ItemBlockFramingWall.java` | the insulation state and the item that carries it |
| `…/BlockFramingSpan.java` | joists, deck and structural steel: one axis, two states |
| `…/BlockWoodRafter.java` | the 45° rafter, which needs four facings rather than two axes |
| `…/AbstractBlockSteelFraming`, `…WoodFraming`, `AbstractBlockSteel*` | material properties per family, so each member is a registry name and its own quirks |
| `modules/building/.../tabs/CsmTabStructureFraming.java` | the tab, `@CsmTab.Load(order = 14)` |

Nothing under `models/block/buildingmaterials/shared_models/framing/`, nothing named
`steel_*`/`wood_*` under `blockstates/`, and neither framing texture folder is hand edited. Change
the catalogue in the generator and re-run it.

## The wall

### Post and arm, not a panel

A wall is drawn like a fence: an arm reaching from the block centre toward each side that connects,
and a post at the centre only where a wall really has one.

The alternative — a panel down the block's facing axis — was tried first on paper and cannot do
corners at all. A panel centres the wall on one axis, so two perpendicular runs in adjacent blocks
sit half a block apart and never meet. Post-and-arm draws a straight run, an L, a T and a cross
from the same two models, and the corner post falls out of it for free.

### The post is conditional

The post is drawn only at a **junction** (two perpendicular connections) or at a **run end**
(exactly one connection), through an eight-way `OR` in the blockstate. A straight run has no post
at all.

That is not a saving, it is the stud spacing. With a post always at the centre, a run put studs at
roughly 4, 8 and 12 and then 20, 24 and 28 — gaps of 4, 4, 8, repeating. Dropping the post from
straight runs leaves only the stud each arm carries, at a constant 8/16 pitch that continues
unbroken across the block boundary.

Two studs to a block is **sparser than scale accuracy wants**. Real framing is 16 in on centre,
which at Minecraft's metre block is about 2.5 studs. Three was tried and read as packed stripes at
sixteen pixels. The looser spacing is a deliberate departure, not a correction toward realism.

### Track only at the ends of a stack

A real stud runs the full height of the wall in one piece and meets track only at the floor and the
ceiling. Drawing track at every course turns a three-block wall into three stacked walls, so the
wall reads its vertical neighbours too and leaves the track out wherever the course carries on. The
stud spans the full height of its block, so stacked studs meet end to end as one member.

That is what `UP` and `DOWN` are for, and it is why the state count is 768 rather than 192.

### Connections are absolute

`NORTH`/`EAST`/`SOUTH`/`WEST`, not the facing-relative left/right the work zone runs use. A wall is
a junction, and naming its sides after however it happened to be placed would describe one physical
corner differently depending on which of its two walls is asked. Absolute directions also let the
blockstate rotate one arm model into all four places.

### Everything joins everything

`ICsmFramingMember` originally carried a framing *kind* and refused to join across it, so steel met
wood at a corner. The photograph this family was built from shows the opposite — tan posts standing
in the same plane as the galvanised studs, in one wall — and that was the case that prompted the
work. With everything joining everything the kind decided nothing, so it is gone. `joinsFraming`
remains as the place to refuse a join if something ever needs to.

### Insulation

`NONE` / `BATT` / `MINERAL`, a state rather than three blocks, so an insulated wall costs no
registry name, blockstate or tab class of its own.

The reasoning behind that was **half wrong, and the fix matters**: a state is free to render and
not free to obtain. A state no item carries and no interaction changes can only be reached with
`/setblock`. Each insulable wall therefore carries sub-blocks — `ItemBlockFramingWall` names each
variant, `damageDropped` and `getPickBlock` return the one actually there, and `registerModels`
gives each metadata an item model of its own.

**Still creative-only.** `CsmFabricatorGui` enumerates blocks, not metadata variants, so the
Fabricator offers the uninsulated wall alone.

### State

768 per wall block. `FACING` (4) and `INSULATION` (3) are stored — 12 of the 16 metadata values —
and the six connections are actual-state only and cost no metadata. `FACING` does **not** orient a
connected wall; the connections do. It is read for the isolated block, which has nothing else to
take an axis from, and by the asymmetric members.

## The horizontal structure

A joist is not a wall, and `BlockFramingSpan` is deliberately not `BlockFramingWall`. A wall is a
junction that reads six neighbours; a joist runs one way and repeats. One property, two values, and
a row still tiles seamlessly because each block is the full length of its own cell.

Bearing seats at the ends of a run were considered and left out. They would be the equivalent of
the wall's post, but unlike the post nothing looks wrong without them.

Each member's collision box is its **own real extent**, not a full cube, so a floor of joists can
be walked between and stood on before it is decked. The box is written once as the member is drawn,
spanning north-south, and turned for the other axis in the base class.

Two members do not get their true extent, both for the same reason: the rafter and the X-brace are
diagonals, and an axis-aligned box cannot describe a diagonal. They take a full cube.

## Why there are no roof trusses

A pitched roof truss **cannot be a single block**. Element rotation in 1.12 allows one axis and
only 22.5° or 45°. A real truss spans 20–40 ft — 6 to 12 blocks — with its top chord rising the
whole way, so at 22.5° the chord leaves its block after 2.4 blocks and at 45° after one. Every
other member in the group is parallel-chord or flat, which is exactly why they tile and a truss
cannot.

A 45° **rafter** does the job instead. It fits its own block exactly, and stacking blocks
diagonally gives an unbroken 12:12 slope that carries on as far as it is built.

Getting that right needs one non-obvious thing: a 16-long box rotated 45° spans only 11.3 of the
block, leaving a gap at each corner that shows as a break at every step of the slope. The member is
drawn to the block's diagonal (22.6) and rotated **without** rescale, so it lands corner to corner.

Genuine Fink and scissor trusses would want the multi-block treatment the mast arm curves have — a
generator that sweeps the shape, splits it across the cells it passes through, and emits the Java
cell layout beside the geometry. That remains open.

## Traps

Each of these cost real time, and none is visible from the code alone.

**A narrow member's UV window.** A face takes the default position-derived UV, so it shows only the
texture columns its own width covers. The 4/16 stud web shows columns 6–9 and nothing else, so a
knockout drawn 6 px wide was cropped into a slot running off the edge of the stud. The 2/16 narrow
stud shows columns 7–8 — *exactly* the knockout and none of its rim, leaving a stud that was
entirely hole. The generator sizes the knockout to the wide web's window and gives the narrow web
an explicit UV.

**`registerModels` registers metadata zero and nothing else.** Anything with subtypes must override
it. The first build of the insulation sub-blocks looked right in the world and showed the
missing-texture chequer for every variant in the creative tab.

**The blockstate format cannot AND a sibling key onto an `OR`.** A `when` holding `OR` plus anything
else is read as an AND over properties, one of which is called "OR". Every condition that needs
both is written out.

**The Fabricator's mounting-hardware rule runs before the tab is looked at**, and its noun list
holds `plate` and `base`. Both structural base plates and the timber sole plate were priced as
brackets — the timber one in *sheet metal*. The framing tab is now exempt from that rule. Note that
display names have parentheses **stripped** before matching, so a bracketed finish suffix does not
move a name's last word out of any list; that was assumed and was wrong.

**A rough opening cannot take a full cube.** A doorway that cannot be walked through is worse than
one whose jambs can be stepped into, so the door openings and the hollow metal frame have no
collision at all, and the floor track is a box one sixteenth high. Check this by **walking** — a
teleport ignores collision and proves nothing.

**Adding a block property needs a client restart, not a resource reload.** Properties are built at
block construction. A reload leaves the old state container in place and the change simply does not
appear, which reads as the fix having failed.

## Related

- `assets/docs/MODULE_SYSTEM.md` — how a module's tabs and assets reach Core
- `assets/docs/SURVIVAL_AND_RECIPES.md` — the Fabricator and how costs are derived
- `assets/docs/BLOCK_AND_ITEM_BASE_CLASSES.md` — the base classes this family extends
- `assets/docs/MAST_ARM_CURVE_SYSTEM.md` — the multi-block approach a real roof truss would need
