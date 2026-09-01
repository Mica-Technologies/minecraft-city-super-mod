# Span Wire

A messenger cable strung between two poles, with signals hanging from it at the height the cable
actually reaches — not at whatever height the block grid happens to offer.

## Building one

1. **Put up two poles.** Anything works; the demo builds use vertical traffic poles.
2. **Place a Span Wire Anchor** near the top of each, facing the other pole.
3. **Take the Span Wire Tool** and right-click the first anchor, then the second.

The tool reports what it strung:

```
Span wire anchored at (-192,11,-1476). Click the far anchor to string it.
Span strung: 4 hangers, 0.85 block sag at midspan.
```

That hanger count is the confirmation to read. It is how many mounts the span found and adopted.

!!! warning "Place mounts before you string, or re-string after"

    A span adopts the mounts that exist when it is strung. Add a mount to an already-strung span
    and it will sit under the wire unattached. Right-click both anchors again and the count will go
    up.

4. **Hang mounts** under the cable — a Wire Mount for a single head, a Signal Cluster Mount for two
   to four. Put the signal head in the block directly below the mount.

## What you get

Signals **rise to meet the cable** rather than sitting on their block. The cable is solved as a real
catenary, so a head near midspan hangs lower than one near an anchor, and the hardware follows: a
mast down to the housing, a clamp on the messenger, a saddle, and the coiled slack conductor with a
pigtail running back into the mast.

Backplates follow their head, so a plate stays framed around a signal that has risen, and their
retroreflective bands pick up light after dark the way the real tape does.

<figure markdown="span">
  ![Span wire signals at night, the yellow retroreflective bands around their backplates glowing](../assets/img/span-wire-retroreflective-night.png){ loading=lazy }
  <figcaption>The same hardware after dark. The bands are drawn as retroreflective rather than lit, so they read without turning the plate into a lamp.</figcaption>
</figure>

## Diagonals

<figure markdown="span">
  ![A span wire strung diagonally between two poles, with signal heads on it facing different ways](../assets/img/span-wire-diagonal.png){ loading=lazy }
  <figcaption>A 45° span. The heads on it face independently — each one is aimed at its own approach, not at the cable.</figcaption>
</figure>

Spans do not have to run along an axis. A cable strung across an intersection at 45° works, and the
hardware follows it: the clamp sits square to a skewed messenger, cluster brackets run along the
cable rather than along a block axis, and heads on it can face **any direction independently** —
which is the whole point of a diagonal crossing an intersection with approaches on several sides.

## Clusters

<figure markdown="span">
  ![A signal cluster mount carrying two backplated heads that face opposite directions](../assets/img/span-wire-cluster.png){ loading=lazy }
  <figcaption>A cluster carrying two heads that face opposite ways, on one bracket under one mast, tied at the bottom.</figcaption>
</figure>

A Signal Cluster Mount carries two to four heads from one point, on a horizontal bracket under a
single mast.

- Heads on a cluster **need not face the same way.** A cluster covering a side road and a main
  approach is a real arrangement and is what a cluster is for.
- The bracket runs **along the cable**, and the mast comes down at the middle of it.
- Heads on a bracket hang **level with each other** rather than each tracing the cable, because a
  bracket is rigid.
- A thin tie bar joins the bottoms of the housings, which is what stops them swinging
  independently.

Cluster width is set from the mount's options screen. Coverage is centred on the mount, and the
bracket trims itself to the columns that actually hold something — an empty cluster does not reach
into thin air.

## The box span

<figure markdown="span">
  ![A diagonal box span: a messenger cable above and a tether cable below the signals](../assets/img/span-wire-box-span.png){ loading=lazy }
  <figcaption>A box span on a diagonal run — messenger above carrying the weight, tether below holding the heads square.</figcaption>
</figure>

A second, lower tether cable running parallel to the messenger, tying the bottoms of the heads
together. Real installations use one to stop heads twisting in wind.

Turn it on from **any mount's options screen** — it is a property of the whole span, not of one
mount. Place a second pair of anchors lower on the same poles first and the tether will dead-end on
them; without them it hangs at a derived height instead.

The tether is strung far tighter than the messenger, because the messenger carries the weight and is
allowed to sag while the tether only has to stop things turning.

## The mount options screen

Right-click a mount with anything other than the Span Wire Tool:

| Setting | What it does |
|---|---|
| **Mount Style** | Flush to the wire, or an extending mast |
| **Conductor Coil** | None, one side, or both sides |
| **Signal Side** | Shifts the whole span sideways so it lines up over the signal housings, which do not sit on their blocks' centre lines. Auto follows the signals |
| **Lower Tether** | None, or box span |
| **Cluster Width** | Two to four. Only shown on a cluster mount |

## Things worth knowing

- **The cable is purely visual.** It carries no power and has no collision. Signals are wired the
  way they always were.
- **There is no Immersive Engineering dependency.** The span wire is CSM's own implementation.
- **A back-guy is optional**, not automatic. Nothing runs from a pole to the ground unless you put
  it there.
- **A mount above the cable draws nothing.** That is a placement error being reported rather than
  hidden — move the mount below the wire.
