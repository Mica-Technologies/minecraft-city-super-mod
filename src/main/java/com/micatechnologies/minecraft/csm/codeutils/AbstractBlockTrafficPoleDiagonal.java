package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract block class which provides the same common methods and properties as
 * {@link AbstractBlockTrafficPole} with the addition of diagonal traffic pole mounting properties
 *
 * @version 1.0
 * @see Block
 * @see AbstractBlock
 * @see AbstractBlockTrafficPole
 */
public abstract class AbstractBlockTrafficPoleDiagonal extends AbstractBlockTrafficPole {

  /**
   * Cached combined ignore-block array for diagonal poles, computed lazily on first use.
   */
  private volatile Class<?>[] cachedCombinedIgnoreBlockDiag;

  /**
   * The east mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_NORTH = PropertyBool.create("mountnorth");

  /**
   * The west mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_SOUTH = PropertyBool.create("mountsouth");

  /**
   * Creates a new {@link BlockStateContainer} for the block with the required property for
   * rotation.
   *
   * @return a new {@link BlockStateContainer} for the block
   *
   * @see Block#createBlockState()
   * @since 1.0
   */
  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN,
        MOUNT_NORTH, MOUNT_SOUTH);
  }

  /**
   * Updates the block state based on the presence of adjacent blocks, considering the block's
   * facing direction.
   * <p>
   * This method is called to retrieve the actual state of the block, taking into account its
   * environment. It checks for the presence of blocks in each direction (east, west, north, south,
   * up, down) relative to the block's facing direction. The method uses helper methods to determine
   * if there are blocks adjacent to the block and updates the block state with properties
   * indicating the presence of these blocks.
   * </p>
   *
   * @param state   The current block state.
   * @param worldIn The world in which the block is located.
   * @param pos     The position of the block in the world.
   *
   * @return The updated block state with properties indicating the presence of adjacent blocks.
   */
  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state,
      @NotNull
      IBlockAccess worldIn,
      @NotNull
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);

    if (cachedCombinedIgnoreBlockDiag == null) {
      cachedCombinedIgnoreBlockDiag = buildCombinedIgnoreBlock(getIgnoreBlock());
    }
    Class<?>[] ignoreBlock = cachedCombinedIgnoreBlockDiag;

    // Check for blocks in each direction relative to the block's facing direction.
    // For each direction, also verify that NSEWUD-rotatable blocks (like mount kits) are
    // actually facing toward this pole block — prevents false connectors on adjacent
    // diagonal poles that happen to be geometrically adjacent but not the mount target.
    //
    // Offset directions come from the shared precomputed lookup table in BlockUtils — hoisted
    // out of this class so the regular (non-diagonal) pole variant can share it too.
    EnumFacing dirEast = BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.EAST);
    EnumFacing dirWest = BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.WEST);
    EnumFacing dirUp = BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.UP);
    EnumFacing dirDown = BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.DOWN);
    EnumFacing dirNorth = BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.NORTH);
    EnumFacing dirSouth = BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.SOUTH);

    boolean isBlockToEast = checkConnectable(worldIn, pos, dirEast, ignoreBlock);
    boolean isBlockToWest = checkConnectable(worldIn, pos, dirWest, ignoreBlock);
    boolean isBlockAbove = checkConnectable(worldIn, pos, dirUp, ignoreBlock);
    boolean isBlockBelow = checkConnectable(worldIn, pos, dirDown, ignoreBlock);
    boolean isBlockToNorth = checkConnectable(worldIn, pos, dirNorth, ignoreBlock);
    boolean isBlockToSouth = checkConnectable(worldIn, pos, dirSouth, ignoreBlock);

    return state.withProperty(MOUNT_EAST, isBlockToEast)
        .withProperty(MOUNT_WEST, isBlockToWest)
        .withProperty(MOUNT_UP, isBlockAbove)
        .withProperty(MOUNT_DOWN, isBlockBelow)
        .withProperty(MOUNT_NORTH, isBlockToNorth)
        .withProperty(MOUNT_SOUTH, isBlockToSouth);
  }

  /**
   * Checks if a block in the given direction is connectable, with special handling for
   * NSEWUD-rotatable blocks (like traffic signal mount kits). For rotatable blocks, this
   * verifies that the block's facing direction actually points its back/mount side toward
   * this pole position. This prevents false connectors on adjacent diagonal poles that are
   * geometrically adjacent to the mount but not the pole the mount is attached to.
   */
  private static boolean checkConnectable( IBlockAccess worldIn, BlockPos polePos,
      EnumFacing direction, Class<?>[] ignoreBlock ) {
    BlockPos adjPos = polePos.offset( direction );
    boolean isBlock = isMountableAdjacent( worldIn, adjPos, ignoreBlock );
    if ( !isBlock ) {
      return false;
    }

    // If the adjacent block is a rotatable block with a facing property (mount kit, sign,
    // etc.), check that it actually faces toward this pole. A mount or sign's "back"
    // (attachment side) is the opposite of its FACING property. Only show the connector if
    // the back faces this pole — prevents false connectors on diagonal poles that are
    // geometrically adjacent to the block but not the pole the block is clamped to.
    //
    // Each rotation abstract uses a different facing property type, so each branch reads
    // the one that applies:
    //   AbstractBlockRotatableNSEWUD   → BlockDirectional.FACING        (EnumFacing, 6 dirs)
    //   AbstractBlockRotatableNSEW     → BlockHorizontal.FACING          (EnumFacing, 4 dirs)
    //   AbstractBlockRotatableHZEight  → AbstractBlockRotatableHZEight.FACING
    //                                                                   (DirectionEight, 8 dirs)
    IBlockState adjState = worldIn.getBlockState( adjPos );
    Block adjBlock = adjState.getBlock();
    BlockPos expectedPolePos = null;
    if ( adjBlock instanceof AbstractBlockRotatableNSEWUD ) {
      try {
        EnumFacing adjFacing = adjState.getValue( BlockDirectional.FACING );
        expectedPolePos = adjPos.offset( adjFacing.getOpposite() );
      }
      catch ( IllegalArgumentException ignored ) {
        // Fall through — treat as a non-directional connectable block.
      }
    }
    else if ( adjBlock instanceof AbstractBlockRotatableNSEW ) {
      try {
        EnumFacing adjFacing = adjState.getValue( BlockHorizontal.FACING );
        expectedPolePos = adjPos.offset( adjFacing.getOpposite() );
      }
      catch ( IllegalArgumentException ignored ) {
        // Fall through — treat as a non-directional connectable block.
      }
    }
    else if ( adjBlock instanceof AbstractBlockRotatableHZEight ) {
      try {
        DirectionEight adjFacing8 = adjState.getValue( AbstractBlockRotatableHZEight.FACING );
        DirectionEight backDir = adjFacing8.getOpposite();
        // DirectionEight is horizontal-only, so Y stays the same as the adjacent block's Y.
        expectedPolePos = adjPos.add( backDir.getOffsetX(), 0, backDir.getOffsetZ() );
      }
      catch ( IllegalArgumentException ignored ) {
        // Fall through — treat as a non-directional connectable block.
      }
    }
    if ( expectedPolePos != null && !expectedPolePos.equals( polePos ) ) {
      return false; // Block is not attached to this pole.
    }

    return true;
  }
}
