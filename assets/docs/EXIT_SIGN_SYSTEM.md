# Exit Sign System

The configurable exit signs of **CSM: Life Safety** (`csm_lifesafety`): a traditional sign with a
stencil legend in an opaque housing, and the specialty fixtures, each set up after it is placed --
arrows, letter colour, housing, mount, emergency lamp heads and legend -- rather than being one
block per combination. The older edge-lit and green running-man signs are plain factory blocks
and are not part of this system.

| Block | Registry | Housings | Mounts | Heads | States |
|---|---|---|---|---|---|
| Traditional Exit Sign | `exit_sign_traditional_flat` | white, black | wall, ceiling, end (either side) | none, square LED, round | 3,072 |
| Traditional Exit Sign (Rounded) | `exit_sign_traditional_rounded` | white, black | all four | none, square, round | 3,072 |
| Exit Sign / Emergency Light Combo | `exit_sign_combo_compact` | white, black | all four | square, round (always fitted, above the top corners) | 2,048 |
| Die-Cast Exit Sign | `exit_sign_diecast` | brushed aluminium, black, white | all four | none | 768 |
| Vandal-Resistant Exit Sign | `exit_sign_vandal_resistant` | white, black | wall, ceiling | none | 256 |
| Photoluminescent Exit Sign | `exit_sign_photoluminescent` | white, black | wall, ceiling | none | 256 |
| Explosion-Proof Exit Sign | `exit_sign_explosion_proof` | brushed aluminium | wall, ceiling | none | 128 |

Every sign offers four arrows (none, left, right, both), red or green letters (the
photoluminescent sign defaults to green) and the legend EXIT or SALIDA. All of it lives in
`com.micatechnologies.minecraft.csm.lifesafety.exitsign`.

## A sign's setup is tile entity data, not metadata

Metadata is four bits, and holds only what changes without the player: the facing (horizontal,
2 bits) and, for a sign that can have emergency heads, `POWERED`. Everything a player chooses is
an `ExitSignConfig` in the sign's `TileEntityExitSign`, one byte per option under the short keys
`ar lc hs mt hd lg`. `AbstractBlockExitSign.getActualState` reads it and sets one block state
property per option, which is what the multipart blockstate picks parts with. The item carries
the same config under the stack tag `csmExitSign`, so creative presets, pick-block and drops all
keep it.

**What a block offers is its `ExitSignSpec`,** built once in a static `SPEC` field (the block
state container is created inside the `Block` constructor, before any instance field exists).
An option offered with more than one value becomes a `PropertyEnum` holding only those values;
an option with one value has no property at all. Any stored value the block does not offer is
clamped to its default before it reaches a model (`ExitSignSpec.clamp`), so a stale or
hand-edited tile entity can never ask for a part that was never written.

**Mains power only where it shows.** Redstone is mains power, as for the emergency lights:
powered is normal operation, unpowered is on battery. All it changes is whether emergency heads
light, so `POWERED` exists only on a sign that can have heads; the specialty signs leave it out,
which halves their state count.

**Placement** faces the sign away from the wall it is placed against, or toward the player if it
is placed on a ceiling or floor; placed under a block it gets a ceiling mount, anywhere else a
wall mount (`ItemBlockExitSign.placeBlockAt`). End mounts are only chosen in the screen. An end
mount's name is the wall's side seen from the front: `end_left` has the wall on the viewer's
left, which for a north-facing model is east.

## The setup screen

Right-click opens `GuiExitSign` (GUI id 31, `LifeSafetyGuiProvider.EXIT_SIGN_GUI_ID`): a row per
option the spec offers, left-click the next value, right-click the previous, with the sign's item
icon drawn large as a preview. Every change is sent at once as `ExitSignConfigPacket` -- the
position and `ExitSignConfig.pack()`, eighteen bits in one int -- so the sign changes behind the
screen as the player clicks. The server checks reach only (as every other config screen does),
that the block and tile entity are an exit sign's, rejects any out-of-range ordinal
(`ExitSignConfig.unpack` returns null) and clamps the rest to the spec.

## Drawing

At rest a sign is baked models only: a vanilla multipart blockstate per block, written with
everything else by `dev-env-utils/scripts/gen_exit_signs.py`.

**A face is three cells.** The legend sits between two arrow cells, and each cell is its own
element meeting the next edge to edge, so an arrow is a multipart part rather than a texture per
arrow combination, and nothing overlaps to z-fight. The right cell is the left one with its UVs
mirrored. Each housing finish has one 512 texture sheet (`textures/blocks/lifesafety/exit_signs/
{white,black,brushed}.png`) holding EXIT and SALIDA lit red and lit green, the chevron lit in
each colour and unlit, square-cornered and rounded, and a patch of bare housing; its `_e`
companion carries only the lit parts, for OptiFine's emissive rendering.

