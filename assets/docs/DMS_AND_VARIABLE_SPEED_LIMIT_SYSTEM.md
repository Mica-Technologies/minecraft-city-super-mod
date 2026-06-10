# DMS and Variable Speed Limit Sign System

This system adds three TESR-rendered highway electronic-display blocks to the Traffic
Accessories subsystem. All three build on the infrastructure originally created for the
Portable Changeable Message Sign (PCMS, `portable_message_sign`): the same tile-entity
shape, GUI conventions, server-side update packet pattern, and `RenderHelper` box-drawing
helpers. None of them carry a Forge-Energy or redstone connection — they are purely visual,
operator-configured signs.

| Block | Registry name | GUI ID | Tile entity | Renderer |
|---|---|---|---|---|
| Overhead Gantry DMS | `overhead_message_sign` | 10 | `TileEntityOverheadMessageSign` | `TileEntityOverheadMessageSignRenderer` |
| Portable Variable Speed Limit Trailer | `portable_speed_limit_sign` | 11 | `TileEntityVariableSpeedLimit` | `TileEntityPortableSpeedLimitRenderer` |
| Large Overhead Variable Speed Limit Sign | `overhead_speed_limit_sign` | 12 | `TileEntityOverheadSpeedLimit` | `TileEntityOverheadSpeedLimitRenderer` |

All three block classes live in
`src/main/java/com/micatechnologies/minecraft/csm/trafficaccessories/`, extend
`AbstractBlockRotatableNSEW`, and implement `ICsmTileEntityProvider`. Each opens its GUI
from `onBlockActivated` via `player.openGui(Csm.instance, <id>, ...)`. All render with
`BlockRenderLayer.CUTOUT_MIPPED` and are non-opaque, non-full-cube.

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
