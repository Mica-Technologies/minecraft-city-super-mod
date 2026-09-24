package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A close-coupled toilet. Right-click the seat to sit on it (Core's seat, as a chair); right-click
 * the cistern behind it, or its flush button on top, to flush it: a synthesised flush and a
 * swirl of water in the bowl. Which one a click does is decided by where it lands: the back
 * third of the block, or anywhere above the seat, is the cistern.
 *
 * @since 2026.9
 */
public class BlockToilet extends BlockResidentialFurniture {

  /** The top of the seat, in sixteenths. */
  private static final double SEAT_TOP = 6.75;
  /** How far forward of the block's middle the seat's middle is, in sixteenths. */
  private static final double SEAT_FORWARD = 1.0;
  /** A click this far behind the block's middle, in blocks, is on the cistern. */
  private static final double CISTERN_BEHIND = 0.2;
  /** A click this high, in blocks, is on the cistern (or its lid, or the raised seat lid). */
  private static final double CISTERN_ABOVE = 0.5;

  /**
   * Constructs a toilet.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockToilet(String registryName, int[] box) {
    super(registryName, box, FixtureMaterial.PORCELAIN.getMaterial(),
        FixtureMaterial.PORCELAIN.getSound(), FixtureMaterial.PORCELAIN.getHardness(), SEAT_TOP,
        SEAT_FORWARD, 0);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    EnumFacing back = state.getValue(FACING).getOpposite();
    double behind = (hitX - 0.5) * back.getXOffset() + (hitZ - 0.5) * back.getZOffset();
    if (behind > CISTERN_BEHIND || hitY > CISTERN_ABOVE) {
      flush(world, pos, state);
      return true;
    }
    return sit(world, pos, state, player);
  }

  /** The flush: its sound, and water swirling in the bowl. */
  private void flush(World world, BlockPos pos, IBlockState state) {
    SoundEvent event = FurnishingsSounds.TOILET_FLUSH.getSoundEvent();
    if (event != null) {
      world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.9F, 1.0F);
    }
    if (world instanceof WorldServer) {
      EnumFacing front = state.getValue(FACING);
      double x = pos.getX() + 0.5 + front.getXOffset() * 0.0625;
      double z = pos.getZ() + 0.5 + front.getZOffset() * 0.0625;
      ((WorldServer) world).spawnParticle(EnumParticleTypes.WATER_SPLASH, x, pos.getY() + 0.4,
          z, 14, 0.1, 0.02, 0.15, 0.0);
    }
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
