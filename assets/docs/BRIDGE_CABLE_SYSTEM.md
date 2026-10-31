# Bridge Cable System

Stay cables, suspension main cables, hangers and rope, strung between anchor blocks at any
length and angle (GitHub #323). Purely visual: no collision, nothing along a cable is checked, so
a cable passes through whatever is in its way. Built for the City of Alto's landmark bridges over
I-50: a cable-stayed bridge (32 stays, 13 to 57 blocks), a self-anchored suspension bridge (two
main cables, sag 6, hangers every 5) and tied arches (vertical hangers).

Everything lives in Roads, `trafficaccessories/bridgecable/`, and in the Traffic Accessories tab
right after the span wire tool. Assets come from `dev-env-utils/scripts/gen_bridge_cables.py`.

---

## What there is

| Registry name | What it is | Faces |
|---|---|---|
| `bridge_cable_socket` | A stay's socket on a pylon face: bearing plate, bolts, a boss the anchor pipe leaves from | Away from the face it was placed on (six ways) |
| `bridge_cable_clevis` | A forked clevis and pin, for an open end | Away from the face it was placed on (six ways) |
| `bridge_cable_saddle` | A suspension tower's saddle; the main cable passes over it in a groove | The way the builder faces (four ways): the groove runs that way |
| `bridge_cable_deck_anchor` | Sits on top of a deck edge beam, where a stay or hanger comes down | The way the builder faces (four ways) |
| `bridge_cable_tool` (item) | Strings a cable by clicking | |

All four anchors share one tile entity (`tileentitybridgecableanchor`), which holds the cables
that anchor **owns**. A cable is drawn by its owner only; the block at its other end (another
anchor, or any block at all) just has a cable arriving at it.

## Using it

| Action | How |
|---|---|
| String a cable | Bridge Cable Tool: click the owning anchor, then the anchor or block it runs to |
| Change what the tool strings | Sneak + click anything that is not an anchor, or sneak + use in the air: stay, main (10% sag), main with hangers every 5, hanger, rope |
| Clear a half-made link | Use the tool in the air |
| Remove an anchor's cables | Sneak + click it with the tool |
| Anything else (thickness, colour, exact sag, stripes, LED, hanger spacing) | `/blockdata` on the owning anchor, below |

The tool never draws a cable twice: linking A to B first drops a B-to-A cable B may own. A
cable strung both ways by NBT is drawn once, by the anchor at the lower position
(`BlockPos.compareTo`), so both ends decide alike without either one changing its links.

## NBT

The anchor's tile entity takes two forms. Both are written for `/blockdata` (or MCMCP's
`server_set_block` with NBT) so a generator can place hundreds of cables without a click.

**One cable at a time** (what a generator writes): the keys at the top of the compound.

```
/blockdata -2535 117 -2340 {target:[-2532,85,-2387],style:"stay",thick:4,color:"white",sag:0}
```

Each such command **adds** the cable, or **replaces** the one already running to the same block.
So a pylon socket carrying several stays takes one command per stay, and re-running a generator
changes nothing. The top-level keys are read once and never written back: the next save holds
only the list below.

**The list** (what is saved, and what `/blockdata` shows): `cables:[{...},{...}]`, each entry the
same keys. Setting `cables` replaces the whole list; `{cables:[]}` removes every cable.

