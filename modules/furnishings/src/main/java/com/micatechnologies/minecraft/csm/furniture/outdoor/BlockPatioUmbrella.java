package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialTall;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A free-standing patio umbrella on a weighted base, two blocks tall, its square canopy 2.25 m
 * across at a little over two metres. A right-click on either block opens or furls it
 * ({@link #OPEN}, stored in both, in the top bit over {@link BlockResidentialTall#UPPER} and the
 * facing); the upper block's model is the open canopy or the furled one.
 *
 * @since 2026.9
 */
public class BlockPatioUmbrella extends BlockResidentialTall {

  /** Whether the canopy is open. */
  public static final PropertyBool OPEN = PropertyBool.create("open");

  /**
   * Constructs a patio umbrella.
   *
   * @param registryName its registry name, ending in its canopy's colour
   * @param box          its box facing north, in sixteenths from the ground, up to 32
   */
  public BlockPatioUmbrella(String registryName, int[] box) {
    super(registryName, box, true, 0, null, null);
    setDefaultState(getDefaultState().withProperty(OPEN, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, UPPER, OPEN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 7).withProperty(OPEN, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(OPEN, true);
  }

  /** Opens or furls the canopy, from either block. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      world.setBlockState(pos, state.withProperty(OPEN, open), 3);
      BlockPos other = state.getValue(UPPER) ? pos.down() : pos.up();
      IBlockState otherState = world.getBlockState(other);
      if (otherState.getBlock() == this) {
        world.setBlockState(other, otherState.withProperty(OPEN, open), 3);
      }
      UmbrellaSounds.play(world, pos, open);
    }
    return true;
  }
}
