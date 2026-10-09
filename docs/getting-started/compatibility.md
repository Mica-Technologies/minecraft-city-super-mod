# Compatibility

## Minecraft versions

The City Super Mod supports:

- **[Forge 1.12.2](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html)**

Support is not provided or planned for versions not listed above. If you would like to help add
support for another version, contributions are welcome — see the
[repository README](https://github.com/Mica-Technologies/minecraft-city-super-mod).

## Other mods

CSM is written to sit alongside other mods rather than take anything over:

- **It adds nothing to world generation.** No ores, no structures, no biome changes. A CSM world
  generates exactly as a vanilla one does, so the mod can be added to an existing save without
  changing terrain that is already there.
- **It claims no vanilla behaviour.** Nothing here replaces or overrides a vanilla block, item or
  recipe.
- **Power uses Forge Energy**, so the power grid blocks interoperate with any other mod that speaks
  it.

### Known interactions

| Mod | Note |
|---|---|
| **OptiFine** | Works. The mod's own renderers write their lighting per vertex specifically so shader packs read it correctly. |
| **Immersive Engineering** | No dependency in either direction. CSM's span wire is its own implementation and does not use IE's wire system. |
| **WorldEdit / FAWE** | Fine. Blocks with tile entities (signals, panels, dynamic signs) keep their configuration through a copy only if the tool copies tile entity data. |
| **Dynmap** | CSM blocks need their own renderdata files for the web map to draw their shapes. DynmapBlockScan's automatic output fails on CSM's models and floods the server log with warnings. Generate `csm-models.txt` and `csm-texture.txt` with the repository's Dynmap Renderdata tool ([Tooling](../developer/tooling.md#java-tools)), copy both into the server's `dynmap/renderdata/` folder, and restart. Regenerate them for each CSM version, since new blocks need new entries. |
| **SUM (server utility mod)** | Optional. With SUM's economy on and `csm_roads` allowed in its `economy_integration.allowedMods`, parking meters charge the player's wallet instead of taking emeralds. Without SUM they take emeralds; nothing else changes. |
| **Road mods with sloped road blocks** | Work zone devices and everything in the Streetscape tab settle onto sloped and partial-height road and sidewalk blocks instead of floating above them. |

## Adding CSM to an existing world

Safe. Because the mod generates nothing, adding it to a world mid-playthrough changes no terrain and
breaks no chunks.

## Removing CSM from a world

Removing the mod leaves every CSM block as air with a Forge "missing block" warning on load. Back up
first, and clear the blocks before removing it if you want to keep the world tidy.
