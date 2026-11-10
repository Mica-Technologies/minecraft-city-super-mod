# Concrete Pole System

The concrete traffic poles (`trafficpolevertical*concrete*`, `trafficpolehorizontal*concrete*`)
and the concrete finish of the pole family's accessories. This document covers what they are,
how a pole decides what its ends show, how the concrete accessories are made from the painted
ones, how the old hand-placed pieces retire, and the traps.

It builds directly on the pedestal pole. Read "Which end is which" in
`PEDESTAL_POLE_SYSTEM.md` first. The concrete pole reuses that design, and the model-space
naming of its ends is not repeated here.

## What it is in game

Four pole blocks, in Traffic Accessories after the pedestal poles:

| Block | Id | Shaft | Real thing |
|---|---|---|---|
| Concrete Thick Traffic Pole | `trafficpoleverticalconcrete` | 12 across, 16-gon | spun (centrifugally cast) round pole |
| Octagon Concrete Thick Traffic Pole | `trafficpoleverticalconcreteoctagon` | 12 across the flats | precast octagonal pole |
| Concrete Thin Traffic Pole | `trafficpolehorizontalconcrete` | 8 across | the same, at the thin pole's width |
| Octagon Concrete Thin Traffic Pole | `trafficpolehorizontalconcreteoctagon` | 8 across the flats | likewise |

Each is one block, stacked to any height, which decides for itself what the two ends of its
shaft show:

| The block… | shows at that end |
|---|---|
| has another concrete pole beyond it (either profile, any facing) | a seamless joint |
| has a post-top fixture beyond it (`ICsmPostTopFixture`) | a collar and the tenon the fixture slips over |
| has air, plants, snow layers or water beyond it | a cap band and a chamfered top |
| is vertical, and that end stands on anything that is not pole hardware | a stepped plinth |
| runs into any other solid block | a plain end |

So there are no separate base, middle or top blocks to place. Put a post light on a pole and
its cap turns into a tenon; break the light and the cap comes back.

