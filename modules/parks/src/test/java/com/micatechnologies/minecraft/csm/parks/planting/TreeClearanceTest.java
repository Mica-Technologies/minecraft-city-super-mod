package com.micatechnologies.minecraft.csm.parks.planting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class TreeClearanceTest {

  private static final String ROAD = "roadmod:road_block_fine";
  private static final String WALK = "roadmod:sidewalk_clean";
  private static final String GRASS = "minecraft:grass";
  private static final String PAINT = "roadmod:line_white_straight_full";
  private static final String AIR = "minecraft:air";

  /**
   * A street running north-south at ground level y 64: road at x 0..5, a walk at x 6..7, a grass
   * strip at x 8..9 and a lawn beyond. Paint on the road at x 2, a lamp post on the walk at x 7.
   */
  private static final class Street implements TreeClearance.Ground {

    final Map<BlockPos, String> extra = new HashMap<>();
    final Map<BlockPos, Boolean> opaque = new HashMap<>();

    @Override
    public String id(BlockPos pos) {
      String e = extra.get(pos);
      if (e != null) {
        return e;
      }
      if (pos.getY() > 64) {
        return AIR;
      }
      if (pos.getY() < 64) {
        return "minecraft:dirt";
      }
      int x = pos.getX();
      return x <= 5 ? ROAD : x <= 7 ? WALK : GRASS;
    }

    @Override
    public boolean seeThrough(BlockPos pos) {
      Boolean o = opaque.get(pos);
      if (o != null) {
        return !o;
      }
      return pos.getY() > 64; // the ground is full blocks; what stands on it is not
    }
  }

  private static final BlockPos TRUNK = new BlockPos(8, 65, 0);

  @Test
  void keepsTheRoadAndWalkHeightsClear() {
    TreeClearance c = TreeClearance.parse("roadmod:road*:12,roadmod:sidewalk*:6", null);
    Street s = new Street();
    assertTrue(c.keepsClear(new BlockPos(3, 65, 0), TRUNK, s));
    assertTrue(c.keepsClear(new BlockPos(3, 76, 0), TRUNK, s), "12 above the road");
    assertFalse(c.keepsClear(new BlockPos(3, 77, 0), TRUNK, s), "13 above the road");
    assertTrue(c.keepsClear(new BlockPos(6, 70, 0), TRUNK, s), "6 above the walk");
    assertFalse(c.keepsClear(new BlockPos(6, 71, 0), TRUNK, s), "7 above the walk");
    assertFalse(c.keepsClear(new BlockPos(9, 66, 0), TRUNK, s), "over grass");
  }

  @Test
  void neverKeepsTheTrunkColumnClear() {
    TreeClearance c = TreeClearance.parse("minecraft:grass:12", null);
    Street s = new Street();
    assertFalse(c.keepsClear(new BlockPos(8, 66, 0), TRUNK, s));
    assertTrue(c.keepsClear(new BlockPos(9, 66, 0), TRUNK, s));
  }

  @Test
  void looksThroughPaintAndPostsButNotAFullBlock() {
    TreeClearance c = TreeClearance.parse("roadmod:road*:12,roadmod:sidewalk*:6", null);
    Street s = new Street();
    s.extra.put(new BlockPos(2, 65, 0), PAINT);
    assertTrue(c.keepsClear(new BlockPos(2, 66, 0), TRUNK, s), "paint is looked through");
    for (int y = 65; y <= 68; y++) {
      s.extra.put(new BlockPos(7, y, 0), "csm:blackmetal_fence");
    }
    assertTrue(c.keepsClear(new BlockPos(7, 69, 0), TRUNK, s), "a lamp post is looked through");
    s.extra.put(new BlockPos(4, 70, 0), "minecraft:stone");
    s.opaque.put(new BlockPos(4, 70, 0), true);
    assertFalse(c.keepsClear(new BlockPos(4, 71, 0), TRUNK, s), "a full block is a new ground");
  }

  @Test
  void skipPatternsLookThroughFullBlocks() {
    Street s = new Street();
    s.extra.put(new BlockPos(4, 65, 0), "roadmod:kerb_stone");
    s.opaque.put(new BlockPos(4, 65, 0), true);
    TreeClearance plain = TreeClearance.parse("roadmod:road*:12", null);
    assertFalse(plain.keepsClear(new BlockPos(4, 66, 0), TRUNK, s));
    TreeClearance skipping = TreeClearance.parse("roadmod:road*:12", "roadmod:kerb*");
    assertTrue(skipping.keepsClear(new BlockPos(4, 66, 0), TRUNK, s));
  }

  @Test
  void parsesPatterns() {
    Street s = new Street();
    TreeClearance any = TreeClearance.parse("*sidewalk*:6", "");
    assertTrue(any.keepsClear(new BlockPos(6, 66, 0), TRUNK, s));
    TreeClearance bare = TreeClearance.parse("grass:3", null);
    assertTrue(bare.keepsClear(new BlockPos(9, 67, 0), TRUNK, s), "no namespace is minecraft's");
    assertTrue(TreeClearance.parse(null, null).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> TreeClearance.parse("roadmod:road", null));
    assertThrows(IllegalArgumentException.class, () -> TreeClearance.parse("road:x", null));
  }

  /** A grown tree on the grass strip never puts a block in the road or walk clearances. */
  @Test
  void aGrownTreeStaysOutOfTheClearances() {
    TreeClearance c = TreeClearance.parse("roadmod:road*:12,roadmod:sidewalk*:6", null);
    Street s = new Street();
    for (TreePreset preset : TreePreset.values()) {
      for (long seed = 0; seed < 4; seed++) {
        TreeSpace space = rel -> {
          BlockPos at = TRUNK.add(rel);
          if (at.getY() <= 64) {
            return TreeSpace.Cell.BLOCKED;
          }
          return c.keepsClear(at, TRUNK, s) ? TreeSpace.Cell.BLOCKED : TreeSpace.Cell.FREE;
        };
        TreePlan plan = TreeGenerators.grow(preset, Math.PI, new Random(seed), space);
        for (BlockPos rel : plan.parts().keySet()) {
          BlockPos at = TRUNK.add(rel);
          assertFalse(c.keepsClear(at, TRUNK, s), preset + " seed " + seed + " grew into the"
              + " clearance at " + at);
        }
      }
    }
  }

  @Test
  void batchParsesTheCompactForm() {
    TreeBatch b = TreeBatch.parseCompact(new String[]{"cherryyoshino",
        "10,64,20,n,7;11,64,30", ";12,64,40,@0:40", "clear=roadmod:road*:12", "seed=5", "dry"},
        (axis, text) -> Integer.parseInt(text));
    assertEquals(3, b.spots.size());
    assertEquals(TreePreset.YOSHINO_CHERRY, b.spots.get(0).preset);
    assertEquals(Long.valueOf(7), b.spots.get(0).seed);
    assertEquals("n", b.spots.get(0).heading);
    assertNull(b.spots.get(1).heading);
    assertEquals(Long.valueOf(5), b.seed);
    assertTrue(b.dry);
    assertFalse(b.clearance.isEmpty());
  }

  @Test
  void batchParsesJson() {
    TreeBatch b = TreeBatch.parseJson("{\"preset\":\"plane\",\"clear\":[\"roadmod:road*:12\"],"
        + "\"trees\":[{\"x\":1,\"y\":64,\"z\":2},{\"x\":3,\"y\":64,\"z\":4,\"preset\":62,"
        + "\"heading\":90,\"seed\":11}]}");
    assertEquals(2, b.spots.size());
    assertEquals(TreePreset.PLANE, b.spots.get(0).preset);
    assertEquals(TreePreset.values()[62], b.spots.get(1).preset);
    assertEquals("90", b.spots.get(1).heading);
    assertThrows(IllegalArgumentException.class, () -> TreeBatch.parseJson("{\"trees\":[{\"x\":1,"
        + "\"y\":2,\"z\":3}]}"), "no preset anywhere");
    assertThrows(IllegalArgumentException.class, () -> TreeBatch.preset("nosuchtree"));
  }

  @Test
  void headingsRoundTripThroughYaw() {
    for (int yaw = 0; yaw < 360; yaw += 15) {
      Double h = TreeBatch.heading(String.valueOf(yaw), BlockPos.ORIGIN);
      assertNotNull(h);
      assertEquals(yaw, TreeBatch.yawOf(h));
    }
    assertEquals(180, TreeBatch.yawOf(TreeBatch.heading("n", BlockPos.ORIGIN)));
    assertEquals(0, TreeBatch.yawOf(TreeBatch.heading("s", BlockPos.ORIGIN)));
    assertEquals(90, TreeBatch.yawOf(TreeBatch.heading("w", BlockPos.ORIGIN)));
    assertEquals(270, TreeBatch.yawOf(TreeBatch.heading("@10:0", BlockPos.ORIGIN)), "east");
    assertNull(TreeBatch.heading("-", BlockPos.ORIGIN));
  }

  @Test
  void aSeedGrowsTheSameTree() {
    long seed = TreeBatch.seedAt(42, new BlockPos(100, 64, -30));
    assertEquals(seed, TreeBatch.seedAt(42, new BlockPos(100, 64, -30)));
    TreePlan a = TreeGenerators.grow(TreePreset.YOSHINO_CHERRY, 1.0, new Random(seed),
        TreeSpace.OPEN);
    TreePlan b = TreeGenerators.grow(TreePreset.YOSHINO_CHERRY, 1.0, new Random(seed),
        TreeSpace.OPEN);
    assertEquals(a.parts().keySet(), b.parts().keySet());
  }
}
