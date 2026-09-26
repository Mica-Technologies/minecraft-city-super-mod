package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * The sensor faucet of a commercial restroom's wall-hung lavatory and trough sink: a hand held
 * under the spout (a right-click with an empty hand) runs it for a moment, a short stream of
 * water from the spout's outlet into the basin and the sound of it running. Nothing is stored
 * and nothing ticks; a bucket or a bottle is filled at the tap as at any other
 * ({@link IWaterTap}).
 *
 * @since 2026.9
 */
public final class SensorFaucet {

  private SensorFaucet() {
  }

  /**
   * Runs the faucet once.
   *
   * @param world  the world
   * @param pos    the block with the faucet
   * @param facing the way the block faces
   * @param spout  the spout's outlet, {x, y, z} facing north in sixteenths
   */
  public static void run(World world, BlockPos pos, EnumFacing facing, double[] spout) {
    if (!(world instanceof WorldServer)) {
      return;
    }
    WorldServer server = (WorldServer) world;
    double x = spout[0] / 16.0 - 0.5;
    double z = spout[2] / 16.0 - 0.5;
    // Turn (x, z) from north to the block's facing, as the blockstate's y does.
    for (int i = 0; i < (facing.getHorizontalIndex() + 2) % 4; i++) {
      double t = x;
      x = -z;
      z = t;
    }
    double wx = pos.getX() + 0.5 + x;
    double wz = pos.getZ() + 0.5 + z;
    double wy = pos.getY() + spout[1] / 16.0;
    int water = Block.getStateId(Blocks.WATER.getDefaultState());
    server.spawnParticle(EnumParticleTypes.BLOCK_DUST, wx, wy - 0.05, wz, 12, 0.015, 0.06,
        0.015, 0.02, water);
    server.spawnParticle(EnumParticleTypes.WATER_SPLASH, wx, wy - 0.22, wz, 6, 0.06, 0.0, 0.06,
        0.0);
    SoundEvent event = FurnishingsSounds.SHOWER_SPRAY.getSoundEvent();
    if (event != null) {
      world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.3F, 1.4F);
    }
  }
}
