package com.micatechnologies.minecraft.csm.furniture.residential;

import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Candles that are lit and snuffed with a right-click, standing on whatever is under them as
 * the counter pieces do. Lit, their flames are drawn (the blockstate swaps the flame texture
 * in), they give a candle's small light and now and then send up a wisp of smoke; the flame is
 * drawn and never real, so it sets nothing alight.
 *
 * @since 2026.9
 */
public class BlockCandle extends BlockCounterLight {

  /** The wicks, facing north standing on the floor, in sixteenths: {x, top of flame, z}. */
  private final double[][] wicks;

  /**
   * Constructs candles.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor, in sixteenths
   * @param lightLevel   the light they give lit, 0 to 15
   * @param wicks        where the flames are: {x, y, z} each, facing north, in sixteenths
   */
  public BlockCandle(String registryName, int[] box, int lightLevel, double[][] wicks) {
    super(registryName, box, Material.WOOD, SoundType.CLOTH, BlockRenderLayer.CUTOUT, lightLevel);
    this.wicks = wicks;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean lit = !state.getValue(LIT);
      world.setBlockState(pos, state.withProperty(LIT, lit), 3);
      world.playSound(null, pos, lit ? SoundEvents.ITEM_FLINTANDSTEEL_USE
              : SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, lit ? 0.5F : 0.2F,
          lit ? 1.2F : 2.0F);
    }
    return true;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
    if (!state.getValue(LIT) || wicks.length == 0) {
      return;
    }
    IBlockState actual = state.getActualState(world, pos);
    double[] wick = wicks[rand.nextInt(wicks.length)];
    double[] w = BlockFireplace.toWorld(actual.getValue(FACING), wick[0], wick[2]);
    double y = pos.getY() + (wick[1] - actual.getValue(REST).getDrop()) / 16.0;
    if (rand.nextInt(3) == 0) {
      world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, pos.getX() + w[0], y + 0.03,
          pos.getZ() + w[1], 0.0, 0.0, 0.0);
    }
  }
}
