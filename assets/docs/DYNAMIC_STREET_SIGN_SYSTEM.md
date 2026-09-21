# Dynamic Street Sign System

The dynamic street sign is the intersection street-name blade: the green (or blue, brown, white)
plate that hangs off a signal mast arm or bolts to a wall, carrying a street name and, optionally,
a cardinal prefix, a street-type suffix, a city line, a block number, a route shield or civic
logo, and a directional arrow -- optionally with a second blade for another street hung directly
below the first. A single placeable block renders the whole thing through a
TileEntitySpecialRenderer (TESR); players edit it in-world through a four-tab GUI, and the
configuration is stored as a JSON document in the tile entity's NBT and synced to the server with
one update packet.

- Block registry name: `dynamic_street_sign`
- GUI ID: `20` (`BlockDynamicStreetSign.GUI_ID`)
- Subsystem package: `trafficaccessories/` (block, TE, renderer, GUI) and
  `trafficaccessories/streetsign/` (data model)

It is a deliberately **simpler sibling** of the [dynamic guide sign](DYNAMIC_GUIDE_SIGN_SYSTEM.md)
and shares a great deal with it — read that document first. What it does *not* have: multiple
panels, multiple rows, a polymorphic element list, exit tabs, an arrow-per-lane band, posts, or
external luminaires.

---

## Architecture

### Data model (`trafficaccessories/streetsign/`)

`StreetSignData` is a **flat, fixed-slot** document, not a tree. A street blade always has the
same anatomy, so there is nothing for a polymorphic element list to buy — and a flat document
means a flat GUI. It is pure data plus clamping validation with no Minecraft or rendering
dependency, so it is usable on both sides. Gson serializes it to one NBT string (`"signData"`).

