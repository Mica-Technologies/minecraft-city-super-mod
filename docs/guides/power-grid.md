# Power Grid

Utility poles, cross-arms and the hardware that hangs off them — plus a Forge Energy layer for the
blocks that actually carry power.

## Two halves

Most of the Power Grid tab is **structural**: poles, mounts and cross-arms that make a street look
like a street. A small part of it is **functional**, bridging to Forge Energy so other mods and
CSM's own equipment can draw from it.

## Building a pole

The pole system is modular and stacks:

| Segment | What it is |
|---|---|
| **Pole bottom** | The base section |
| **Pole middle** | Stackable middle sections — as many as you need |
| **Pole top** | The top, which cross-arms attach to |

All of them rotate in six directions, so poles can lean or run horizontally where you need them to.

## Cross-arms

Cross-arms attach to pole tops and carry insulators and wires. Four families, so a build can mix the
eras you would actually see on one street:

| Series | Style |
|---|---|
| **Old Brooks** | Traditional wooden arm |
| **New Brooks** | Updated arm design |
| **PCA** | Post-cap arms |
| **MLUVMB** | Multi-level utility mounts |

## Forge Energy

The functional blocks speak **Forge Energy**, Minecraft's standard energy capability. That means:

- Other mods' generators and machines interoperate with CSM's grid.
- CSM's own [HVAC equipment](hvac.md) draws from it.

## What is in the tab

46 blocks — poles, arms, insulators, wire mounts and the safety equipment that goes with them.
Listed in the [Power Grid reference](../reference/power-grid.md).

!!! note "Overhead signal wiring is a different system"

    The messenger cable that carries traffic signals across an intersection is **not** part of the
    power grid. See [Span Wire](span-wire.md) — it is its own system, and the cable there is purely
    visual.
