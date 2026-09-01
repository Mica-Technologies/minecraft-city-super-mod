# Span Wire System

Wire-span mounting for traffic signals: a messenger cable strung between two poles, with signal
heads, signs and hardware hanging from it at the height the cable actually reaches rather than at
whatever height the block grid offers.

Everything lives in `trafficaccessories/spanwire/`.

---

## What a span is made of

```
   ANCHOR                MOUNT       MOUNT       MOUNT                ANCHOR
  (pole side)                                                       (pole side)
      ●━━━━━━━━━━━━━━━━━━━━●━━━━━━━━━━━━●━━━━━━━━━━━━●━━━━━━━━━━━━━━━━━━━━●   messenger
      ┊  segment owned      ┊            ┊            ┊   segment owned    ┊
      ┊  by left anchor     ╪            ╪            ╪   by mount 3       ┊
   thimble               clamp        clamp        clamp              thimble
   (+ optional guy)     + coils      + coils      + coils          (+ optional guy)
                           │            │            │
                         [head]      [head]       [sign]
                           │            │            │
      ──────────────────────────────────────────────────────────────────    tether (box span)
```

* **Anchors** (`spanwireanchor`) terminate the span on a pole. Two of them define it.
* **Mounts** are the existing `tlitehorzwiremount` / `tlitevertwiremount` blocks, plus
  `spanwireclustermount` for two-to-four heads on one bracket and `spanwiredisconnectbox` for the
  break-out box. All four share one tile entity type's behaviour.
* **Guy anchors** (`spanwireguyanchor`) sit on the ground behind a pole. A back-guy is
  **optional and explicit** — see below.
* The **span wire tool** (`spanwiretool`) links two anchors. Mounts between them are found
  automatically.

## Using it

| Action | How |
|---|---|
| String a span | Click one anchor with the tool, then the other |
| Remove a span | Sneak + click either anchor |
| Plain or box span, when stringing | Sneak + click anything that is not part of a span, then link |
| Add or remove the tether afterwards | Mount screen, *Lower Tether* |
| Check a mount | Click it with the tool — reports its drop against its style's limit |
| Configure a mount | Right-click it with anything but the tool |
| Add a back-guy | Click an anchor with the tool, then a ground anchor |
| Remove a back-guy | Sneak + click the ground anchor |

A mount's screen carries mount style (flush or extending mast), conductor coil (off, one side,
both), signal side, lower tether, and cluster width on a cluster mount. The last row is drawn only
for a cluster, so **cluster width must stay last in `SpanWireMountConfigAction`** -- the screen
draws one row per enum value in order and simply leaves the final one off.

Signal side and lower tether apply to the **whole span**, not the mount they are set from. Both are
single runs; a per-mount answer would let the wires zigzag, or exist along only part of their own
length.

## The back-guy is optional, and that is a correction

An earlier version ran a guy automatically: an anchor searched the blocks below itself and guyed
down to any ground anchor it found. It was wrong, and worth recording as a shape of mistake rather
than a one-off. A signal span is hung pole to pole and a guy is the exception, so deriving one
from "is there a ground anchor nearby" put a wire on nearly every installation that should not
have had one — and the builder had no way to say no short of moving the block.

The pairing is now made explicitly with the tool, and nothing creates one on its own.
`SpanWireGuyFinder` no longer finds anything; what is left of it is the range check the tool
validates against and the cleanup that drops a guy when its ground anchor is broken. Those two
share one definition of "in range", deliberately: because the tool refuses a pairing outside the
box, a bounded sweep at break time cannot miss one.

The general lesson: deriving a *decoration* from proximity is not the same as deriving a
*constraint* from it. Geometry can be inferred. Whether the builder wanted a wire cannot.

## Hardware meets the housing, not the visor

A signal head is not centred in its block. Its body sits at the back and the visors hang off the
front, so hardware coming straight down the block's centre line lands on the top visor —
conspicuously, since that is the lens a driver looks at.

`ISpanWireHangable.getSpanHardwareOffset` lets the payload say where its hardware should meet it.
Signal heads answer with `TrafficSignalBoundingBoxHelper.BODY_CENTRE_SETBACK` backwards along
their facing; that constant is derived from the same model numbers the bounding box is built
from, so re-proportioning a signal moves the drop with it. Everything else returns zero, which is
why zero is the default.

