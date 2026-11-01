# Core and shared tools

Scripts that serve every module: the block index and guidebook reference, the Fabricator audit, the
atlas budget, the model checkers and `model_depth.py`, the benchmarks, the module-split audits, the
release checks, the MCMCP orchestrator client, and Core's wayfinding panel assets.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_part_textures.py` -- crafting part textures, item models and recipes; validates ingredients before writing
- `gen_fabricator_textures.py` -- CSM Fabricator block textures
- `csm_block_index.py` -- resolves every block to its registry name, package and creative tab by parsing the sources; importable as a module by other scripts
- `gen_wiki_reference.py` -- the guidebook's block catalogue under `docs/reference/`, generated from that index.
  The site publishes only what is committed, so `--check` (writes nothing, exits 1 on drift) runs on every pull request
- `audit_fabricator_costs.py` -- mirrors the Fabricator cost rules against that index to sanity check what every block costs, without launching the game
- `atlas_budget.py` -- how full CSM leaves the block atlas: collects every sprite the game loads
  and packs them with a port of Forge's `Stitcher`. Warns above 95% of 8192 x 4096 and exits 1
  once the atlas outgrows it (which silently doubles its GPU memory and stitch time). Run it
  before adding a large batch of textures; see "Memory" in PERFORMANCE_AND_SECURITY.md
- `model_depth.py` -- separates the coplanar faces that z-fight in JSON block models. Every
  JSON-model generator runs it at the end of `generate` / `write_all` (the furniture, novelties and
  Parks generators, the Life Safety catalogue in `life_safety_gen_common.py`, the Building
  generators and the exit signs, via `separate_dirs` where files go to one folder per kind, and
  `separate_outputs` where a generator builds them in memory first; `keep=` pins a face a
  renderer draws over, such as the preempt confirmation lights' lenses): it takes the models a blockstate draws together (under 0.2 px apart, opaque, different pixels where
  they overlap, measured off the textures) and moves the smaller face along its normal by growing
  its box, in models that generator wrote only, at most 0.65 px in all. `model_depth.py <module>
  [--apply] [--models ...]` runs it once over hand-made models, writing only the numbers that move
  and more carefully: 0.45 px at most, and never the stacked end caps of a round shape made of
  turned copies (fanning those out stair-steps a pole's end). Never run it over a generator's
  output or over a model a generator reads (the silver pole parts `gen_concrete_poles.py` copies,
  the lamp models `gen_lighting_lit_atlases.py` reads, the sign plates `gen_official_faces.py`
  measures). Draw a detail flush and let it separate it; see Residential Furniture in
  `NOVELTIES_SYSTEM.md`. After separating it prunes: a face is removed when every point just in
  front of it lies inside a closed box of the same model (all six faces present, every one opaque
  in every texture set the model is drawn with, child models and Forge blockstate textures
  included). A box's volume hides nothing, only its faces do, so an open box (a basin, a crate
  with no top) never hides what is inside it. `--prune` / `--prune-only` do it once for hand-made
  models, deleting only the removed entries from the file's text
- `forge_multipart.py` -- turns a generator's Forge blockstate whose properties only swap
  textures into multipart (one model location; a child model per texture set under
  `states/<block>/`); `verify_multipart_conversion.py <rev> --changed` checks the result draws
  every state as the Forge form did (PERFORMANCE_AND_SECURITY.md "Memory")