**The owner can overrule the top.** The Street Light Configuration Tool's finial mode, clicked
anywhere on a concrete pole, switches its top between the flat cap and the **finial**: the
tenon's casting shown in the open as well (the round pole's collar and spigot, the octagon's
stepped taper). That is what the legacy top pieces always showed, with or without a light on
them, and a legacy top keeps it when it retires (GitHub #345).

**And the base.** The tool's base mode, clicked anywhere on an upright concrete pole, steps the
foot of the stack through its own plinth, the **other profile's plinth** (an octagon plinth
under a round pole, a round one under an octagon) and **no base**, for a direct-buried pole
(GitHub #346). A legacy base piece keeps its own profile when it retires, since it becomes a
pole of that profile, and a legacy middle piece standing on the ground with no base piece under
it stays bare.

The flanks behave like every other pole in the family. Anything mountable beside the shaft grows
a mount stub toward it (the family's `large_mount` / `small_mount` models, in concrete).
`getPoleRadius()` reports 6 or 4, so the pole-fitted arms (light mounts, NOV tapered masts) and
the mast arm curves meet the shaft. An octagon's flats sit at the same distance as the round
pole's skin, so the same fits serve both profiles.

A post light takes its colour from the pole below (`AbstractBrightLightPoleColored`). A concrete
pole reports `SILVER`: it has no paint to lend, and the fixture on one is bare metal. A post light
on one of the old concrete top pieces showed black (the fallback for anything that is not a
traffic pole), so those turn silver as the pieces retire.

## The accessories

Every live accessory family of the pole system comes in concrete, round in section, and the
straight vertical pole sections also come in octagon:

| Family | Round concrete | Octagon concrete |
|---|---|---|
| vertical connector | `trafficpoleverticalconnectorconcrete` | `…connectorconcreteoctagon` |
| angled connector | `…connectorangledconcrete` | `…connectorangledconcreteoctagon` |
| double connector | `…connectordoubleconcrete` | `…connectordoubleconcreteoctagon` |
| quad mount | `…quadmountconcrete` | `…quadmountconcreteoctagon` |
| signal mount | `…signalmountconcrete` | `…signalmountconcreteoctagon` |
| double guy mount | `…doubleguymountconcrete` | `…doubleguymountconcreteoctagon` |
| double vertical pole | `trafficpolevertdblconcrete` | `trafficpolevertdblconcreteoctagon` |
| double horizontal pole | `trafficpolehorzdblconcrete` | — |
| horizontal sign mount | `trafficpolehorizsignmountconcrete` | — |
| curve connector | `…curveconnectorconcrete` | — |
| curved double guy connector | `…curveconnectordoubleguyconcrete` | — |
| light mount (pole-fitted) | `…lightmountconcrete` | — |
| angled thin pole | `trafficpolehorizontalangleconcrete` | — |
| mast arm curves (5 sizes) | `trafficpolemastarmcurve<size>concrete` | — |

**Why curves and arms stay round.** The arm hardware on a real octagonal concrete pole is round
steel, so a round arm on an octagon pole is correct rather than a gap. The curve connectors are
hundreds of elements swept along a bend, and an octagon section would need a new sweep generator.
It is deferred, not ruled out.

**Why the light mount has no octagon version.** It has no pole body: it is an arm with a plate,
and pole fit already moves the plate to the pole behind it.

**What was deliberately left out:**

- The families that are **retiring**: the horizontal single and double mounts and the angle
  mounts 1–3, in every colour. They convert into poles that mount automatically, so a concrete
  version would retire the day it shipped.
- `trafficpolebase<colour>`: the concrete pole draws its own plinth.
- `dmpt*` (the NOV decorative metal pole top): it is a metal casting, not a finish.
- A concrete pedestal pole, and the finial on a concrete pole. The pedestal is a 6-across
  aluminium tube, and the finial's collar is cut for it alone.
- `EXTEND_DOWN`, the reach-through-the-guardrail extension of the painted thin and thick poles.
  The pedestal pole leaves it out too, and it would double the state count. Deferred.

## Files

| Path | Role |
|---|---|
| `modules/roads/.../trafficaccessories/BlockTrafficPoleConcrete.java` | the pole: one class, constructed per profile and width from the tab |
| `modules/roads/.../trafficaccessories/BlockTrafficPoleHorizontalAngleConcrete.java` | the angled thin pole in concrete |
| `src/main/.../codeutils/ICsmPostTopFixture.java` | the Core marker a fixture carries so a pole under it shows a tenon |
| `modules/lighting/.../BlockBrightLightFactory.java` (`PostTop`), `BlockBrightLightPoleColoredFactory.java` | the fixtures that carry it |
| `dev-env-utils/scripts/gen_concrete_poles.py` | every asset below |
| `shared_models/concretepole_<profile>_<width>_<end>_<n\|s>.json`, `…_inv.json` | the pole's end models |
| `shared_models/<model>_concrete.json`, `<model>_octagon.json` | the accessory models with their body redrawn |
| `blockstates/…concrete*.json` | the poles' and accessories' blockstates (except the mast arm curves) |
| `textures/blocks/trafficsignals/shared_textures/concrete_light_pole.png` (Core) | the concrete |
| `dev-env-utils/scripts/gen_mast_arm_curves.py` | the mast arm curves; `concrete` is the last entry of its `COLORS` |

Nothing the generator writes is edited by hand. `python gen_concrete_poles.py --check`
regenerates into a temp directory and fails on drift, the texture included.

## The pole

`BlockTrafficPoleConcrete extends AbstractBlockTrafficPole`. Registry name, shaft radius and
plinth radius are constructor arguments, passed past `super()` with the pedestal pole's
`ThreadLocal` trick. It adds `END_NORTH` / `END_SOUTH` (`endn` / `ends`) with four values,
`none`, `cap`, `tenon` and `base`. All of them are actual state; metadata holds only the facing,
exactly as it did for the legacy pieces. 6 facings × 16 mount combinations × 16 end combinations
is 1,536 model locations.

### The owner's choices: stored, never drawn

`TOP` (`auto`, `finial`) and `BASE` (`auto`, `other`, `none`) are the stored properties besides
the facing. They are inputs to `getActualState` and nothing else. A finial is the `tenon` end
value; the other profile's plinth is a fifth end value, `basealt`, whose blockstate entry names
the other pole's own base model (a plinth draws no shaft, so no new model file); no base is
`none`. So neither property picks a model itself. The block implements
`ICsmModelIgnoredProperties` (Core), which gives it a state mapper that leaves both out of the
model locations, and the blockstate file does not name them. The states multiply by six; the
variants Forge bakes go from 1,536 to 2,400 a pole (6 facings x 16 mounts x 25 end pairs), for
the fifth end value alone.

The metadata keeps the facing in the six values the pole always used, so every pole already in a
world reads back unchanged, and puts the choices in the spare ten: 6-10 an upright pole with a
choice other than the defaults, 11-14 a pole on its side (north, south, west, east) with a
finial. An upright pole with a choice is always stored facing `UP`; the choice is about the
world's up, not a model end, and a pole facing `DOWN` draws the same shaft. `topEnd` says which
end the choice is about: up for an upright pole, the facing for one on its side.

`endStyle` is the pedestal pole's rule with the tenon inserted, in this order:

1. another `BlockTrafficPoleConcrete`: `NONE`
2. an `ICsmPostTopFixture`: `TENON`
3. air, anything replaceable, vanilla clutter or an `ICsmTrafficPoleBystander` (a tree, a
   bench, a crate): `CAP`
4. vertical, pointing down, and the block below is neither a pole nor `ICsmTrafficPoleIgnored`:
   `BASE`
5. otherwise `NONE`

`ICsmPostTopFixture` lives in Core because the fixtures are in Lighting and the pole is in Roads,
and a module may only reference Core. It is a marker, not a flag, for the reason
`ICsmPoleFitted` is. Today it is carried by every post light (`BlockBrightLightPoleColoredFactory`)
and by the Alto Classic Post Light and Alto Park Light (`BlockBrightLightFactory.PostTop`). A new
post-top luminaire should use one of the two.

### Why each end carries half the shaft

The pedestal pole draws its tube in every block and hangs its cap or base on the end. That does
not work here: the concrete cap is a chamfer and the octagon's tenon a stepped taper, both
**narrower** than the shaft, and they must sit inside the block, as the legacy top pieces did, or
every pole would grow a block taller at the top. So the shaft is cut at the middle of the block.
Each end value is a model of **its half** of the shaft plus whatever that end wears: `none` a full
half, `cap` and `tenon` a shortened one, and `base` none at all, since the plinth encloses it.

In the blockstate, `endn` sets the model and `ends` adds a submodel. One property may set the
model and another add submodels over it; two properties both setting `model` would fight.

### The geometry

A round shaft is the family's 16-gon: eight narrow rectangles, `d × d·tan(π/16)`, turned to
−45…45. An octagon is four, `d × d·tan(π/8)`, at 0 and 45. Every piece is a stack of these
(`POLES` in the generator): the plinth is two steps, the cap is a band and a chamfer, and the
tenon is a collar with a spigot (round) or a stepped taper (octagon). The round thick pole's
plinth follows the legacy `rcpb2`, and the octagon's pieces follow `ocpb` / `ocpt`. The plinth's
upper step is `d + 3` rather than the legacy `d + 2`, so a one-block pole wearing both a plinth
and a tenon collar (`d + 2`) never has two coplanar walls.

## The accessories: bodies redrawn

The concrete accessories are clones of the silver blockstates with the silver texture swapped
out (a sign plate's art is left alone). Swapping the texture alone was not enough. The painted
models draw their 16-gon body with a whole `0..16` UV stretched across each 2.4-wide strip. On
flat metal that is invisible; on concrete it streaks, which is how the five concrete accessories
that predate this system always looked.

So for every accessory whose body is a plain 16-gon (the generator asserts it: the first eight
elements, one width, one axis), the generator writes `<model>_concrete.json` with the body
redrawn at the same width with proportional UVs, and `<model>_octagon.json` with it redrawn as an
octagon. Every other element, the fittings, is kept exactly as drawn. A body along Y is a vertical
section whose ends are joints, so they stay open. A body along Z is an arm, so its outer ends are
closed. Bodies longer than a block (the signal mount is 48 units) are cut at block boundaries,
because a UV span longer than the sprite cannot be wrapped.

The five pre-existing concrete accessories (`trafficpoleverticalconnectorconcrete`,
`…quadmountconcrete`, `…signalmountconcrete`, `trafficpolevertdblconcrete`,
`trafficpolehorzdblconcrete`) now come from the generator too, with the same ids and a new body
model. Their lang lines were already in the tree and are not rewritten.

## The texture

`concrete_light_pole.png` used to be a photo of exposed aggregate: pebbles a quarter of a block
across, at full contrast. On a 12-across pole it read as white blotches, and on the narrow strips
of a 16-gon as stripes. It is now generated at 64 px (four texels per model unit): the old
texture's mean colour, gently mottled with tileable value noise on 3, 5 and 11 cell lattices
(none divides the tile, so no grid shows), sparse dark pits in warm browns and greys, a few larger
pits, and the rare bright grain. It was modelled on a photo of a cast concrete face, a shade
darker. `TEX_SEED` fixes it, so `--check` can reproduce it byte for byte.

## Retiring the legacy pieces

The seven hand-placed pieces keep their classes and ids but moved to `CsmTabLightingHidden`, and
implement `ICsmRetiringBlock`:

| Legacy | Retires into |
|---|---|
| `rcpb`, `rcpb2`, `rcpm`, `rcpt` (NOV Round Concrete Pole …) | `trafficpoleverticalconcrete` |
| `ocpb`, `ocpm`, `ocpt` (NOV Octagon Concrete Pole …) | `trafficpoleverticalconcreteoctagon` |

`AbstractBlock.randomTick` carries the facing across, and the new pole works out its ends from
its neighbours, so a legacy base-middle-top stack becomes a pole with its plinth and its top
without anything else being done. What a neighbour cannot tell it, the legacy piece does:
`ICsmRetiringBlock.getReplacementProperties` names property values for the replacement to start
with (by name, since the two blocks are in different modules): the top pieces answer
`top=finial` and the middle pieces `base=none`. It happens on random ticks, so a stack converts a block at a time over a minute or
two of loaded play. Until it has, the pole treats a legacy piece that retires into a concrete
pole as one already, so a half-converted stack has no plinth or cap in the middle of it.

A top piece that converted **before** the finial could be stored (releases up to 2026.11.09)
became a pole with a flat cap and nothing records what it was. Those are put back by hand with
the configuration tool.

The legacy blocks stay in Lighting; their replacements are in Roads. On an install with Lighting
but not Roads, `CsmRegistry.getBlock` finds no replacement and the legacy pieces simply stay as
they are.

## Decorative and historic poles

The same idea carries the decorative street light poles of GitHub #99
(`BlockTrafficPoleDecorative`, Traffic Accessories after the luminaire arms). Each design is one
block stacked to any height whose ends are decided from its neighbours, in the concrete pole's
order: another decorative pole beyond an end (any design) is a plain joint, an
`ICsmPostTopFixture` gets the design's **capital**, the open air its **cap**, and the bottom of a
vertical pole standing on the ground its cast **base**. The values are `none`, `cap`, `tenon`
and `base`, as here, so a post light in Lighting needs nothing new to sit on one.

| Block | Design | Fit radius |
|---|---|---|
| `decopolelafluted` | the 1920s Los Angeles fluted cast electrolier column (16 flutes), moulded base, bronze paint | 4 (thin) |
| `decopolelabroadwayrose` | Broadway Rose: 12 flutes and three rose garlands climbing the shaft a third of a turn a block, so they run on unbroken through a stack | 4 |
| `decopolelaconcrete` | City Design 815 concrete electrolier (museum frames 09, 13): plain concrete shaft, plinth, a flared capital | 4 |
| `decopolelawestwood` | Westwood Village: bronze column on a base tiled in UCLA blue and gold | 4 |
| `decopolelahollywood` | Hollywood Boulevard: a slim grey steel shaft on a round base cover | 3 (pedestal) |
| `decopoleanaheim` | City of Anaheim Special Design #749: 12-flute green pole, two-piece cast base with its door, a cast ring with two banner arm mounts and the Aztec Gold finial as its cap; carries `luminairearmanaheim8ftgreen` (MAST_ARM_CURVE_SYSTEM.md, "Luminaire arms") | 3 |
| `decopolereprofluted` | the reproduction fluted cast-iron post (black, 12 flutes, ball finial) | 3 |
| `decopolecalheights` | Long Beach's California Heights lamp post: a slim navy steel shaft, a flared base, an aluminium capital under `lbcalheightslantern` | 3 |

What differs from the concrete pole:

- **The shaft is drawn by every block** (the pedestal pole's arrangement) and the ends are
  submodels over it, because every cap, capital and base here is wider than the shaft. The shaft's
  walls run the whole block, so a stack has no seam; only its end discs are inset.
- **One class for every design.** Registry name, the radius the arms fit to, the drawn shaft and
  base radii and the post-light colour are constructor arguments (`ThreadLocal` trick).
  `getPoleRadius()` returns the fit radius, so a luminaire arm or a pole-fitted arm meets a
  4-across fluted shaft with its thin-pole saddle.
- **Assets come from `gen_decorative_poles.py`**, which also writes the post-top fixtures for
  Lighting (LIGHTING_SYSTEM.md, "Decorative and Historic Post Tops"). Parts per design:
  `decopole_<id>_shaft`, `_base_n/_s`, `_cap_n/_s`, `_tenon_n/_s`, `_mount`, `_inv` (OBJ), one
  `decopole.mtl` with `body`, `trim` and `tile`, and `blockstates/decopole<id>.json`. The paint
  swatches live in Core (`textures/blocks/decorativepoles/`) because both modules use them.
- **Flutes and ledges.** A fluted lathe cuts rounded grooves (three samples a flute). Wherever a
  fluted section meets a round moulding, the moulding carries a ledge reaching in past the groove
  depth, or the grooves leave slivers to see through. The audit's see-through warnings on the end
  parts are expected: each is an open ring round a shaft drawn by another submodel.
- **Caps may rise above the block.** The Anaheim finial reaches y 21; nothing else leaves its
  block. The generator asserts every other part inside it.

## Traps

- **A multi-block accessory model leaves air in the world.** The signal mount is three blocks
  of model in one block. The poles above and below it have air beyond their ends, so they wear
  caps inside the mount's body, which shows as a slight collar. Telling a real open end from
  one filled by a neighbour's model would need every multi-block accessory to declare its
  reach, and a collar at that joint reads as a coupling anyway, so it is left as is.
- **The mast arm generator has no `--check`.** Running it writes the whole tree. It regenerates
  identically, so `git status` after a run is the check. `concrete` is the last `COLORS` entry, so
  every existing curve keeps its blockstate and its tab line.
- **Line endings.** The generator writes LF; the tree is checked out CRLF. `same_generated_text`
  ignores the difference, and so does the texture comparison, which compares pixels.
