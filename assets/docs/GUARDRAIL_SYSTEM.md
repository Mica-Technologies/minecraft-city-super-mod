# Guardrail System

Four rail families, their end treatments, a W-to-thrie transition and a crash cushion, in the
Roads module's `trafficaccessories` package. 25 blocks and one configuration tool, all of them
joining blocks: each cell works out from its neighbours what to draw, and nothing carries a tile
entity.

Everything here is procedurally generated. Never hand-edit a model, texture or blockstate these
scripts write.

---

## Overview

| Block | Class | Notes |
|---|---|---|
| `w_beam_guardrail` `_wood` `_double` `_wood_double` | `BlockGuardrail` | the classic two-corrugation rail |
| `thrie_beam_guardrail` `_wood` `_double` `_wood_double` | `BlockGuardrail` | three corrugations, deeper |
| `box_beam_guardrail` `_double` `_stacked` `_stacked_double` | `BlockGuardrailBoxBeam` | square tube on a bolted base plate; the stacked pair carries a second tube half a block under it |
| `cable_barrier` `_double` | `BlockGuardrail` | three tensioned cables with clips |
| `w_beam_thrie_transition` | `BlockGuardrailTransition` | carries one rail into the other |
| `guardrail_end_flared` `_boxing_glove` `_terminal` `_turndown` | `BlockGuardrailEnd` | W-beam ends |
| `guardrail_end_flared_thrie` `_turndown_thrie` | `BlockGuardrailEnd` | the thrie's own folded shapes |
| `guardrail_end_bullnose_box` | `BlockGuardrailEnd` | box beam's own end |
| `cable_barrier_anchor` | `BlockGuardrailEnd` | a raked deadman, not a deflecting end |
| `crash_cushion_nose` `_bay` | `BlockCrashCushion` | telescoping steel bays |
| `guardrail_tool` | `ItemGuardrailTool` | right-click / sneak right-click |

Generators: `guardrail_geometry.py` is the shared contract every other one imports;
`gen_guardrails.py`, `gen_guardrail_ends.py`, `gen_guardrails_thrie.py`,
`gen_guardrails_box_cable.py` and `gen_crash_cushion.py` write the models, textures, blockstates
and the lang/tab fragments.

## Options are sibling blocks, not state

Post material, sidedness and rail family are **separate blocks**, swapped in place by the tool. A
guardrail run is long; a tile entity per cell puts thousands of them on a highway, which is the
cost pattern the performance work is trying to reduce elsewhere. Swapping a block for its sibling
is stateless and free.

Only the POST boolean is stored, in the one spare meta bit — eight facings take three of the four.
`BlockGuardrail` overrides **both** `getMetaFromState` and `getStateFromMeta`: a block that writes
a bit it cannot read back loses the post every time its chunk unloads.

Everything else — both connections, the diagonal filler, the slope, an end's mirror — is derived in
`getActualState` from the neighbours, so cutting a cell out of the middle of a run closes it up and
re-grading the ground under one re-ramps it, with nothing to notify.

## What joins what: the RAIL, not the block

`GuardrailJoins` is separate from `WorkZoneJoins` rather than an option on it, because the two
answer the question differently and both answers are right. A barricade joins only the identical
block — its stripes slope toward the side traffic should pass, so a keep-left meeting a keep-right
contradicts itself. A guardrail joins on the **rail**, so a run may change post material or pick up
a second rail part way along and still read as one run. W-beam still refuses thrie: those are
different sections, and a real transition between them is its own piece of hardware.

`ICsmGuardrailRail` is how a block says what it presents. It asks what rail is at each **END**
rather than what rail the block is, which exists entirely for the transition — see below.

## Slopes, and settling onto the ground

A run climbs rather than staircasing: `GuardrailSlope` is `flat` / `up` / `down`. `up` rises across
the cell to a neighbour a block up on its RIGHT; `down` falls across it from a neighbour a block up
on its LEFT, finishing level.

