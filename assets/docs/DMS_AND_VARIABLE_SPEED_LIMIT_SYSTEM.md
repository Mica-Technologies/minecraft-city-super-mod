# DMS and Variable Speed Limit Sign System

This system adds five TESR-rendered electronic-display sign blocks to the Traffic
Accessories subsystem. All of them build on the infrastructure originally created for the
Portable Changeable Message Sign (PCMS, `portable_message_sign`): the same tile-entity
shape, GUI conventions, server-side update packet pattern, and `RenderHelper` box-drawing
helpers.

| Block | Registry name | GUI ID | Tile entity | Renderer |
|---|---|---|---|---|
| Overhead Gantry DMS | `overhead_message_sign` | 10 | `TileEntityOverheadMessageSign` | `TileEntityOverheadMessageSignRenderer` |
| Portable Variable Speed Limit Trailer | `portable_speed_limit_sign` | 11 | `TileEntityVariableSpeedLimit` | `TileEntityPortableSpeedLimitRenderer` |
| Large Overhead Variable Speed Limit Sign | `overhead_speed_limit_sign` | 12 | `TileEntityOverheadSpeedLimit` | `TileEntityOverheadSpeedLimitRenderer` |
| School Zone Beacon Assembly | `school_zone_beacon` | 19 | `TileEntitySchoolZoneBeacon` | `TileEntitySchoolZoneBeaconRenderer` |
| Radar Speed Feedback Sign | `radar_speed_sign` | 21 | `TileEntityRadarSpeedSign` | `TileEntityRadarSpeedSignRenderer` |

Every block class lives in
`modules/roads/src/main/java/com/micatechnologies/minecraft/csm/trafficaccessories/`, extends
`AbstractBlockRotatableNSEW`, and implements `ICsmTileEntityProvider`. Each opens its GUI
from `onBlockActivated` via `player.openGui(Csm.instance, <id>, ...)`. All render with
`BlockRenderLayer.CUTOUT_MIPPED` and are non-opaque, non-full-cube.

Blocks 1-3 are purely visual and operator-configured, with no Forge-Energy or redstone
connection. Blocks 4 and 5 are not: the school zone beacon answers to the **world clock**, and
the radar sign answers to **what is moving in front of it** and emits a redstone signal while
that is over the posted speed.

---

## Block 1: Overhead Gantry DMS (`overhead_message_sign`)

A large overhead electronic message sign for highway gantries — the kind of three-line
amber-LED dynamic message sign mounted over freeway lanes. It is a full-block-collision
block (`getBlockBoundingBox` is `0,0,0 → 1,1,1`) whose TESR draws a wide housing centered
on the block.

It **reuses the PCMS tile entity wholesale**: `TileEntityOverheadMessageSign` extends
`TileEntityPortableMessageSign` and only overrides `getRenderBoundingBox()` to expand the
render volume (roughly 9 blocks wide × 5 tall × 5 deep around the block, since the housing
is far larger than one block). It inherits the PCMS page model — up to 8 pages, each three
lines of up to 16 characters, plus a cycle speed in seconds. The trailer/flasher/color/angle
fields exist on the parent TE but are unused here aside from housing color.

### Rendering (`TileEntityOverheadMessageSignRenderer`)

Dimensions are in model units (16 = 1 block), drawn after a `scale(0.0625, ...)`:

- Housing: `SIGN_WIDTH = 144`, `SIGN_HEIGHT = 64.02`, `SIGN_DEPTH = 20`, `SIGN_FRAME = 2.0`
  (a ~9 × 4 block face, one-and-a-quarter blocks deep). The housing is centered on the block
  (`CY = 8`) and runs from the front face back to the rear block edge (`BACK_Z = 16`).
- A lighter frame box is drawn slightly oversize behind the housing; the housing body is
  tinted by the selected `TrafficSignalBodyColor` (housing color).
- A near-black sign face (`COL_SIGN_FACE = 0.06`) is inset on the front to form the dark
  LED background.
- Text: three lines rendered with `CsmFontRenderer.electronicSign()` at
  `TEXT_SCALE = 1.55`, in amber `TEXT_COLOR_AMBER = 0xFFAA00`. The text pass forces the
  lightmap to fullbright (240/240) so the LEDs glow at night instead of dimming with world
  light. Empty pages/lines are skipped.

> Note: the shipped housing is considerably larger than the early plan's 96×40×20 sketch;
> the renderer constants above are authoritative.

### GUI (`BlockOverheadMessageSignGui`)

