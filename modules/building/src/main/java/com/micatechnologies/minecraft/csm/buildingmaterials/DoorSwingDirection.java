package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Which way a door swings, and the upper half's metadata that stores it. Kept apart from
 * {@link BlockBuildingDoor} so it can be tested without a game.
 *
 * <p>Every door kind has a default: most swing in, as a vanilla door does, and the exit,
 * storefront and fire doors swing out ({@link #OUTSWING}), as real ones do. Each placed door may
 * be <b>reversed</b> from its kind's default -- an interior door made to swing out, an exit door
 * made to swing in -- and that one bit is all a door stores about its swing. The way a placed door
 * actually swings is its kind's default, flipped if it is reversed ({@link #outward}).</p>
 *
 * <p>The upper half's four bits: {@code 8} marks it the upper half, {@code 1} a right hinge,
 * {@code 2} a fitted closer, {@code 4} reversed. Bit {@code 4} used to mean "swinging"; whether a
 * door is swinging is now whether its upper half has a {@link TileEntityDoorSwing}, which only a
 * client ever makes.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class DoorSwingDirection {

  /** The upper half's marker bit (the lower half has it clear). */
  static final int UPPER = 8;
  /** The upper half: hinged on the right, seen from outside. */
  static final int HINGE_RIGHT = 1;
  /** The upper half: a door closer is fitted. */
  static final int CLOSER = 2;
  /** The upper half: the door swings the other way from its kind's default. */
  static final int REVERSED = 4;

  /**
   * SHARED with gen_doors.OUTSWING: the doors that swing out by default, toward the outside -- an
   * exit door opens the way people escape, which is what lets its push bar work at all.
   */
  private static final Set<String> OUTSWING = Collections.unmodifiableSet(new HashSet<>(
      Arrays.asList("door_metal_fire", "door_metal_exit", "door_storefront_bronze")));

  private DoorSwingDirection() {
  }

  /**
   * Whether a kind of door swings out unless it is reversed.
   *
   * @param registryName the door's registry name
   *
   * @return whether it swings out by default
   *
   * @since 1.0
   */
  static boolean outswingKind(String registryName) {
    return OUTSWING.contains(registryName);
  }

  /**
   * Whether a placed door swings out: its kind's default, the other way if it is reversed.
   *
   * @param outswingKind whether its kind swings out by default
   * @param reversed     whether this door is reversed from that
   *
   * @return whether it swings out
   *
   * @since 1.0
   */
  static boolean outward(boolean outswingKind, boolean reversed) {
    return outswingKind != reversed;
  }

  /**
   * The upper half's metadata.
   *
   * @param hingeRight whether it is hinged on the right
   * @param closer     whether a closer is fitted
   * @param reversed   whether it swings the other way from its kind's default
   *
   * @return the metadata, 8 to 15
   *
   * @since 1.0
   */
  static int upperMeta(boolean hingeRight, boolean closer, boolean reversed) {
    return UPPER | (hingeRight ? HINGE_RIGHT : 0) | (closer ? CLOSER : 0)
        | (reversed ? REVERSED : 0);
  }

  static boolean isUpper(int meta) {
    return (meta & UPPER) != 0;
  }

  static boolean hingeRight(int meta) {
    return (meta & HINGE_RIGHT) != 0;
  }

  static boolean closer(int meta) {
    return (meta & CLOSER) != 0;
  }

  static boolean reversed(int meta) {
    return (meta & REVERSED) != 0;
  }
}
