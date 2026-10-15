package com.micatechnologies.minecraft.csm.hvac;

import net.minecraft.util.math.BlockPos;

/**
 * Something that ties a space into existence and reads it: a thermostat, a vent, a heater or
 * cooler, or a player standing in a room with no equipment of its own. A space lives while at
 * least one anchor is attached to it.
 *
 * @author Mica Technologies
 * @since 2026.9
 */
final class ThermalAnchor {

  static final int THERMOSTAT = 0;
  static final int ZONE = 1;
  static final int VENT = 2;
  static final int UNIT = 3;
  static final int PLAYER = 4;

  final BlockPos pos;
  final int kind;

  /**
   * The temperature this anchor last saw, from its tile entity's saved data or from its space
   * just before the space was rebuilt. A space that is found again after a chunk unload, a server
   * restart or a rescan starts from these, so a room that was warm comes back warm.
   */
  float savedTemp = Float.NaN;

  /** The space the anchor is in, or null while it has none. */
  ThermalSpace space;

  /** The packed cell of {@link #space} the anchor reads, when attached. */
  long cell;

  /** Region of {@link #space} the anchor reads, when attached. */
  int region = -1;

  /** World tick before which a failed attach is not retried. */
  long retryTick;

  /** Why the last attach failed, or {@link ThermalScanner.Status#OK}. */
  ThermalScanner.Status status = ThermalScanner.Status.OK;

  /**
   * After an attach that failed because part of the room is not loaded: the chunks it reached
   * that were not loaded. Until one of them loads (or a block in the room changes) another
   * attempt can only fail the same way, so none is made. Null when not waiting.
   */
  long[] waitChunks;

  /** The flood that stopped at an unloaded chunk, if any: a block change in it ends the wait. */
  HvacThermalWorld.UnloadedFlood waitFlood;

  /** {@link #openAt} when there is no such cell. */
  static final long NO_CELL = Long.MIN_VALUE;

  /**
   * After an attach that failed as {@link ThermalScanner.Status#NOT_ENCLOSED} (no enclosed air at
   * the anchor or beside it): the first of those cells that was open to the sky, packed with
   * {@link ThermalScanner#pack}, or {@link #NO_CELL} when every one was solid (the device is
   * buried in a wall). {@code /csmhvac info} names it, so a builder can find the hole.
   */
  long openAt = NO_CELL;

  /**
   * Whether the anchor has no space and no attach has failed: it has not been looked at yet, or
   * is waiting its turn behind the per-tick flood budget. Its status is still the default
   * {@link ThermalScanner.Status#OK}, which must not be read as "open to the outdoors".
   */
  boolean findingRoom() {
    return space == null && status == ThermalScanner.Status.OK;
  }

  ThermalAnchor(BlockPos pos, int kind) {
    this.pos = pos.toImmutable();
    this.kind = kind;
  }

  /** The temperature at the anchor: its region's if attached, else the saved value (may be NaN). */
  float temperature() {
    return space != null && region >= 0 ? space.temperature[region] : savedTemp;
  }
}
