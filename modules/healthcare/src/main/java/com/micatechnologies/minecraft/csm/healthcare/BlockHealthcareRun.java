package com.micatechnologies.minecraft.csm.healthcare;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A Healthcare piece placed side by side into a run: waiting room beam seating, the parallel
 * bars, lecture hall seating. Its ends (legs, arms, posts) are drawn only where the run stops, so
 * three placed in a row read as one piece.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the front faces, plus a
 * subclass's flags in bits 2-3. The ends ({@link #EAST}, {@link #WEST}, actual state, never
 * stored) are named in model space as the lab bench's are: {@code east} is the end at the left
 * of someone facing the piece ({@code facing.rotateY()} in the world). An end is joined when the
 * next cell that way holds a run piece of the same group (any colour of beam seating) facing the
 * same way.</p>
 *
 * @since 2026.10
 */
public class BlockHealthcareRun extends BlockHealthcare {

  /** Joined at the model's east end. */
  public static final PropertyBool EAST = PropertyBool.create("east");
  /** Joined at the model's west end. */
  public static final PropertyBool WEST = PropertyBool.create("west");

  private static final IProperty<?>[] ACTUAL = {EAST, WEST};

  private final String group;

  /**
   * Constructs a run piece.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param material     what it is made of
   * @param collides     whether it can be walked into
   * @param group        what it joins: pieces of the same group join each other
   */
  public BlockHealthcareRun(String registryName, int[] box, HealthcareMaterial material,
      boolean collides, String group) {
    super(registryName, box, material, Mount.FLOOR, collides);
    this.group = group;
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    return ACTUAL;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    return state.withProperty(EAST, false).withProperty(WEST, false);
  }

  private boolean joins(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() instanceof BlockHealthcareRun
        && group.equals(((BlockHealthcareRun) other.getBlock()).group)
        && other.getValue(FACING) == facing;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(EAST, joins(world, pos.offset(facing.rotateY()), facing))
        .withProperty(WEST, joins(world, pos.offset(facing.rotateYCCW()), facing));
  }
}