- An **unlit arrow** is moulded into the housing, shaded as if lit from the upper left, as on
  every real sign: a blank end would read as a different sign.
- The letterforms are polygons measured off a head-on photo of a Dual-Lite sign (stroke 0.12 H,
  E 0.31 H, X and T 0.32 H). **EXIT is centred on its stems** -- the E's edge to the T's stem --
  with the T's crossbar overhanging the right arrow, as on the real signs; centring the bounding
  box reads as shifted left. SALIDA sets six letters where EXIT has four, so its cell is wider,
  its letters shorter (6 px against 8) and its arrow cells narrower.
- The face is 16 x 10.5 px at 16 texels a pixel, so a stroke is exactly one model pixel.

**Wall and hung.** A wall-mounted sign sits against the block's south face (the models are drawn
facing north), single-faced. A ceiling or end-mounted sign hangs down the middle and shows the
legend on both faces; its **arrow keeps pointing the same way in the world**, so a left arrow
from the front is a right arrow from behind, as a real double-faced sign's knockout is.

**Housings** are picked by the generator's `kind`: plastic (flat; rounded, whose end cells step
in under an alpha-cut 1.5 px corner; combo), die-cast (a quarter-pixel lip round the face),
vandal-resistant (a clear shield on a gasketed plate: a cutout rim and glints), photoluminescent
(a thin panel with corner screws, hung on two rods) and explosion-proof (a cast frame a pixel wide
round a recessed face, corner bolts, and a conduit hub that is also what it hangs from). Trim that
is not the housing's colour -- shield, gasket, bare metal -- is on `trim.png`.

**Item icons** are one per legend, letter colour, housing and heads, so every creative preset
looks like itself: `ExitSignItemModels` registers them all and a mesh definition picks one from
the stack's config (`AbstractBlockExitSign.getItemModelName`). An icon is the hung sign with its
arrows and heads dark.

### Emergency heads

Square LED heads and round lamps sit on arms off each end (the combo's above its top corners);
the head on the wall side of an end mount is left off. On battery they show a lit lens, raise the
block's light to 15 (12 otherwise; 4 for the photoluminescent sign) and glow with the emergency
lights' renderer. `IEmergencyLightBlock` answers the renderer's questions -- lit or not, facing,
where each bulb is, and a variant that keys the shared glow display list alongside the block id --
with defaults that keep the emergency lights as they were. The exit signs place a bulb on each
head's lens and ask for the **narrow cone** (`hasNarrowCone`): the emergency lights' wide cone
spread across the sign's own face and left bright bars at its edges.

**Only a glowing sign reaches the renderer.** `TileEntityExitSign.shouldRenderInPass` is true
only with heads fitted and the sign on battery, read from the tile entity's cached metadata. Every
other exit sign costs nothing per frame.

## Adding

**A value to an existing option** (a legend, a colour, a finish): append it to its enum in
`ExitSignConfig` -- never reorder or remove one, the ordinal is what every placed sign saved --
teach the generator to draw it, add it to the specs that should offer it and to `STYLES`, add its
lang keys (`csm.exitsign.<option>.<value>`), and run the generator. Check the state count first:
`ExitSignSpecTest` holds every sign under 5,184, the largest block in the mod.

**A new sign:** a block class extending `AbstractBlockExitSign` with a static `SPEC`, an entry in
the generator's `STYLES` (plus a `kind` if its housing is new), a line in
`ExitSignBlockstateTest`, the tab registration and the lang lines in all four languages, then
`gen_exit_signs.py` and `gen_wiki_reference.py`. Override `getFaceBottom`, `getHousingMargin` and
`getBodyDepth` so its outline matches its model.

## Traps

- **The generator repeats the specs.** `STYLES` in `gen_exit_signs.py` lists what each block
  offers, and the Java specs are the truth. A multipart condition naming a value a block does not
  offer fails the whole blockstate at load; `ExitSignBlockstateTest` fails the build instead, and
  also on an item icon a block asks for that was never written.
- **Multipart cannot retexture,** so every part is written once per housing finish. That is why
  the models run to hundreds of files; they are generated and `--check`ed, never edited.
- **Ordinals are saved.** In the tile entity, the item tag and the packet. Append only.
- **A block whose metadata changes keeps its tile entity** (`shouldRefresh` is block-only), which
  is what lets redstone toggle `POWERED` without losing the setup.
- **Nothing here may be translucent.** The vandal-resistant shield was once a faint haze, which
  put the whole block in the translucent layer. There faces are sorted by their centres and still
  write depth, so from some angles the shield's big faces sorted ahead of the sign's end cell and
  cut the end of the sign off. A block has one layer for every quad of its multipart model, so the
  shield is cutout -- a clear face with an opaque rim and two glints -- like everything else.
- **The glow list is keyed on (block, variant).** A block whose bulbs could differ without the
  variant differing would draw another sign's glow; `getGlowVariant` must change whenever
  `getBulbs` does.
