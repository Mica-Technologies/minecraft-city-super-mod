# Mast Arms

The realistically scaled curved upsweeps that take a signal mast arm off a pole and out over the
roadway.

## Why they are several blocks

A real curved mast arm covers **10–25 feet of run and 4–10 feet of rise**, leaves the pole at
roughly 20–35°, and arrives at the horizontal run with **zero** slope so the joint does not kink.

None of that fits in one Minecraft block model. The original upsweep,
`trafficpoleverticalcurveconnector`, is already as large as a Forge JSON model is legally allowed
to be — and it is not really a curve at all, but 130 stacked slabs making a constant-slope ramp
with a staircased silhouette. It is still there and still works; the curves on this page are the
bigger, smoother alternative.

So each curve is a **connected staircase of real blocks**, one per cell the arm passes through,
each drawing its own slice of one continuous sweep. That costs more blocks, and buys three things
a single giant model could never have:

1. **It does not vanish.** A model belongs to its block's chunk section. A six-block arm hanging
   off one block disappears the instant that block is culled while the arm is still on screen.
2. **Things can mount to it.** Signal heads, signs and luminaires attach to *blocks*. With one
   block, nothing could hang off the curved part — and real intersections hang things there.
3. **Auto-connect works.** The pole mounting system reads neighbouring block positions, and empty
   air is invisible to it.

The geometry still reads as continuous, because each band of the sweep is drawn whole by whichever
cell owns it.

## The sizes

Five curves, named for the road they suit:

| Curve | Run × rise | Suits |
|---|---|---|
| 4×1 | 4 blocks out, 1 up | Local street |
| 5×1 | 5 × 1 | Collector |
| 6×2 | 6 × 2 | Arterial |
| 8×2 | 8 × 2 | Major intersection |
| 10×2 | 10 × 2 | Wide arterial |

Each comes in the full set of pole colours. All of them are in the
[Traffic Accessories](../reference/traffic-accessories.md) reference.

## Building with one

1. Put up a vertical traffic pole to the height you want the arm to leave at.
2. Place the curve blocks along the cell path, starting at the pole.
3. Continue with horizontal pole sections for the straight run out over the road.
4. Hang signal heads and signs off any cell using the normal mounts.

The curve arrives at horizontal with zero slope, so the straight run continues from it without a
visible kink.

## Mounting hardware

Every cell can grow a bracket toward any of its four sides — up, down, and both sides — wherever
there is something mountable against it, the same way a pole does. Along the arm itself there is no
stub, because that is where the arm goes.

A curve deliberately ignores exactly what a pole ignores, and one cell of a curve will not sprout
hardware into the next cell of the same curve.

!!! note "Why the hardware is per cell"

    On a pole, one bracket model serves everywhere, because a pole is a straight tube centred in
    its block. A curve cell's tube is neither centred nor level, and its pose is different in every
    cell — so a shared model would float off the tube in most of them. Each cell carries its own.
