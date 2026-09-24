package com.micatechnologies.minecraft.csm.furniture.outdoor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A block that is bounced on: the trampoline, the bounce castle's floor, the diving board. Landing
 * on it sends whatever lands back up with most of the speed it came down at, and hurts nothing; a
 * jump off it goes higher than a jump off the ground, and higher again for each jump made as it
 * lands, up to {@link #getMaxLaunch()}. Sneaking stops the bouncing. {@link Bounce} does the work,
 * from the block's {@code onLanded} and {@code onFallenUpon} and from the jump event.
 *
 * @since 2026.9
 */
public interface IBouncy {

  /**
   * Whether the block in {@code state} is bounced on: a bounce castle's walls are not.
   *
   * @param state the block's state
   *
   * @return true if landing on it bounces
   */
  default boolean isBouncy(IBlockState state) {
    return true;
  }

  /**
   * How much of the speed something lands with it gives back.
   *
   * @return a fraction, below 1 so that bouncing without jumping dies away
   */
  default double getRestitution() {
    return 0.85;
  }

  /**
   * The fastest it sends anything up, in blocks a tick (1.1 is a bounce of about six blocks).
   *
   * @return the most upward speed a bounce or a jump off it gives
   */
  default double getMaxLaunch() {
    return 1.1;
  }

  /**
   * How much a jump off it adds, in blocks a tick, to the speed of the bounce the jump was made
   * on (or to a jump's own speed, standing).
   *
   * @param world  the world
   * @param pos    the block jumped off
   * @param entity who jumped
   *
   * @return the added speed
   */
  default double getJumpBoost(World world, BlockPos pos, Entity entity) {
    return 0.3;
  }
}
