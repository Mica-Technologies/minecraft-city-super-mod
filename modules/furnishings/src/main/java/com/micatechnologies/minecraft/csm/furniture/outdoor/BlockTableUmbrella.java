package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
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
 * A patio umbrella for the middle of a patio table: set on the table, its pole runs down through
 * the table top to a small base on the ground, and its canopy opens over the table at the height
 * the free-standing umbrella's does ({@link BlockPatioUmbrella}). A right-click opens or furls
 * it ({@link #OPEN}, stored in the bit above the facing), and the model follows.
 *
 * @since 2026.9
 */
public class BlockTableUmbrella extends BlockResidentialFurniture {

  /** Whether the canopy is open. */
  public static final PropertyBool OPEN = BlockPatioUmbrella.OPEN;

  /**
   * Constructs a table umbrella.
   *
   * @param registryName its registry name, ending in its canopy's colour
   * @param box          its box facing north, in sixteenths (the pole)
   */
  public BlockTableUmbrella(String registryName, int[] box) {
    super(registryName, box, true);
    setDefaultState(getDefaultState().withProperty(OPEN, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, OPEN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(OPEN, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(OPEN, true);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      world.setBlockState(pos, state.withProperty(OPEN, open), 3);
      UmbrellaSounds.play(world, pos, open);
    }
    return true;
  }
}
