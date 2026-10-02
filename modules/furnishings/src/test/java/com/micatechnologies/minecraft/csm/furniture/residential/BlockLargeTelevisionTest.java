package com.micatechnologies.minecraft.csm.furniture.residential;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The big TVs' state: the block can be constructed (vanilla asks every state for its metadata
 * from inside Block's constructor, before the TV's own fields are set), the state count is what
 * the blockstate files are written for, and the metadata (facing and column) round-trips for
 * every state and every metadata value.
 */
class BlockLargeTelevisionTest {

  @BeforeAll
  static void bootstrap() {
    Bootstrap.register();
  }

  @ParameterizedTest
  @CsvSource({"3, 2, true", "3, 2, false", "4, 2, true", "4, 2, false"})
  void constructsAndRoundTripsMeta(int cols, int rows, boolean wall) {
    BlockLargeTelevision tv = new BlockLargeTelevision(
        "test_big_tv_" + cols + "x" + rows + (wall ? "_wall" : "_stand"),
        new int[]{0, 0, 9, 16 * cols, 16 * rows, 16}, cols, rows, wall);

    // facing x cell x channel (x two rests on a stand)
    int expected = 4 * cols * rows * TvChannel.values().length * (wall ? 1 : 2);
    assertEquals(expected, tv.getBlockState().getValidStates().size());
    assertEquals(0, tv.getMetaFromState(tv.getDefaultState()) >> 2);

    for (IBlockState state : tv.getBlockState().getValidStates()) {
      int meta = tv.getMetaFromState(state);
      IBlockState back = tv.getStateFromMeta(meta);
      assertEquals(state.getValue(BlockLargeTelevision.FACING),
          back.getValue(BlockLargeTelevision.FACING));
      assertEquals(meta, tv.getMetaFromState(back));
      assertEquals(meta >> 2, BlockLargeTelevision.colOfMeta(meta, cols));
    }
    for (int meta = 0; meta < 16; meta++) {
      IBlockState state = tv.getStateFromMeta(meta);
      int col = BlockLargeTelevision.colOfMeta(meta, cols);
      assertEquals(BlockLargeTelevision.packMeta(meta & 3, col), tv.getMetaFromState(state));
      assertEquals(EnumFacing.byHorizontalIndex(meta & 3),
          state.getValue(BlockLargeTelevision.FACING));
    }
  }
}
