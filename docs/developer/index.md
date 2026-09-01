# Developer

Working on the mod rather than playing with it.

<div class="grid cards" markdown>

-   :material-hammer:{ .lg .middle } **[Building from source](building.md)**

    ---

    The toolchain, the Gradle tasks, and the two ways to run a dev client on Apple Silicon — only
    one of which currently gives a working window.

-   :material-file-tree:{ .lg .middle } **[Block & item base classes](base-classes.md)**

    ---

    Which base class to extend, what the constructor does for you, and the two rotation traps that
    read as "rotation is broken" when they are not.

-   :material-plus-box:{ .lg .middle } **[Adding content](adding-content.md)**

    ---

    Blocks, items and sounds, step by step — including everything you deliberately do *not* have to
    do, such as writing a crafting recipe.

-   :material-wrench-cog:{ .lg .middle } **[Tooling](tooling.md)**

    ---

    The integrity checkers, asset generators, benchmarks and the in-game render-pass toggles.

</div>

## Where things live

```
src/main/java/com/micatechnologies/minecraft/csm/
├── codeutils/            Base classes every block and item extends
├── tabs/                 One file per creative tab; registration order is display order
├── buildingmaterials/    ┐
├── furniture/            │
├── hvac/                 │
├── lifesafety/           ├ One package per subsystem, matching the creative tabs
├── lighting/             │
├── powergrid/            │
├── technology/           │
├── trafficaccessories/   │
├── trafficsignals/       │
└── trafficsigns/         ┘

src/main/resources/assets/csm/
├── blockstates/          One JSON per block; Forge format preferred
├── models/block/         Block models, with shared geometry under shared_models/
├── textures/blocks/      Organised by subsystem
├── lang/en_us.lang       Display names — also the source for this site's block reference
└── recipes/              Tier-1 JSON recipes for parts and the Fabricator
```

## Developer tooling

`dev-env-utils/` is a separate Maven project holding the tools that keep the asset set honest —
integrity checking, bounding box extraction, atlas generation, and the generator that produces the
[block reference](../reference/index.md) on this site.

`dev-env-utils/scripts/` holds Python generators for textures, models and documentation.

!!! tip "The block reference is generated"

    `dev-env-utils/scripts/gen_wiki_reference.py` rebuilds `docs/reference/` from the Java sources
    and the lang file. Run it after adding blocks; never hand-edit those pages.
