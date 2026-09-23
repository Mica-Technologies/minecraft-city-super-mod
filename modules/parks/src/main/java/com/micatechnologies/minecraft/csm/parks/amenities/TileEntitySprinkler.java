package com.micatechnologies.minecraft.csm.parks.amenities;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumParticleTypes;

/**
 * A sprinkler's spray: while its block is powered, a rotor stream sweeps round, drawn on the client
 * as water particles thrown along an arc. Nothing is saved and nothing runs on the server.
 *
 * @since 2026.9
 */
public class TileEntitySprinkler extends AbstractTickableTileEntity {

  /** How far the stream reaches, in blocks. */
  private static final double REACH = 3.5;
  /** Radians the rotor turns each tick: a full sweep every eight seconds or so. */
  private static final double TURN = 2 * Math.PI / 160;

  @Override
  public boolean doClientTick() {
    return true;
  }

  @Override
  public boolean pauseTicking() {
    return world == null || !world.isRemote;
  }

  @Override
  public long getTickRate() {
    return 1;
  }

  @Override
  public void onTick() {
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockSprinkler) || !state.getValue(BlockSprinkler.POWERED)) {
      return;
    }
    double angle = world.getTotalWorldTime() * TURN;
    double dx = Math.cos(angle);
    double dz = Math.sin(angle);
    double x0 = pos.getX() + 0.5;
    double y0 = pos.getY() + 0.3;
    double z0 = pos.getZ() + 0.5;
    // Points along the arc of the stream: up and out, falling back to the lawn at its reach.
    for (int i = 1; i <= 6; i++) {
      double t = i / 6.0 + world.rand.nextDouble() * 0.08;
      double r = REACH * t;
      double y = y0 + 1.2 * 4 * t * (1 - t);
      world.spawnParticle(EnumParticleTypes.WATER_SPLASH, x0 + dx * r, y, z0 + dz * r,
          dx * 0.05, 0.02, dz * 0.05);
    }
    if (world.rand.nextInt(3) == 0) {
      world.spawnParticle(EnumParticleTypes.WATER_DROP, x0 + dx * REACH, y0, z0 + dz * REACH, 0,
          0, 0);
    }
  }
}
