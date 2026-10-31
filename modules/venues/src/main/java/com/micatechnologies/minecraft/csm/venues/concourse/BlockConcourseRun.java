package com.micatechnologies.minecraft.csm.venues.concourse;

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
 * A concourse piece that joins side by side into a run: the concession stand's back counter
 * (stainless cabinets and worktop with a backsplash), the suite bar counter (a walnut front, a
 * stone bar top, a brass foot rail, the work surface behind) and the suite drink rail (a ledge on
 * a slim panel along the suite front). Each block draws its own length; where the run stops it
 * draws an end panel ({@code end_l}, {@code end_r}, actual state), so nothing about the run is
 * stored.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3). The back counter faces the server
 * working at it (its doors), the bar counter its guests, the drink rail the event floor (its
 * guests sit behind it, on bar stools). The tops are a block high, so anything set on them
 * stands in the cell above at its floor.</p>
 *
 * @since 2026.10
 */
public class BlockConcourseRun extends BlockVenue {

  /** Which piece. */
  public enum Kind {
    /** The concession stand's back counter. */
    BACK_COUNTER(box(0, 0, 0, 16, 16, 16), Material.IRON, SoundType.METAL),
    /** The suite bar counter. */
    SUITE_BAR(box(0, 0, 0, 16, 16, 16), Material.WOOD, SoundType.WOOD),
    /** The suite drink rail. */
    DRINK_RAIL(box(0, 0, 0, 16, 16, 8.5), Material.WOOD, SoundType.WOOD);

    final AxisAlignedBB box;
    final Material material;
    final SoundType sound;

    Kind(AxisAlignedBB box, Material material, SoundType sound) {
      this.box = box;
      this.material = material;
      this.sound = sound;
    }
  }

  /** The run stops on the left (looking the way the piece faces). Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** The run stops on the right. Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");

  private final Kind kind;

  /**
   * Constructs a run piece.
   *
   * @param registryName its registry name
   * @param kind         which piece
   */
  public BlockConcourseRun(String registryName, Kind kind) {
    super(registryName, kind.material, kind.sound, 1.5F);
    this.kind = kind;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(END_L, true).withProperty(END_R, true));
  }

  static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1, double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, END_L, END_R);
  }

  @Override
  @Nonnull
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
    return state.withProperty(END_L, !same(world, pos.offset(f.rotateYCCW()), f))
        .withProperty(END_R, !same(world, pos.offset(f.rotateY()), f));
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() == this && s.getValue(FACING) == facing;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return kind.box;
  }
}
