package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Interface for blocks a traffic pole should ignore only in some of their states.
 *
 * <p>{@link ICsmTrafficPoleIgnored} is type-based, so it can only say "always" or "never". That is
 * the wrong answer for a block whose mounting hardware is part of its configuration rather than
 * its type. A block implementing this interface is asked per placed block, so it can be ignored in
 * one state and mounted in another.
 *
 * <p>Today that is the dynamic street sign: on either of its hanging mounts it swings below a mast
 * arm on hardware it draws itself, so a hanging blade is ignored, while a flat blade is bolted to
 * whatever is behind it and stays mountable.
 *
 * @see AbstractBlockTrafficPole
 * @see ICsmTrafficPoleIgnored
 * @since 2026.9
 */
public interface ICsmTrafficPoleStateIgnored {

  /**
   * Returns whether this particular block, in its current state at {@code pos}, should be ignored
   * by an adjacent traffic pole.
   *
   * <p>This is called from {@code getActualState}, which runs during chunk load as well, so an
   * implementation must cope with the tile entity not being attached yet.
   *
   * @param world the world/block access
   * @param pos   the position of this block
   *
   * @return {@code true} if a pole should not mount to this block as it currently stands
   *
   * @since 2026.9
   */
  boolean isIgnoredForTrafficPole(IBlockAccess world, BlockPos pos);
}
