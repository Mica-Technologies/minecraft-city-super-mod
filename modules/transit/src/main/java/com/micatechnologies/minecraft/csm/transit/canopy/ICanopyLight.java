package com.micatechnologies.minecraft.csm.transit.canopy;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A light hung under a bus station canopy deck ({@link BlockCanopyDownlight},
 * {@link BlockCanopyLightStrip}): switched together with every other light under the same canopy
 * ({@link CanopyLights#switchCanopy}), and told of power at the deck above it.
 *
 * @since 2026.11
 */
public interface ICanopyLight {

  /**
   * Turns this light on or off, if it is not already, with its light-up air.
   *
   * @param world the world
   * @param at    the light's position
   * @param state the light's state there
   * @param lit   whether it is to be lit
   */
  void setCanopyLight(World world, BlockPos at, IBlockState state, boolean lit);
}
