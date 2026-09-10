package com.micatechnologies.minecraft.csm.trafficaccessories;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A barricade that joins onto the ones beside it, so a line of them reads as one long run.
 *
 * <p>Each barricade always draws its rails and the upright on its left-hand edge. The pieces
 * that come and go are the ENDS: the overhang past the upright, the cap over the rail ends, and
 * on the right-hand side the upright itself. They are drawn only where nothing connects.</p>
 *
 * <p>That asymmetry — left upright always, right upright only when free — is what leaves exactly
 * one upright on each joint. Drawing both and hiding both at a seam would leave the joint with
 * no leg at all; drawing both and hiding neither would leave two back to back.</p>
 *
 * <p>Barricades only join along their own length, and only to a barricade of the same kind facing
 * the same way. The striping slopes toward the side traffic should pass, so a keep-left joined to
 * a keep-right would contradict itself in the middle of the run.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWorkZoneBarricade extends AbstractBlockWorkZoneBarricade {

  /**
   * Whether a matching barricade adjoins the model's left-hand end.
   *
   * @since 1.0
   */
  public static final PropertyBool CONNECT_LEFT = PropertyBool.create("connectleft");

  /**
   * Whether a matching barricade adjoins the model's right-hand end.
   *
   * @since 1.0
   */
  public static final PropertyBool CONNECT_RIGHT = PropertyBool.create("connectright");

  /**
   * Constructs a {@link BlockWorkZoneBarricade} instance.
   *
   * @param registryName the registry name of the barricade
   * @param topY         the height of its uprights, in 1/16 block units
   * @param boundingBox  the bounding box of the barricade, in block space
   *
   * @since 1.0
   */
  public BlockWorkZoneBarricade(String registryName, float topY, AxisAlignedBB boundingBox) {
    super(registryName, topY, boundingBox);
    setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(CONNECT_LEFT, false)
        .withProperty(CONNECT_RIGHT, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, CONNECT_LEFT, CONNECT_RIGHT);
  }

  /**
   * Works out which ends this barricade needs, from its neighbours.
   *
   * <p>Connection is resolved here rather than stored, so breaking a barricade out of the middle
   * of a run closes up the two halves without anything having to be notified.</p>
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   *
   * @return the state with its connections set
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess access,
      @Nonnull BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    return state
        .withProperty(CONNECT_LEFT, joinsAt(access, pos, facing, facing.rotateYCCW()))
        .withProperty(CONNECT_RIGHT, joinsAt(access, pos, facing, facing.rotateY()));
  }

  /**
   * Gets whether the block one step in the given direction is a barricade this one should join.
   *
   * @param access    the block access
   * @param pos       this barricade's position
   * @param facing    this barricade's facing
   * @param direction the direction to look in
   *
   * @return true if the neighbour is the same barricade facing the same way
   *
   * @since 1.0
   */
  private boolean joinsAt(IBlockAccess access, BlockPos pos, EnumFacing facing,
      EnumFacing direction) {
    IBlockState neighbour = access.getBlockState(pos.offset(direction));
    return neighbour.getBlock() == this
        && neighbour.getPropertyKeys().contains(FACING)
        && neighbour.getValue(FACING) == facing;
  }
}
