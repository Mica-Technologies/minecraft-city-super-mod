# Pedestal Pole System

The pedestal traffic pole (`trafficpolepedestal<colour>`): the slim aluminium post that carries
a pedestrian signal, a push button or a small sign at the kerb. In the field it is a 4½ in
schedule-40 tube threaded into a cast, tapered pedestal base with a bolted access door, and a
domed cap on top. This document covers how the block draws that from one stackable block type,
and why it is built the way it is. It does **not** cover the thin and thick poles it sits
beside (`trafficpolehorizontal*`, `trafficpolevertical*`), which are unchanged.

## What it is in game

One block per colour, in the five finishes the pole family already has: silver, black, tan,
white and unpainted. Stack it to any height, exactly like the thin pole. Each block decides
for itself what it shows, from its neighbours alone, so there is no separate base block and no
separate cap block to place:

| The block…                                                   | shows                          |
|---------------------------------------------------------------|--------------------------------|
| has another pedestal pole beyond an end of its tube           | a seamless tube, no coupling   |
| is vertical and stands on the ground (or any solid block)     | the pedestal base at that end  |
| has air, plants, snow layers or water beyond an end           | the domed cap at that end      |
| runs into any other solid block, such as a signal head on top | a plain tube end               |

So adding a block on top of a pole moves the cap up with it, breaking the bottom block takes
the base away, and a horizontal run capped at both ends is one block placed sideways. Flank
mounting is the family's: anything mountable beside the tube grows a band-clamp bracket toward
it, and blocks that draw their own hardware (`ICsmTrafficPoleIgnored`) do not.

The access door is on the world **south** face of the base for both vertical facings. That is
a consequence of the geometry being one model per end rather than per facing (see below), and
was judged not worth a rotation property: the door is a detail, and the base is symmetrical
otherwise.

The tube is 6/16 across, against the thin pole's 8/16. A real 4½ in pedestal tube on a real
8 in signal pole is 0.56 of the width; 0.75 is as slim as it can go here and still read as a
pole at Minecraft's texel density and player scale.

## Files

| Path | Role |
|------|------|
| `modules/roads/.../trafficaccessories/BlockTrafficPolePedestal.java` | the block: one class, constructed per colour from the tab |
| `dev-env-utils/scripts/gen_pedestal_pole.py` | generates every asset below from one description of the shape |
| `models/block/trafficaccessories/shared_models/pedestalpole_shaft.obj` | the tube, drawn by every block |
| `…/pedestalpole_cap_n.obj`, `…/pedestalpole_cap_s.obj` | the domed cap, at the model-north / model-south end |
| `…/pedestalpole_base_n.obj`, `…/pedestalpole_base_s.obj` | the pedestal base, likewise |
| `…/pedestalpole_mount.obj` | the band-clamp bracket a flank grows toward a device |
| `…/pedestalpole_inv.obj` | base + tube + cap in one block, the inventory icon |
| `…/pedestalpole.mtl` | one material, `body`; each colour's blockstate retextures it |
| `blockstates/trafficpolepedestal<colour>.json` | five blockstates, per-property Forge format |

Nothing under `shared_models/pedestalpole_*` or the five blockstates is hand edited. Change the
shape in the script's constants and re-run it; `python gen_pedestal_pole.py --check`
regenerates into a temp directory and fails if the tree has drifted, which is the guard to run
before committing anything that touches these files.

## The block

`BlockTrafficPolePedestal extends AbstractBlockTrafficPole`. It inherits `FACING` (NSEWUD) and
the four flank mount booleans, and adds two enum properties:

```java
public enum EndStyle { NONE, CAP, BASE }
public static final PropertyEnum<EndStyle> END_NORTH = PropertyEnum.create("endn", EndStyle.class);
public static final PropertyEnum<EndStyle> END_SOUTH = PropertyEnum.create("ends", EndStyle.class);
```

