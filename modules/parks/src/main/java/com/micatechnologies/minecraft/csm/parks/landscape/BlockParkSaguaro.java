package com.micatechnologies.minecraft.csm.parks.landscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A saguaro, stacked from blocks into one cactus of any height: the trunk in every block, the
 * rounded crown only on the top one, and a pair of arms on the second block of a saguaro three
 * or more blocks tall (a real saguaro branches only once it is several metres tall, so a one- or
 * two-block saguaro is a young column). The arms rise past the block into the one above. All of
 * it is actual state, read from the blocks above and below; the multipart blockstate draws it
 * ({@code gen_park_plantings.py}). A cactus: it pricks what presses against it.
 *
 * @since 2026.10
 */
public class BlockParkSaguaro extends BlockParkProp {

  /** This is the top of the saguaro: no saguaro above. */
  public static final PropertyBool CAP = PropertyBool.create("cap");
  /** This block carries the arms: the second of a saguaro three or more blocks tall. */
  public static final PropertyBool ARMS = PropertyBool.create("arms");

  /**
   * Constructs a saguaro.
   *
   * @param registryName its registry name
   * @param inset        how far its trunk stands in from each side, in sixteenths
   */
  public BlockParkSaguaro(String registryName, int inset) {
    super(registryName, Kind.CACTUS, 16, inset);
    setDefaultState(blockState.getBaseState().withProperty(CAP, true).withProperty(ARMS, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, ARMS, CAP);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    boolean above = world.getBlockState(pos.up()).getBlock() == this;
    boolean below = world.getBlockState(pos.down()).getBlock() == this;
    boolean arms = above && below && world.getBlockState(pos.down(2)).getBlock() != this;
    return state.withProperty(CAP, !above).withProperty(ARMS, arms);
  }
}
