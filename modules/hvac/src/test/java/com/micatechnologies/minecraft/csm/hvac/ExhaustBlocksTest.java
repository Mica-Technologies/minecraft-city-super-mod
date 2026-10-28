package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Without a world: the exposed duct's joins (#314 item 3, stacked axis-y ducts), the lab exhaust
 * fan's footprint for every facing and a part finding its master from every cell, and the part's
 * metadata.
 */
class ExhaustBlocksTest {

  private static BlockExposedDuct round;
  private static BlockExposedDuct roundWhite;
  private static BlockHvacLabExhaustFan lab;
  private static BlockHvacExhaustFanPart part;

  @BeforeAll
  static void bootstrap() {
    Bootstrap.register();
    round = new BlockExposedDuct("duct_round_test_galvanized");
    roundWhite = new BlockExposedDuct("duct_round_test_white");
    lab = new BlockHvacLabExhaustFan("hvac_lab_exhaust_fan_test");
    part = new BlockHvacExhaustFanPart();
  }

  /** A world of the given blocks, air everywhere else. */
  private static IBlockAccess world(Map<BlockPos, IBlockState> blocks) {
    return (IBlockAccess) Proxy.newProxyInstance(IBlockAccess.class.getClassLoader(),
        new Class<?>[]{IBlockAccess.class}, (p, m, a) -> {
          if ("getBlockState".equals(m.getName())) {
            return blocks.getOrDefault(a[0], Blocks.AIR.getDefaultState());
          }
          if ("isAirBlock".equals(m.getName())) {
            return !blocks.containsKey(a[0]);
          }
          throw new UnsupportedOperationException(m.getName());
        });
  }

  private static IBlockState duct(BlockExposedDuct b, Axis axis) {
    return b.getStateFromMeta(axis.ordinal());
  }

  @Test
  void stackedVerticalDuctsJoinUpAndDown() {
    Map<BlockPos, IBlockState> m = new HashMap<>();
    BlockPos a = new BlockPos(0, 64, 0);
    m.put(a, duct(round, Axis.Y));
    m.put(a.up(), duct(roundWhite, Axis.Y)); // another colour of the same kind
    m.put(a.up(2), duct(round, Axis.Y));
    IBlockAccess w = world(m);
    for (BlockPos p : new BlockPos[]{a, a.up(), a.up(2)}) {
      IBlockState s = m.get(p).getActualState(w, p);
      assertTrue(s.getValue(BlockExposedDuct.UP), "up at " + p);
      assertTrue(s.getValue(BlockExposedDuct.DOWN), "down at " + p);
      assertFalse(s.getValue(BlockExposedDuct.NORTH));
      assertFalse(s.getValue(BlockExposedDuct.ROD));
    }
    // metadata 1 is axis y, as /setblock ... duct_round_galvanized 1 places it
    assertEquals(Axis.Y, round.getStateFromMeta(1).getValue(BlockExposedDuct.AXIS));
  }

  @Test
  void levelRunsJoinAlongTheirAxisAndTurnIntoATee() {
    Map<BlockPos, IBlockState> m = new HashMap<>();
    BlockPos a = new BlockPos(0, 64, 0);
    m.put(a, duct(round, Axis.X));
    m.put(a.east(), duct(round, Axis.X));
    IBlockAccess w = world(m);
    IBlockState s = m.get(a).getActualState(w, a);
    assertTrue(s.getValue(BlockExposedDuct.EAST));
    assertTrue(s.getValue(BlockExposedDuct.WEST), "an open end reaches out");
    assertFalse(s.getValue(BlockExposedDuct.UP));
    // a riser pointing down at a level run: the run reaches up to it
    m.put(a.up(), duct(round, Axis.Y));
    s = m.get(a).getActualState(world(m), a);
    assertTrue(s.getValue(BlockExposedDuct.UP));
    IBlockState riser = m.get(a.up()).getActualState(world(m), a.up());
    assertTrue(riser.getValue(BlockExposedDuct.DOWN));
  }

  @Test
  void partMetadataRoundTrips() {
    for (IBlockState s : part.getBlockState().getValidStates()) {
      IBlockState back = part.getStateFromMeta(part.getMetaFromState(s));
      assertEquals(s, back);
    }
  }

  @Test
  void theLabFanFillsTwoByTwoByFiveAndEveryPartFindsItsMaster() {
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      BlockPos master = new BlockPos(10, 64, 10);
      List<BlockPos> cells = BlockHvacLabExhaustFan.partCells(master, f);
      assertEquals(19, cells.size());
      Set<BlockPos> all = new HashSet<>(cells);
      all.add(master);
      assertEquals(20, all.size(), "distinct cells facing " + f);
      int minX = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      for (BlockPos p : all) {
        minX = Math.min(minX, p.getX());
        maxX = Math.max(maxX, p.getX());
        assertTrue(p.getY() >= 64 && p.getY() <= 68);
      }
      assertEquals(1, maxX - minX, "two wide facing " + f);

      Map<BlockPos, IBlockState> m = new HashMap<>();
      m.put(master, lab.getDefaultState().withProperty(BlockHvacLabExhaustFan.FACING, f));
      for (int i = 0; i < cells.size(); i++) {
        int[] c = BlockHvacLabExhaustFan.CELLS[i];
        m.put(cells.get(i), part.getDefaultState().withProperty(BlockHvacExhaustFanPart.FACING, f)
            .withProperty(BlockHvacExhaustFanPart.PIECE,
                BlockHvacLabExhaustFan.pieceAt(c[0], c[1], c[2])));
      }
      IBlockAccess w = world(m);
      for (BlockPos p : cells) {
        assertEquals(master, BlockHvacLabExhaustFan.findMaster(w, p), p + " facing " + f);
        assertNotNull(part.getBlockBoundingBox(m.get(p), w, p));
      }
      // the nozzle stands over the master: three drawing parts in its column
      int drawing = 0;
      for (int i = 0; i < cells.size(); i++) {
        if (m.get(cells.get(i)).getValue(BlockHvacExhaustFanPart.PIECE)
            != BlockHvacExhaustFanPart.Piece.NONE) {
          drawing++;
          assertEquals(master.getX(), cells.get(i).getX());
          assertEquals(master.getZ(), cells.get(i).getZ());
        }
      }
      assertEquals(3, drawing);
      // with the master gone a part is an orphan
      m.remove(master);
      assertNull(BlockHvacLabExhaustFan.findMaster(world(m), cells.get(0)));
    }
  }
}
