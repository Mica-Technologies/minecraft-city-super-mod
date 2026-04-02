package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side registry tracking which block positions have active fire alarm strobes.
 * Populated by {@link FireAlarmSoundPacketHandler} when sound START/STOP packets arrive
 * (piggybacking on the existing device position data in those packets). Queried by
 * {@link TileEntityFireAlarmStrobeRenderer} to decide whether to render the flash.
 */
@SideOnly(Side.CLIENT)
public final class ActiveStrobeRegistry {

  private static final Set<BlockPos> activePositions = new HashSet<>();

  private ActiveStrobeRegistry() {}

  public static void addPositions(Collection<BlockPos> positions) {
    activePositions.addAll(positions);
  }

  public static void removePositions(Collection<BlockPos> positions) {
    activePositions.removeAll(positions);
  }

  public static void clearAll() {
    activePositions.clear();
  }

  public static boolean isActive(BlockPos pos) {
    return activePositions.contains(pos);
  }
}