`getActualState` calls the superclass for the flanks, then decides each end. All six properties
are actual-state only; metadata still holds just the facing, so nothing about this block is
stored differently from the rest of the family. 6 facings × 16 mount combinations × 9 end
combinations is 864 block states.

The class takes its registry name and colour as constructor arguments, so there is one class
for the five colours instead of five near-identical files. `getBlockRegistryName()` is called
from the `AbstractBlock` constructor before the subclass's fields exist, so the arguments are
parked in a `ThreadLocal` through the `super(stash(…))` argument expression, the same trick
`BlockTrafficPoleMastArmCurve` uses. It needed one thing the pole family did not have: a
`protected AbstractBlockTrafficPole(Material)` constructor, because the no-argument one gives
the argument expression nowhere to go.

### Deciding an end

`endStyle` looks at the block beyond the end, in this order:

1. **Another pedestal pole** — `NONE`, whatever its facing. A perpendicular pole meeting the
   tube gets a flush T-joint rather than a cap poking into it.
2. **Nothing worth abutting** — `CAP`. Air, anything `isReplaceable` (tall grass, snow layers,
   water) and the vanilla natural clutter the pole family ignores for mounting (leaves, vines,
   torches, rails, carpets…). The CSM opt-out marker `ICsmTrafficPoleIgnored` is deliberately
   *not* in this set: a signal head sat on the post top is solid hardware the tube should run
   straight into, even though the flanks ignore it.
3. **Vertical, and the end points down, and the block below is not pole hardware** — `BASE`.
   "Pole hardware" is any `AbstractBlockTrafficPole` or any `ICsmTrafficPoleIgnored`: stand a
   pedestal pole on a thick pole or a mast arm cell and the two meet flush.
4. Otherwise `NONE`.

### Which end is which

This is the one subtle point, and it is why the properties are named for **model space**
rather than "top" and "bottom".

The tube is authored running north–south, and the blockstate's `facing` variants are the
vanilla `x`/`y` rotations that carry a north-facing model to each facing (`x: 270` for up,
`x: 90` for down, `y` for the compass). Those rotations put model north at the facing
direction, so:

* the **model-north** end of the tube always points in the `facing` direction, and
* the **model-south** end points the opposite way.

`endDirection(facing, north)` is that rule, and it is the only place it lives. A pole placed on
the ground faces `UP` (`getDirectionFromEntityLiving` from the placer standing above it), so its
base is at the model-south end; a pole placed above head height faces `DOWN` and its base is at
the model-north end. Each end therefore has its own base and cap model — `_n` and `_s` — and the
blockstate attaches a fixed submodel to each property value with **no transform**, letting the
facing rotation carry it to the right world end.

The alternative — one `base` boolean plus a facing-dependent transform — cannot be written in a
per-property Forge blockstate, because a submodel's transform cannot depend on another
property's value. It could be written by enumerating every `facing=…,base=…` combination by
hand, which is 864 variants, or with a vanilla `multipart` blockstate, which cannot retexture an
OBJ material and would need five OBJ copies per part. Two extra OBJ files is the cheap way out.

### Bounding box

The tube's box is `[5, 5, 0] → [11, 11, 16]` along model Z, rotated by the facing like every
NSEWUD block. `getBlockBoundingBox` receives the actual state (the rotatable base class fetches
it), so the block that carries the base returns the base's footprint instead,
`[1.8, 1.8, 0] → [14.2, 14.2, 16]`, and the pedestal is selectable out to its corners.

## The geometry

Everything is authored in a **standing frame** — Y up, base on the floor, door on +Z — and
mapped into model space at write time. Two maps, both proper rotations about the block centre
(determinant +1, so winding and normals survive):

| Map | Standing → model | Standing floor lands at | Standing top lands at | Used for |
|-----|------------------|-------------------------|-----------------------|----------|
| `to_model_south` | `(x, y, z) → (x, z, 16 − y)` | model z = 16 (south) | model z = 0 (north) | `base_s`, `cap_n`, the shaft |
| `to_model_north` | `(x, y, z) → (x, 16 − z, y)` | model z = 0 (north) | model z = 16 (south) | `base_n`, `cap_s` |

