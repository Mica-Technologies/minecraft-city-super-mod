package com.micatechnologies.minecraft.csm.trafficaccessories;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A steel road plate, which joins the plates around it on all four sides.
 *
 * <p>Plates are laid in a PATCH rather than a line, which is why this joins by world direction
 * rather than by the ends of something that faces a way. A plate has no facing at all; "left"
 * and "right" would mean nothing to it.</p>
 *
 * <p>The piece that comes and goes is the rim on each side. Inside a patch a rim is not there to
 * be seen, and two rims in one plane z-fight anyway; without dropping them a patch reads as a
 * grid of separate tiles rather than as one steel surface.</p>
 *
 * @version 1.0
 * @see WorkZoneJoins
 * @since 2026.9
 */
public class BlockWorkZonePlate extends BlockWorkZoneDevice {

  /** Whether another plate abuts this one's north side. */
  public static final PropertyBool CONNECT_NORTH = WorkZoneJoins.CONNECT_NORTH;

  /** Whether another plate abuts this one's south side. */
  public static final PropertyBool CONNECT_SOUTH = WorkZoneJoins.CONNECT_SOUTH;

  /** Whether another plate abuts this one's west side. */
  public static final PropertyBool CONNECT_WEST = WorkZoneJoins.CONNECT_WEST;

  /** Whether another plate abuts this one's east side. */
  public static final PropertyBool CONNECT_EAST = WorkZoneJoins.CONNECT_EAST;

  /**
   * Constructs a {@link BlockWorkZonePlate} instance.
   *
   * @param registryName the registry name of the plate
   * @param boundingBox  the bounding box of the plate, in block space
   *
   * @since 1.0
   */
  public BlockWorkZonePlate(String registryName, AxisAlignedBB boundingBox) {
    super(registryName, boundingBox);
    setDefaultState(this.blockState.getBaseState()
        .withProperty(CONNECT_NORTH, false)
        .withProperty(CONNECT_SOUTH, false)
        .withProperty(CONNECT_WEST, false)
        .withProperty(CONNECT_EAST, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, CONNECT_NORTH, CONNECT_SOUTH, CONNECT_WEST,
        CONNECT_EAST);
  }

  /**
   * Overridden method which stores nothing in metadata.
   *
   * <p>All four properties are worked out from the neighbours every time they are asked for, so
   * none of them has anything to survive in metadata. Saying so is not optional: the base class
   * does not override this, and the vanilla default THROWS for any state it was not told how to
   * encode, which fails the whole mod's pre-initialization rather than this one block.</p>
   *
   * @param state the block state
   *
   * @return zero, always
   *
   * @since 1.0
   */
  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  /**
   * Overridden method which reads nothing back out of metadata.
   *
   * @param meta the metadata value
   *
   * @return the default state
   *
   * @see #getMetaFromState(IBlockState)
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess access,
      @Nonnull BlockPos pos) {
    return WorkZoneJoins.resolveSides(this, state, access, pos);
  }
}
