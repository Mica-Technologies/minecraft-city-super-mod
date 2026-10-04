package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Something that knows how to grind a tree stump away: which logs are a stump rather than a
 * standing tree or part of a building, which go with it, and what is left where it stood.
 * <p>
 * A machine that grinds stumps (CSM: Vehicles' stump grinder) asks {@link CsmStumpGrinders}
 * rather than any grinder directly, so it never names the module the rules come from (CSM: Parks
 * & Greenery, whose hand-guided stump grinder uses the same rules). A grinder is registered from
 * its module's pre-initialization.
 *
 * @version 1.0
 * @see CsmStumpGrinders
 * @since 2026.10
 */
public interface ICsmStumpGrinder {

  /**
   * Whether the block at a position is a log of any tree, this grinder's or another mod's: what a
   * machine looks for under its cutter before asking to grind.
   *
   * @param world the world (server side)
   * @param pos   the position
   *
   * @return whether it is a log
   *
   * @since 1.0
   */
  boolean isLog(World world, BlockPos pos);

  /**
   * Grinds the stump the log at a position belongs to, with its roots, and leaves what a ground
   * stump leaves. A log that is not a stump (a standing tree, a log on a floor or a foundation,
   * one too tall) is left alone.
   *
   * @param world  the world (server side)
   * @param log    a log of the stump
   * @param player who is grinding, for build permissions; null for a machine nobody is driving,
   *               which then grinds only where no permission is needed
   *
   * @return how many logs were ground, 0 if it was not a stump
   *
   * @since 1.0
   */
  int grind(World world, BlockPos log, @Nullable EntityPlayer player);
}