- `audit_model_weight.py` -- how heavy every JSON block model is: quads per model and module, the
  heaviest models, and how many of their faces are hidden by the model's own closed opaque boxes
  (what `model_depth.py`'s pruning would remove). Offline, a few seconds a module
- `gen_wayfinding.py` -- the wayfinding panels' assets (#307), for all three blocks: Core's two
  512 px sheets (the glyphs: Series D from the committed highway font, with the fourteen ASCII
  symbols the font lacks drawn in its stroke, and a white swatch; the icons: 46 pictograms and two
  arrows as alpha masks in 64 px cells) and `WayfindingSheet.java` from the same metrics, the empty
  world model, and the airport, hospital and generic panels' blockstates and item models;
  `--check` (the Java too), `--sheet` (an icon review sheet)
- `mcmcp_orchestrator.py` -- a minimal MCP client for the MCMCP orchestrator over its stdio shim,
  for build scripts: every call names its game by instance id, so a script cannot land in another
  running game (a fixed port such as 25586 belongs to whichever game bound it first, which has
  been a live server). New scripts should use it rather than `csm_bench.py`'s port client
- `audit_obj_models.py` -- checks generated OBJ models for the faults that only show up in game:
  coplanar overlapping faces and faces lying on a block boundary (both z-fighting), inconsistent
  winding (a surface that culls from the side you are looking at), and open boundary edges
- `preview_block_model.py` -- renders a Forge JSON element model or an OBJ against its texture offline, with Minecraft's face winding and UV origin, so stretched UVs and transparent bleed can be caught without launching the game
- `audit_inventory_renders.py` -- measures, in a running dev client, how far every OBJ-backed item
  actually sits inside its 16px inventory slot, by putting each one alone in a hotbar slot and
  differencing the frame against the empty slot (the slot rectangle calibrated off a vanilla cube).
  `--fix` recentres and rescales the ones that hang out, editing the values where they sit so the
  compact JSON style most of these blockstates use survives. Whether an inventory render fits
  cannot be read off the blockstate -- an offline estimate of this was wrong on most of the tree --
  and the translation in a Forge blockstate transform is in BLOCK units, not the 1/16 a vanilla
  `models/item` display uses. A correction needs a rebuild AND a client restart to re-measure: a
  resource reload does not rebake these, and stopping the Gradle task leaves the game JVM holding
  the MCMCP port, so the next run silently measures the stale client
- `csm_bench.py` -- builds a dense grid of CSM content in a throwaway world and measures client
  frame time against it over MCMCP (`build` / `measure` / `compare`). It pins the time, weather and
  view distance, and refuses to report a figure taken while the frame rate is sitting at the
  configured cap or before the scene has finished loading -- both of which have produced confident
  nonsense before
- `csm_bench_attribute.py` -- answers "where does the frame time actually go" by deleting one
  category of block at a time and differencing. Subtractive rather than instrumented, so it cannot
  be fooled by a mistaken belief about which code runs
- `audit_package_deps.py` -- Java package dependency matrix: which subsystem packages reference
  which, with the symbols. A module may only reference Core, so every non-zero cell between two
  subsystems is a modularization to-do
- `audit_asset_ownership.py` -- resolves every blockstate to its owning creative tab and follows it
  to the models and textures it reaches, then lists every reach into a folder named for a different
  subsystem (the shared assets), unreached models, unreferenced textures, and sound/lang ownership
- `partition_assets.py` -- works out which module ships each resource and moves it there with
  `git mv`, keeping every `assets/csm` path unchanged so no JSON, OBJ or MTL is rewritten. Owner is
  the creative tab for a blockstate, reachability for what it names, the subsystem folder for files
  nothing reaches, the module sound enums for `sounds.json`, and the key's owner for lang. A file
  two modules reach stays in Core. `--dry-run` prints the whole plan first
- `check_module_assets.py` -- resolves every model, texture and sound a module ships against that
  module plus Core and nothing else, which is what a partial install would have. Run it after
  anything that moves an asset between trees
- `diff_registry_dumps.py` -- compares two MCMCP `game_dump_registries` dumps and fails on anything
  a module split must not change: a missing or reordered block, item, tile entity, sound event or
  recipe, or a changed display name or tab. `--ignore-class`, `--ignore-tab-index`,
  `--unordered-sounds` and `--unordered-hidden` waive the four differences the split legitimately
  causes
- `check_reobf_refs.py` -- disassembles release jars and fails if they name a Minecraft member by its dev
  (MCP) name where the previous release did not: the reobfuscation gap a dev client and a green build
  both miss, and that crashed the first real launcher test of the module jars. Run it before tagging a release
