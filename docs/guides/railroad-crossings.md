# Railroad Crossings

Everything a grade crossing needs: the signs on the approach, the crossbuck at the tracks,
the flashers and the gates.

## The signs

- **Railroad Crossing Advance Warning** — the round yellow RR, on the approach.
- **Railroad Crossing Sign (Crossbuck)** and the **2 Tracks** plaque under it, at the tracks.
- **Do Not Stop On Tracks**, where a queue could back up over the crossing.

All in the Road Signs tab, and they go on posts like any other sign.

## The flasher

**Railroad Crossing Flasher** is the top of the mast — crossbuck, crossarm, the two red
lamps and the bell — and goes on top of any traffic pole. It is dark until it gets
**redstone**; then the lamps alternate and the bell rings, and it stays that way until the
redstone drops.

That is the whole control system: a **detector rail** on the track is the track circuit. Run
its signal to the mast (or to a gate) and a train activates the crossing on its own, with no
controller to configure. A lever works too, for a museum crossing or a test.

Every flasher in the world alternates in step, like every RRFB, because the flash is in the
texture rather than in the block.

## The gates

**Railroad Crossing Gate** comes in one-, two- and three-lane lengths. The block is the
mechanism cabinet; the arm swings down across the road when the block is powered, taking about
eight seconds, and back up when the power drops, taking ten. Its tip lamp burns steadily and the
two behind it alternate while the arm is anywhere but up.

The arm extends to the cabinet's **right as you face it** — so put the cabinet on the
right-hand shoulder of each approach, facing the traffic, and it closes the lanes coming toward
it. A two-lane road with a gate on each side is two one- or two-lane gates facing opposite ways.

The arm is not solid: a real one is meant to be driven through in an emergency.

!!! note "Interconnected crossings"
    A crossing next to a signalised intersection is, in reality, wired into the signal's
    railroad preempt. Here the two are separate for now: the crossing runs on redstone, and the
    ADVANCED controller's preempt runs on its own sensor zone. Feeding the same detector to both
    keeps them together.
