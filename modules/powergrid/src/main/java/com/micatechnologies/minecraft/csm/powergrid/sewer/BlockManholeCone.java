package com.micatechnologies.minecraft.csm.powergrid.sewer;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The cone that tops a manhole: from the riser ring's size down to the cast iron frame that
 * Streetscape's manhole cover sits on, a block above it. It is a manhole section for the rings
 * below it (so they are not open-topped) and climbs like them, but has no ends of its own to
 * choose: four states, its facing (the steps' side).
 *
 * @since 2026.9
 */
public class BlockManholeCone extends BlockStackedSection {

  /** Its walls, facing north: thicker than a ring's, since the opening narrows to the frame. */
  private static final AxisAlignedBB[] WALLS = {
      new AxisAlignedBB(0, 0, 0, 1, 1, 0.17), new AxisAlignedBB(0, 0, 0.83, 1, 1, 1),
      new AxisAlignedBB(0, 0, 0, 0.17, 1, 1), new AxisAlignedBB(0.83, 0, 0, 1, 1, 1)};
  private static final AxisAlignedBB[] CUT_WALLS = {
      new AxisAlignedBB(0, 0, 0.83, 1, 1, 1), new AxisAlignedBB(0, 0, 0.5, 0.17, 1, 1),
      new AxisAlignedBB(0.83, 0, 0.5, 1, 1, 1)};

  /**
   * @param registryName its registry name
   * @param cutaway      whether it is the back half only
   */
  public BlockManholeCone(String registryName, boolean cutaway) {
    super(registryName, "manhole", true, cutaway);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state;
  }

  @Override
  protected boolean hasBench(IBlockState state, IBlockAccess world, BlockPos pos) {
    return false;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    EnumFacing facing = state.getValue(FACING);
    for (AxisAlignedBB wall : isCutaway() ? CUT_WALLS : WALLS) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(wall, facing));
    }
  }
}