| Key | Values | Default | Notes |
|---|---|---|---|
| `target` | `[x,y,z]` absolute (a list, `[I;x,y,z]` or `{x:,y:,z:}`) | — | Where the cable runs to |
| `dx`, `dy`, `dz` | whole numbers | — | The same, relative to the anchor; used when there is no `target` |
| `off` | `[I;dx,dy,dz]` | — | How a cable is **saved**: as an offset, so a bridge pasted elsewhere keeps its cables. `target` wins over `off` when both are given |
| `style` | `"stay"`, `"main"`, `"hanger"`, `"rope"` | `"stay"` | Aliases: `suspender`, `suspension`, `net`, `cable_net` |
| `thick` | pixels, 1 to 16; use 2, 4, 6 or 8 | stay 4, main 8, hanger 2, rope 2 | `"4px"` also reads |
| `color` | `white`, `light_grey`, `galvanized`, `black`, `red`, `international_orange`, `blue`, `yellow`, `green`, or `#rrggbb` | stay white, the rest galvanized | Aliases: `orange`, `grey`/`gray`, `light_gray`, `silver`, `alto_blue` |
| `sag` | a number of blocks (`6`), or a string percentage of the plan span (`"8%"`) | main `"10%"`, rope `"4%"` | Below the straight line between the ends, at the middle. Stays and hangers never sag |
| `stripes` | a colour | none | Two helical stripes wound round the sheath |
| `led` | a colour | none | An LED strip along the top of the cable, drawn at full brightness |
| `hangers` | spacing in blocks | 0 (none) | Main and rope only: drops a hanger every N blocks; see below |
| `hangerThick` | pixels | 2 | |
| `hangerColor` | a colour | the cable's colour | |
| `remove` | `[x,y,z]` | — | Top level only: drops the cable to that block |

An anchor holds at most 16 cables. A target on the anchor itself is ignored.

### Examples the bridge generators can copy

A stay, pylon socket to deck anchor (the cable-stayed bridge, one command per stay):

```
/setblock -2535 117 -2341 csm:bridge_cable_socket 2
/blockdata -2535 117 -2341 {target:[-2532,85,-2387],style:"stay",thick:4,color:"white"}
/blockdata -2535 117 -2341 {target:[-2532,85,-2381],style:"stay",thick:4,color:"white"}
```

The socket's metadata is its facing (`EnumFacing` index: 0 down, 1 up, 2 north, 3 south,
4 west, 5 east); place it in the air block against the pylon face, facing away from it. The deck
anchor goes on top of the edge beam (y 85 for a beam whose top is y 84).

A suspension main span with hangers every 5 to the deck, and the side span down to the anchorage
from the same saddle (the self-anchored bridge: one saddle owns both cables over it):

```
/setblock -1320 111 -2380 csm:bridge_cable_saddle 3
/blockdata -1320 111 -2380 {cables:[{target:[-1320,111,-2300],style:"main",thick:8,color:"international_orange",sag:6,hangers:5,hangerThick:2},{target:[-1320,85,-2397],style:"main",thick:8,color:"international_orange",sag:1}]}
```

Arch hangers, one per pair of anchors (the tied arches; the clevis on the arch's underside faces
down, the deck anchor on the tie):

```
/setblock -230 100 -2350 csm:bridge_cable_clevis 0
/blockdata -230 100 -2350 {target:[-230,85,-2350],style:"hanger",thick:2,color:"galvanized"}
```

A white stay with a blue sheath stripe and a blue LED strip, then removing a cable:

```
/blockdata ~ ~ ~ {target:[0,64,30],style:"stay",thick:6,color:"white",stripes:"blue",led:"blue"}
/blockdata ~ ~ ~ {remove:[0,64,30]}
```

## The shapes

* **Stay** and **hanger** are straight.
* **Main** is a parabola through the ends, falling exactly `sag` blocks below their chord at the
  middle, whatever the difference in height between the ends. A suspension bridge's main cable
  carries a deck spread evenly along it, and that load makes a parabola, not a catenary.
* **Rope** is a catenary (it carries only itself), from the span wire's own solver
  (`SpanWireCatenary`). The solver takes slack, not sag, so `BridgeCableShape.slackForSag` finds
  the slack that hangs the asked sag by bisection.

