# Shared Models Restructure & Blockstate Texture Migration Plan

Move shared models from `models/block/shared_models/<subsystem>/` to
`models/block/<subsystem>/shared_models/` to match the texture folder structure.
Also consolidate child-parent model chains into Forge blockstate texture overrides
where possible.

**Created:** 2026-03-31
**Status:** In progress

---

## Resume Prompt

> Progress doc: `assets/docs/agent_progress/SHARED_MODELS_RESTRUCTURE_PLAN.md`
>
> **Goal:** Move 441 shared models from `shared_models/<subsystem>/` to
> `<subsystem>/shared_models/`. Then consolidate duplicate child-parent model files
> by moving texture info into Forge blockstate texture overrides.
>
> **Cross-subsystem analysis done:** hvac, lifesafety, novelties, powergrid, technology
> have 0-1 cross-subsystem refs (safe to move). Traffic accessories has 36 and traffic
> signals has 841 cross-subsystem refs (need careful handling).

---

## Phase 1: Move shared_models to tab-specific folders

Move `models/block/shared_models/<subsystem>/` → `models/block/<subsystem>/shared_models/`

### Cross-subsystem analysis

| Subsystem | Models | Cross-subsystem refs | Safe to move directly? |
|---|---|---|---|
| hvac | 5 | 0 | Yes |
| lifesafety | 84 | 0 | Yes |
| lighting | 105 | 11 | Mostly (11 refs from other tabs) |
| novelties | 9 | 0 | Yes |
| powergrid | 42 | 1 | Mostly (1 ref from other tab) |
| technology | 33 | 0 | Yes |
| trafficaccessories | 63 | 36 | Partially (backplate models ref'd by signals) |
| trafficsignals | 100 | 841 | Complex (heavily cross-referenced) |

### Checklist

- [ ] Move hvac shared_models (5 models, 0 cross-refs)
- [ ] Move novelties shared_models (9 models, 0 cross-refs)
- [ ] Move lifesafety shared_models (84 models, 0 cross-refs)
- [ ] Move technology shared_models (33 models, 0 cross-refs)
- [ ] Move powergrid shared_models (42 models, 1 cross-ref)
- [ ] Move lighting shared_models (105 models, 11 cross-refs)
- [ ] Move trafficaccessories shared_models (63 models, 36 cross-refs)
- [ ] Move trafficsignals shared_models (100 models, 841 cross-refs)
- [ ] Remove empty shared_models/ directory
- [ ] Build verify after each move
- [ ] In-game test

### Reference update pattern

All references follow the pattern:
```
"parent": "csm:block/shared_models/<subsystem>/<model>"
```
Must become:
```
"parent": "csm:block/<subsystem>/shared_models/<model>"
```

Also blockstates with direct model refs:
```
"model": "csm:shared_models/<subsystem>/<model>"
```
Must become:
```
"model": "csm:<subsystem>/shared_models/<model>"
```

## Phase 2: Consolidate child-parent model chains (future)

Many blocks have a per-block model file that simply sets a parent shared model and
overrides textures. These can be eliminated by moving the texture info into the
Forge blockstate's `defaults.textures` block, reducing the model count significantly.

**Example before:**
```
blockstates/art1.json:     "model": "csm:hvac/air_return_1_white"
models/block/hvac/air_return_1_white.json:
  "parent": "csm:block/hvac/shared_models/large_circle_vent"
  "textures": { "all": "csm:blocks/hvac/air_return_1_white" }
```

**Example after (blockstate handles textures directly):**
```
blockstates/art1.json:
  "model": "csm:hvac/shared_models/large_circle_vent"
  "textures": { "all": "csm:blocks/hvac/air_return_1_white" }
```
→ `models/block/hvac/air_return_1_white.json` can be deleted.

This is a larger effort and should be done subsystem-by-subsystem after Phase 1
is complete.
