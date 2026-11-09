package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Without a world: the large rooftop unit's footprint for every facing, each part finding its
 * master and its own cell (two units side by side included), each cell's share of the unit's
 * boxes, the part's metadata, and the generated blockstate drawing each cell with that cell's
 * slice of the model.
 */
class PackagedRtuTest {

  private static BlockHvacPackagedRtu master;
  private static BlockHvacPackagedRtu masterBlack;
  private static BlockHvacPackagedRtuPart part;
  private static final BlockHvacPackagedRtu.Size SIZE = BlockHvacPackagedRtu.LARGE;

  @BeforeAll
  static void bootstrap() {
    Bootstrap.register();
    master = new BlockHvacPackagedRtu("hvac_rtu_large_test");
    masterBlack = new BlockHvacPackagedRtu("hvac_rtu_large_test_black");
    part = new BlockHvacPackagedRtuPart("hvac_rtu_large_part_test");
  }

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

  /** A whole unit, as its master fills it, into {@code blocks}. */
  private static void unit(Map<BlockPos, IBlockState> blocks, BlockHvacPackagedRtu m,
      BlockPos at, EnumFacing facing) {
    blocks.put(at, m.getDefaultState().withProperty(BlockHvacPackagedRtu.FACING, facing));
    IBlockState ps = part.getDefaultState().withProperty(BlockHvacPackagedRtuPart.FACING, facing)
        .withProperty(BlockHvacPackagedRtuPart.FINISH,
            BlockHvacPackagedRtuPart.Finish.values()[m.getFinish()]);
    for (BlockPos c : BlockHvacPackagedRtu.partCells(SIZE, at, facing)) {
      blocks.put(c, ps);
    }
  }

