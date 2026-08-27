# Testing CSM under a constrained heap

The dev client runs with `-Xmx6G`, which hides how much memory the mod actually needs. This init
script caps it so the floor can be found and re-checked after changes that add blocks, models or
caches.

    ./gradlew -I dev-env-utils/gradle/lowmem.gradle -Dcsm.testHeap=2G runClient

A later `-Xmx` wins in HotSpot, so appending overrides the default without editing `build.gradle`.

## Measured floor (2026-08-27, CSM + JEI + TheOneProbe + FAWE + WorldEdit)

| heap | result |
| --- | --- |
| 1024 MB | **fails** — `OutOfMemoryError` in `ModelLoader.setupModelRegistry` |
| 1536 MB | **fails** — same place |
| 2048 MB | starts; 1441 MB used at the main menu, 1465 MB after touring 100 intersections |

The failure is in **baking the block model registry at startup**, before a world exists and before
any render cache holds anything. That is CSM's 1,550-plus blocks, and it is the whole memory story:

* ~1.4 GB is resident at the main menu, with no world loaded.
* Loading a 100-intersection scene and filling every display-list cache to its bound added **24 MB**
  on top of that.
* The Java side of the display-list caches is under 1 MB at full occupancy (computed from the
  structures: ~128 bytes per position plus ~72 per cached state, bounded at 1024 positions).

So shrinking the render caches under memory pressure would save under a megabyte while 1.4 GB of
baked models sits untouched. If CSM's memory use needs to come down, the model registry is where
the work is, not the caches.

Numbers are *used* heap, which includes garbage the JVM has not collected; treat them as an upper
bound on the live set rather than an exact figure.
