package com.micatechnologies.minecraft.csm.furniture.residential;

import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A commercial restroom's trough sink: a solid surface or stainless trough hung on the wall
 * with a sensor faucet over every block. Troughs of one finish placed side by side join into
 * one long trough ({@link BlockResidentialRun}), its end caps drawn only where the run stops.
 * Every block's faucet fills a bucket or a bottle ({@link IWaterTap}) and runs for a moment
 * for an empty hand ({@link SensorFaucet}); an appliance beside it is plumbed in.
 *
 * @since 2026.9
 */
public class BlockTroughSink extends BlockResidentialRun implements IWaterTap {

  private final double[] spout;

  /**
   * Constructs a trough sink.
   *
   * @param registryName its registry name, ending in its finish ({@code _stainless} is steel,
   *                     anything else solid surface)
   * @param box          one block's box facing north, in sixteenths
   * @param spout        the spout's outlet, {x, y, z} facing north in sixteenths
   */
  public BlockTroughSink(String registryName, int[] box, double[] spout) {
    super(registryName, box, registryName.endsWith("_stainless") ? FixtureMaterial.METAL
        : FixtureMaterial.PORCELAIN);
    this.spout = spout.clone();
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (IWaterTap.fillAtTap(world, pos, player, hand)) {
      return true;
    }
    if (player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    SensorFaucet.run(world, pos, state.getValue(FACING), spout);
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
