package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A cell of the exterior marquee board ({@code marquee_board}): an LED video cabinet one block
 * square. Cells facing the same way side by side and stacked are one board, built to any size up
 * to {@link TileEntityMarquee#MAX_W} x {@link TileEntityMarquee#MAX_H}: each cell draws its own
 * square of the dark screen and, where the board stops (actual state {@code left}, {@code right},
 * {@code up}, {@code down}), its share of the frame. What the board shows is drawn once across it
 * by the controller ({@link BlockMarqueeController}), the board's bottom-left cell as the reader
 * sees it, so a board of a hundred cells has one tile entity and one renderer.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), towards the reader; bit 2
 * double-sided. A single-sided cabinet sits on the back of its cell, against the wall it is
 * mounted on; a double-sided one is centred in its cell with a screen on both faces, for a
 * board on a pylon ({@link BlockMarqueePylon}) or hung over a plaza. A cell joins only cells of
 * its own facing and sidedness. Placed by hand it faces the placer, double-sided when placed
 * while sneaking.</p>
 *
 * @since 2026.10
 */
public class BlockMarqueeBoard extends BlockVenue {

  /** Screens on both faces, the cabinet centred. Stored, bit 2. */
  public static final PropertyBool DOUBLE = PropertyBool.create("double");
  /** No board cell to the reader's left: the frame's left edge. Actual. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** No board cell to the reader's right. Actual. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  /** No board cell above. Actual. */
  public static final PropertyBool UP = PropertyBool.create("up");
  /** No board cell below. Actual. */
  public static final PropertyBool DOWN = PropertyBool.create("down");

  private static final AxisAlignedBB SINGLE_BOX = new AxisAlignedBB(0, 0, 8 / 16.0, 1, 1, 1);
  private static final AxisAlignedBB DOUBLE_BOX = new AxisAlignedBB(0, 0, 3 / 16.0, 1, 1,
      13 / 16.0);

  /**
   * Constructs a board cell.
   *
   * @param registryName its registry name
   */
  public BlockMarqueeBoard(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 2.0F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(DOUBLE, false).withProperty(LEFT, true).withProperty(RIGHT, true)
        .withProperty(UP, true).withProperty(DOWN, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, DOUBLE, LEFT, RIGHT, UP, DOWN);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(DOUBLE, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(DOUBLE) ? 4 : 0);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite())
        .withProperty(DOUBLE, placer.isSneaking());
  }

  /** Whether the block at {@code pos} is a cell of the same board face as {@code state}. */
  public static boolean joins(IBlockAccess world, BlockPos pos, IBlockState state) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockMarqueeBoard
        && s.getValue(FACING) == state.getValue(FACING)
        && s.getValue(DOUBLE) == state.getValue(DOUBLE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing right = state.getValue(FACING).rotateYCCW();
    return state.withProperty(LEFT, !joins(world, pos.offset(right.getOpposite()), state))
        .withProperty(RIGHT, !joins(world, pos.offset(right), state))
        .withProperty(UP, !joins(world, pos.up(), state))
        .withProperty(DOWN, !joins(world, pos.down(), state));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(DOUBLE) ? DOUBLE_BOX : SINGLE_BOX;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