The offset is taken up by **a short arm at the top of the mast, not by leaning the mast across**.
Leaning is the obvious implementation and is wrong: the setback is fixed but the drop is not, so
a mount sitting close under the cable — most of them, near midspan — would lean about forty
degrees. An upright mast with an offset arm at the clamp is also what the real hardware looks
like.

**The two offsets are measured from different origins, and that is load-bearing.** The cable sits
at the *span's* offset from the block centre line; the mast foot sits at the *payload's*. So
`getHardwareFootPoint` is built from the raw `SpanWireDefinition.attachPoint`, **not** from
`getAttachPoint`, which is already shifted by the span. Adding one to the other applies the setback
twice and stands the mast off the back of the housing — which is exactly what happened once the
span's own default started following these same payload offsets.

Kept separate, the geometry corrects itself: when span and payload offsets agree, which is what
`AUTO` arranges, the offset arm has zero length and the mast is plumb. When a builder overrides the
side, the arm grows by the difference and still reaches. Neither case needs a special path.

**Anything with a fixed block model needs the same treatment.** An anchor's eyebolt is drawn by a
JSON model, identical for every anchor in the world and sitting on the block's centre line, so once
the span runs to one side of that line the cable dead-ends *beside* the eyebolt rather than through
it. `emitOffsetLink` closes the gap with a short shackle — the real hardware for exactly this, a
fitting taking up the difference between a fixed plate and where the wire has to land. It draws
nothing when the span runs down the centre line.

The other blocks avoid the problem in their own ways, worth knowing before adding a fifth: a
cluster mount hides its model entirely once linked, and a disconnect box carries no payload, so its
foot is already on the block centre line and the mast's own arm absorbs the offset.

## The five things worth knowing before changing any of it

**1. Mounts go *below* the cable, never above.** Hardware hangs downward from a clamp, so a mount
above the messenger cannot be reached by anything. A mount left level with its anchors is
therefore *wrong*: the cable sags away from the line joining them, so it ends up above the cable
everywhere except at the anchors. At the default 5% sag a builder wants mounts about a block below
the anchors on a 20-block span. `SpanWireDefinition.cableDropAt` returns this signed — positive is
buildable, negative is a placement error — and the link tool reports the two cases separately
because they need opposite corrections.

**2. The messenger is one smooth catenary.** It does not kink at mounts. What looks like a kink in
photographs is the lashed conductor coiled up at the clamp. `SpanWireCatenary` solves
`y = a·cosh((x−x₀)/a) + y₀` by Newton iteration; `DEFAULT_SLACK = 1.0066` gives a 5% sag and holds
that ratio at *every* span length, so it never needs re-tuning for a wider intersection.

**3. Everything derived from a span is computed when the span changes, never per frame.**
Minecraft asks every visible tile entity for its render bounding box every frame, and the renderer
needs the solved curve on top of that. `AbstractTileEntitySpanWireAttachment` solves once in
`onSpanChanged` and caches the cable, the tether, the attach point, the parameters and the bounds.
A Newton solve per attachment per frame would be invisible until someone built a corridor of them.

**4. Each attachment draws only its own piece of cable**, from itself to the next attachment.
That keeps each tile entity's geometry to the gap between two mounts, so culling and chunk
unloading degrade a long span gracefully instead of taking all of it away with one chunk. Every
piece is a slice of the same solved curve, so the joins are exact.

**5. This package knows nothing about signals or signs, and must not learn.** The payload calls
in — `SpanWireHangOffset.computeFor` for a vertical offset, `hangsFromSpan` for a yes/no — never
the other way. A new kind of hangable thing implements `ISpanWireHangable` and needs no edit here.

## How a payload hangs

Three different mechanisms, because three different kinds of block can hang:

| Payload | Mechanism | Why |
|---|---|---|
| Signal heads | Sub-block vertical offset via `getSignalYOffset`, cached on the head's tile entity on a 20-tick refresh | Fully TESR-drawn, so it can move a fraction of a block. The hitbox follows for free through `TrafficSignalBoundingBoxHelper`, and covers and mount kits follow because they read the same hook |
| Signs | The existing **setback** mode, triggered by `hangsFromSpan` | A sign is a plain block model and *cannot* take a sub-block offset. Setback was built for wall and pole mounting, but the shift it produces is exactly what a span needs. One change in `AbstractBlockSign` covers all 472 signs |
| Boxes and anything new | A strap drawn from the mount, via `ISpanWireHangable.needsSpanHangerStrap()` | Nothing of its own to line up; the strap is what stops it reading as floating |

