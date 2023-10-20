package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Class representation of a traffic signal controller's overlaps. Overlaps are used to determine
 * which signals can safely be green at the same time.
 *
 * <p>For example, in some scenarios, a left turn signal on circuit 1 can be green at the same time
 * as a right turn signal on circuit 2 if the movements are physically separated.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #fromNBT(NBTTagCompound)
 * @see #toNBT()
 * @since 2023.2.1
 */
public class TrafficSignalControllerOverlaps {

  // region: Static/Constant Fields

  // endregion

  // region: Instance Fields

  private final Map<BlockPos, List<BlockPos>> overlaps = new HashMap<>();

  // endregion

  // region: Instance Methods

  /**
   * Gets a {@link TrafficSignalControllerOverlaps} object from the given {@link NBTTagCompound}.
   *
   * @param nbt The {@link NBTTagCompound} containing the data for the
   *            {@link TrafficSignalControllerOverlaps} object in NBT format.
   *
   * @return The {@link TrafficSignalControllerOverlaps} object from the given
   *     {@link NBTTagCompound}.
   *
   * @throws IllegalArgumentException If the given {@link NBTTagCompound} is null.
   * @see #toNBT()
   * @since 1.0
   */
  public static TrafficSignalControllerOverlaps fromNBT(NBTTagCompound nbt) {
    // Validate the NBT
    if (nbt == null) {
      throw new IllegalArgumentException("The NBT cannot be null.");
    }

    // Create the overlaps object
    TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();

    // Deserialize overlaps
    for (String key : nbt.getKeySet()) {
      overlaps.overlaps.put(BlockPos.fromLong(Long.parseLong(key)),
          SerializationUtils.getBlockPosListFromBlockPosNBTArray(nbt.getTag(key)));
    }

    // Return the overlaps object
    return overlaps;
  }

  /**
   * Gets the {@link List<BlockPos>} of overlaps for the given source {@link BlockPos}. If no
   * overlaps are configured for the given source, null is returned.
   *
   * @param overlapSource The {@link BlockPos} of the source device.
   *
   * @return The {@link List<BlockPos>} of overlaps for the given source {@link BlockPos}. If no
   *     overlaps are configured for the given source, null is returned.
   *
   * @since 1.0
   */
  public List<BlockPos> getOverlapsForSource(BlockPos overlapSource) {
    return overlaps.get(overlapSource);
  }

  /**
   * Gets the number of overlaps configured for the traffic signal controller.
   *
   * @return The number of overlaps configured for the traffic signal controller.
   *
   * @since 1.0
   */
  public int getOverlapCount() {
    return overlaps.size();
  }

  /**
   * Adds the given overlap to the traffic signal controller and returns true if successful.
   *
   * @param overlapSource the {@link BlockPos} of the source device
   * @param overlapTarget the {@link BlockPos} of the target device
   *
   * @return {@code true} if the overlap was added, {@code false} otherwise.
   *
   * @since 1.0
   */
  public boolean addOverlap(BlockPos overlapSource, BlockPos overlapTarget) {
    // Check if the overlap already exists
    boolean overlapExists = false;
    if (overlaps.containsKey(overlapSource)) {
      if (overlaps.get(overlapSource).contains(overlapTarget)) {
        overlapExists = true;
      }
    }

    // If the overlap does not exist, add it
    if (!overlapExists) {
      // Create the overlap
      if (overlaps.containsKey(overlapSource)) {
        overlaps.get(overlapSource).add(overlapTarget);
      } else {
        ArrayList<BlockPos> overlapTargets = new ArrayList<>();
        overlapTargets.add(overlapTarget);
        overlaps.put(overlapSource, overlapTargets);
      }
    }

    return !overlapExists;
  }

  /**
   * Removes the given overlap from the traffic signal controller and returns true if successful.
   *
   * @param overlapSource the {@link BlockPos} of the source device
   * @param overlapTarget the {@link BlockPos} of the target device
   *
   * @return {@code true} if the overlap was removed, {@code false} otherwise.
   *
   * @since 1.0
   */
  public boolean removeOverlap(BlockPos overlapSource, BlockPos overlapTarget) {
    // Check if the overlap exists
    boolean overlapExists = overlaps.containsKey(overlapSource) &&
        overlaps.get(overlapSource).contains(overlapTarget);

    // If the overlap exists, remove it
    boolean removedOverlap = false;
    if (overlapExists) {
      // Remove the overlap
      overlaps.get(overlapSource).remove(overlapTarget);

      // If the source has no more overlaps, remove it
      if (overlaps.get(overlapSource).isEmpty()) {
        overlaps.remove(overlapSource);
      }

      // Set the flag
      removedOverlap = true;
    }

    return removedOverlap;
  }

  // endregion

  // region: Serialization Methods

  /**
   * Removes all overlaps for the given source {@link BlockPos} from the traffic signal controller
   * and returns true if successful.
   *
   * @param overlapSource the {@link BlockPos} of the source device
   *
   * @return {@code true} if the overlaps were removed, {@code false} otherwise.
   *
   * @since 1.0
   */
  public boolean removeOverlaps(BlockPos overlapSource) {
    // Check if any overlaps exist
    boolean overlapsExist = overlaps.containsKey(overlapSource);

    // If any overlaps exist, remove them
    boolean removedOverlaps = false;
    if (overlapsExist) {
      // Remove the overlaps
      overlaps.remove(overlapSource);

      // Set the flag
      removedOverlaps = true;
    }

    return removedOverlaps;
  }

  /**
   * Gets an {@link NBTTagCompound} containing the data for this
   * {@link TrafficSignalControllerOverlaps} in NBT format.
   *
   * @return The {@link NBTTagCompound} containing the data for this
   *     {@link TrafficSignalControllerOverlaps} in NBT format.
   *     <p>
   *     The returned {@link NBTTagCompound} can be used to reconstruct the
   *     {@link TrafficSignalControllerOverlaps} object.
   *     </p>
   *
   * @see #fromNBT(NBTTagCompound)
   * @since 1.0
   */
  public NBTTagCompound toNBT() {
    // Create the compound
    NBTTagCompound compound = new NBTTagCompound();

    // Serialize overlaps
    for (Map.Entry<BlockPos, List<BlockPos>> entry : overlaps.entrySet()) {
      compound.setTag(String.valueOf(entry.getKey().toLong()),
          SerializationUtils.getBlockPosNBTArrayFromBlockPosList(entry.getValue()));
    }

    // Return the compound
    return compound;
  }

  /**
   * Gets the hash code of this {@link TrafficSignalControllerCircuit} object.
   *
   * @return The hash code of this {@link TrafficSignalControllerCircuit} object.
   *
   * @since 1.0
   */
  @Override
  public int hashCode() {
    return Objects.hash(overlaps);
  }

  /**
   * Checks if the given {@link Object} is equal to this {@link TrafficSignalControllerCircuit}
   * object.
   *
   * @param o The {@link Object} to check.
   *
   * @return {@code true} if the given {@link Object} is equal to this
   *     {@link TrafficSignalControllerCircuit} object, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrafficSignalControllerOverlaps that = (TrafficSignalControllerOverlaps) o;
    return Objects.equals(overlaps, that.overlaps);
  }

  // endregion
}
