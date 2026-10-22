package com.micatechnologies.minecraft.csm.api;

import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Lets a module's block follow a fire alarm control panel without naming Life Safety, which owns
 * the panels: a fare gate or a station service gate that opens while its panel is in alarm.
 *
 * <p>Life Safety registers the query ({@link #setPanelQuery}) from its pre-initialization and
 * links any block that is a {@link Follower}, with its linking tool and with
 * {@code /csmfirealarm link}, by handing the follower the panel's position. The follower keeps
 * that position and asks {@link #isPanelInAlarm} when it looks, as Life Safety's own door
 * holders do. Without Life Safety installed nothing is ever in alarm and nothing links.</p>
 *
 * @since 2026.10
 */
public final class CsmFireAlarmLink {

  @Nullable
  private static volatile BiPredicate<World, BlockPos> panelQuery;

  private CsmFireAlarmLink() {
  }

  /**
   * A block that follows a fire alarm control panel.
   */
  public interface Follower {

    /**
     * Makes the block at {@code pos} follow the panel at {@code panel}.
     *
     * @return {@code true} when it was not following that panel already
     */
    boolean followFireAlarmPanel(World world, BlockPos pos, BlockPos panel);
  }

  /** Registered by Life Safety: whether the panel at a position is in fire alarm. */
  public static void setPanelQuery(BiPredicate<World, BlockPos> query) {
    panelQuery = query;
  }

  /** Whether the panel at {@code panel} is in fire alarm; false without Life Safety. */
  public static boolean isPanelInAlarm(World world, @Nullable BlockPos panel) {
    BiPredicate<World, BlockPos> query = panelQuery;
    return panel != null && query != null && query.test(world, panel);
  }
}
