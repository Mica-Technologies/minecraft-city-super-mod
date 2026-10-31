package com.micatechnologies.minecraft.csm.venues.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The aluminium bleacher's understructure ({@code aluminium_bleacher_frame}): the cells under the
 * rows. Each cell carries a ledger along the front at its top (under the riser of the row above)
 * and, every third cell along the row and at the run's ends, a post down its front edge with a
 * diagonal brace back to the next post line, so a stand reads as frames on a grid rather than a
 * forest of posts. Where a post goes is worked out from the cell's position and its neighbours
 * ({@code post_l}, {@code post_r}, actual state); nothing is stored but the facing.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the bleacher's. Fill every cell under the
 * rows with it, a column under each row down to the ground.</p>
 *
 * @since 2026.10
 */
public class BlockBleacherFrame extends BlockVenue {

  /** A post at the cell's left edge (looking the way the bleacher faces). Actual. */
  public static final PropertyBool POST_L = PropertyBool.create("post_l");
  /** A post at the right edge: the run's right end. Actual. */
  public static final PropertyBool POST_R = PropertyBool.create("post_r");

  /** Posts this many cells apart along a run. */
  static final int SPACING = 3;

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

  /**
   * Constructs the frame.
   *
   * @param registryName its registry name
   */
  public BlockBleacherFrame(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 2.0F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(POST_L, true).withProperty(POST_R, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, POST_L, POST_R);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    EnumFacing left = f.rotateYCCW();
    // a coordinate that grows towards the left, so every row puts its posts on the same lines
    int along = left.getAxis() == EnumFacing.Axis.X ? pos.getX() * left.getXOffset()
        : pos.getZ() * left.getZOffset();
    boolean endL = !same(world, pos.offset(left), f);
    boolean endR = !same(world, pos.offset(f.rotateY()), f);
    return state.withProperty(POST_L, endL || Math.floorMod(along, SPACING) == 0)
        .withProperty(POST_R, endR);
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() == this && s.getValue(FACING) == facing;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }
}