A page editor: prev/next page, three text fields (16-char max, force-uppercased on save),
add/remove page (1–8 pages), a cycle-speed stepper (1–10 s), and a housing-color cycle
button. Save sends a **`TileEntityPortableMessageSignUpdatePacket`** (the PCMS packet,
reused) carrying all pages, `FLASHER_NONE`, the cycle speed, zero color/angle, and the
housing color NBT.

---

## Block 2: Portable Variable Speed Limit Trailer (`portable_speed_limit_sign`)

A trailer-mounted variable speed limit sign: a standard MUTCD "SPEED LIMIT" panel with the
numeric limit shown on a digital LED screen below the static legend, mounted on a mast above
a PCMS-style tow trailer with outriggers, wheels, and two 8-inch beacon flashers. Its
collision box is the low trailer footprint (`0,0,0.125 → 1,0.5,0.875`); the TESR renders the
full mast-and-sign assembly upward and outward (render bbox ~9 wide × 7 tall × 9 deep).

This block introduces the **shared speed-limit tile entity**, `TileEntityVariableSpeedLimit`
(extends `AbstractTileEntity`), which stores:

- `speedValue` — clamped to **20–95** (default 35).
- `flasherMode` — `FLASHER_NONE` (0) / `FLASHER_OFF` (1) / `FLASHER_ON` (2).
- `trailerColor` — index into `COLOR_NAMES` (Orange, Yellow, Black, Silver, White).
- `signAngle` — index into `ANGLE_NAMES` (Normal, Left/Right Tilt, Left/Right Angle).
- `housingColor` — a `TrafficSignalBodyColor` (used by the overhead variant, below).

`setData(...)` clamps every field and calls `markDirtySync` to push the change to clients.

### Rendering (`TileEntityPortableSpeedLimitRenderer`)

The renderer is bound to the base `TileEntityVariableSpeedLimit` type. Key constants:

- Sign panel: `SIGN_WIDTH = 27`, `SIGN_HEIGHT = 36`, `SIGN_DEPTH = 3`, on a mast of height 24
  above a trailer body matching the PCMS (length 36, width 19.5).
- The sign face is split at `SIGN_DIVIDER_Y = SIGN_BOTTOM + SIGN_HEIGHT * 0.42` into an
  **upper "SPEED LIMIT" legend area** and a **lower LED screen**. The upper area shows the
  static legend ("SPEED" / "LIMIT") drawn in black with `CsmFontRenderer.highwayGothic()`.
  The lower area is a bright white inset screen (`COL_SCREEN_WHITE`, drawn at fullbright)
  on which the numeric speed is rendered in black digits (`TEXT_COLOR_BLACK = 0x111111`,
  `SPEED_TEXT_SCALE = 1.35`). This mirrors a real digital speed-limit display: a white LED
  matrix showing dark numerals, not an amber-on-black message board.
- Flashers reuse the traffic-signal 8-inch geometry (`TrafficSignalVertexData`,
  `SIGNAL_ATLAS`). When `flasherMode == FLASHER_ON` the yellow LED bulbs blink on a
  500 ms cycle; `FLASHER_OFF` shows dark beacons; `FLASHER_NONE` omits the beacon arms.
- `signAngle` applies an extra Y rotation (`ANGLE_ROTATIONS = 0, -15, 15, -45, 45`) so the
  trailer can be aimed off-axis; `trailerColor` tints the trailer/outrigger bodies.

### GUI (`BlockPortableSpeedLimitGui`)

Speed stepper (± 5, clamped 20–95), flasher-mode cycle, trailer-color cycle, sign-angle
cycle, plus Save/Cancel. Save sends a **`TileEntityVariableSpeedLimitUpdatePacket`** with
speed, flasher mode, color, and angle.

---

## Block 3: Large Overhead Variable Speed Limit Sign (`overhead_speed_limit_sign`)

A gantry-mounted variable speed limit sign with no trailer or flashers — a large fixed
housing displaying "SPEED LIMIT" over a big LED number. Full-block collision; the TESR
renders a housing centered on the block (render bbox ~5 wide × 7 tall × 5 deep).

It **reuses the speed-limit tile entity** via subclassing: `TileEntityOverheadSpeedLimit`
extends `TileEntityVariableSpeedLimit` and adds one field, `fullScreen` (boolean, NBT key
`fScr`). Speed value and housing color carry over from the parent; flasher/trailer/angle
fields are present but unused for this block.

