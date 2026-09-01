# Lighting

Street lights, floodlights, post lights and a decorative pendant and sconce family — all of them
switchable, and all of them able to actually light the ground.

## The four-state switch

Every light has **four states**, and right-clicking cycles through them. This is the thing worth
learning about CSM lighting, because it is what lets one fixture be both automatic and manual.

| State | Light is | Redstone |
|---|---|---|
| **Redstone off** | Off | Responds — power it and it comes on |
| **Redstone on** | On | Responds — cut power and it goes off |
| **Manual off** | Off | **Ignored** — stays off regardless |
| **Manual on** | On | **Ignored** — stays on regardless |

The first two are automatic mode: wire the light to redstone and it follows the signal. The second
two are manual override: the light does what you told it and ignores the wire entirely.

!!! tip "Lighting a street without wiring it"

    Set the lights to **manual on** and they simply stay lit. Use redstone only where you want them
    to switch — a daylight sensor on a city block, say.

## Lighting the ground

Minecraft calculates light from the block a light source occupies, so a fixture ten blocks up over a
road barely lights the road at all.

CSM works around it: when a light turns on, it scans **downward up to 16 blocks**, finds the first
air block, and places an invisible **light-up air** block there — passable, invisible, and emitting
full light. Turn the light off and the block is removed.

So a street light on a tall pole lights the pavement under it the way it should, and you do not have
to do anything to get that.

## What is in the tab

Fixtures modelled after real manufacturers and styles — Acuity, Alto and others — across street
lights, area and flood lighting, post-top lights, and wall packs. Many come in a range of pole
colours.

The [Lighting reference](../reference/lighting.md) lists all 139 of them. The old wiki was missing
38.

## The decorative family

A separate group of **pendants and sconces** for interiors and storefronts, built from a shared
three-material pattern — a metal finish, a shade, and a lens — so the whole family reads as one set
rather than as unrelated fixtures.

They use the same four-state switch as everything else.
