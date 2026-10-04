package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The registry of {@link ICsmStumpGrinder}s, and the one place a stump grinding machine asks
 * whether there is a stump under its cutter and grinds it.
 * <p>
 * CSM: Parks & Greenery registers its stump rules from its pre-initialization; CSM: Vehicles'
 * stump grinder calls {@link #isLog} and {@link #grind}. Neither names the other, which is what
 * lets a module reference only Core. Without Parks installed nothing is registered, and the
 * machine grinds nothing.
 *
 * @version 1.0
 * @since 2026.10
 */
public final class CsmStumpGrinders {

  /**
   * The registered grinders, in registration order.
   *
   * @since 1.0
   */
  private static final List<ICsmStumpGrinder> GRINDERS = new ArrayList<>();

  /**
   * The grinders whose failure has been logged, so each is logged once.
   *
   * @since 1.0
   */
  private static final Set<ICsmStumpGrinder> FAILED =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * Private constructor: this class is a static registry and is never instantiated.
   *
   * @since 1.0
   */
  private CsmStumpGrinders() {
    throw new UnsupportedOperationException("CsmStumpGrinders must not be instantiated");
  }

  /**
   * Registers a grinder. Call this from a mod's pre-initialization.
   *
   * @param grinder the grinder
   *
   * @since 1.0
   */
  public static synchronized void register(ICsmStumpGrinder grinder) {
    if (grinder != null) {
      GRINDERS.add(grinder);
    }
  }

  /**
   * Whether any grinder is registered: without one a machine has nothing to grind with.
   *
   * @return whether any grinder is registered
   *
   * @since 1.0
   */
  public static boolean available() {
    return !GRINDERS.isEmpty();
  }

  /**
   * Whether any registered grinder takes the block at a position for a log.
   *
   * @param world the world (server side)
   * @param pos   the position
   *
   * @return whether it is a log
   *
   * @since 1.0
   */
  public static synchronized boolean isLog(World world, BlockPos pos) {
    for (ICsmStumpGrinder grinder : GRINDERS) {
      try {
        if (grinder.isLog(world, pos)) {
          return true;
        }
      } catch (RuntimeException e) {
        failed(grinder, e);
      }
    }
    return false;
  }

  /**
   * Grinds the stump the log at a position belongs to, by the first grinder that takes it.
   *
   * @param world  the world (server side)
   * @param log    a log of the stump
   * @param player who is grinding, or null for a machine nobody is driving
   *
   * @return how many logs were ground, 0 if no grinder took it for a stump
   *
   * @since 1.0
   */
  public static synchronized int grind(World world, BlockPos log, @Nullable EntityPlayer player) {
    if (world == null || world.isRemote) {
      return 0;
    }
    for (ICsmStumpGrinder grinder : GRINDERS) {
      try {
        int ground = grinder.grind(world, log, player);
        if (ground > 0) {
          return ground;
        }
      } catch (RuntimeException e) {
        failed(grinder, e);
      }
    }
    return 0;
  }

  private static void failed(ICsmStumpGrinder grinder, RuntimeException e) {
    if (FAILED.add(grinder)) {
      Csm.getLogger().error("Stump grinder {} failed; it is skipped where it fails",
          grinder.getClass().getName(), e);
    }
  }
}