  @Test
  void theFootprintIsThreeByTwoByFive() {
    assertEquals(29, SIZE.cells.size());
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      Set<BlockPos> seen = new HashSet<>();
      seen.add(BlockPos.ORIGIN);
      int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
      for (BlockPos c : BlockHvacPackagedRtu.partCells(SIZE, BlockPos.ORIGIN, f)) {
        assertTrue(seen.add(c), "every cell once");
        minX = Math.min(minX, c.getX());
        maxX = Math.max(maxX, c.getX());
        minY = Math.min(minY, c.getY());
        maxY = Math.max(maxY, c.getY());
        minZ = Math.min(minZ, c.getZ());
        maxZ = Math.max(maxZ, c.getZ());
      }
      int across = f.getAxis() == EnumFacing.Axis.Z ? maxX - minX + 1 : maxZ - minZ + 1;
      int along = f.getAxis() == EnumFacing.Axis.Z ? maxZ - minZ + 1 : maxX - minX + 1;
      assertEquals(3, across, f + ": three wide");
      assertEquals(5, along, f + ": five long");
      assertEquals(0, minY);
      assertEquals(1, maxY);
    }
  }

  @Test
  void theUnitReachesAwayFromItsFront() {
    // facing north (front toward -z, the placer), the unit runs back to +z
    for (BlockPos c : BlockHvacPackagedRtu.partCells(SIZE, BlockPos.ORIGIN, EnumFacing.NORTH)) {
      assertTrue(c.getZ() >= 0);
    }
    for (BlockPos c : BlockHvacPackagedRtu.partCells(SIZE, BlockPos.ORIGIN, EnumFacing.EAST)) {
      assertTrue(c.getX() <= 0, "facing east it runs back to -x");
    }
  }

  @Test
  void everyPartFindsItsMasterAndCell() {
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      Map<BlockPos, IBlockState> blocks = new HashMap<>();
      BlockPos at = new BlockPos(10, 64, -7);
      unit(blocks, master, at, f);
      IBlockAccess w = world(blocks);
      List<BlockPos> cells = BlockHvacPackagedRtu.partCells(SIZE, at, f);
      for (int i = 0; i < cells.size(); i++) {
        BlockPos c = cells.get(i);
        assertEquals(at, BlockHvacPackagedRtu.findMaster(w, c), f + " cell " + i);
        IBlockState actual = part.getActualState(blocks.get(c), w, c);
        assertEquals(i, (int) actual.getValue(SIZE.cell), f + " cell " + i);
      }
    }
  }

  @Test
  void unitsSideBySideKeepTheirOwnParts() {
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      Map<BlockPos, IBlockState> blocks = new HashMap<>();
      BlockPos a = BlockPos.ORIGIN;
      BlockPos b = a.add(BlockHvacPackagedRtu.turn(3, 0, 0, f));   // the next one along
      BlockPos c = a.add(BlockHvacPackagedRtu.turn(0, 0, 5, f));   // the next one behind
      unit(blocks, master, a, f);
      unit(blocks, master, b, f);
      unit(blocks, masterBlack, c, f);
      IBlockAccess w = world(blocks);
      for (BlockPos m : new BlockPos[]{a, b, c}) {
        for (BlockPos p : BlockHvacPackagedRtu.partCells(SIZE, m, f)) {
          assertEquals(m, BlockHvacPackagedRtu.findMaster(w, p), f + " " + p);
        }
      }
    }
  }

  @Test
  void anOrphanDrawsNothing() {
    Map<BlockPos, IBlockState> blocks = new HashMap<>();
    BlockPos p = new BlockPos(0, 64, 0);
    blocks.put(p, part.getDefaultState());
    IBlockAccess w = world(blocks);
    assertEquals(null, BlockHvacPackagedRtu.findMaster(w, p));
    assertEquals(SIZE.none(), (int) part.getActualState(blocks.get(p), w, p)
        .getValue(SIZE.cell));
  }

  @Test
  void everyCellHasItsShareOfTheUnit() {
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      AxisAlignedBB m = BlockHvacPackagedRtu.share(SIZE, BlockPos.ORIGIN, f, BlockPos.ORIGIN);
      assertNotNull(m);
      for (BlockPos c : BlockHvacPackagedRtu.partCells(SIZE, BlockPos.ORIGIN, f)) {
        AxisAlignedBB s = BlockHvacPackagedRtu.share(SIZE, BlockPos.ORIGIN, f, c);
        assertNotNull(s, f + " " + c);
        assertTrue(s.minX >= -1e-9 && s.maxX <= 1 + 1e-9 && s.minY >= -1e-9
            && s.maxY <= 1 + 1e-9 && s.minZ >= -1e-9 && s.maxZ <= 1 + 1e-9, "inside its cell");
      }
    }
  }

  @Test
  void metadataHoldsFacingAndFinish() {
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      for (BlockHvacPackagedRtuPart.Finish fin : BlockHvacPackagedRtuPart.Finish.values()) {
        IBlockState s = part.getDefaultState().withProperty(BlockHvacPackagedRtuPart.FACING, f)
            .withProperty(BlockHvacPackagedRtuPart.FINISH, fin);
        IBlockState back = part.getStateFromMeta(part.getMetaFromState(s));
        assertEquals(f, back.getValue(BlockHvacPackagedRtuPart.FACING));
        assertEquals(fin, back.getValue(BlockHvacPackagedRtuPart.FINISH));
      }
    }
    assertEquals(0, master.getFinish());
    assertEquals(2, masterBlack.getFinish());
    assertEquals(1, BlockHvacPackagedRtu.finishOf("hvac_rtu_large_silver"));
  }

  @Test
  void theBlockstateDrawsEachCellWithItsOwnSlice() throws Exception {
    Path p = Paths.get("modules/hvac/src/main/resources/assets/csm/blockstates/"
        + "hvac_rtu_large_part.json");
    JsonObject cellMap;
    try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
      cellMap = new JsonParser().parse(r).getAsJsonObject().getAsJsonObject("variants")
          .getAsJsonObject("cell");
    }
    assertEquals(SIZE.cells.size() + 1, cellMap.size(), "a model for every cell, and none");
    for (int i = 0; i < SIZE.cells.size(); i++) {
      int[] c = SIZE.cells.get(i);
      String want = String.format("rtu_large_x%s_y%s_z%s.obj", n(c[0]), n(c[1]), n(c[2]));
      String model = cellMap.getAsJsonObject(String.valueOf(i)).get("model").getAsString();
      assertTrue(model.endsWith("/" + want), "cell " + i + ": " + model + " is not " + want);
    }
  }

  private static String n(int v) {
    return v < 0 ? "m" + -v : String.valueOf(v);
  }
}
