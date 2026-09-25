package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The corner of an L-shaped sofa: a seat with its back along two sides (behind it and to its
 * right), open on the other two -- its left and its front -- where sofas of the same fabric join
 * it. A sofa to its left faces the way the corner does; a sofa in front of it faces the corner's
 * left, its back running on from the corner's right-hand back.
 *
 * <p>{@link #LEFT} and {@link #FRONT} say whether such a sofa is there (actual state). Where an
 * L of sofas stops at the corner -- a sofa on one open side, none on the other -- the multipart
 * blockstate closes the open side with an arm; a corner on its own is a corner chair with no
 * arms. Right-click to sit, facing the way the corner faces.</p>
 *
 * @since 2026.9
 */
public class BlockSofaCorner extends BlockResidentialFurniture {

  /** A sofa joins on the corner's left. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** A sofa joins at the corner's front. */
  public static final PropertyBool FRONT = PropertyBool.create("front");

  /** The corner's box facing north, in sixteenths: all of the block but the arms' overhang. */
  private static final int[] BOX = {0, 0, 0, 16, 14, 16};
  /** The seat: its top, and its middle forward and left of the block's middle, in sixteenths. */
  private static final double SEAT_TOP = 7.25;
  private static final double SEAT_FORWARD = 1.5;
  private static final double SEAT_LEFT = 1.5;

  /**
   * Constructs a sofa corner.
   *
   * @param registryName its registry name, ending in its fabric
   */
  public BlockSofaCorner(String registryName) {
    super(registryName, BOX, true, SEAT_TOP, SEAT_FORWARD, SEAT_LEFT);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, FRONT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing g = s.getValue(FACING);
    return s.withProperty(LEFT, sofa(world, pos.offset(g.rotateYCCW()), g))
        .withProperty(FRONT, sofa(world, pos.offset(g), g.rotateYCCW()));
  }

  /** Whether a sofa of this corner's fabric is at {@code at}, facing {@code facing}. */
  private boolean sofa(IBlockAccess world, BlockPos at, EnumFacing facing) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() instanceof BlockSofa
        && ((BlockSofa) other.getBlock()).getFinish().equals(getFinish())
        && other.getValue(FACING) == facing;
  }
}
