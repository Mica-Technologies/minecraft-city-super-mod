package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The jersey wall rack ({@code jersey_wall_rack}): a waterfall arm off a wall standard with three
 * jerseys facing out on hangers, the front one lowest. A {@link BlockTeamStoreFixture}: the
 * jerseys wear the fixture's team colour with trim (cuffs, hem stripes, collar and number) in its
 * trim colour.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), towards the shopper, the
 * standard on the wall behind; bit 2 numbered (the front jersey carries a number, 19). Placed
 * by hand it is numbered, or blank if placed while sneaking.</p>
 *
 * @since 2026.10
 */
public class BlockJerseyRack extends BlockTeamStoreFixture {

  /** The front jersey carries a number. Stored, bit 2. */
  public static final PropertyBool NUMBERED = PropertyBool.create("numbered");

  /**
   * Constructs the rack.
   *
   * @param registryName its registry name
   */
  public BlockJerseyRack(String registryName) {
    super(registryName, Kind.JERSEY_RACK);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(NUMBERED, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, NUMBERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(NUMBERED, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(NUMBERED) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(NUMBERED, !placer.isSneaking());
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }
}
