package com.micatechnologies.minecraft.csm.venues.ballpark;

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
 * The outfield wall: a block of wall with its dark green padding on the field side, stacked to
 * the wall's height (two blocks is 8 ft). The top block draws the yellow home run line along the
 * pad's top edge ({@code top}, actual state: no wall above). {@code outfield_wall} runs along a
 * block's side; {@code outfield_wall_diagonal} across its cell corner to corner, meeting the
 * straight runs and the next diagonal block at the cell corners, so a fence of straight runs and
 * 45 degree runs is one wall (the dasher corner's convention).
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), towards the field. A diagonal block faces
 * between its facing and the next facing clockwise: facing south looks south-west (a right field
 * wall, the field to its south-west), facing east south-east (left field).</p>
 *
 * @since 2026.10
 */
public class BlockOutfieldWall extends BlockVenue {

  /** Nothing of the wall above: the yellow line on top. Actual. */
  public static final PropertyBool TOP = PropertyBool.create("top");
  /** The distance marker's shape: across its cell (stored, bit 2). */
  public static final PropertyBool DIAGONAL = PropertyBool.create("diagonal");

  /** 0 straight, 1 diagonal, 2 the marker (either, by metadata). */
  private static final ThreadLocal<Integer> PENDING = new ThreadLocal<>();

  /** 0 straight, 1 diagonal, 2 the marker. */
  protected final int mode;

  /**
   * Constructs a wall block.
   *
   * @param registryName its registry name
   * @param diagonal     across its cell corner to corner
   */
  public BlockOutfieldWall(String registryName, boolean diagonal) {
    this(registryName, diagonal ? 1 : 0);
  }

  /**
   * Constructs a wall block of a mode.
   *
   * @param registryName its registry name
   * @param mode         0 straight, 1 diagonal, 2 the marker
   */
  protected BlockOutfieldWall(String registryName, int mode) {
    super(stash(registryName, mode), Material.ROCK, SoundType.STONE, 2.0F);
    this.mode = mode;
    PENDING.remove();
    IBlockState s = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(TOP, true);
    if (mode == 2) {
      s = s.withProperty(DIAGONAL, false);
    }
    setDefaultState(s);
  }

  private static String stash(String registryName, int mode) {
    PENDING.set(mode);
    return registryName;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    Integer m = PENDING.get();
    if (m != null && m == 2) {
      return new CsmBlockStateContainer(this, FACING, DIAGONAL, TOP);
    }
    return new CsmBlockStateContainer(this, FACING, TOP);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    IBlockState s = getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    return mode == 2 ? s.withProperty(DIAGONAL, (meta & 4) != 0) : s;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = state.getValue(FACING).getHorizontalIndex();
    return mode == 2 && state.getValue(DIAGONAL) ? meta | 4 : meta;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(TOP,
        !(world.getBlockState(pos.up()).getBlock() instanceof BlockOutfieldWall));
  }

  /** A whole block: a 45 degree block's field-side corner too, which the wall stands over. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }
}
