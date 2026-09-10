package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A barrier wall that joins the ones beside it, so a line of them reads as one wall.
 *
 * <p>Covers both the water-filled plastic barrier and the precast concrete one. They are
 * different shapes and different jobs, but a wall is a wall: each spans its cell exactly, and the
 * piece that comes and goes is the cap over its open end, drawn only where nothing connects.</p>
 *
 * <p>Without that, two segments meeting on the cell boundary put their end faces in one plane and
 * z-fight along the whole joint. Leaving a hair of gap instead avoids the z-fight for nothing,
 * but then a run reads as a line of separate blocks rather than as a wall.</p>
 *
 * @version 1.0
 * @see WorkZoneJoins
 * @since 2026.9
 */
public class BlockWorkZoneWall extends BlockWorkZoneDeviceDiagonal {

  /**
   * Whether a matching wall adjoins the model's left-hand end.
   *
   * @since 1.0
   */
  public static final PropertyBool CONNECT_LEFT = WorkZoneJoins.CONNECT_LEFT;

  /**
   * Whether a matching wall adjoins the model's right-hand end.
   *
   * @since 1.0
   */
  public static final PropertyBool CONNECT_RIGHT = WorkZoneJoins.CONNECT_RIGHT;

  /**
   * Whether this device closes the gap to a DIAGONAL neighbour on its right.
   *
   * @see WorkZoneJoins#DIAG_FILL
   * @since 1.0
   */
  public static final PropertyBool DIAG_FILL = WorkZoneJoins.DIAG_FILL;

  /**
   * Constructs a {@link BlockWorkZoneWall} instance.
   *
   * @param registryName the registry name of the wall
   * @param boundingBox  the bounding box of the wall, in block space
   *
   * @since 1.0
   */
  public BlockWorkZoneWall(String registryName, AxisAlignedBB boundingBox) {
    super(registryName, boundingBox);
    setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, DirectionEight.N)
        .withProperty(CONNECT_LEFT, false)
        .withProperty(CONNECT_RIGHT, false)
        .withProperty(DIAG_FILL, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, CONNECT_LEFT, CONNECT_RIGHT, DIAG_FILL);
  }

  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess access,
      @Nonnull BlockPos pos) {
    return WorkZoneJoins.resolve(this, state, access, pos);
  }
}