### Rendering (`TileEntityOverheadSpeedLimitRenderer`)

- Housing: `SIGN_WIDTH = 48`, `SIGN_HEIGHT = 64`, `SIGN_DEPTH = 12`, `SIGN_FRAME = 2.0`
  (a 3 × 4 block face), centered on the block and running back to `BACK_Z = 16`. A border
  and frame box wrap the housing, all tinted by the housing color.
- Same 42% face split: upper "SPEED LIMIT" legend (`LABEL_TEXT_SCALE = 1.45`) and a lower
  LED number (`SPEED_TEXT_SCALE = 2.4`), both `highwayGothic`, dark digits
  (`TEXT_COLOR_BLACK = 0x111111`) on a white screen.
- **Full-screen mode** (`fullScreen`): when off, only the inset lower screen panel is drawn
  at fullbright and the housing takes world ambient light; when on, the entire sign face is
  lit fullbright (the whole panel glows as one large LED matrix).

### GUI (`BlockOverheadSpeedLimitGui`)

Speed stepper (± 5, clamped 20–95), housing-color cycle, full-screen toggle, Save/Cancel.
Save sends a **`TileEntityVariableSpeedLimitUpdatePacket`** using the extended constructor
that also carries `housingColor` and the `fullScreen` flag.

---

## Block 4: School Zone Beacon Assembly (`school_zone_beacon`)

A SCHOOL SPEED LIMIT assembly with amber beacons that flash during the hours the zone is
posted for. It is in this family because it is a TESR-drawn panel with a settable speed, but it
differs from the three above in one important way: **it answers to the world clock, not to a
player setting or a controller.**

The face is a real assembly's, not one panel: a **white S5-1 regulatory sign** under a coloured
**S4-3 SCHOOL plaque**, separated by a strip of the border. The white body is regulatory and
stays white; the plaque takes the `MutcdSignFaceColor` choice described under Block 5, because
the MUTCD permits either standard yellow or fluorescent yellow-green on a warning face and both
are in service.

**Data** (`TileEntitySchoolZoneBeacon`): posted speed (5–45 in fives), panel scale
(75/100/125/150/200%), beacon arrangement, beacon size, plaque colour, a mode
(Off / Scheduled / Always On), and four schedule hours — AM start/end and PM start/end.

**The beacons are real signal sections.** They are drawn from `TrafficSignalVertexData` and the
signal bulb atlas — body, door, circle visor and bulb — exactly as
`TileEntityPortableSpeedLimitRenderer` draws its flashers, rather than as hand-drawn lamps. That
is what makes the **8 inch / 12 inch** choice free: both section sizes already exist. The three
arrangements are the ones that get installed: **one above and one below**, **one above**, and
**two above** on a crossbar.

**The schedule.** Two windows, because that is what a real school zone posts: one around
arrival and one around dismissal. Hours are 0–23 against Minecraft's clock, which starts its
day at 06:00, so `hour = ((worldTime / 1000) + 6) % 24`. Two edge cases are decided
deliberately and covered by `TileEntitySchoolZoneBeaconTest`:

- a window whose end is at or before its start **wraps past midnight**, so 22→02 reads the way
  it looks rather than being empty;
- a zero-length window (start == end) is **off**, not all day — the alternative turns a
  mis-click into a beacon that flashes around the clock.

**Nothing ticks.** `isFlashingNow()` is a pure function of world time and the stored schedule,
so the renderer asks per frame, no state needs syncing beyond the schedule itself, and a chunk
that reloads mid-window comes back flashing.

**Rendering.** Sized to the roadside sign family rather than to Blocks 2 and 3 — their panels
are wider than a block, which reads as a gantry sign rather than something on a post. The panel
is 16 × 30, near `metal_sign_ultratall`'s proportions, and the assembly draws **no post of its
own** so it mounts on whatever pole is behind it. The two lamps in a bar are the halves of
`TrafficSignalFlashPattern.OFF`/`.B` — the signal system's existing wig-wag pair — so a school
zone blinks at the same rate as everything else, and a lower bar runs opposite the upper one so
a two-bar assembly alternates rather than blinking in unison.

**Scale grows upward.** The scale pivot is the bottom of the assembly, not its centre. Scaling
about the centre buried the lower half at 200% — the panel sank through the ground and took
WHEN FLASHING with it. At 100% the pivot makes no difference.

**Three things about the geometry that were corrected by looking at it in world**, and are worth
preserving because none of them fail a build:

- The panel's **Z pivot is derived from the panel depth**, not written as its own number. It was
  once a magic `15.0` against a 1-deep panel, which put the back face at 15.5 and left half a
  unit of daylight between the sign and whatever it was bolted to.
- **Bracketry has to look like it holds the weight.** The first bracket was 2.2 wide by 2 deep on
  the sign's own plane, so a section-sized head appeared to hang off a thread. It now runs the
  full depth behind the head (z 11.8–16) at 4 wide, and a two-head assembly hangs from a crossbar
  spanning both heads with a central pillar down to the sign.
- **Rounded corners are stepped bands that touch, never overlapping boxes.** Overlapping boxes
  share a face plane and z-fight. The helper is `RenderHelper.addRoundedRect`, shared with the
  radar sign; radius 1.5 reads as a stamped blank, and 2.2 read as too round.

**Fabricator cost** is the traffic accessories tab default (sheet metal + fastener kit), the
same as the pole-mount electronic speed limit sign. Pricing the electronics-bearing accessories
higher would mean giving that tab its own `ICsmFabricatorCostRule`, which would move several
blocks at once and is deliberately not done here.

> **Testing note.** Beacons that are correctly dark look exactly like beacons that are missing.
> `/time set` does not stop the clock, so a window can lapse between setting the time and taking
> a screenshot. Freeze it with `/gamerule doDaylightCycle false` before judging anything about
> the beacons.

---

## Block 5: Radar Speed Feedback Sign (`radar_speed_sign`)

The "YOUR SPEED 32" board that reads an approaching vehicle and shows its speed back to it. It
is the only block in this family that **measures** anything, and the only one with an output.

