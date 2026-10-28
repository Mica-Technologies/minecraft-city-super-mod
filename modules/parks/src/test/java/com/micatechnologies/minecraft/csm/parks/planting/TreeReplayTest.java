package com.micatechnologies.minecraft.csm.parks.planting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Issue #320: the {@code spot} a {@code /csmtree} report gives each tree plants that very tree
 * again, block for block, and {@code best=} keeps the least trimmed of its tries.
 */
class TreeReplayTest {

  private static final BlockPos BASE = new BlockPos(-854, 70, -5965);

  /** A wall three blocks east and a road clearance two blocks south, so crowns get trimmed. */
  private static final TreeSpace TIGHT = rel -> rel.getX() >= 3
      || (rel.getZ() >= 2 && rel.getY() > 0) ? TreeSpace.Cell.BLOCKED : TreeSpace.Cell.FREE;

  private static TreeBatch parse(String preset, String spot, String... options) {
    String[] args = new String[options.length + 2];
    args[0] = preset;
    args[1] = spot;
    System.arraycopy(options, 0, args, 2, options.length);
    return TreeBatch.parseCompact(args, (axis, text) -> Integer.parseInt(text));
  }

  private static long seedOf(TreeBatch batch, TreeBatch.Spot spot) {
    return spot.seed != null ? spot.seed : TreeBatch.seedAt(batch.seed, spot.pos);
  }

  private static TreeBatch.Grown grow(TreeBatch batch, TreeSpace space) {
    TreeBatch.Spot spot = batch.spots.get(0);
    return batch.grow(spot, spot.pos, seedOf(batch, spot), space);
  }

  private static Map<BlockPos, String> blocks(TreePlan plan) {
    Map<BlockPos, String> m = new TreeMap<>();
    for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
      TreePlan.Part p = e.getValue();
      m.put(e.getKey(), p.kind + " " + p.block + " " + p.axis);
    }
    return m;
  }

  private static String at(BlockPos p) {
    return p.getX() + "," + p.getY() + "," + p.getZ();
  }

  @Test
  void theReportedSpotReplaysTheTree() {
    for (TreePreset preset : TreePreset.values()) {
      for (int seed = 1; seed <= 6; seed++) {
        for (String heading : new String[]{null, "ne", "186", "@-840:-5960"}) {
          TreeBatch first = heading == null ? parse(preset.id, at(BASE), "seed=" + seed, "dry")
              : parse(preset.id, at(BASE), "seed=" + seed, "heading=" + heading, "dry");
          TreeBatch.Grown a = grow(first, TIGHT);
          String spot = TreeBatch.replaySpot(BASE, a);
          // Other options left on the call must not change the replayed tree
          TreeBatch again = parse(preset.id, spot, "seed=999", "heading=w", "dry");
          TreeBatch.Grown b = grow(again, TIGHT);
          String what = preset.id + " seed=" + seed + " heading=" + heading + " -> " + spot;
          assertEquals(a.seed, b.seed, what);
          assertEquals(TreeBatch.yawOf(a.heading), TreeBatch.yawOf(b.heading), what);
          assertEquals(a.plan.getTrimmed(), b.plan.getTrimmed(), what);
          assertEquals(blocks(a.plan), blocks(b.plan), what);
        }
      }
    }
  }

  @Test
  void theSeedAndYawAloneDoNotAlwaysReplayADrawnHeading() {
    // Why the spot is reported: a drawn heading is the first number taken from the seed, so the
    // same seed with its yaw given grows from a different place in the stream (the #320 report)
    int differ = 0;
    for (TreePreset preset : TreePreset.values()) {
      TreeBatch first = parse(preset.id, at(BASE), "seed=4", "dry");
      TreeBatch.Grown a = grow(first, TIGHT);
      TreeBatch byYaw = parse(preset.id,
          at(BASE) + "," + TreeBatch.yawOf(a.heading) + "," + a.seed, "dry");
      if (!blocks(a.plan).equals(blocks(grow(byYaw, TIGHT).plan))) {
        differ++;
      }
    }
    assertTrue(differ > 0, "expected the seed and yaw to miss some trees");
  }

  @Test
  void bestKeepsTheLeastTrimmedTryAndReplaysIt() {
    for (TreePreset preset : TreePreset.values()) {
      TreeBatch one = parse(preset.id, at(BASE), "seed=4", "dry");
      TreeBatch.Grown plain = grow(one, TIGHT);
      TreeBatch bestOne = parse(preset.id, at(BASE), "seed=4", "best=1", "dry");
      assertEquals(blocks(plain.plan), blocks(grow(bestOne, TIGHT).plan), preset.id + " best=1");

      TreeBatch eight = parse(preset.id, at(BASE), "seed=4", "best=8", "dry");
      TreeBatch.Grown pick = grow(eight, TIGHT);
      long s0 = TreeBatch.seedAt(4, BASE);
      for (int i = 0; i < 8; i++) {
        TreeBatch.Grown g = TreeBatch.grow(preset, null, BASE, TreeBatch.trySeed(s0, i), TIGHT);
        if (g.fits()) {
          assertTrue(pick.fits(), preset.id);
          assertTrue(pick.plan.getTrimmed() <= g.plan.getTrimmed(), preset.id + " try " + i);
        }
      }
      assertTrue(!plain.fits() || pick.plan.getTrimmed() <= plain.plan.getTrimmed(), preset.id);

      // The pick's spot plants it without best; a spot with its own seed ignores best
      TreeBatch replay = parse(preset.id, TreeBatch.replaySpot(BASE, pick), "best=8", "dry");
      assertEquals(blocks(pick.plan), blocks(grow(replay, TIGHT).plan), preset.id + " replay");
    }
  }

  @Test
  void bestIsBounded() {
    assertThrows(IllegalArgumentException.class,
        () -> parse("elm", "0,64,0", "best=0"));
    assertThrows(IllegalArgumentException.class,
        () -> parse("elm", "0,64,0", "best=" + (TreeBatch.MAX_BEST + 1)));
    assertEquals(TreeBatch.MAX_BEST,
        parse("elm", "0,64,0", "best=" + TreeBatch.MAX_BEST).best);
  }
}
