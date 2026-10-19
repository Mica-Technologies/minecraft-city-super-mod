package com.micatechnologies.minecraft.csm.healthcare.diagnostics;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A laboratory bench ({@code lab_bench}, {@code lab_bench_sink}): cabinets under a black epoxy
 * worktop a block high, with a reagent shelf over the back of it. Benches side by side join into
 * one bench, with an end panel only where the run stops.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the front faces. The
 * ends ({@link #EAST}, {@link #WEST}, actual state) are named in model space as the nurses'
 * station's are: {@code east} is the end at the left of someone facing the bench
 * ({@code facing.rotateY()} in the world). An end is joined when the next cell that way holds any
 * lab bench. The worktop is at the top of the cell, so a bench-top instrument (centrifuge,
 * microscope, haematology analyser) is set in the cell above.</p>
 *
 * @since 2026.10
 */
public class BlockLabBench extends BlockHealthcare {

  /** Joined at the model's east end. */
  public static final PropertyBool EAST = PropertyBool.create("east");
  /** Joined at the model's west end. */
  public static final PropertyBool WEST = PropertyBool.create("west");

  private static final IProperty<?>[] ACTUAL = {EAST, WEST};

  /**
   * Constructs a bench.
   *
   * @param registryName its registry name
   */
  public BlockLabBench(String registryName) {
    super(registryName, new int[]{0, 0, 0, 16, 16, 16}, HealthcareMaterial.WOOD, Mount.FLOOR,
        true);
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    return ACTUAL;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    return state.withProperty(EAST, false).withProperty(WEST, false);
  }

  private static boolean bench(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockLabBench;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(EAST, bench(world, pos.offset(facing.rotateY())))
        .withProperty(WEST, bench(world, pos.offset(facing.rotateYCCW())));
  }
}
