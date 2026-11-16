package com.micatechnologies.minecraft.csm.transit.canopy;

import com.micatechnologies.minecraft.csm.transit.LightSwitchRipple;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What every light under a canopy shares: switching the whole canopy at once, and reading power
 * at the light or at the deck it hangs from.
 *
 * @since 2026.11
 */
public final class CanopyLights {

  /** The most deck tiles a switch walks. */
  public static final int WALK_LIMIT = 4096;

  private CanopyLights() {
  }

  /**
   * Sets every light under the canopy over {@code light} (downlights and strips alike), or that
   * light alone when no deck is above it: walks the decks joined to the one above it, side to
   * side, up to {@link #WALK_LIMIT} tiles, and sets the light under each. {@code light} itself
   * switches at once; the rest are queued in the order the walk meets them and switch a few a
   * tick ({@link LightSwitchRipple}), so the canopy lights as a ripple out from it rather than
   * stalling the server for a tick of relighting.
   *
   * @param world the world
   * @param light the light switched
   * @param lit   whether the lights are to be lit
   */
  public static void switchCanopy(World world, BlockPos light, boolean lit) {
    LightSwitchRipple.now(world, light, lit, CanopyLights::set);
    BlockPos start = light.up();
    if (!(world.getBlockState(start).getBlock() instanceof BlockCanopyDeck)) {
      return;
    }
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> open = new ArrayDeque<>();
    open.add(start);
    seen.add(start);
    while (!open.isEmpty() && seen.size() <= WALK_LIMIT) {
      BlockPos at = open.poll();
      BlockPos under = at.down();
      if (!under.equals(light)
          && world.getBlockState(under).getBlock() instanceof ICanopyLight) {
        LightSwitchRipple.queue(world, under, lit, CanopyLights::set);
      }
      for (EnumFacing side : EnumFacing.HORIZONTALS) {
        BlockPos next = at.offset(side);
        if (!seen.contains(next) && world.isBlockLoaded(next)
            && world.getBlockState(next).getBlock() instanceof BlockCanopyDeck) {
          seen.add(next);
          open.add(next);
        }
      }
    }
  }

  private static boolean set(World world, BlockPos at, boolean lit) {
    IBlockState s = world.getBlockState(at);
    return s.getBlock() instanceof ICanopyLight
        && ((ICanopyLight) s.getBlock()).setCanopyLight(world, at, s, lit);
  }

  /**
   * Whether a light is powered: at its own block, or at the deck it hangs from, so a photocell
   * (an inverted daylight sensor) set on the canopy's roof switches the lights under it.
   *
   * @param world the world
   * @param light the light's position
   *
   * @return {@code true} if powered
   */
  public static boolean powered(World world, BlockPos light) {
    return world.isBlockPowered(light) || (world.getBlockState(light.up()).getBlock()
        instanceof BlockCanopyDeck && world.isBlockPowered(light.up()));
  }
}