**The ramp is always drawn in the LOWER of the two cells.** It used to be read off the right-hand
neighbour alone, so a run descending to the right ramped down out of the HIGHER cell — and the
higher cell stands on something solid. The last part of that ramp was drawn inside the block under
it: the ramp seemed to rise out of the top of the wall and the lower run butted into its side, on
two of the four facings (issue #193). The lower cell has nothing but air above its rail, so a ramp
drawn there is always seen whole. `down` is exactly the reflection of `up` about the middle of the
cell, which the transition generator now asserts for every slope.

A single cell in a dip, with a higher neighbour on both sides, can ramp to only one of them: the
right-hand one wins and the left-hand joint stays a step.

**The slope is chosen from where the rails actually ARE, not from block positions.** Guardrails are
`ICsmRoadSurfaceAware` and settle onto whatever they stand on, so a cell on bare ground and one a
block up on a snow layer are one block apart in Y and a couple of sixteenths apart in the world.
Reading the block positions answers that with a whole block's ramp and dives the rail into the
ground. `GuardrailSlope.forRise` takes a real height and picks whichever of the three is nearer.

**Whole-block ramps only — this is a deliberate limit.** Finer ramps need a slope value per step,
and SLOPE multiplies the whole block-state cross product: 1/8-block steps would take this family
from 5,760 block states to 32,640. So a sub-block grade draws level and steps at each cell. The
rail still SITS at each cell's own settled height, which is the part that matters.

## The transition, and why the join test is mutual

`w_beam_thrie_transition` is W-beam at one end and thrie at the other. That is the whole reason
`ICsmGuardrailRail` asks per END: a block that is two different rails would otherwise match neither
neighbour, and a run would stop dead at the very piece that exists to prevent it.

Two guardrails join when **either accepts what the other presents** at the ends that meet. For a
plain rail both clauses ask the same question, since it presents and accepts one section. Only a
piece carrying two rails can answer yes on the second — which is exactly the transition, whose
presented rails are fixed at its ends and do **not** swap when it is drawn mirrored.

`presentsRailOnLeft` stays STRICT for that reason. It asks what the neighbour actually presents
rather than what the two would tolerate; going through the mutual test there would answer yes
whichever way round the run reads, and the mirror would never flip.

## End treatments are chiral

Both ends of a run carry the same facing — facing is the way the rail LOOKS, and the rail looks the
same way along the whole run. So an end treatment is a mirror image of itself at the other end.
`MIRRORED` is derived, not placed: the block finds the run and mirrors itself when the run lies to
its RIGHT. With a run on both sides or on neither it stays unmirrored — a lone end has no hand to
take, and one in the middle of a run is a mistake the player can see.

The mirrored model is REFLECTED about x = 8, never rotated 180°. A rotation would flip z too and
put the rail on the far side of the cell. A reflection reverses handedness, so the generators also
negate the normals' x and reverse every face's winding; positions alone leave the model inside-out.

`BlockGuardrailEnd` also reflects its bounding box, and sets `MIRRORED` false explicitly in its
constructor — `PropertyBool`'s first allowed value is TRUE, so without that every end would default
to its mirror image.

## The crash cushion

Laid the way a run is — a nose and as many bays as the site wants — because a real one is twenty to
thirty feet long and a single block would read as a toy beside the rail it terminates.

Joining is deliberately **one way**: `nose → bay → bay → any rail`. A bay accepts only
`crash_cushion` on its left and anything but a nose on its right, which is what lets one cushion
terminate all four rail families without a per-family backup, and what stops a run reading into the
impact face from the wrong side.

It is its own class rather than a `BlockGuardrail` subclass because a cushion has no post — and a
state bit nothing draws is one somebody eventually toggles by accident. `ItemGuardrailTool` tests
`instanceof BlockGuardrail`, so it passes over the cushion without needing to know about it.

## Collision

`GuardrailJoins.standTall` raises every guardrail, end and cushion to **1.5 blocks** of collision
while the drawn and selectable box stays on the steel. A standing jump clears a block and a
quarter and no rail here is drawn that tall, so a box stopping at the steel is a barrier you walk
over. A vanilla fence solves it the same way and at the same height.

It measures from the box's own FLOOR, not the cell's, so a run settled onto snow or a sloped road
stands its full height above the ground the player is actually walking on.

## Poles and signs stood on a guardrail

A builder puts a sign or a pole up behind the rail by placing it on top of the run, and a post
that stopped on the rail would hover a block above the ground everything else stands on (issue
#190). Every guardrail, end and cushion carries Core's `ICsmPostPassesThrough` marker, and the
two things that stand on it look for that marker below them:

- A vertical `AbstractBlockTrafficPole` (the thick and thin poles) sets `extenddown` and draws
  one more block of itself reaching down through the rail. The extension is supplied by the two
  VERTICAL facing variants in the pole's blockstate, because which way along the pole is down
  depends on its facing; `extenddown` sorts before `facing`, so its `false` branch nulls the
  extension with the same merge trick the rails use. The pedestal and diagonal poles build their
  own state containers and do not take part.
- A sign sets `DOWNWARD`, the property it already used to reach onto a slab, and draws its post
  one block further down. Only the slab case extends the sign's BOX: a guardrail has its own box
  in that cell, and a sign box lapping over it would take the rail's clicks.

A sign placed on a guardrail also no longer takes the rail's facing. `AbstractBlockRotatableHZEight`
copied the facing of ANY eight-way block below, which was meant for stacking a sign on its own
post; it now asks `inheritsFacingFrom`, which is the same class by default and any sign block for
signs.

## Geometry

`dev-env-utils/scripts/guardrail_geometry.py` holds every dimension shared between generators — rail
profiles, the block-out, post, slope rise, the diagonal gap, the end-treatment mounting heights.
Both the rails and the ends import it so they cannot disagree about where they meet. An end whose
rail is two units off the run builds green, loads clean, and is wrong from one angle in game.

`GUARDRAIL_TOP_Y` is the top of every rail family and every post: the top of the cell (issue #192).
It is set once, as the top, rather than as a lift applied to each family, so a run that changes
rail part way along keeps its top line, and the block-outs, posts, ends and transition follow
without being told separately. Every family's other heights are measured down from it. An upward
face on the cell's top points out of the block, so standing flush with it fights nothing; what the
flush top does need is the block-out tucked 0.2 under the rail (`BLOCKOUT_TUCK`), since on the end
treatments the post bites into it and two top faces would otherwise share the plane. The impact
head's rail stub stops at the head's face for the same reason, and the bullnose's object marker
moved from the top of the tube to the post under the nose.

The crash cushion is not raised. It is its own hardware, and its heights do not come from here.

### Stacking

With every top at the top of its cell, a guardrail placed on a guardrail continues its posts, so
two cells of rail read as one taller barrier. The upper one takes the lower one's facing
(`inheritsFacingFrom`), and it settles by exactly as much as the one it stands on
(`getRoadSurfaceOffset`) — by the ordinary rule a surface-aware block below counts as nothing to
settle onto, and a stack on a snow layer or a sloped road would open a gap at the joint. On a
grade the lower cell's post reaches half a cell into the upper one, where the upper post also
stands; the two are the same part in the same texture, so the overlap does not read.

Box beam is the one rail whose post stands on a visible base plate, and a plate half way up a
stacked post reads as a splice nobody would build. So the box beam blocks are their own subclass,
`BlockGuardrailBoxBeam`, which derives `stacked` — true when the block below is a guardrail cell
with a post — and the plate is a separate submodel that `stacked` supplies only when false. `post`
sorts before `stacked`, so a cell with no post nulls the plate first. The post itself stands from
the floor with no bottom face, so it reaches down to the post below without the plate. Only box
beam carries the property; it would double every other family's state count for nothing.

The two-tube `box_beam_guardrail_stacked` (and `_double`) carries a second tube `BOX_STACK_PITCH`,
half a block, under the first (issue #191). A two-tube block on a one-tube one makes three evenly
spaced tubes, and two two-tube blocks make four, so there is no three- or four-tube block. The
lower tube is capped at both ends in the core rather than by the connect-driven end pieces: a run
it joins may have no lower tube to carry it on, and where two do meet, the caps face away from
each other inside one continuous tube.

A cell is one long and a diagonal step is √2, so a 45° run leaves `DIAGONAL_GAP` at every joint;
the right-hand end of each connected block fills it. The filler is excluded from the inventory
model on purpose — the inventory mesh is where the bounding boxes come from, and a 1.414-long
filler would put the box a third of the way into the next cell.

## Adding a rail family

1. Add its profile and registry names to `guardrail_geometry.py`.
2. Write or extend a generator against that contract; emit core / post / fill / end_left /
   end_right pieces per slope, plus the inventory model.
3. `python dev-env-utils/scripts/audit_obj_models.py <the new .obj files>` — 0 FAIL.
4. Register in `CsmTabTrafficAccessories` with the bounding boxes the generator's tab fragment
   emits, passing the rail-kind constant.
5. Add it to `ItemGuardrailTool`'s ring if it has siblings.
6. Lang in all four languages, then `validate_lang_translations.py`.
7. `./gradlew build`, `check_module_assets.py`, then **check in game** — a facing, slope or mirror
   bug compiles green and loads clean.

## Traps

- **A submodel's value in a Forge blockstate must be a full model OBJECT**, not the bare path
  string. Forge parses that slot as a model definition; a string there fails the WHOLE blockstate
  with `Not a JSON Object` and paints every state of the block purple, with only a log line to say
  so.
- **`refreshResources` (the MCMCP resource reload) is broken on this install** —
  `IllegalArgumentException: MALFORMED` out of `ZipFile.getZipEntry`, from a mod jar in `run/mods`
  with a non-UTF-8 entry name. The reload silently does nothing, so a model fix appears not to have
  worked. Restart the client to test a blockstate change.
- **Driving the tool's sneak gesture over MCMCP needs TWO MCP HTTP sessions.** One call blocks for
  the whole key hold, so a single session serialises the hold and the click and the click lands
  unsneaked — which silently reads as the plain gesture, not as a failure.
- **Settle the geometry questions from directly overhead.** A steep ground-level view has misread
  diagonal joins and post positions more than once.