**Every payload hangs from one central drop.** Two earlier attempts are worth recording, because
both looked reasonable and neither survived contact:

* A short strap drawn down from the mast foot. It was *invisible* — the foot sits three quarters of
  the way up the payload's own block, so the strap ran entirely inside the sign it was meant to be
  holding. Geometry that is drawn but never seen looks identical to geometry that was never drawn,
  and no build catches it.
* A pair of drops with a bar across the top, on the theory that a wide panel on one pin reads as
  balanced rather than hung. In game two poles straddling a sign read as scaffolding. One centre
  drop, reaching the payload's own top, is what it should have been.

What a payload *does* still change is whether its mount shows coiled conductor.
`needsSpanConductorFeed()` is true by default, since most of what hangs from a span is powered, and
false for signs: the coil is surplus conductor left from dropping power into the thing below, so on
a sign — bolted to the wire and wired to nothing — it is hardware for a circuit that does not
exist.

**Stringing a span fires no block change at a payload's own position.** `setSpan` therefore calls
`notifyNeighborsOfStateChange`, which is the vanilla path a sign's setback cache already listens
on. Without it, a sign placed before the link keeps a stale answer forever.

## A linked mount stops drawing its own bracket

The bracket in `tliteverticalwiremount` / `tlitehorizontalwiremount` is the decorative mount these
blocks have always been. Once a mount is on a span the renderer draws the real hardware, so the
old bracket is a second bracket in the same place — and it hangs into the block below, where a
signal that has risen to meet the cable now is, showing through the lens as a **black square**.

`BlockSpanWireHangerMount.LINKED` is a `PropertyBool` set only in `getActualState`, so block
metadata still holds nothing but the facing and existing builds are bit-for-bit unchanged. The
blockstate maps `linked=true` to `shared_models/span_wire_hidden`, a model with no elements.

The **disconnect box deliberately has no such variant**: its model *is* the object hanging on the
wire, so it must keep drawing when linked. That distinction lives in the blockstate JSON, not in
code — a new mount block opts in or out by what its own blockstate says.

## Payloads on a cluster bracket do not rise

A cluster bracket is rigid and hangs at a fixed height under its mast, so everything on it sits
level with everything else on it — which is what a real cluster looks like. `SpanWireHangOffset`
therefore returns zero for a clustered payload, the same as it does for an extending mast.

This is not a nicety. The clearance is tiny: a head one block down reaches 0.5 above its own
block, and the attach point is 0.75 up, so `BRACKET_BELOW` has about a quarter of a block to work
in. Letting the heads rise drove their tops straight up through the bracket that is supposed to be
carrying them, and the bar came out across the lenses.

## Sideways alignment

Signal housings do not sit on their blocks' centre lines, but a cable strung between two block
centres does. `SpanWireSignalSide` (CENTRED / LEFT / RIGHT, `OFFSET` = 0.3125) shifts the **whole
span** — messenger, tether, masts, clamps, thimbles, guys — through
`SpanWireDefinition.attachPointOn`. The static `attachPoint` remains the raw block centre that
geometry is defined against.

It is stored **on the span**, not per mount, because the wires are single runs and a per-mount
answer would let them zigzag. The screen resolves the relative side to a compass direction for the
span it is on, because "left" is not something a builder can act on while standing under a wire.

**The default is `AUTO`, and it measures rather than guesses.** Which way to shift cannot be
derived from the span — it depends which way the heads face, which this package does not know —
but it does not have to be *asked* either. Each payload already reports where its hardware wants
to be met, so `SpanWireManager.measureAutoOffset` projects those onto the span's left-hand normal
and takes the **median**: a span carrying four signals and one sign runs over the signals, not at
an average depth suiting neither. `CENTRED`, `LEFT` and `RIGHT` remain as manual overrides.

Two things about that measurement are load-bearing:

* **It runs after the span is applied, not before**, and the span is then applied a second time
  with the result. A sign only reports where its panel is once it is in setback, and it only goes
  into setback once it knows it is on a span — so measuring first gets the answer for a sign still
  sitting at the front of its block. Linking is a click, not a tick, so applying twice is cheap.
* **`AUTO` is appended last in the enum, not inserted first.** The ordinal is what goes to NBT, so
  appending keeps every span saved before this existed reading back as the side it was actually
  set to. A tag with no side key still means centred, which is what those spans were.

Because the amount is measured when the span is strung, changing what hangs on an existing span
does not move its wires until it is restrung. That is the trade for not re-measuring on every
neighbour change, and the tool's summary is where a builder finds out.

