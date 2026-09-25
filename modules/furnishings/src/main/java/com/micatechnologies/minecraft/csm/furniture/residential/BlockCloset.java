package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A built-in closet: two blocks tall like a wardrobe ({@link BlockResidentialTall}), but set
 * side by side into a run along a wall, like the living room's run pieces: its end panels and
 * the ends of its cornice are drawn only where the run stops. {@link BlockResidentialRun#LEFT}
 * and {@link BlockResidentialRun#RIGHT} say whether the same closet, facing the same way and
 * the same half, continues on that side; both are actual state.
 *
 * @since 2026.9
 */
public class BlockCloset extends BlockResidentialTall {

  /**
   * Constructs a closet.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths from the floor (up to 32)
   * @param slots        how many slots a closet (both halves) holds
   * @param openSound    the sound of its doors opening, or null
   * @param closeSound   the sound of them closing, or null
   */
  public BlockCloset(String registryName, int[] box, int slots, @Nullable ICsmSound openSound,
      @Nullable ICsmSound closeSound) {
    super(registryName, box, false, slots, openSound, closeSound);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UPPER, BlockResidentialRun.LEFT,
        BlockResidentialRun.RIGHT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(BlockResidentialRun.LEFT, continues(world, pos, s, facing.rotateYCCW()))
        .withProperty(BlockResidentialRun.RIGHT, continues(world, pos, s, facing.rotateY()));
  }

  private boolean continues(IBlockAccess world, BlockPos pos, IBlockState state,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() == this && other.getValue(FACING) == state.getValue(FACING)
        && other.getValue(UPPER) == state.getValue(UPPER);
  }
}