**Data** (`TileEntityRadarSpeedSign`): posted speed (5–75 in fives), a speed multiplier, panel
scale (the school zone sign's five steps), an optional R2-1 header panel, the sign face colour,
and its scan zone.

### The speed maths

Speed is not read from `motionX`/`motionZ`. **Those fields are not maintained server-side for
players**, so reading them returns zero for exactly the entity this block most needs to measure.
Instead the tile entity samples entity positions every `SAMPLE_INTERVAL_TICKS` (4) and
differences them by UUID, **horizontally only** — otherwise falling registers as speeding. A
sample implying more than 40 blocks/s is a teleport and is discarded, and the position map is
pruned for entities that stop being seen, so an unloaded chunk cannot leak.

The conversion is deliberately the same one the **SUM mod's speed HUD** uses:

```
mph = blocksPerSecond * 2.2369362920544 * multiplier
```

A block is a metre and a tick is 1/20 s, so blocks per second *is* metres per second and the
constant is exact. Copying it from `HudFormat` rather than deriving something similar means a
sign and that HUD can never disagree about the same journey.

Read literally, though, nothing in Minecraft moves at road speed: a sprinting player covers
`SPRINT_BLOCKS_PER_SECOND` (5.612) blocks/s, which is about **12.5 mph**, so a realistically
posted 25 mph zone would never once be exceeded and the block would be inert. That is what
`MULTIPLIERS` (1x through 5x) is for. It defaults to **1x** — true to the HUD — and scales up for
anyone who would rather their traffic behaved like traffic.

### Display states

| Condition | Face |
|---|---|
| Zone empty for more than `HOLD_TICKS` (60) | Blank |
| At or under the posted speed | Steady amber digits |
| Over the posted speed | Flashing digits |
| Over posted + `SLOW_DOWN_MARGIN` (15) | Flashing `SLOW DOWN` |

The hold matters more than it looks: without it the number vanishes the instant a driver passes
the sign, which is precisely when they look at it. Three seconds is what the real boards give.
With several vehicles in the zone the sign shows **the fastest**, as a real radar reads the
strongest return.

**Redstone.** `canProvidePower` is true and `getWeakPower` returns full strength while
`isOverLimit()` — so a sign can drive a beacon, a camera, or anything else in the mod, without
that behaviour being built into the block.

### The scan zone

A freshly placed sign works immediately: `defaultScanZone` builds a box reaching 12 blocks out
along `BlockHorizontal.FACING`, about 3 wide, from `y-1` to `y+3`. `ItemSensorZoneTool` then
overrides it with two explicit corners, exactly as it programs a signal sensor's zone.

> **The activate-before-use trap.** A block's `onBlockActivated` runs **before** the held item's
> `onItemUse`, so the sign declines the click while that tool is held. Without the exemption the
> configuration GUI opens over the top of every attempt to program a zone, and the tool appears
> to be broken. The lane control controller and lane control signal need the same branch for the
> link tool.

What counts as a vehicle is `TrafficEntitySelectors.VEHICLE` — lifted out of
`TileEntityTrafficSignalSensor`, which now aliases it, because two definitions of "what is a
vehicle" would have drifted apart.

### Sign face colour (`MutcdSignFaceColor`)

Not a housing palette. The MUTCD permits a warning sign face to be **standard yellow** *or*
**fluorescent yellow-green**, the same choice it permits on crosswalk signs, and both are in
service — so the enum holds exactly those two and nothing else. The school zone assembly's
SCHOOL plaque takes the same setting.

### Rendering notes

- **Rebind the white swatch after every `CsmFontRenderer.drawString`.** See the warning in
  `TRAFFIC_SIGNAL_SYSTEM.md` § Shader-Compatible TESR Rendering — this cost an entire debugging
  round on the school zone beacon.
- Amber digits use `CsmFontRenderer.electronicSign()`; the white header legend uses
  `highwayGothic()`.
- **The R2-1 header is 20 tall on a 16-wide face.** A real R2-1 blank is 24×30, and a first pass
  at 14 read as a squashed placard rather than a speed limit sign.

Inventory texture: `dev-env-utils/scripts/gen_radar_speed_sign_texture.py`.

---

## Shared Infrastructure

### Tile entities & packets

- `TileEntityVariableSpeedLimit` is shared by Blocks 2 and 3 (Block 3 via subclass). Both
  speed-limit signs are driven by **`TileEntityVariableSpeedLimitUpdatePacket`** and its
  server handler **`TileEntityVariableSpeedLimitUpdateHandler`**.
- The DMS (Block 1) instead reuses the PCMS tile entity and the existing
  **`TileEntityPortableMessageSignUpdatePacket`**.

The update packet has two constructors — a 4-field form (speed/flasher/color/angle, used by
the portable trailer) and a 7-field form that adds `housingColor` and `fullScreen` (used by
the overhead sign). `fromBytes` reads the extra fields only `if (buf.isReadable())`, so both
wire formats decode safely.

### Packet security

`TileEntityVariableSpeedLimitUpdateHandler` schedules the mutation on the server thread and
gates it on **`CsmPacketUtils.canPlayerReach(player, message.getPos())`** before touching the
tile entity — a player who cannot reach the block cannot edit its value. The handler applies
`setFullScreen` first when the TE is a `TileEntityOverheadSpeedLimit`, then calls the
clamping `setData(...)` for the common fields.

### Registration points

- **GUIs** — `CsmGuiHandler` maps IDs 10/11/12 to the three GUIs. Because
  `TileEntityOverheadSpeedLimit` *is a* `TileEntityVariableSpeedLimit`, the handler checks
  the more-specific id 12 / `TileEntityOverheadSpeedLimit` branch separately; id 11 is keyed
  to the portable trailer.
- **Packets** — registered in `Csm.java` (`init`) via `CsmNetwork.registerNetworkMessage(...)`,
  `Side.SERVER`.
- **TESRs** — bound in `CsmClientProxy` (each `TileEntity*.class → new …Renderer()`).
- **Creative tab** — all three are registered in `tabs/CsmTabTrafficAccessories.java` via
  `initTabBlock(...)`.

Plus the usual per-block assets: blockstate JSON, block/item model JSON, placeholder texture,
and an `en_us.lang` `tile.<registry_name>.name` entry.

---

## Extending: adding another message/speed variant

To add a new electronic-message variant, subclass `TileEntityPortableMessageSign` (override
`getRenderBoundingBox` for the new size), write a TESR following
`TileEntityOverheadMessageSignRenderer`, add a block + GUI, allocate a new GUI ID in
`CsmGuiHandler`, and reuse `TileEntityPortableMessageSignUpdatePacket`.

To add a new speed-limit variant, subclass `TileEntityVariableSpeedLimit` (add fields and
extend `readNBT`/`writeNBT` if needed, as `TileEntityOverheadSpeedLimit` does for
`fullScreen`), write a TESR with the 42% legend/screen split, add a block + GUI + new GUI ID,
and reuse `TileEntityVariableSpeedLimitUpdatePacket` — extend its byte format with trailing
`isReadable()`-guarded fields to stay backward-compatible. Remember to register the new TESR
in `CsmClientProxy`, the block in `CsmTabTrafficAccessories`, and (for a brand-new packet
type) the handler in `Csm.java`.
