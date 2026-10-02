package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialRun;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A board hung on the wall -- the whiteboard, the chalkboard, the cork notice board -- that
 * joins with the same board beside it and above or below it into one board of any size. As a
 * {@link BlockResidentialRun} it knows whether the board goes on to its left and right; it also
 * knows whether it goes on {@link #UP} and {@link #DOWN} (the same board, facing the same way).
 * The multipart blockstate ({@code gen_furniture_office.py}) draws the frame only round the
 * outside of the whole board: the top rail where nothing is above, the bottom rail and the tray
 * where nothing is below, a side stile where the run stops, and the surface carried on to the
 * block's edge wherever the board continues, so it reads as one sheet.
 *
 * <p>All four are actual state, nothing is stored: a board placed before boards stacked keeps
 * its metadata (the facing) and simply joins.</p>
 *
 * @since 2026.10
 */
public class BlockWallBoard extends BlockResidentialRun {

  /** The board goes on above. */
  public static final PropertyBool UP = PropertyBool.create("up");
  /** The board goes on below. */
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /** The box facing north for each of up (2) and down (1). */
  private final AxisAlignedBB[] boxes = new AxisAlignedBB[4];

  /**
   * Constructs a wall board.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing alone, in sixteenths
   */
  public BlockWallBoard(String registryName, int[] box) {
    super(registryName, box, false);
    for (int i = 0; i < 4; i++) {
      boolean up = (i & 2) != 0;
      boolean down = (i & 1) != 0;
      boxes[i] = new AxisAlignedBB(box[0] / 16.0, down ? 0 : box[1] / 16.0, box[2] / 16.0,
          box[3] / 16.0, up ? 1 : box[4] / 16.0, box[5] / 16.0);
    }
    setDefaultState(getDefaultState().withProperty(UP, false).withProperty(DOWN, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, RIGHT, UP, DOWN);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(UP, continues(world, pos, facing, EnumFacing.UP))
        .withProperty(DOWN, continues(world, pos, facing, EnumFacing.DOWN));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boxes[(state.getValue(UP) ? 2 : 0) | (state.getValue(DOWN) ? 1 : 0)];
  }
}
