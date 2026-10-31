package com.micatechnologies.minecraft.csm.venues.pool;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A pool's starting block, numbered by metadata: {@code pool_starting_block} meta 0-9 is the lane
 * number shown on its front and sides (10-15 are 9). It stands on the end wall's gutter, in the
 * block above it, and faces the water: its facing is not stored but read from the world (the
 * first side with water in front of it, a block lower or level), so one metadata value is a lane
 * number whichever end of the pool it is on.
 *
 * <p><b>Where it sits in its block.</b> Lanes are 2.5 m wide, so a lane's middle falls a
 * quarter of the way across a block, from one side or the other in turn. With the pool's side
 * walls on block edges and the outside lanes 0.5 m from them (an 8-lane pool 21 m wide, lanes 1-8;
 * a 10-lane pool 25 m wide, lanes 0-9, the same lanes in the same places), an odd lane's middle is
 * 4 px from the left of its block as a swimmer on the block sees it and an even lane's 12 px, at
 * either end of the pool. The block is drawn there, so the number alone places it.</p>
 *
 * @since 2026.10
 */
public class BlockPoolStartingBlock extends BlockVenue {

  /** The lane number (metadata). */
  public static final PropertyInteger NUMBER = PropertyInteger.create("number", 0, 9);

  public BlockPoolStartingBlock(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(NUMBER, 1));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, NUMBER);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(NUMBER, Math.min(9, meta & 15));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(NUMBER);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(NUMBER, Math.min(9, meta & 15));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(FACING, waterSide(world, pos));
  }

  /** The side with water in front of it, a block lower or level; north if none. */
  static EnumFacing waterSide(IBlockAccess world, BlockPos pos) {
    for (int dy = -1; dy <= 0; dy++) {
      for (EnumFacing f : EnumFacing.HORIZONTALS) {
        if (world.getBlockState(pos.offset(f).up(dy)).getMaterial() == Material.WATER) {
          return f;
        }
      }
    }
    return EnumFacing.NORTH;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    double cx = (state.getValue(NUMBER) % 2 == 1 ? 4.0 : 12.0) / 16.0;
    return new AxisAlignedBB(Math.max(0, cx - 0.3), 0, 0.05, Math.min(1, cx + 0.3), 0.75, 0.8);
  }
}
