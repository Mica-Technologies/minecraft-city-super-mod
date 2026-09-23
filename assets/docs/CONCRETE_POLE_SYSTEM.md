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
is 1,536 states.

`endStyle` is the pedestal pole's rule with the tenon inserted, in this order:

1. another `BlockTrafficPoleConcrete`: `NONE`
2. an `ICsmPostTopFixture`: `TENON`
3. air, anything replaceable, or vanilla clutter: `CAP`
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
its neighbours, so a legacy base-middle-top stack becomes a pole with its plinth and its cap (or
tenon, under a light) without anything else being done. It happens on random ticks, so a stack
converts a block at a time over a minute or two of loaded play. Until it has, a new pole stacked
directly on a legacy piece shows a plinth there.

The legacy blocks stay in Lighting; their replacements are in Roads. On an install with Lighting
but not Roads, `CsmRegistry.getBlock` finds no replacement and the legacy pieces simply stay as
they are.

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
