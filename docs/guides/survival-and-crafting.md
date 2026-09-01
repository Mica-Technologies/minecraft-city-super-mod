# Survival & Crafting

The City Super Mod is aimed at creative building, and that has not changed. But everything in it is
obtainable in survival, through one chain.

!!! info "CSM adds nothing to world generation"

    No ores, no structures, no loot, no villager trades, no mob spawns, no biome hooks. **No CSM
    block appears in a world unless a player places it.** Adding the mod to an existing save changes
    no terrain.

## The two-tier chain

``` mermaid
graph LR
    A[Vanilla ores<br/>and ingots] -->|crafting table| B[CSM parts]
    B -->|CSM Fabricator| C[Any CSM block]
```

**Tier 1** is fourteen crafting parts, made at a normal crafting table from vanilla materials.
**Tier 2** is every CSM block, made at the CSM Fabricator from those parts.

## Tier 1 — the parts

The parts live in the **CSM: Materials** creative tab. They have no behaviour of their own; they
exist to be ingredients.

| Part | Made from |
|---|---|
| Sheet Metal | 3 iron ingots in a row → 4 |
| Fastener Kit | 4 iron nuggets → 4 |
| Pole Section | 3 sheet metal in a column → 2 |
| Enclosure Shell | 8 sheet metal around iron bars |
| Wiring Harness | redstone + iron nugget + string → 2 |
| Control Board | gold nuggets + redstone + quartz |
| LED Module | glowstone + redstone over glass panes → 2 |
| Lens Assembly | 2 glass panes + any dye → 2 |
| Sounder Driver | sheet metal + note block + redstone |
| Reflective Sheeting | quartz + white dye → 2 |
| Sign Blank | sheet metal + reflective sheeting → 2 |
| Concrete Mix | gravel + sand + clay ball → 4 |
| Ducting | 4 sheet metal in a ring → 4 |
| Optical Sensor | lens assembly + control board |

Recipes use Forge's ore-dictionary variants, so vanilla inputs accept equivalents and other mods'
materials work where they should.

The **CSM Fabricator** itself is crafted at a bench, from sheet metal, a control board and an
enclosure shell. That is the one thing you have to build before anything else is reachable.

## Tier 2 — the Fabricator

Right-click the Fabricator to open a searchable picker listing every fabricable block, grouped by
creative tab. Choose a batch size, pick a block, and the parts come out of your inventory.

### Why there is no crafting recipe per block

This was the original plan, and it does not work — which is worth explaining, because "just add
recipes" is the obvious suggestion.

The mod has over 1,500 blocks built from fourteen parts. Grouping blocks by everything a cost table
can actually tell apart — creative tab plus base class — yields **47 groups**. So 846 of the 852
non-sign blocks would share ingredients with at least one other block, and Minecraft resolves
identical recipes to whichever loaded first.

Per-block recipes would therefore have produced roughly **47 craftable blocks and 805 permanently
uncraftable ones**, while looking complete from the outside. The 574 road signs cannot have
distinguishable grid recipes either.

The Fabricator sidesteps all of it by selecting its output *explicitly* instead of inferring it from
ingredients. A cost only has to be **fair**, not **unique**.

### How costs are decided

Costs are computed at runtime from what a block is — its creative tab and its base class — so
nothing is generated and nothing has to be maintained per block. Each is two or three ingredients,
and may mix CSM parts with vanilla items so the recipe says something true about the material:
coloured metal takes the matching dye, wooden furniture takes planks, utility crossarms take timber.

A new block is therefore **craftable the moment it is registered**, at its subsystem's cost, with no
recipe to write.

## Mining behaviour

Blocks return themselves when mined with the right tool. The tool and harvest level for any
particular block are listed alongside it in the [block reference](../reference/index.md) — a blank
column there means the block does not declare one, not that it needs nothing.