A base sits at the standing *floor* and a cap at the standing *top*, so the two pieces named
for the same model end take **opposite** maps. Getting that backwards is easy and looks like a
ring one block below the top of the pole (the cap's lip, upside down at the far end of the
block) with a flat tube end where the dome should be; the generator's comment at the parts
table says the same, because that is exactly what its first version did.

Under the facing rotation both maps send the standing +Z face to world south, which is why the
door is south either way. The mount is authored directly in model space, pointing north from the
axis, and the shaft is symmetric so either map serves.

The parts, in 1/16 block units:

* **Shaft** — a hexadecagon of radius 3, full block length, with end discs 0.01 inside the
  block boundary. Everything that hides an end (cap, base, the next pole) covers them; an open
  end against a non-full block shows a plausible tube mouth rather than a hole in the world.
  The vertex angles are offset by half a step so a flat facet faces each compass direction:
  that is what lets a flat clamp plate sit on the tube instead of straddling a vertex.
* **Cap** — a lip of radius 3.5 from y 14.4 to 15.2, then a dome closing at y 16. Its bottom
  ring is at radius 3.02, a hair off the tube, so it shares no surface with it.
* **Base** — a square foot 12.4 across and 1 high with a top ledge, a three-segment concave
  taper from 12.0 across at y 1 to 7.8 across at y 10, a square shoulder, then the threaded
  collar (radius 3.6, y 10 to 11.6, bevelled) the tube disappears into. The collar has no
  bottom disc, because that disc would lie exactly on the shoulder's top face. The **access
  door** on the south face is a plate 5.2 wide from y 2 to 8.8 standing 0.35 proud of the
  taper and *following* it (it is built from the same profile), with four corner bolt heads
  and a latch boss, each sunk slightly into the plate so nothing floats where it slopes.
* **Mount** — a 4.4 × 4.4 clamp plate whose outer face is 3.1 off the axis (the tube's flat
  facet is at 2.94) and whose inner face is sunk to 1.8, so its corners do not float where the
  tube curves away; four bolt heads; and an octagonal arm of radius 1.4 out to the block face.
  It is symmetric about its own axis on purpose: the blockstate rotates one model into all four
  flank directions, and the tube runs along X in two of those rotations and Y in the other two,
  so nothing on the bracket may prefer either. That is also why the bracket is a plate and not
  the full band clamp the real hardware uses — a band around the tube cannot be authored once
  for both of those rotations.

The four mount transforms in the blockstate are the thin pole's, verbatim. Forge submodel
rotations are right-handed where the vanilla facing `x`/`y` are negated, which is why
`mounteast` — a block to the pole's *relative west*, per `BlockUtils.getRelativeFacingOpposite`
— is drawn with `y: 90` and comes out pointing the right way. Do not "fix" the signs; the
whole family depends on that pairing.

### Guards in the generator

* Every triangle is wound to face its analytic vertex normal, which every primitive sets from
  the geometry (a lathe from its profile slope, a frustum face from its taper). A face cannot
  come out inside-out; a degenerate one fails the run.
* Every part's bounds are asserted inside `[−0.02, 16.02]`.
* `audit_obj_models.py` reports the cap and base as see-through from some angles when audited
  **alone**. That is the open ring the tube passes through, and it is expected; the inventory
  model, which is the same parts assembled, audits clean, and that is the check that matters.

## Adding a colour

Add a tuple to `COLORS` in the script (id, texture, label), re-run it, paste the new line from
`_pedestal_out/tab_fragment.txt` into `CsmTabTrafficAccessories` and the lang line into each
locale. `TRAFFIC_POLE_COLOR` has to have the value; it feeds the lighting accessories that
inherit a pole's colour.
