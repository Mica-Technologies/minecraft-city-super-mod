package com.micatechnologies.minecraft.csm.furniture.residential;

import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A wall-mounted shower head with its mixer valve below it, hung on the wall at the top of the
 * block it is placed in, so a floor of tiles and a wall behind make a shower. Right-click turns
 * the water on and off: while it runs, water falls from the rose and splashes where it lands,
 * with the spray's loop ({@link ShowerSpray}). {@link ShowerSpray#ON} is stored, in the bit
 * above the facing.
 *
 * @since 2026.9
 */
public class BlockShowerHead extends BlockBathroomFixture {

  /** The middle of the rose's underside, facing north, in sixteenths. */
  private static final double[] ROSE = {8, 11.5, 10.5};

  /**
   * Constructs a shower head.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockShowerHead(String registryName, int[] box) {
    super(registryName, box, FixtureMaterial.METAL);
    setDefaultState(getDefaultState().withProperty(ShowerSpray.ON, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, ShowerSpray.ON);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(ShowerSpray.ON, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(ShowerSpray.ON) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(ShowerSpray.ON, false);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean on = !state.getValue(ShowerSpray.ON);
      world.setBlockState(pos, state.withProperty(ShowerSpray.ON, on), 3);
      ShowerSpray.turned(world, pos, on);
      if (on) {
        world.scheduleUpdate(pos, this, ShowerSpray.EVERY);
      }
    }
    return true;
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (world.isRemote || !state.getValue(ShowerSpray.ON)) {
      return;
    }
    ShowerSpray.spray(world, pos, state.getValue(FACING), ROSE);
    world.scheduleUpdate(pos, this, ShowerSpray.EVERY);
  }
}
