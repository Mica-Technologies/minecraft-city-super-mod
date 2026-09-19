package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.BlockPos;

/**
 * The custom doors moving right now, on this client: their lower halves, which way each is going
 * and when it started. While a door is here its baked model draws nothing and the moving-door
 * renderer draws it instead; it is taken out, and the chunk redrawn, when the move is over.
 *
 * <p>Common code with nothing client-only in it, so the block can ask it from its extended state;
 * only the client ever puts anything in it. A handful of entries at most, and none at rest -- which
 * is how a custom door costs nothing a frame when it is not moving.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class CustomDoorMotion {

  private CustomDoorMotion() {
  }

  /**
   * One door's move.
   *
   * @since 1.0
   */
  public static final class Move {

    public final boolean opening;
    public final long startTick;
    public final int ticks;

    Move(boolean opening, long startTick, int ticks) {
      this.opening = opening;
      this.startTick = startTick;
      this.ticks = ticks;
    }

    /**
     * How far the door is from shut, 0 to 1, eased.
     *
     * @param worldTime    the world time, with the partial tick
     *
     * @return the openness
     *
     * @since 1.0
     */
    public double openness(double worldTime) {
      double p = Math.max(0.0, Math.min(1.0, (worldTime - startTick) / ticks));
      double eased = p * p * (3 - 2 * p);
      return opening ? eased : 1 - eased;
    }

    public boolean done(double worldTime) {
      return worldTime - startTick >= ticks;
    }
  }

  private static final Map<BlockPos, Move> MOVING = new ConcurrentHashMap<>();

  public static void start(BlockPos lowerPos, boolean opening, long now, int ticks) {
    MOVING.put(lowerPos.toImmutable(), new Move(opening, now, ticks));
  }

  public static boolean isMoving(BlockPos lowerPos) {
    return MOVING.containsKey(lowerPos);
  }

  public static Map<BlockPos, Move> all() {
    return MOVING;
  }

  public static void clear() {
    MOVING.clear();
  }
}
