package com.micatechnologies.minecraft.csm.trafficaccessories;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The orange mesh safety fence, which joins a diagonal neighbour but needs nothing at a square
 * one.
 *
 * <p>It is the only device in this tab shaped that way, and the reason is its own geometry: the
 * panel spans the whole cell and the stakes are inset from the edges, so a run laid square to the
 * grid is already one continuous fence with a pair of stakes at each joint, exactly as a real one
 * has. There is nothing to take off and nothing to add.</p>
 *
 * <p>A DIAGONAL run is a different matter. Cell centres a diagonal step apart are sqrt(2) cells
 * apart while the panel is one cell wide, so the run leaves about four tenths of a cell of daylight
 * at every joint. That is what {@link WorkZoneJoins#DIAG_FILL} closes, and it is the only join
 * property this block carries — {@link WorkZoneJoins#resolveFill} rather than the full
 * {@link WorkZoneJoins#resolve} the barriers and barricades use.</p>
 *
 * @version 1.0
 * @see BlockWorkZoneWall
 * @since 2026.9
 */
public class BlockWorkZoneFence extends BlockWorkZoneDeviceDiagonal {

  /**
   * Whether this fence closes the gap to a DIAGONAL neighbour on its right.
   *
   * @see WorkZoneJoins#DIAG_FILL
   * @since 1.0
   */
  public static final PropertyBool DIAG_FILL = WorkZoneJoins.DIAG_FILL;

  /**
   * Constructs a safety fence.
   *
   * @param registryName the block's registry name
   * @param boundingBox  its bounding box at the default facing
   *
   * @since 1.0
   */
  public BlockWorkZoneFence(String registryName, AxisAlignedBB boundingBox) {
    super(registryName, boundingBox);
    setDefaultState(this.blockState.getBaseState().withProperty(DIAG_FILL, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, DIAG_FILL);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Resolved from the neighbours every time rather than stored, so breaking a fence out of the
   * middle of a run closes the two halves up without anything having to be notified.</p>
   */
  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess access,
      @Nonnull BlockPos pos) {
    return WorkZoneJoins.resolveFill(this, super.getActualState(state, access, pos), access, pos);
  }
}
