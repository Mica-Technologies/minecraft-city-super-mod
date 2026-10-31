package com.micatechnologies.minecraft.csm.venues.golf;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A golf course's grass, mown: {@code golf_turf}. Metadata bits 0-2 are the cut (0 putting green,
 * 1 collar, 2 fairway, 3 rough, 4 tee, 5 putting green with the cup cut in it), bit 3 the way the
 * mower ran (0 north-south, 1 east-west). The light and dark stripes are actual state, from the
 * block's position: a stripe is a block wide on a green, its collar and a cup, two on a fairway
 * and a tee, so neighbouring blocks with the same cut and direction always make one pattern. The
 * rough is not striped.
 *
 * @since 2026.10
 */
public class BlockGolfTurf extends BlockArenaFloorBase {

  /** The cut. */
  public static final PropertyInteger CUT = PropertyInteger.create("cut", 0, 5);
  /** The mower ran east-west. */
  public static final PropertyBool ACROSS = PropertyBool.create("across");
  /** This block is on a dark stripe (actual). */
  public static final PropertyBool DARK = PropertyBool.create("dark");

  /** The cup's cut. */
  public static final int CUP = 5;
  private static final int[] STRIPE = {1, 1, 2, 0, 2, 1};

  public BlockGolfTurf(String registryName) {
    super(registryName, Surface.TURF, null);
    setDefaultState(blockState.getBaseState().withProperty(CUT, 2).withProperty(ACROSS, false)
        .withProperty(DARK, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, CUT, ACROSS, DARK);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(CUT, Math.min(5, meta & 7))
        .withProperty(ACROSS, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(CUT) | (state.getValue(ACROSS) ? 8 : 0);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return getMetaFromState(state);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getStateFromMeta(meta);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int w = STRIPE[state.getValue(CUT)];
    if (w == 0) {
      return state.withProperty(DARK, false);
    }
    int c = state.getValue(ACROSS) ? pos.getZ() : pos.getX();
    return state.withProperty(DARK, (Math.floorDiv(c, w) & 1) != 0);
  }
}
