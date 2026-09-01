# Adding Content

## Adding a block

1. **Create the class** in the right subsystem package, extending
   [the right base class](base-classes.md). Registry name in `snake_case`.

2. **Create the blockstate** at
   `src/main/resources/assets/csm/blockstates/<registry_name>.json`. Prefer the Forge format:

    ```json
    {
      "forge_marker": 1,
      "defaults": {
        "model": "csm:block/shared_models/<subsystem>/<model>",
        "textures": { "all": "csm:blocks/<subsystem>/<texture>" }
      },
      "variants": {
        "facing": { "north": {}, "east": { "y": 90 },
                    "south": { "y": 180 }, "west": { "y": 270 } },
        "inventory": [{}]
      }
    }
    ```

    The `inventory` variant handles the item render, so no separate item model file is needed.

3. **Create the block model** at `models/block/<registry_name>.json`, with its parent pointing at
   the shared geometry.

4. **Add textures** under `textures/blocks/<subsystem>/`, power-of-two PNG.

5. **Add the lang entry**:

    ```
    tile.<registry_name>.name=Human Readable Name
    ```

6. **Register it in a tab** — `tabs/CsmTab*.java`, with `initTabBlock(...)`.

!!! tip "Creative order is registration order"

    Where a block appears in its creative tab is decided by where you put the `initTabBlock` call,
    not by its name. Road signs are grouped by MUTCD category this way.

### What you do **not** have to do

- **No crafting recipe.** Fabricator costs are derived at runtime from the block's creative tab and
  base class, so a new block is craftable the moment it is registered, at its subsystem's cost. Only
  a brand-new creative tab, or a block that is a genuinely new *kind* of equipment, needs
  `CsmFabricatorCosts.java` touched. See [Survival & Crafting](../guides/survival-and-crafting.md).
- **No registration call.** The constructor does it.
- **No wiki edit.** Re-run the reference generator (below).

## Adding an item

1. Create the class extending `AbstractItem`.
2. Create `models/item/<registry_name>.json`.
3. Add the texture under `textures/items/`.
4. Add the lang entry: `item.<registry_name>.name=Human Readable Name`.
5. Register with `initTabItem(...)`.

Items **are not fabricable** — if an item should be obtainable in survival it needs a JSON recipe
under `resources/recipes/`.

!!! tip "Use a factory for near-identical items"

    Items differing only in registry name and tooltip should go through
    `codeutils/ItemDecorativeFactory` or `materials/ItemCraftingPart` and be registered with the
    `initTabItem(Item)` overload, rather than getting a class each.

## Adding a sound

1. Add the `.ogg` to `resources/assets/csm/sounds/`, `snake_case`, OGG Vorbis.
2. Add an entry to `sounds.json` with `"name": "csm:<filename_without_ext>"`.
3. Add the enum entry to `CsmSounds.java`.

Referenced in code as `"csm:<sound_event_id>"`, matching the `sounds.json` key.

## Regenerating the block reference

The [block reference](../reference/index.md) on this site is generated. After adding blocks:

```bash
python3 dev-env-utils/scripts/gen_wiki_reference.py
```

It prints what it wrote and how much it resolved:

```
Blocks written      : 1639
With a display name : 1639  (0 missing a lang entry)
With resolved stats : 1385  (254 left blank)
```

A block missing a display name means a missing lang entry — worth fixing before committing.

!!! warning "Never hand-edit docs/reference/"

    Those pages are overwritten on the next run. The old GitHub Wiki was 415 blocks short precisely
    because its catalogue and the mod could drift apart; this one cannot, as long as nobody edits
    the output.

## Before you commit

Compiling green is not the same as working:

```bash
JAVA_HOME="<your Java 17>" ./gradlew build
grep -iE "missing model|exception loading model|texture.*not found" run/logs/latest.log
```

A missing model or texture loads without an exception and renders as the purple-and-black
placeholder in game. The log is the only place it shows up before you see it.