| Class | Role | Key facts |
|---|---|---|
| `StreetSignData` | The whole sign | `VERSION = 1`. **Extends `StreetSignLegend`**: the inherited legend is the upper (or only) blade. Adds panel style (color, border 0–4, corners, mount, extruded frame, both-sides), lighting (`internalLight` + `SignLightMode`), the shared legend style (`textScale` 0.5–3.0, `affixVertical`), `minWidth` / `minHeight` floors (16–320 / 8–64 sign px), and `lowerBlade` -- a nested `StreetSignLegend` for a second blade, null for one blade. `fromJson` is defensive — null/empty/malformed yields a fresh default and never throws, because it runs in the renderer and on the network path — and it repairs both legends (nulls, over-long strings), because Gson bypasses the setters. `copy()` is a JSON round trip. |
| `StreetSignLegend` | What one blade says | `prefix`, `streetName`, `suffix`, `cityText`, the block-number slot, the emblem slot and the arrow slot. String setters clamp to per-field length caps. |
| `StreetSignMount` | How the blade hangs | `HANGING` (suspended below two hangers that reach half a block above, panel centered in the block's depth), `HANGING_BRACKET` (the same panel, hung off a horizontal support beam carried on a single centre drop) or `FLAT` (back against the block behind, like a guide sign). `canBeDoubleSided()` is true for either hanging style. Ask `isHanging()` rather than comparing against a constant -- the second hanging style had to be appended after `FLAT` because ordinals are serialized. |
| `StreetSignSlotPosition` | Where an optional slot sits | `NONE` / `LEFT` / `RIGHT`, used independently by the block number, the emblem, and the arrow. |
| `StreetSignVerticalPos` | Vertical placement | `TOP` / `MIDDLE` / `BOTTOM`. Used by the block number (real blades put it against an edge far more often than centered) and by `affixVertical`, which aligns the prefix and suffix against the street name's cap line, center, or baseline. |
| `StreetSignEmblemKind` | What the emblem slot holds | `NONE` / `SHIELD` (a `GuideSignShieldType` with its route number drawn over it) / `LOGO` (a `StreetSignLogoType` cell). |
| `StreetSignLogoType` | Civic logos | 16 generic municipal motifs in the shared atlas, rows 11–12 (see below). |
| `StreetSignTemplates` | Preset configurations | 6 cycling presets: Standard Green Blade, Blue Blade, Illuminated Blade, Numbered Cross Street, Historic District, Flat Wall Blade. Each `get(index)` returns a fresh instance. |

**Reused wholesale from the guide sign's model:** `GuideSignColor` (7 FHWA colors),
`CornerStyle`, `SignLightMode`, `GuideSignArrowType` (10 arrows), `GuideSignShieldType` (67
markers), `GuideSignAtlas`, and `GuideSignFontRenderer` (the FHWA legend font). One atlas, one
font, one visual language.

**Why the legend is a superclass.** Gson writes a superclass's fields at the top level under their
own names, so moving the legend fields out of `StreetSignData` into `StreetSignLegend` left the
document's keys exactly where they were: an old save loads as the upper blade with no second
blade, and a sign without a second blade writes back the same keys and values (the key *order*
changes, which JSON does not care about). `lowerBlade` is null unless switched on, and Gson omits
a null field, so it adds nothing to a single-blade document. `StreetSignDataTest` pins all of
this against a literal old-format payload. Never rename a legend field -- its name is the key.

**Every enum here is append-only** — ordinals are serialized. Each exposes `next()` for GUI
cycling and a clamping `fromOrdinal` that returns a safe default for out-of-range values, so a
hand-edited or truncated document can never produce an invalid enum.

### Block / TileEntity / Renderer / Network

| Component | File | Notes |
|---|---|---|
| Block | `trafficaccessories/BlockDynamicStreetSign.java` | Extends `AbstractBlockRotatableNSEW` + `ICsmTileEntityProvider`. Material IRON, SoundType METAL, `CUTOUT_MIPPED`, not opaque/full cube. `onBlockActivated` opens GUI 20. **The AABB follows the mount** (see below). `getBlockConnectsRedstone` is true and `neighborChanged` / `onBlockPlacedBy` cache the power state on the TE for `SignLightMode.REDSTONE`. |
| TileEntity | `trafficaccessories/TileEntityDynamicStreetSign.java` | Extends `AbstractTileEntity`. JSON in NBT key `"signData"`, powered flag in `"lightPowered"`. Lazy `cachedData`, invalidated on any write. `getRenderBoundingBox` covers ±10 blocks horizontally and −3/+4 vertically (−9 below with a second blade, which can end about 8.6 blocks down) — a blade may be forced to 20 blocks wide, and a hanging one carries hangers above the block. `getMaxRenderDistanceSquared` is 96². |
| Renderer | `trafficaccessories/TileEntityDynamicStreetSignRenderer.java` | Direct immediate-mode rendering. Same conventions as the guide sign TESR. |
| Update packet | `codeutils/packets/DynamicStreetSignUpdatePacket.java` | `BlockPos` (as long) + `signDataJson` (UTF-8). |
| Packet handler | `codeutils/packets/DynamicStreetSignUpdateHandler.java` | `Side.SERVER`. Reach check via `CsmPacketUtils.canPlayerReach`, then a `MAX_JSON_LENGTH = 4096` cap, then `setSignDataJson`. |
| GUI | `trafficaccessories/DynamicStreetSignGui.java` | 4-tab `GuiScreen`. |

Registration touch-points: GUI case in `CsmGuiHandler.java` (keyed off `BlockDynamicStreetSign.GUI_ID`);
packet in `Csm.java`; TESR in `CsmClientProxy.java`; block in `CsmTabTrafficAccessories.java`;
lang keys in `en_us`/`de_de`/`es_es`/`sv_se`.

### Mount-aware hitbox

A hanging mount and a flat one put the panel in completely different places, so one hitbox
cannot serve both — using a face-hugging slab for a hanging blade left it untargetable from one
side.

- **FLAT** → a `1.5/16` slab on the face the TESR draws on. The mapping is derived from the
  TESR's rotation and matches the guide sign's: FACING SOUTH/WEST/NORTH/EAST → south/east/north/
  west face.
- **Either hanging mount** → a slab through the middle of the block (`6.5/16 … 9.5/16` on
  whichever axis the panel's thickness runs along), full height so the hangers are clickable
  too. The two hanging styles differ only in the hardware above the blade, so they share a box.

A second blade does not grow either box. Both already span the block's full height, and the part
of a stacked pair outside the block is handled the way an oversized single blade always was:
drawn, not clickable. A box past `0..1` would buy nothing -- a ray only tests the blocks whose
cells it enters -- and would hand the sign a collision box in its neighbours' cells.

`getBlockBoundingBox` reads the mount off the tile entity. It is called during chunk load
**before** tile entities are attached, so the lookup falls back to the default mount when there
is no TE — never assume one is there.

---

## Rendering

All renderer constants are in **sign pixels** (the renderer scales by 1/16, so 16 units = one
block). The four inherited invariants, all of which the guide sign learned the hard way:

1. **Mirrored pixel space.** After the 1/16 scale the renderer applies `translate(16,0,0);
   scale(-1,1,1)` so `+X` in all layout math is the READER's right. Overlays then draw
   *unflipped*: text advances toward `+X` and atlas quads map `u0` to their left edge. Never
   re-add a per-overlay flip.
2. **The viewer is at smaller Z, and every depth comes from the Z layer table.** No offset is
   written at a call site. The gaps in that table are a correctness constraint, not a style
   choice: the guide sign's 0.05 sign-px gap between the border plate and its back slab
   z-fights into a flickering outline once the sign is more than a few blocks away — which is
   exactly the range a street blade is normally read from. Every gap here is at least 0.15
   sign px (~0.01 blocks).

   | Layer | Offset from `faceZ` |
   |---|---|
   | Extruded frame (front) | `-1.00` |
   | Route number over a shield | `-0.85` |
   | Emblem / arrow atlas quad | `-0.70` |
   | Legend text | `-0.55` |
   | Colored face plate | `-0.36` … `-0.11` |
   | Border plate | `-0.18` … `+0.07` |
   | Core slab (front) | `+0.02` |

   The sleeve invariant is what bounds how far apart these can go: each painted plate's rear
   and side faces must land INSIDE the next layer back, or an illuminated blade glows along
   its edges. Front to back, the colored face is sleeved by the border plate and the border
   plate by the core slab.
3. **Lit faces must be sleeved.** `addBoxesToBufferLit` lights all six faces of a box alike, so
   a fullbright plate spanning the assembly's depth glows along its edges and its reverse. The
   painted plates are only `LIT_FACE_DEPTH = 0.25` deep and the ambient-lit core slab is drawn
   `BACK_SLEEVE_MARGIN` oversize, so it sleeves their side and rear faces.
4. **Bind white, don't disable texturing.** Untextured geometry binds a 1×1 white texture
   (`textures/blocks/white1px.png`) rather than calling `disableTexture2D`, because shaders
   ignore that global state. Re-bind it after every font or atlas pass.

### Layout

`computeLayout(data)` returns a `Layout` holding every measurement the draw pass and the GUI
preview's fit math need — computed once so the two can never disagree. With a second blade it is
the top blade's layout, and the lower blade's is on `Layout.lower`; `blades()` walks them top to
bottom, which is how every per-blade pass (core, face, legend, frame) is written.

| Constant | Value | Controls |
|---|---|---|
| `SIGN_DEPTH` | `1.5` | Thickness of the whole panel assembly. |
| `LIT_FACE_DEPTH` | `0.25` | Depth of the painted plates only. |
| `BORDER_INSET` | `1.0` | Multiplier: actual border thickness = `borderWidth × 1.0`. Deliberately larger than the guide sign's `0.4` -- that sign is several blocks tall, where 0.4 px reads as a proper border; a one-block-tall blade needs about a pixel before the border is visible at all. |
| `PAD_SIDE` / `PAD_TOP` / `PAD_BOTTOM` | `3.0` / `2.0` / `2.0` | Padding between the panel edge and the content. |
| `NAME_CAP_HEIGHT` | `6.5` | Street-name capital height at `textScale` 1.0. |
| `AFFIX_CAP_FRACTION` | `0.55` | Prefix and suffix cap height, as a fraction of the name's. |
| `CITY_CAP_FRACTION` / `BLOCK_CAP_FRACTION` | `0.42` / `0.50` | City line and block-number cap heights, same basis. |
| `TEXT_VISUAL_FACTOR` | `1.32` | Line height over cap height (room for descenders). |
| `AFFIX_GAP` / `SLOT_GAP` / `CITY_GAP` | `1.4` / `2.5` / `1.2` | Gaps around the affixes, beside a side slot, and under the name. |
| `EMBLEM_SIZE` / `ARROW_SIZE` | `11.0` / `9.0` | Rendered size of the emblem and arrow. |
| `GuideSignShieldType` route text | per shield | Route-number cap height, maximum width and centre over a shield emblem, shared with the guide sign (see its Rendering Proportions): a state marker's number sits where the real marker sets it, not always in the middle. |
| `CORNER_STEP` | `0.6` | Chamfer per outer corner for ROUND corners. |
| `HANG_DROP` | `5.5` | How far below the block's top edge a hanging blade's top rail sits. Shared by both hanging styles, so switching between them swaps the hardware without moving the panel. |
| `HANGER_REACH_ABOVE` | `8.0` | How far above its own block the hanger run reaches, so the clamp lands on the underside of a top slab. |
| `BEAM_GAP` / `BEAM_THICKNESS` | `2.6` / `1.4` | Bracket mount: clearance from the blade's top edge to the underside of the support beam, and the beam's square section. |
| `BEAM_OVERHANG` | `3.2` | How far the support beam runs past each end of the blade. |
| `DROP_POST_WIDTH` / `DROP_SADDLE_HEIGHT` | `1.3` / `1.5` | The single centre drop's section, and the saddle casting where it meets the beam. |
| `HANGER_INSET_FRACTION` / `BRACKET_LINK_INSET_FRACTION` | `0.22` / `0.12` | How far in from each end the two hangers, and the bracket mount's two links, grip the blade. |

`textScale` multiplies every legend metric and every gap, so the blade stays proportionate at
any size.

**Horizontal.** Content is centered on the block's X center. Side slots run **outward from the
text column in a fixed order — block number innermost, then emblem, then arrow** — which is how
a real blade reads at both ends:

```
[arrow] [emblem] [block] | prefix  NAME  suffix | [block] [emblem] [arrow]
                         |      city line       |
```

The text column is as wide as the wider of the name group and the city line, and both are
centered within it.

**Vertical.** The name is centered in the content band; the city line sits under it. The emblem
and arrow are centered on the band.

Every *small* legend element — the prefix, the suffix, the block number — is placed by
`alignToNameCap`, against the **street name's cap box** and never against the content band. TOP
puts its cap top on the name's cap line, BOTTOM puts its baseline on the name's baseline, MIDDLE
centers it on the name. `affixVertical` drives the prefix and suffix (TOP gives the raised
`W … RD` look, BOTTOM the equally common flush style); `blockVertical` drives the block number.

Aligning to the band instead is the obvious-looking mistake and it is wrong: the band is sized by
the tallest thing in the row, normally the emblem at half again the name's cap height, so a block
number set to BOTTOM sat visibly *below* the name's baseline rather than flush with it.

**Sizing.** The panel auto-sizes to its content, then takes the max with `minWidth` / `minHeight`.
Surplus from a floor leaves the content centered.

A long name or a wide `minWidth` makes a panel **wider than the block it is placed in**, and it
simply reaches into its neighbours: two blades four blocks apart with five-block panels overlap,
exactly coplanar, and which one covers a given pixel is decided by depth order, so it changes with
the camera and reads as one blade's legend disappearing behind the other. Nothing can resolve a tie
between two surfaces in the same plane -- space wide blades far enough apart that their panels do
not meet.

### Stacked blades

A road that changes name at a junction gets two blades on one block: the top one lettered from
the sign's own legend, the lower one from `lowerBlade`. Two blocks placed one above the other were
never going to stack cleanly -- each sized itself from its own content, so different names gave
different panels, and a hanging blade's hangers run half a block up through whatever is above it.
One block owning both is what fixes both problems.

- **Sizing.** `measureBlade` sizes each blade from its own content; `computeLayout` then gives
  both the **wider** width and the **taller** height, and `placeBlade` centers each legend in its
  panel. Everything else -- colour, border, corners, frame, lighting, text size, affix alignment,
  the size floors -- is one setting for the pair, so the blades cannot drift apart.
- **Gap.** `BLADE_GAP = 3.0` sign px of clear space, outer edge to outer edge (border and frame
  included), about a fifth of a blade: enough to read as two blades on one assembly, and to show
  the links between them.
- **Hanging.** The top blade stays exactly where a single blade hangs, and the hangers, bracket,
  clamp height and `HANGER_REACH_ABOVE` are untouched -- they grip the top blade only. The lower
  blade hangs from the top one on **links**: a shoe on its top edge, a clip on the upper blade's
  bottom edge and a rod across the gap, at the same x as the hardware above (`HANGER_INSET_FRACTION`
  or `BRACKET_LINK_INSET_FRACTION`), so the load path reads straight down. A framed pair also runs
  a short feed cable across the gap at the same end as the main feed.
- **Flat.** The pair centers on the block **as a whole**, so the top blade rises by half of what
  the lower one adds. No links: both are bolted to the wall behind.
- **Double-sided.** Unchanged: the back pass rotates the whole assembly, both blades, 180°.
- **Display lists.** The core, face and frame lists each hold both blades. Any edit already sets
  the tile entity's dirty flag; `structureKey` also carries a bit for the stack (bit 33) so a list
  compiled for one shape can never replay for the other.

`DynamicStreetSignLayoutTest` covers the shared size in either order, the hanging top blade not
moving, the gap, the flat pair's centering and the preview box.

### Mount geometry

| | FLAT | HANGING | HANGING_BRACKET |
|---|---|---|---|
| Panel front (`faceZ`) | `16 − SIGN_DEPTH` = 14.5 | `8 − SIGN_DEPTH/2` = 7.25 | same as HANGING |
| Core slab rear | `16.05` | `16 − faceZ − BACK_SLEEVE_LIP` = 8.70 | same as HANGING |
| Vertical placement | Centered on the block (`signTop = 8 + h/2`) | Hangs from `signTop = 16 − HANG_DROP` | same as HANGING |
| Back face | Never (a block is behind it) | When `doubleSided` | When `doubleSided` |
| Hangers | None | Two: shoe + rod + clamp, reaching half a block above | Two short links to a beam, then one centre drop to the same height |
| Power feed cable | None | When framed: one, from the end casting to the same height | When framed: one, from the end casting into the beam |

**The back face is the same draw inside a 180° Y rotation about the block center.** That
rotation is orientation-preserving, so combined with the outer mirror the legend reads correctly
— not mirrored — to a viewer standing behind the blade. It also lands a face drawn at `z = f` at
`z = 16 − f`, which is exactly where the hanging mount's rear plates belong. The core slab is
drawn once, outside the rotation.

**The arrow is the one thing the rotation must not carry unchanged.** Text and emblems are paint
on the panel and rotate with it, but an arrow names a direction in the *world*, and the rotation
swaps the reader's left and right — so an arrow drawn identically on both faces would send a
viewer behind the blade to the opposite street. `renderFace` therefore takes a `backFace` flag and
`renderArrow` mirrors the arrow quad's U coordinates when it is set, which undoes exactly that
swap and nothing else (the symmetric arrows — UP, DOWN, LEFT_RIGHT, UP_LEFT_RIGHT — are
unaffected). The arrow is drawn after `glCallList`, outside the geometry-only face list, so
nothing about the display-list cache key changes.

### Extruded frame

`extrudedFrame` draws the dark aluminum extrusion an internally-illuminated blade is built in:
top and bottom rails welded to cast end pieces, standing `FRAME_PROUD` in front of the painted
face so the panel reads as recessed inside it. The rails and end pieces sit **outside** the
painted panel, biting into it by only `FRAME_SEAM` -- enough to close the seam against the core
slab's oversize margin, and no more, because a deeper bite swallows the white legend border that
on a real extruded blade stays visible inside the frame. On a double-sided blade the frame
reaches symmetrically past **both** faces, or the reverse renders unframed. It is structural metal, so it draws at **ambient**
light and does not glow along with the face.

### Hangers

Two assemblies inset `HANGER_INSET_FRACTION` from each end of the blade, each a shoe on the
blade's top edge, a rod, and a clamp. Members overlap by `JOINT_OVERLAP` so no two faces are ever
coplanar. On a blade too narrow for two, they collapse to a single hanger down the middle.
Ambient-lit, like the frame.

The run reaches `HANGER_REACH_ABOVE = 8` sign px **past the top of its own block** — half a block
up — and the clamp grips at the top of that run rather than at the block boundary, so whatever
the run reaches is what the blade appears to hang from.

Stopping at the boundary (`y = 16`) was only ever right for the things whose underside *is* that
boundary: a full block, or a bottom slab. Anything mounted higher in the space above — a top
slab, an upper step — left the blade hanging from nothing across a visible gap. Reaching `y = 24`
covers those and costs nothing in the cases that already worked, because there the extra length
is buried inside the block or slab above and never seen. The tile entity's render bounding box
already extends four blocks up, so no culling change is needed; the block's own hitbox
deliberately stays within `0..1` (the part of the hanger above the block is not clickable, the
same way the guide sign's posts are not).

### Bracket hanger

`HANGING_BRACKET` hangs the same panel a different way: the blade swings on two **short** links
up to a horizontal support beam, and the beam is carried on a **single** centre drop — one
attachment point on the mast arm instead of two. It is the arrangement you get whenever the blade
is not directly under a convenient stretch of arm.

The drop's clamp reaches the same `HANGER_REACH_ABOVE` height the two hangers do, so switching
between the two hanging styles swaps the hardware without moving the blade or changing what it
appears to hang from. Both mounts share one `bladeTop` / `hangerCenters` / shoe / clamp helper
set for exactly that reason.

They do **not** share an inset. `hangerCenters` takes the fraction as a parameter, and the
bracket mount grips at `BRACKET_LINK_INSET_FRACTION = 0.12` against the hangers' `0.22`: the
links here are short fittings rather than a full-height run, so at the hangers' inset they bunch
in toward the centre drop instead of reading as carrying the blade.

Three details are what make it read as an assembly rather than a plus sign:

- The beam **overhangs** the blade by `BEAM_OVERHANG` at each end and is capped there. Flush with
  the blade, it reads as one more rail of the panel's own frame.
- Each link's collar and the centre saddle **wrap** the beam — they are drawn a hair oversize in
  Y and Z rather than butting into it — so the beam reads as continuous under them instead of
  broken into segments.
- The centre drop's post can be squeezed to almost nothing by a deeply bordered *and* framed
  blade, which pushes the beam most of the way to the top of the run on its own. `computeLayout`
  therefore takes the whole hardware stack as a floor on `assemblyTop`; without it the post's box
  inverts and renders inside out. The renderer reads its clamp height off `assemblyTop` rather
  than recomputing it, so the guard cannot be bypassed.

### Traffic poles

A traffic pole beside a blade decides whether to sprout a mount stub toward it, and the answer
depends on the mount. A hanging blade -- either style -- already carries its own hardware, so a
pole stub would put a second, contradictory mount on the same sign: `isHanging()` blades are
ignored. A flat blade is bolted to whatever is behind it, which is exactly what a mount stub
depicts, so it is left mountable.

`AbstractBlockTrafficPole.IGNORE_BLOCK` cannot express that: it is type-based, so it can only
say always or never. The decision therefore lives in the block, behind Core's
`ICsmTrafficPoleStateIgnored`: `BlockDynamicStreetSign.isIgnoredForTrafficPole` returns whether
the blade's mount is hanging, and the pole calls it from `isMountableAdjacent` as a third filter
alongside the type list and the config-driven registry-name set. It is checked per placed block
against its tile entity, and both pole types (straight and diagonal) funnel through that one
call. With no tile entity attached yet -- it runs during chunk load -- it falls back to the same
default mount the sign's own hitbox uses, so the two cannot disagree.

Changing the mount has to reach the neighbours, and the ordinary sync does not do it:
`markDirtySync` explicitly does not re-render, and the pole's decision is evaluated in *its*
`getActualState` during a chunk rebuild. `TileEntityDynamicStreetSign.refreshNeighborsOnMountChange`
covers both sides -- a render update around the block on the client, a neighbour notification on
the server -- and fires only when the mount actually changed.

### Power feed cable

A framed **hanging** blade gets a thin cable from the top of its extruded frame's end casting up
to the same height the hangers reach, so it disappears into whatever the blade hangs from rather
than stopping in mid-air. `CABLE_THICKNESS = 0.65` sign px matches the wire radius the sensor
blocks' OBJ models use (0.018–0.022 blocks), so every cable in the mod reads at one weight, and
it is drawn a touch darker than the hangers so it reads as jacketed cable rather than bracketry.

Drawn only when the frame is on **and** the mount is hanging — the only configuration with
anything to feed, since the frame is the housing of an internally-lit sign and a flat blade's
conduit runs inside whatever it is bolted to. It leaves from beyond the blade's end rather than
from the top rail, both because that is where the real ones are dressed and because there it is
never hidden behind the panel.

Where it terminates follows the mount. On `HANGING` it runs the full height the hangers reach, so
it disappears into the same thing they grip. On `HANGING_BRACKET` the support beam already spans
that x — it overhangs the blade further than the cable leaves — so the cable ends **inside the
beam**, which is where the real ones are dressed anyway; run to full height there it would pass
visibly through the beam.

The run bellies `CABLE_BOW` away from the blade at mid-height and returns to the same x at both
ends, which is what a slack cable between two fixed points does; dead straight, it reads as a
rod. It is stepped in `CABLE_SEGMENTS` boxes, each spanning both of its endpoints' x so
consecutive boxes overlap along the bow and the run reads as one cable rather than a ladder.
Ambient-lit like the rest of the metalwork, so it stays dark against a glowing blade at night.

### Lighting

`internalLight` selects the hardware (there is only one kind — the face itself lights) and
`SignLightMode` selects when it is energized. `resolveLightOn` runs once per render:

- `ON` → always. `OFF` / no internal light → never.
- `REDSTONE` → the TE's cached, synced `isPowered()`, so the renderer never polls neighbors.
- `NIGHT` → a photocell: `getLightFor(SKY, pos) − calculateSkylightSubtracted(1.0f)` at or below
  `LIGHT_NIGHT_SKY_THRESHOLD = 8`. **Use `calculateSkylightSubtracted`, never
  `getSkylightSubtracted`** — the cached field behind the getter is written once in the
  `WorldClient` constructor and never updated, so client-side it forever reports the sky as it
  was when the player joined.

When lit, the painted face and the legend go fullbright (240); the core slab, frame, and hangers
keep the block's real light, so a night scene still reads as night around a glowing blade. The
sign emits **no** Minecraft block light — this is a rendering effect only, so there is no
chunk-relight cost when a photocell blade switches at dusk.

**Where the blade's brightness comes from.** One `getCombinedLight` at the blade's own block,
re-read every frame and baked into each vertex through the `BLOCK` vertex format -- per vertex
rather than through `OpenGlHelper.setLightmapTextureCoords`, because OptiFine's shader programs
read the lightmap from the vertex attribute and ignore that global state. The whole panel
therefore takes the light of the block the sign is in, however far past it the panel reaches, and
`combinedLight` is part of the display-list key (`structureKey`), so a light change recompiles
rather than replaying the old brightness. A blade that looks a level or two too dark is
**the client's own light data** for that block, not the cache: it survives edits (which discard
the lists and re-bake) and clears on a relight or a client restart. That is what a blade
`/setblock`-ed and then built over looked like once -- dark until the client restarted, at the
same brightness through several edits.

### LOD

Past `LOD_FULL_DETAIL_DIST_SQ` (48 blocks) the renderer draws the core, the border, and the
painted face and stops. The legend is unreadable at that range and the font and atlas passes are
the expensive part.

---

## Texture Atlas

The blade shares the guide sign's atlas,
`assets/csm/textures/blocks/trafficaccessories/guidesign/sign_atlas.png` (512×1024, 64×64 cells,
8 columns). Route shields and arrows come straight from `GuideSignShieldType` /
`GuideSignArrowType`; civic logos occupy **rows 11 and 12**, addressed by coordinate through
`GuideSignAtlas.getCellUV(col, row)` (made public for exactly this).

Unlike a shield cell, each logo cell is **self-contained artwork with its own background plate**,
so it reads on a green, blue, brown, or white blade, and the renderer draws no text over it.

| Row 11 | Row 12 |
|---|---|
| Star Seal, Laurel Seal, Civic Shield, Fleur-de-lis, Oak Leaf, Pine Tree, City Skyline, Bridge | Mountain, River, Sunrise, Compass Rose, Capitol Dome, Transit, Airport, Hospital |

All sixteen are generic civic motifs drawn programmatically by
`GuideSignAtlasTool.drawLogos(...)` — no real municipality's seal or agency mark, which would be
someone's trademark. Regenerate with the `Generate Sign Atlas` IntelliJ run config, or:

```bash
mvn -f dev-env-utils/pom.xml compile exec:java \
  -Dexec.mainClass=com.micatechnologies.minecraft.csm.tools.GuideSignAtlasTool \
  -Dexec.args="<repo-root>"
```

### Adding a logo

1. Append an entry to `StreetSignLogoType` with the next free `(col, row)` — row 12 is full at
   col 7, so the next one starts at row 13 col 0. **Append only; ordinals are serialized.**
2. Add a `draw<Name>(g, col, row)` helper in `GuideSignAtlasTool` and a call in `drawLogos`.
   `logoPlate(g, col, row, background, circle)` fills the plate and returns the padded drawing
   rectangle; `makeStar(...)` is there for seals.
3. Regenerate the atlas. The renderer and GUI pick it up automatically — logos are looked up by
   atlas coordinate and the GUI cycles `StreetSignLogoType.values()`.

---

## Network / Edit Path

### GUI (`DynamicStreetSignGui`)

Four tabs, using the guide sign GUI's scrolling-viewport pattern: content between the tab strip
and the Save/Cancel row scrolls with the mouse wheel, off-viewport widgets are hidden
(`visible = false`, `setVisible(false)`) rather than moved so they cannot intercept clicks or
bleed through the fixed strips, and a scrollbar indicator is drawn when content overflows.

**A widget counts as on-viewport only when it fits there entirely** (`CsmScrollViewport.isRowVisible`,
shared with the guide sign editor, and the rule the preview's scissor box and every scrolled label
follow too). Merely overlapping is not enough, and the difference is not cosmetic: vanilla's
`GuiScreen.mouseClicked` walks the entire button list without stopping at the first hit, so a
content button peeking out from under the Save button was pressed by the same click that pressed
Save -- invisible, because Save is drawn over it. On a 1904x1041 window (GUI scale 4, so 476x261
scaled) the Save row lands exactly on the Text tab's emblem-kind button, and every save silently
cycled the blade's emblem. Which row it catches depends on the window height, which is why it
looked like nothing at all at most sizes.

- **Text**: prefix / street name / suffix on one line in blade order, the optional city line,
  text size, the prefix/suffix alignment, the block number and its side and height, the emblem
  (kind, side, and either a shield chooser with a route field or a logo chooser), and the arrow.
  Each label row's Y is recorded while the tab is built rather than recomputed from the same
  increments in the draw pass — doing the latter meant every inserted row silently desynced the
  labels from the widgets they describe.
- **Blade 2**: a `Second Blade: ON/OFF` switch (off by default), and while it is on the Text
  tab's legend rows for the lower blade -- everything except text size and prefix/suffix
  alignment, which the pair shares. The tabs are one builder (`buildLegendRows`) pointed at a
  different `StreetSignLegend`; every legend control acts on `activeLegend()`, never on the
  document directly. Switching the blade off parks its legend in the screen, so switching it back
  on in the same session restores what was typed; only what is on when Save is pressed is sent.
- **Style**: color, corners, mount, both-sides, extruded frame, border, min width, min height,
  internal illumination and its mode, template cycle, and copy/paste.
- **Preview**: a live WYSIWYG render at the top — drawn by the TESR's own `renderForGui`,
  fullbright, scaled to fit and scissor-clipped — followed by a text summary. The GUI transform
  flips Y because sign pixel space is +Y up, and flips Z because GUI ortho treats larger z as
  closer while the readable face sits at *smaller* z than the core. It fits to
  `computePreviewBox(data)`, which covers the hangers as well as the panel.

`actionPerformed` calls `syncFields()` **before** acting, because a press can follow typing and
most controls rebuild the tab (which drops the fields). Most controls then call `initGui()`
rather than patching individual buttons, since choosing an emblem kind adds its chooser and a
flat mount disables the back-face toggle.

**Copy/paste** uses a process-static `clipboardJson`, letting a player duplicate a blade across
blocks within a session. Save sends one `DynamicStreetSignUpdatePacket`; Cancel closes without
sending.

### Server apply

`DynamicStreetSignUpdateHandler` runs `Side.SERVER`, schedules work on the server thread, and:

1. Rejects the packet if the player cannot reach the position (`CsmPacketUtils.canPlayerReach`).
2. Rejects a document over `MAX_JSON_LENGTH = 4096` — the flat document is a few hundred bytes in
   practice, so this is generous while still bounding the NBT payload.
3. Applies it via `setSignDataJson`, which stores the JSON, invalidates the cache, and
   `markDirtySync`s so the change reaches clients.

---

## Survival

Nothing to do. Fabricator costs derive at runtime from the block's creative tab and base class,
so the blade is craftable at the traffic accessories cost like every other block in that tab. See
[SURVIVAL_AND_RECIPES.md](SURVIVAL_AND_RECIPES.md).

## Assets

- `blockstates/dynamic_street_sign.json` — Forge format; the placed block is fully transparent
  (the TESR draws everything) and only the `inventory` variant carries a model.
- `models/block/dynamic_street_sign.json` — a flat plate carrying the inventory texture.
- `textures/blocks/trafficaccessories/dynamic_street_sign.png` — generated by
  `dev-env-utils/scripts/gen_dynamic_street_sign_texture.py` (a green `MAIN ST` blade on two
  hangers). It never appears on the placed block; it exists so the item in the creative tab and
  in a hand looks like what it places.

## The post-top blades

`signpoststreetnamesignmount1` and `signpoststreetnamesignmount2` are the street name blade
carried across the top of a sign post -- the green plate over a STOP sign at an intersection,
with a second blade crossing it for the other street. They were two blank plates nothing could
be written on; they are now **this sign's document on a different bracket**, and share its
editor, its update packet and its renderer rather than reimplementing any of the three.

| | |
|---|---|
| `BlockStreetNameBlade` | one class, two instances, in `trafficsigns/` |
| `TileEntityStreetNameBlade` + two subclasses | `TileEntityDynamicStreetSign` with its mount held to the block's bracket |
| `StreetSignMount.POST_TOP_CLAMP` / `POST_TOP_CROSS` | the flat clamp plate and the collar |
| GUI id 29 | `DynamicStreetSignGui`, with the mount shown and not offered |

**They are `AbstractBlockSign`s, not a second kind of dynamic sign.** That is the whole reason
they live in `trafficsigns/`: they stack on the mod's sign posts and take the eight facings, the
extension post onto a slab, the setback in front of a signal arm and the back-to-back pairing
exactly as the 472 signs beside them do, with no code of their own for any of it.

**Blade 2 crosses rather than stacks.** Under a post-top mount the editor's existing second
blade sits just above the first and turns away from it, which is how a post-top pair is
actually built. `lowerBlade` is the crossing blade here. It cannot be more geometry in the
same pass -- a display list is compiled once and replayed, and the two blades differ by a
matrix, not by vertices -- so `renderSign` draws the assembly twice, with a key bit that
keeps the two passes' cached lists apart.

**Each blade points where it likes.** `StreetSignLegend.bladeTurn` is eighths of a turn from
the block's facing, so a pair can cross at any of the eight angles rather than only at a
right angle -- a fork or a skewed junction is signed the way it really runs. It lives on the
legend, so each blade has its own; a newly added second blade starts at a quarter turn so it
crosses rather than hiding above the first. The turn is applied about the POST, which is the
one part of the assembly that must not come round with them: the post is the block's own
model and stays where the sign pole below it is.

The field is boxed and left null at zero so Gson omits it, and a document that never turned
a blade writes back exactly the keys it was saved with -- `StreetSignDataTest` holds that
invariant for every old document, and this is the same trick `lowerBlade` uses. The editor
shows the control only on a post-top mount, in the shared legend rows, which is what gives
each blade its own without a second control being written.

**The mount is the block's, not the player's.** `cycleMountType` skips both post-top brackets,
so the mast-arm sign never offers hardware it has no post to grip, and the blade's tile entity
coerces the document's mount on the way out so a document pasted from a hanging blade still
comes back mounted on its post.

### Three things that had to move, and why

- **The assembly is scaled, not re-measured.** The layout is sized for a blade hung over a road,
  most of a block deep; on a sign post beside a one-block STOP sign that is enormous. A real
  blade is six inches against a thirty-inch sign, so the whole assembly is scaled to
  `POST_TOP_SCALE` about the post top. Re-tuning the caps, pads, insets and floors would have
  had to keep a dozen constants in proportion; one transform cannot get that wrong.
- **The panel clears the post, and its two faces straddle it.** Centred on the post's axis,
  the post's own bars stand in front of the legend and read as a bar painted through the
  street's name, so the panel is offset clear of it. The offset is written in the units it
  ENDS UP in and divided by the scale, because the assembly is shrunk about the post top
  afterwards; set in plain model units it was shrunk with everything else and put the panel
  back inside the post.

  The reverse face then has to go on the **other** side of the post, which is what the half
  turn's axis decides. A hanging blade is centred in the block's depth, so its panel, the
  block's centre and the arm it hangs from are all the same plane and the question never
  came up; turning a post-top blade about its own panel leaves both faces on one side with
  the post behind the pair, which is not how a sign sits on a pole here. It turns about the
  POST, which puts one face each side of it -- the geometry `SignShift.BACKTOBACK` gives a
  pair of road signs sharing one post, which is what these are. The post between the faces
  is then what tells them apart, so the offset only has to clear it, not stand off from it.
- **The double-sided back face turns about the panel, not the block.** They are the same axis for
  a hanging blade, which is centred in the block's depth, and that is how it was written. A
  post-top blade is not, and turning its back face about the block's centre threw it most of a
  block clear of its front, leaving a loose white plate hanging beside the sign.

### The renderer reads two different facing properties

`TileEntityDynamicStreetSignRenderer` now draws two kinds of block, and they do not carry the
same facing: the dynamic street sign has the vanilla four-way `BlockHorizontal.FACING`, the
blades are road signs and face eight ways. Reading one and assuming the other throws out of
`getValue` -- it crashed the first blade ever looked at. `facingRotation` asks which the block
has; the two agree on the angles they share.

