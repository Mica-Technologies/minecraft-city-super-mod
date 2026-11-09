package com.micatechnologies.minecraft.csm.codeutils.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The floodlights' beam projection ({@link CsmLightupAir#projectBeam}) in a real light engine
 * ({@link LightTestWorld}): where its light-up air goes, that it heals and clears cleanly, and
 * what it costs. {@link #fiftyFloodsCostAndCoverage} times fifty floodlights switched together,
 * one light per beam against a row of three and against Immersive Engineering's thirteen-ray
 * fan, and prints the numbers (in the test report's standard output); they decided which the
 * floodlights use.
 */
class CsmLightupAirBeamTest {

  private static final int FLOOR = 63;
  /** A wall-mounted flood's aim: north and 35 degrees down. */
  private static final double PITCH = Math.toRadians(35);
  private static final double DY = -Math.sin(PITCH);
  private static final double DZ = -Math.cos(PITCH);
  private static final int FLOODS = 50;
  private static final int SPACING_X = 6;
  private static final int MOUNT_Y = 72;

  private static Block air;

  @BeforeAll
  static void setUp() {
    Bootstrap.register();
    air = BlockLightupAir.get() != null ? BlockLightupAir.get() : new BlockLightupAir();
  }

  /** The test world, with a stone wall along x at z = 0 (the floods' mounting) to above them. */
  private static LightTestWorld world() {
    return new LightTestWorld(-3, -4, (FLOODS * SPACING_X) / 16 + 2, 1, FLOOR,
        (x, y, z) -> z == 0 && y <= MOUNT_Y + 3 && x >= -8 && x <= FLOODS * SPACING_X + 8);
  }

  private static BlockPos flood(int i) {
    return new BlockPos(i * SPACING_X, MOUNT_Y, -1);
  }

  @Test
  void beamLightsTheCellInFrontOfWhatItHits() {
    LightTestWorld w = world();
    BlockPos pos = flood(3);
    assertEquals(1, CsmLightupAir.projectBeam(true, w, pos, 0, DY, DZ, 24, 1, 4));
    List<BlockPos> path = CsmLightupAir.beamPath(w, pos, 0, DY, DZ, 24);
    assertNotNull(path);
    BlockPos end = path.get(path.size() - 1);
    assertEquals(air, w.getBlockState(end).getBlock());
    // just above the floor, out in front of the wall where a 35 degree beam lands
    assertEquals(FLOOR + 1, end.getY());
    assertTrue(end.getZ() <= -10 && end.getZ() >= -14, end.toString());
    assertEquals(pos.getX(), end.getX());
    // the heal: lit again, nothing more is placed
    assertEquals(0, CsmLightupAir.projectBeam(true, w, pos, 0, DY, DZ, 24, 1, 4));
    assertEquals(1, CsmLightupAir.projectBeam(false, w, pos, 0, DY, DZ, 24, 1, 4));
    assertTrue(w.isAirBlock(end));
  }

  @Test
  void rowLiesAlongTheFloorUnderTheBeamAndClearsWhole() {
    LightTestWorld w = world();
    BlockPos pos = flood(5);
    assertEquals(3, CsmLightupAir.projectBeam(true, w, pos, 0, DY, DZ, 24, 3, 4));
    List<BlockPos> path = CsmLightupAir.beamPath(w, pos, 0, DY, DZ, 24);
    List<BlockPos> targets = CsmLightupAir.beamTargets(path, pos, 3, 4);
    List<BlockPos> lights = CsmLightupAir.beamLights(w, path, pos, DY, 3, 4);
    assertEquals(3, targets.size());
    assertEquals(3, lights.size());
    // the lit spot is where the beam lands; its own light-up air does not stop the beam
    assertEquals(targets.get(0), path.get(path.size() - 1));
    assertEquals(targets.get(0), lights.get(0));
    for (int i = 0; i < lights.size(); i++) {
      assertEquals(air, w.getBlockState(lights.get(i)).getBlock());
      // every one on the floor, under its place on the beam
      assertEquals(FLOOR + 1, lights.get(i).getY());
      assertEquals(targets.get(i).getZ(), lights.get(i).getZ());
      if (i > 0) {
        assertTrue(Math.sqrt(targets.get(i).distanceSq(targets.get(i - 1))) >= 4 - 1e-9);
      }
      assertTrue(Math.sqrt(targets.get(i).distanceSq(pos)) >= CsmLightupAir.BEAM_MIN_DISTANCE);
    }
    // the heal places nothing more
    assertEquals(0, CsmLightupAir.projectBeam(true, w, pos, 0, DY, DZ, 24, 3, 4));
    assertEquals(3, CsmLightupAir.projectBeam(false, w, pos, 0, DY, DZ, 24, 3, 4));
    for (BlockPos t : lights) {
      assertTrue(w.isAirBlock(t));
    }
  }

  @Test
  void beamThatHitsNothingOrLeavesTheLoadedWorldPlacesNothing() {
    LightTestWorld w = world();
    // straight up into open sky
    assertNull(CsmLightupAir.beamPath(w, new BlockPos(10, 70, -10), 0, 1, 0, 24));
    assertEquals(0, CsmLightupAir.projectBeam(true, w, new BlockPos(10, 70, -10), 0, 1, 0, 24, 1,
        4));
    // level, west, off the edge of the loaded chunks before it meets anything
    assertNull(CsmLightupAir.beamPath(w, new BlockPos(-40, 70, -10), -1, 0, 0, 24));
    // a ground uplight under an overhang lights the cell beneath it
    BlockPos slab = new BlockPos(30, 75, -20);
    w.setBlockState(slab, Blocks.STONE.getDefaultState(), 2);
    BlockPos up = new BlockPos(30, FLOOR + 1, -20);
    assertEquals(1, CsmLightupAir.projectBeam(true, w, up, 0, 1, 0, 24, 1, 4));
    assertEquals(air, w.getBlockState(slab.down()).getBlock());
  }

  /** One of the projections the decision was between. */
  private interface Projection {

    /** Lights ({@code on}) or darkens fixture {@code i}; returns light-up air changed. */
    int apply(World w, int i, boolean on);
  }

  /** Immersive Engineering's floodlight, as its source reads: 13 rays, a light every 3rd cell. */
  private static int fan(World w, BlockPos pos, boolean on) {
    int changed = 0;
    List<double[]> rays = new ArrayList<>();
    rays.add(new double[]{0, DY, DZ});
    for (int ring = 1; ring <= 2; ring++) {
      int n = ring == 1 ? 4 : 8;
      double spread = Math.toRadians(12 * ring);
      for (int k = 0; k < n; k++) {
        double a = 2 * Math.PI * k / n;
        double sx = Math.sin(spread) * Math.cos(a);
        double sv = Math.sin(spread) * Math.sin(a);
        // perpendiculars to the aim: x, and (0, cos, -sin) turned with the pitch
        rays.add(new double[]{sx, DY * Math.cos(spread) + sv * Math.cos(PITCH),
            DZ * Math.cos(spread) - sv * Math.sin(PITCH)});
      }
    }
    for (double[] r : rays) {
      List<BlockPos> path = CsmLightupAir.beamPath(w, pos, r[0], r[1], r[2], 32);
      if (path == null) {
        continue;
      }
      for (int i = 2; i < path.size(); i += 3) {
        BlockPos c = path.get(i);
        if (on && w.isAirBlock(c)) {
          w.setBlockState(c, air.getDefaultState(), 3);
          changed++;
        } else if (!on && w.getBlockState(c).getBlock() == air) {
          w.setBlockToAir(c);
          changed++;
        }
      }
    }
    return changed;
  }

  @Test
  void fiftyFloodsCostAndCoverage() {
    String[] names = {"street light column (baseline)", "beam, 1 light", "beam, row of 2",
        "beam, row of 3", "beam, row of 4", "IE fan, 13 rays"};
    Projection[] projections = {
        (w, i, on) -> CsmLightupAir.projectColumn(on, w,
            new BlockPos(i * SPACING_X, MOUNT_Y, -12), 0, 0) ? 1 : 0,
        (w, i, on) -> CsmLightupAir.projectBeam(on, w, flood(i), 0, DY, DZ, 24, 1, 4),
        (w, i, on) -> CsmLightupAir.projectBeam(on, w, flood(i), 0, DY, DZ, 24, 2, 4),
        (w, i, on) -> CsmLightupAir.projectBeam(on, w, flood(i), 0, DY, DZ, 24, 3, 4),
        (w, i, on) -> CsmLightupAir.projectBeam(on, w, flood(i), 0, DY, DZ, 24, 4, 4),
        (w, i, on) -> fan(w, flood(i), on)};
    StringBuilder out = new StringBuilder(
        "\nFifty floodlights switched together (server light engine only; best of 5):\n");
    int[] lit8 = new int[names.length];
    for (int p = 0; p < projections.length; p++) {
      long bestOn = Long.MAX_VALUE;
      long bestOff = Long.MAX_VALUE;
      int placed = 0;
      for (int run = 0; run < 5; run++) {
        LightTestWorld w = world();
            long t0 = System.nanoTime();
        placed = 0;
        for (int i = 0; i < FLOODS; i++) {
          // the fixture's own light, as a lit flood gives light 15 from its own cell
          BlockPos self = p == 0 ? new BlockPos(i * SPACING_X, MOUNT_Y, -12) : flood(i);
          w.setBlockState(self, air.getDefaultState(), 3);
          placed += projections[p].apply(w, i, true);
        }
        long t1 = System.nanoTime();
        int[] counts = litFloor(w);
        lit8[p] = counts[0];
        if (run == 4) {
          out.append(String.format(Locale.ROOT,
              "  %-32s light-up air %4d (%5.1f a flood); floor cells lit >= 8: %5d (%5.1f a flood),"
                  + " >= 12: %5d (%5.1f a flood)%n", names[p], placed, placed / (double) FLOODS,
              counts[0], counts[0] / (double) FLOODS, counts[1], counts[1] / (double) FLOODS));
        }
        long t2 = System.nanoTime();
        for (int i = 0; i < FLOODS; i++) {
          projections[p].apply(w, i, false);
          BlockPos self = p == 0 ? new BlockPos(i * SPACING_X, MOUNT_Y, -12) : flood(i);
          w.setBlockToAir(self);
        }
        long t3 = System.nanoTime();
        bestOn = Math.min(bestOn, t1 - t0);
        bestOff = Math.min(bestOff, t3 - t2);
        assertEquals(0, litFloor(w)[0], names[p] + " left light behind");
      }
      out.append(String.format(Locale.ROOT, "  %-32s on %7.2f ms (%6.3f ms a flood), off %7.2f ms%n",
          "", bestOn / 1e6, bestOn / 1e6 / FLOODS, bestOff / 1e6));
    }
    System.out.println(out);
    // a row lights more of the ground than one light, and every beam more than nothing
    assertTrue(lit8[3] > lit8[1], Arrays.toString(lit8));
    assertTrue(lit8[1] > 0, Arrays.toString(lit8));
  }

  /**
   * One flood on its own, so no pool overlaps another: how much floor each projection lights at
   * three mounting heights. A row's cells on the beam, in the air, against "floor row" ({@code f}), each
   * dropped onto the floor below it ({@link CsmLightupAir#settle}), which is what the beam does.
   */
  @Test
  void oneFloodCoverage() {
    StringBuilder out = new StringBuilder("\nOne flood, floor cells lit >= 8 / >= 12:\n");
    int[] heights = {6, 10, 14};
    int[] counts = {1, 2, 3, 4};
    for (int h : heights) {
      out.append(String.format(Locale.ROOT, "  mounted %2d above the floor:", h));
      for (int n : counts) {
        for (boolean drop : new boolean[]{false, true}) {
          if (n == 1 && drop) {
            continue;
          }
          LightTestWorld w = new LightTestWorld(-3, -4, 3, 1, FLOOR,
              (x, y, z) -> z == 0 && y <= FLOOR + h + 3 && x >= -8 && x <= 8);
          BlockPos pos = new BlockPos(0, FLOOR + h, -1);
          w.setBlockState(pos, air.getDefaultState(), 3);
          List<BlockPos> path = CsmLightupAir.beamPath(w, pos, 0, DY, DZ, 32);
          assertNotNull(path);
          for (BlockPos c : CsmLightupAir.beamTargets(path, pos, n, 4)) {
            BlockPos at = drop ? CsmLightupAir.settle(w, c) : c;
            w.setBlockState(at, air.getDefaultState(), 3);
          }
          int[] lit = litFloor(w);
          out.append(String.format(Locale.ROOT, "  %d%s %4d/%4d", n, drop ? "f" : " ", lit[0],
              lit[1]));
        }
      }
      out.append('\n');
    }
    System.out.println(out);
  }

  /** Floor cells (the air just above it) in front of the wall at block light 8+ and 12+. */
  private static int[] litFloor(World w) {
    int a = 0;
    int b = 0;
    BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
    for (int x = -16; x <= FLOODS * SPACING_X + 16; x++) {
      for (int z = -40; z <= -1; z++) {
        p.setPos(x, FLOOR + 1, z);
        int l = w.getLightFor(EnumSkyBlock.BLOCK, p);
        if (l >= 8) {
          a++;
        }
        if (l >= 12) {
          b++;
        }
      }
    }
    return new int[]{a, b};
  }
}
