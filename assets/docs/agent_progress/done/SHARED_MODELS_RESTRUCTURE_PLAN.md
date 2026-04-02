# Shared Models Restructure & Blockstate Texture Migration Plan

Move shared models from `models/block/shared_models/<subsystem>/` to
`models/block/<subsystem>/shared_models/` to match the texture folder structure.
Consolidate child-parent model chains into Forge blockstate texture overrides.

**Created:** 2026-03-31
**Updated:** 2026-04-01
**Status:** Phase 1 complete, Phase 2 substantially complete, model loading errors resolved

---

## Resume Prompt

> Progress doc: `assets/docs/agent_progress/SHARED_MODELS_RESTRUCTURE_PLAN.md`
>
> **Phase 1 COMPLETE:** All 441 shared models moved from `shared_models/<subsystem>/`
> to `<subsystem>/shared_models/`. The centralized `shared_models/` dir is removed.
>
> **Phase 2 substantially complete:** 663 wrapper models consolidated into Forge
> blockstate texture overrides across all 10 subsystems. 649 models remain that
> cannot be consolidated (multipart fence/slab/stairs variants, blockstate variant
> models, complex models with elements/display, and the transparent TESR placeholder).
>
> **Known special cases:**
> - `signalcontroller.obj` + `.mtl` stay in flat `models/block/` (Forge OBJ limitation)
> - `trafficsignals/mclaglowair.json` is a transparent cube placeholder used as a
>   submodel by ~113 signal blockstates for TESR-rendered blocks. Must NOT be deleted.
> - Building materials has 90 fence/slab/stairs variant models (multipart blockstates)
> - Traffic signals has ~100 block models (crosswalk variants, inventory models) and
>   ~100 shared models (backplate tilt variants, crosswalk mounts, signal geometry)
>
> **2026-04-01 session:**
> - Renamed 17 cryptic lifesafety shared_models to descriptive names
> - Fixed 566 → 0 model loading errors (doubled block/ prefix, cross-subsystem
>   refs, spurious suffixes from batch rename, missing textures)
> - Fixed dslh.json missing striplights texture (lost during Phase 2 consolidation)
> - Renamed 11 corrupted sign textures
> - Audited all blockstate→model references: all now resolve correctly
>
> **Remaining work for future sessions:**
> - 330 lifesafety wrapper models consolidatable only via blockstate rewrite campaign
> - 62 complex lifesafety models (Blockbench geometry) must stay as files
> - 65 traffic accessories backplate tilt variant models must stay

---

## Phase 1: Move shared_models to tab-specific folders (COMPLETE)

Moved `models/block/shared_models/<subsystem>/` → `models/block/<subsystem>/shared_models/`
for all 8 subsystems. 441 files moved, 1,096 reference files updated. Centralized
`shared_models/` directory removed.

- [x] hvac (5 models)
- [x] novelties (9 models)
- [x] lifesafety (84 models)
- [x] technology (33 models)
- [x] powergrid (42 models)
- [x] lighting (105 models)
- [x] trafficaccessories (63 models)
- [x] trafficsignals (100 models, includes backplates/ subdir)

## Phase 2: Consolidate wrapper models (SUBSTANTIALLY COMPLETE)

Eliminated simple parent+texture wrapper models by moving texture info into Forge
blockstate `defaults.textures` blocks.

| Subsystem | Wrappers Eliminated | Remaining | Notes |
|---|---|---|---|
| HVAC | 30 | 0 | Complete |
| Novelties | 17 | 0 | Complete |
| Building Materials | 74 | 90 | 90 = fence/slab/stairs multipart variants |
| Power Grid | 44 | 0 | Complete |
| Technology | 34 | 0 | Complete |
| Lighting | 98 | 2 | 2 complex models with extra keys |
| Life Safety | 121 | 392 | Many shared parent models + variant models |
| Traffic Accessories | 219 | 65 | Backplate tilt variant models |
| Traffic Signals | 26 | 101 | Crosswalk variants, inventory models, mclaglowair |
| **Total** | **663** | **649** | |

### Models that cannot be consolidated

1. **Multipart variants** (90 in buildingmaterials): fence_post, fence_inventory,
   slab_top, stairs_inner, stairs_outer — required by multipart blockstates
2. **Blockstate variant models** (65 in trafficaccessories, ~40 in trafficsignals):
   backplate tilt/fitted variants, crosswalk state variants — referenced by specific
   blockstate variant entries, not `defaults`
3. **Complex models** (2 in lighting, ~350 in lifesafety): models with `elements`,
   `display`, or other keys beyond simple parent+textures
4. **Transparent placeholder** (mclaglowair): used as submodel by ~113 signal blockstates
5. **OBJ model** (signalcontroller.obj + .mtl): Forge OBJ loader doesn't support subdirs

## Current Directory Structure (updated 2026-04-01)

```
models/block/
├── signalcontroller.obj           (OBJ - must stay in flat dir)
├── signalcontroller.mtl
├── buildingmaterials/
│   ├── *_fence_post.json          (90 multipart variant models)
│   └── (no shared_models — inline geometry)
├── hvac/
│   └── shared_models/             (5 shared 3D geometry models)
├── lifesafety/
│   ├── *.json                     (61 fire alarm/emergency wrapper models)
│   └── shared_models/             (84 shared 3D geometry models)
├── lighting/
│   ├── *.json                     (3 block models incl. GE smart node)
│   └── shared_models/             (105 shared 3D geometry models)
├── novelties/
│   └── shared_models/             (9 shared 3D geometry models)
├── powergrid/
│   └── shared_models/             (42 shared 3D geometry models)
├── technology/
│   └── shared_models/             (33 shared 3D geometry models)
├── trafficaccessories/
│   ├── *.json                     (335 border/tilt/pole variant models)
│   └── shared_models/             (63 shared 3D geometry models)
├── trafficsignals/
│   ├── *.json                     (101 crosswalk/signal/inventory models)
│   └── shared_models/             (100 shared models + backplates/)
└── trafficsigns/
    ├── *.json                     (60 sign pole/signpost/sign shape models)
    └── (signs use blockstate texture overrides on these shared shapes)
```
