# Traffic Pole Bases

The bolted bases the painted steel traffic poles stand on (GitHub #348): what they are, how a
pole decides whether to draw one, how the choice is stored, and how the models and blockstates
are made. The concrete poles' plinths and the pedestal pole's base are their own systems
(`CONCRETE_POLE_SYSTEM.md`, `PEDESTAL_POLE_SYSTEM.md`); this covers the thin and thick painted
poles.

## What it is in game

An upright thin pole (`trafficpolehorizontal*`, 8 across) or thick pole (`trafficpolevertical*`,
12 across) standing on the ground draws a base at its foot. Nothing is placed: the bottom block
of the stack draws it, and every pole already in a world gets the default.

| Style | `foot` value | What it is |
|---|---|---|
| Standard square bolted base | `square` | a square plate, a domed nut cover at each corner; the default |
| Standard squircle bolted base | `squircle` | the same plate with its corners rounded off, a little thicker |
| Slim square bolted base | `slim` | a thin plate on exposed anchor bolts: washer, hex nut, stud |
| Pedestal base | `pedestal` | a tall tapered square base, the shape the pedestal pole stands on |
| No base | `none` | the bare tube |

The **Street Light Configuration Tool** in its *Cycle Pole Base* mode steps through them:
right-click anywhere on an upright stack and the click is carried down to the bottom block. The
same mode steps a concrete pole's plinth.

A base takes the pole's own finish.

### When the foot is drawn

Only an upright pole has a foot. From the block under it:

| Under the pole | The foot shows |
|---|---|
| a guardrail or anything else that is `ICsmPostPassesThrough` | one more block of tube, reaching down through it (`extend`) |
| air, anything replaceable, plants, snow layers, water | nothing |
| another traffic pole, or a CSM block that is its own mounting hardware (`ICsmTrafficPoleIgnored`): a hand-placed Traffic Pole Base, a connector, a cabinet | nothing |
| anything else | the owner's choice |

So a pole someone already stood on a hand-placed `trafficpolebase<colour>` block keeps that
block and grows nothing of its own.

## Files

| Path | Role |
|---|---|
| `modules/roads/.../trafficaccessories/AbstractBlockTrafficPoleFooted.java` | the `foot` property, the metadata, the actual state |
| `modules/roads/.../trafficaccessories/BlockTrafficPoleLarge*.java`, `BlockTrafficPoleSmall*.java` | the ten painted poles, now subclasses of it |
| `modules/roads/.../trafficaccessories/ItemStreetLightConfigTool.java` | `cycleFoot` |
| `dev-env-utils/scripts/gen_pole_bases.py` | every asset below |
| `shared_models/polebase_<width>_<style>.obj`, `polebase.mtl` | the bases |
| `blockstates/trafficpolevertical*.json`, `trafficpolehorizontal*.json`, `trafficpolehorzunpainted.json` | the ten poles' blockstates |

`python gen_pole_bases.py --check` fails on drift. The ten blockstates were hand kept before;
they are the generator's now, and it restates each pole's model, mount stub and texture in
`POLES`.

## One property for everything at the foot

`FOOT` is the stored choice and, in the actual state, what is drawn. Its sixth value, `extend`,
is never stored: it is the reach-down extension that `AbstractBlockTrafficPole.EXTEND_DOWN`
carries on the poles that do not have a foot. A base and an extension are never drawn together,
so one property of six values carries both; two properties would have multiplied to ten, and
every value multiplies the variants a Forge blockstate bakes. A painted pole is 576 variants
(6 facings x 16 mount combinations x 6 feet), up from 192.

The blockstate hangs a submodel on each value: `extension` (the pole's own model, translated one
block down) for `extend`, `foot` (the base OBJ) for a style, nothing for `none`.

## Upright poles are always drawn facing up

A pole placed while looking up at it faces `DOWN`, and its foot is then at model north instead
of model south. The tube is the same either way round, so `getActualState` turns every upright
pole to `UP` before the superclass works out the flank mounts (which are relative to the
facing, so they come out right for the pole as drawn). The blockstate therefore needs one base
model per style and width, authored at model south, and the extension is one translation
instead of the two the old blockstates carried under `facing=up` and `facing=down`.

## Metadata

The facing stays in the six values the poles always stored, read back as the default base, so
no pole in an existing world changes block state. Values 6 to 9 are an upright pole with one of
the other four choices (squircle, slim, pedestal, none), always stored facing `UP`. A pole on
its side has no foot and stores no choice.

## The models

Each base is authored standing (Y up, the ground at y = 0, the pole's axis through the block
centre) and written through the pedestal pole generator's `to_model_south`, which is the frame
of a pole facing up. They borrow that generator's mesh, lathe and square frustum rather than
copying them. A plate is an outline extruded (`slab`), a nut cover and an anchor bolt are small
lathes, and the pedestal is a square frustum with a round collar. Fittings scale with the pole
(`WIDTHS`): 0.8 on the thin pole, 1.0 on the thick.

They are OBJ rather than JSON elements for the domes and the taper, and because an OBJ may
leave its block: the thick pole's plates overhang its block by about a pixel a side and its
pedestal by a pixel and a half, which a JSON element could do only with explicit UVs. Nothing
has an underside; it lies on the ground.

The audit (`audit_obj_models.py`) warns that each base is see-through from below and that the
collar is open at the top. Both are expected: the ground is under one and the pole's own tube
fills the other.

## Traps

- **The inventory icon shows no base.** The item is the pole's own model; a base belongs to a
  placed pole standing on something.
- **A pole on its side keeps a stored choice of `square`** (the default state) and draws
  nothing; do not read `FOOT` from a stored state as "has a base".
- **Other poles still use `EXTEND_DOWN`.** Only the classes that extend
  `AbstractBlockTrafficPoleFooted` build their state without it.