Each anchor kind has its own point where a cable meets it (`BridgeCableAnchorKind`, shared with
the generator's models): the socket's boss face, the clevis's pin, the saddle's groove (13/16 up,
an 8 px cable's centre), the deck anchor's pedestal top (6/16 up). A socket or deck anchor end
also wears a **guide pipe** for its first block, drawn by the renderer at whatever angle the
cable leaves, so the socket does not have to be modelled for every angle; a clevis end wears a
short one, a saddle none. A short cable's pipes are cut to a quarter of its length.

A cable that ends on a block that is not an anchor ends on top of it. A cable with nothing at
its far end (air, water or a plant: the far anchor was broken, or the block is not built yet) is
not drawn at all, and its link is kept: it comes back as soon as something is there to hold it.

### Hangers

A main or rope cable with `hangers:N` drops a vertical hanger every N blocks of plan length from
its owner's end, and none within half a block of the far end. Every anchor's point is on a block
centre, so on a cable running along x or z every hanger lands on a block centre too: a 100-block
span with `hangers:5` has 19, at 5, 10 ... 95. Each one falls straight down to the first deck
anchor or the first block with a collision box below it (water and plants do not count), and
ends on top of it. None is drawn where nothing is found within 256 blocks, where the cable runs
through a solid block, or where it would be shorter than a quarter block. Each wears a cable band
where it clamps, square to the cable, and a short guide pipe when it lands on a deck anchor.

Hangers are worked out by the client against the blocks it can see, every 40 ticks or so: a deck
built after the cable gets its hangers within two seconds, without anything telling the anchor.
Explicit hangers (`style:"hanger"` between two anchors) still work, and are what an arch uses.

## Rendering

`TileEntityBridgeCableAnchorRenderer` draws an anchor's whole `BridgeCableLayout` (every cable,
pipe, hanger, band and LED strip it owns) as one display list, through the span wire's tube
builder (`SpanWireCableGeometry.emitTubePath`, given a general form that takes any number of
sides and a colour per facet; the span wire's own calls produce the same vertices as before).
Vertices are BLOCK format with the anchor's light baked in, textured on the shared white pixel;
the LED strip's light is full bright.

* **The white texture is bound outside the list, every frame, immediately before the call**, as
  "Display lists: one texture, no cached state" in `TRAFFIC_SIGNAL_SYSTEM.md` requires. Nothing
  inside the list touches cached GL state.
* The list is keyed on the layout's key (which changes whenever any end or hanger moves) and the
  anchor's combined light. A new key frees the anchor's old lists first.
* **A global renderer** (`isGlobalRenderer` true). Vanilla draws a tile entity only from the
  chunk sections the camera can see, then frustum-tests its box; a stay fifty blocks long would
  vanish whenever its pylon top went behind something or out of view. A global renderer is
  frustum-tested on its box alone.
* **The render box is the whole cable** (`BridgeCableLayout.getBounds`), never the infinite box.
  Until the first layout is worked out, a rough box over the end blocks, sag and a 64-block drop
  for hangers stands in.
* **Render distance** is the view distance (or the long-range 128 blocks, if that is further)
  plus the cable's own reach from its anchor, capped by the performance mode, so a 120-block main
  cable is still drawn from its far end and a far bridge never shows its pylons without its stays.
  A whole bridge costs about 0.06 ms a frame (below), so drawing to the edge of the view is cheap.
* Prism sides follow thickness: 6 at 2 px, 8 at 4 px, 12 above. A plain stay is one segment
  (nothing along it to show); a striped one is sampled twice a block; a hanging cable once a
  block of plan length (8 to 256 segments, doubled with stripes).

`/csm renderpass skip bridgeCable` removes the pass; `bridgeCablePerFrame` draws it without the
display list, for A/B measurement in one session.

### What it costs

Measured in the dev client on 2026-10-30, in the "Bridge Cable Test" world: a cable-stayed bridge
(two H-pylons 50 apart, 64 stays of 2 to 8 px, 13 to 57 blocks, half of them in other colours,
thicknesses, stripes or LED strips), a self-anchored suspension bridge (two 40-block main spans
with sag 6 and seven hangers each, four side spans) and a tied arch with 18 hangers. The camera
stood broadside to the cable-stayed bridge with the suspension bridge behind it, about 70 anchors
drawing; render distance 16, no frame cap, no vsync, A/B/A/B in one session, 10 s a sample:

| Pass | renderWork, mean |
|---|---|
| Cables drawn (display lists) | 1.66 ms (1.66, 1.67, 1.65) |
| `/csm renderpass skip bridgeCable` | 1.60 ms (1.60, 1.59) |
| `bridgeCablePerFrame` (drawn without the lists) | 4.07 ms |

So the whole pass costs about 0.06 ms a frame for roughly 70 anchors and 80 cables in view, under
a microsecond an anchor, as estimated from the span wire's figures. The display list is what makes
it cheap: rebuilding the same tubes every frame costs 2.4 ms. The client also rebuilds each
layout every 40 ticks (spread across anchors) to look for a changed end or a new deck; the list
is recompiled only when that finds a change.

## Traps

* **One end draws.** The tool drops the reverse cable, but NBT does not: two anchors that each
  own a cable to the other keep both links, and the anchor at the higher position leaves its
  copy out of the layout. Its NBT still holds it, so `/blockdata` on that anchor still shows it.
* **Breaking the far anchor** takes the cable out of view, not out of the owner's NBT: put an
  anchor back and the cable returns. Remove it for good with `remove:[x,y,z]`, or by
  sneak-clicking the owner with the tool.
* **The single-cable keys act once.** They are never saved, so they do not reappear in the next
  `/blockdata` read-back, and running the same command twice does not trip `/blockdata`'s "nothing
  changed" check (the merged compound still holds the keys).
* **Offsets, not positions, are saved.** A target typed as absolute is turned into an offset as it
  is read, which is what keeps a pasted bridge's cables. The tile entity's `x,y,z` must be right
  when a target is read, which `/blockdata` and chunk loading both guarantee.
* **Light is sampled once, at the anchor.** A long stay running into a dark underside keeps the
  anchor's light all the way. Fine in daylight; at night the anchor's block light decides.
* **A model face names a texture variable** (`#plate`, `#chord`), never a path: 1.12 reads the
  face's string as a variable name either way, so a path there drew every anchor in the missing
  texture with nothing in the log.
* **A colour's shaded top facet can pass full brightness** on a white cable; `stitch` holds
  shaded colours to 1, because the buffer stores a byte and would wrap round to black.
* **Vec3d's squared length is named differently between mappings**; the layout writes it out, as
  the span wire geometry does.

## Leftovers

* Drawing to the view distance was measured on one test world's three bridges, not on a whole
  city's worth of cables; check the frame time once the city has several cable bridges.
* The sheath stripes wind one facet a segment, which on a long stay reads as a dashed line
  rather than a tight helix.
* The LED strip is always on: no redstone, no dusk switch.
* Light is one sample per anchor (see Traps).
* No GUI: the tool's presets and `/blockdata` cover it.
* The socket's boss is square to its face; the renderer's guide pipe makes up the angle. A model
  per angle was not worth it.

## Tests

`BridgeCableShapeTest`, `BridgeCableLinkTest` and `BridgeCableLayoutTest` (Roads' test tree):
the parabola's ends and its sag below the chord at any height difference, the rope's sag, hanger
stations (count, block centres, end clearance, the cap), segment counts; the NBT read exactly as
`/blockdata` parses a builder's text (absolute, relative and int-array targets, the single form
adding and replacing by target, the list form, `remove`, defaults, colour aliases, the round trip,
a moved anchor keeping its cables, the 16-cable cap); and the layout against a made-up world laid
out like the two test bridges (hangers to the deck and onto a deck anchor, none over a gap, the
key changing when a deck is built, a stay's two guide pipes, the bounds, stripes and LED).
