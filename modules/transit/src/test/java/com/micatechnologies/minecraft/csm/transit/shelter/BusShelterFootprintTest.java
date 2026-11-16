package com.micatechnologies.minecraft.csm.transit.shelter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * What the shelter item places: the full footprint, four long and two deep, and the compact one,
 * three long and one deep, both with the front row in the block clicked, whichever way the
 * shelter faces.
 */
class BusShelterFootprintTest {

  private static final BlockPos AT = new BlockPos(100, 64, -20);

  @Test
  void fullFootprintFacingSouth() {
    // facing south: the open front toward +z, the back row at -z; the placer, south of it and
    // looking north, has +x on their right
    List<BlockPos> bases = BlockBusShelter.footprint(AT, EnumFacing.SOUTH, false);
    assertEquals(BlockBusShelter.FOOT_LENGTH * BlockBusShelter.FOOT_DEPTH, bases.size());
    Set<BlockPos> expected = new HashSet<>();
    for (int i = -1; i <= 2; i++) {
      for (int row = 0; row <= 1; row++) {
        expected.add(AT.add(i, 0, -row));
      }
    }
    assertEquals(expected, new HashSet<>(bases));
  }

  @Test
  void compactFootprintIsOneRowOfThree() {
    for (EnumFacing facing : EnumFacing.HORIZONTALS) {
      List<BlockPos> bases = BlockBusShelter.footprint(AT, facing, true);
      assertEquals(BlockBusShelter.COMPACT_LENGTH, bases.size(), facing.toString());
      assertTrue(bases.contains(AT), facing.toString());
      EnumFacing along = facing.rotateY();
      Set<BlockPos> expected = new HashSet<>();
      for (int i = -1; i <= 1; i++) {
        expected.add(AT.offset(along, i));
      }
      assertEquals(expected, new HashSet<>(bases), facing.toString());
      for (BlockPos p : bases) {
        // one row: nothing in front of or behind the block clicked
        assertEquals(0, facing.getAxis() == EnumFacing.Axis.X ? p.getX() - AT.getX()
            : p.getZ() - AT.getZ(), facing.toString());
      }
    }
  }

  @Test
  void everyFootprintHoldsTheClickedBlockOnce() {
    for (EnumFacing facing : EnumFacing.HORIZONTALS) {
      for (boolean compact : new boolean[]{false, true}) {
        List<BlockPos> bases = BlockBusShelter.footprint(AT, facing, compact);
        assertEquals(bases.size(), new HashSet<>(bases).size());
        assertTrue(bases.contains(AT));
      }
    }
  }
}
