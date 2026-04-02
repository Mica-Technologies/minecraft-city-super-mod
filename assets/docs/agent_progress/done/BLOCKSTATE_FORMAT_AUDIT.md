# Blockstate Format Audit

**Created:** 2026-03-31
**Status:** COMPLETE — no migration needed

## Results

| Format | Count | Percentage |
|--------|-------|------------|
| Forge (`forge_marker: 1`) | 1,298 | 98.9% |
| Multipart | 15 | 1.1% |
| Vanilla | 0 | 0% |

**All 1,298 non-multipart blockstates already use Forge format.**

## Multipart Blockstates (15)

These are all metal fence blocks that require multipart format for fence connection
logic. They cannot be converted to Forge format without losing the conditional
`when`/`apply` connection rules that Minecraft needs for fences.

```
blackmetal_fence, bluemetal_fence, coppermetal_fence, greenmetal_fence,
iridescentmetal_fence, lightbluemetal_fence, limemetal_fence, magentametal_fence,
orangemetal_fence, pinkmetal_fence, purplemetal_fence, redmetal_fence,
silvermetal_fence, whitemetal_fence, yellowmetal_fence
```

These 15 blocks also account for the "UNKNOWN" subsystem in the asset dependency report
(along with their matching slab/stairs variants from `AbstractBlockSetBasic`). All belong
to `buildingmaterials`.

## Conclusion

No blockstate format migration is needed. The previous Model & Blockstate Cleanup project
(Phases 1-2) already converted all eligible blockstates to Forge format.
