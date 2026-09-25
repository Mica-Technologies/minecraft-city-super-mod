package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side registry tracking which block positions have active fire alarm strobes.
 * Populated by {@link FireAlarmSoundPacketHandler} when sound START/STOP packets arrive
 * (piggybacking on the existing device position data in those packets). Queried by
 * {@link TileEntityFireAlarmStrobeRenderer} to decide whether to render the flash.
 *
 * <p>Positions are held per channel, and a strobe flashes while any channel that is still running
 * names it. One appliance can be on two channels at once (a horn strobe is on its horn's channel
 * and on the strobe channel), so stopping one of them must not put out a strobe the other still
 * drives.</p>
 */
@SideOnly(Side.CLIENT)
public final class ActiveStrobeRegistry {

  private static final Map<String, Set<BlockPos>> channels = new HashMap<>();
  private static final Map<BlockPos, Integer> counts = new HashMap<>();

  private ActiveStrobeRegistry() {}

  /** Sets the positions a channel drives, replacing what it drove before. */
  public static void setChannel(String channel, Collection<BlockPos> positions) {
    removeChannel(channel);
    Set<BlockPos> set = new HashSet<>(positions);
    channels.put(channel, set);
    for (BlockPos pos : set) {
      counts.merge(pos, 1, Integer::sum);
    }
  }

  /** Forgets a channel; its strobes go out unless another running channel names them. */
  public static void removeChannel(String channel) {
    Set<BlockPos> set = channels.remove(channel);
    if (set == null) {
      return;
    }
    for (BlockPos pos : set) {
      counts.computeIfPresent(pos, (p, n) -> n > 1 ? n - 1 : null);
    }
  }

  public static void clearAll() {
    channels.clear();
    counts.clear();
    // The cached projections are keyed by position and mean nothing once the world is gone.
    StrobeSurfaceProjection.clear();
  }

  public static boolean isActive(BlockPos pos) {
    return counts.containsKey(pos);
  }
}
