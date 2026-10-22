package com.micatechnologies.minecraft.csm.technology.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The Micafi mounts: a pole device's eight directions folded onto four stored facings (the
 * metadata of a device placed before the diagonals must keep its meaning), the yaw to direction
 * steps, the diagonal selection box, the wall arm's aims (saved by ordinal), and the state counts
 * and metadata round trips of the three blocks.
 */
class MicafiMountsTest {

  private static final double[] DISH = {2.75, 2.75, 3.75, 13.25, 13.25, 16.0};

  @BeforeAll
  static void bootstrap() {
    Bootstrap.register();
  }

  @Test
  void diagonalsFoldOntoFourFacings() {
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      DirectionEight d = BlockMicafiDevice.PoleFitted.diagonalOf(f);
      assertTrue(d.isDiagonal(), f.toString());
      assertEquals(f, BlockMicafiDevice.PoleFitted.storedFacing(d), d.toString());
      // a cardinal is stored as itself, so an old device's metadata keeps its meaning
      assertEquals(f.getHorizontalIndex(), BlockMicafiDevice.PoleFitted.storedFacing(
          DirectionEight.values()[f.getHorizontalIndex()]).getHorizontalIndex());
    }
    assertEquals(DirectionEight.NE, BlockMicafiDevice.PoleFitted.diagonalOf(EnumFacing.NORTH));
  }

  @Test
  void yawPointsTheDeviceAtThePlacer() {
    // looking south (yaw 0) the placer stands north of the device, which faces north
    assertEquals(DirectionEight.N, BlockMicafiDevice.PoleFitted.aimFromYaw(0F));
    assertEquals(DirectionEight.NE, BlockMicafiDevice.PoleFitted.aimFromYaw(45F));
    assertEquals(DirectionEight.E, BlockMicafiDevice.PoleFitted.aimFromYaw(90F));
    assertEquals(DirectionEight.SW, BlockMicafiDevice.PoleFitted.aimFromYaw(-135F));
    assertEquals(DirectionEight.NW, BlockMicafiDevice.PoleFitted.aimFromYaw(675F));
    assertEquals(DirectionEight.N, BlockMicafiDevice.PoleFitted.aimFromYaw(-20F));
  }

  @Test
  void diagonalBoxStaysInsideTheBlock() {
    AxisAlignedBB north = new AxisAlignedBB(DISH[0] / 16, DISH[1] / 16, DISH[2] / 16,
        DISH[3] / 16, DISH[4] / 16, DISH[5] / 16);
    AxisAlignedBB d = BlockMicafiDevice.PoleFitted.diagonalBox(north);
    assertTrue(d.minX >= 0 && d.maxX <= 1 && d.minZ >= 0 && d.maxZ <= 1, d.toString());
    assertEquals(north.minY, d.minY, 1e-9);
    assertEquals(north.maxY, d.maxY, 1e-9);
    // turned about the pole to the south-west, it reaches that corner of the block
    assertEquals(0.0, d.minX, 1e-9);
    assertEquals(1.0, d.maxZ, 1e-9);
  }

  @Test
  void aimsAreAppendOnly() {
    String[] saved = {"centre", "left", "right", "centre_down", "left_down", "right_down"};
    String[] names = new String[MicafiAim.values().length];
    for (int i = 0; i < names.length; i++) {
      names[i] = MicafiAim.values()[i].getName();
    }
    assertArrayEquals(saved, java.util.Arrays.copyOf(names, saved.length));
    assertEquals(MicafiAim.CENTRE, MicafiAim.fromOrdinal(99));
    assertEquals(MicafiAim.CENTRE, MicafiAim.RIGHT_DOWN.next());
  }

  @Test
  void poleDeviceStatesAndMeta() {
    BlockMicafiDevice.PoleFitted dish = new BlockMicafiDevice.PoleFitted("test_micafi_pole_dish",
        DISH, MicafiLight.LINK);
    // 4 facings x 4 lights x 3 pole fits x straight or diagonal
    assertEquals(96, dish.getBlockState().getValidStates().size());
    for (int meta = 0; meta < 16; meta++) {
      IBlockState s = dish.getStateFromMeta(meta);
      assertEquals(meta, dish.getMetaFromState(s));
      assertEquals(EnumFacing.byHorizontalIndex(meta & 3),
          s.getValue(BlockMicafiDevice.FACING));
      assertEquals(false, s.getValue(BlockMicafiDevice.PoleFitted.DIAGONAL));
    }
    IBlockState ne = dish.getDefaultState().withProperty(BlockMicafiDevice.FACING,
        EnumFacing.NORTH).withProperty(BlockMicafiDevice.PoleFitted.DIAGONAL, true);
    assertTrue(dish.variantOf(ne).startsWith("facing=northeast,link="), dish.variantOf(ne));
    assertTrue(!dish.variantOf(ne).contains("diagonal"));
  }

  @Test
  void aimedDeviceStatesAndMeta() {
    BlockMicafiDevice.Aimed beam = new BlockMicafiDevice.Aimed("test_micafi_aimed",
        new double[]{3.75, 1.25, 2.75, 12.25, 13.75, 16.0}, MicafiLight.LINK);
    // 4 facings x 4 lights x 6 aims; no pole fit on a wall arm
    assertEquals(96, beam.getBlockState().getValidStates().size());
    for (int meta = 0; meta < 16; meta++) {
      assertEquals(meta, beam.getMetaFromState(beam.getStateFromMeta(meta)));
    }
  }

  @Test
  void hangerStatesAndMeta() {
    BlockCableTrayHanger hanger = new BlockCableTrayHanger("test_cable_tray_hanger");
    assertEquals(32, hanger.getBlockState().getValidStates().size());
    for (int meta = 0; meta < 2; meta++) {
      assertEquals(meta, hanger.getMetaFromState(hanger.getStateFromMeta(meta)));
    }
  }
}