The offset is perpendicular, so nothing that depends on position *along* a span — the drop, the
ordering, segment ownership — has to know it exists.

## The box span tether

A box span carries a second, lower wire tying the signal bottoms together so the heads do not turn
in wind. Two things about it are not obvious:

* It is strung **far** tighter than the messenger — `TETHER_SLACK = 1.000265` against the
  messenger's `1.0066`, which is 80% less sag. Solved for, not guessed.
* It **dead-ends on a second pair of anchors**, which are ordinary `spanwireanchor` blocks placed
  lower on the same poles. Same block as the messenger's own anchors, deliberately: the lower end
  of a box span then looks exactly like the upper one, has a hitbox, and takes part in the traffic
  poles' auto-connection like any other block. The tether runs anchor to anchor, at whatever
  height they were placed, and the hardware drawn to meet them is the same shackle-and-thimble the
  messenger gets.

  **Both ends or neither.** One end dead-ended and the other hanging at a derived drop would be a
  tether at two different heights depending on which end you looked at -- worse than either answer
  alone -- so a lone lower anchor is ignored until its partner exists, and breaking one drops both
  back to the derived height. `SpanWireManager.onTetherAnchorRemoved` is the inverse of
  `findTetherAnchorBelow` and shares its reach, so the two cannot drift apart.

  With no anchors placed the tether falls back to hanging at the derived clearance below, with a
  plain stub back toward the pole. That is the fallback, not the intent.
* Its drop is **derived, not fixed**. A tight tether and a sagging messenger converge toward
  midspan, so a fixed drop is only correct at the anchors: on a 20-block span a 2.75 drop leaves
  1.95 of real clearance in the middle, straight through the lamps, and a 40-block span is twice
  as bad. `tetherDropAtAnchors` adds the sag difference so the clearance at the *tightest* point
  is `TETHER_MIN_CLEARANCE` at every span length.

`TETHER_MIN_CLEARANCE` is now only the **fallback**. A span with payloads on it measures its own
clearance at link time — `SpanWireManager.measureTetherClearance` — from the deepest thing hanging
on it, plus a gap. The constant could never be right for every head, because how far a head reaches
below the cable depends on its section count and sizes; a figure that clears a three-section head
is cut straight through a taller one.

The **deepest** payload wins here, not the median that the sideways offset uses. The two statistics
answer different questions: sideways, an outlier should be ignored so the run stays where most of
the hardware is; vertically, an outlier must not be sliced through, and every tie has to point
upward to be drawn at all. Payloads that take no tie — signs — are not consulted, since nothing
ties to them.

**The ties themselves reach the payload rather than standing a fixed height.** This is the part that
actually fixes the alignment: the tether is strung far tighter than the messenger, so heads hanging
from the messenger's curve sit at different heights above a taut tether along the span. A fixed stub
is correct at exactly one point and pokes into the lenses everywhere else. Each mount asks its
payload for `getSpanTetherTieY` and draws to it, clamped so a bad answer cannot reach across the
sky.

## Rendering

Geometry is procedural, emitted per frame into a display list — no OBJ files, so
`audit_obj_models.py` does not apply. Everything round goes through
`SpanWireCableGeometry.emitTubePath`.

**The texture is bound outside the display list, unconditionally, immediately before the call.**
This is not optional — see "Display lists: one texture, no cached state" in
`TRAFFIC_SIGNAL_SYSTEM.md`. Binding inside the list can silently record no bind at all, and the
list then samples whatever the block atlas holds at the white pixel's UV.

`/csm renderpass skip spanWireCable` removes the whole pass; `spanWireCablePerFrame` draws it
without the display list, for A/B measurement inside one session.

## Deliberately not carried over from Immersive Engineering

The cable is **purely visual**: no collision, no damage, no energy, no network semantics. All
interaction is on the anchor and mount blocks, which have hitboxes. There is no per-block spatial
index, no obstruction raytracing along the curve, and no per-tick work — a span exists only as the
copies of its `SpanWireDefinition` held by the blocks taking part in it.

## Tests

`SpanWireCatenaryTest` and `SpanWireDefinitionTest` pin the parts that fail silently rather than
crashing: the solver's endpoints and arc length, its stability across six orders of magnitude of
slack, the sag ratio holding at every span length, the NBT round trip including negative
coordinates, the tether's sag ratio and minimum clearance, and that a sideways offset moves the
whole span without moving anything along it.
