package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Tile entity utility class for traffic signal heads. This class assists in tracking and managing
 * the paint colors of the traffic signal head and visor configuration(s).
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2024.8.19
 */
public class TileEntityTrafficSignalHead extends AbstractTileEntity {

  /**
   * The key used to store the paint color in NBT data.
   *
   * @since 1.0
   */
  private static final String PAINT_COLOR_KEY = "paintColor";

  /**
   * The key used to store the body tilt in NBT data.
   *
   * @since 1.0
   */
  private static final String BODY_TILT_KEY = "bodyTilt";

  /**
   * The key used to store the visor type in NBT data.
   *
   * @since 1.0
   */
  private static final String VISOR_TYPE_KEY = "visorType";

  /**
   * The current body paint color.
   *
   * @since 1.0
   */
  private TrafficSignalBodyColor paintColor = TrafficSignalBodyColor.FLAT_BLACK;

  /**
   * The current body tilt.
   *
   * @since 1.0
   */
  private TrafficSignalBodyTilt bodyTilt = TrafficSignalBodyTilt.NONE;

  /**
   * The current visor type.
   *
   * @since 1.0
   */
  private TrafficSignalVisorType visorType = TrafficSignalVisorType.TUNNEL;

  /**
   * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   *
   * @since 2.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    // Get the paint color
    if (compound.hasKey(PAINT_COLOR_KEY)) {
      paintColor = TrafficSignalBodyColor.fromNBT(compound.getInteger(PAINT_COLOR_KEY));
    }

    // Get the body tilt
    if (compound.hasKey(BODY_TILT_KEY)) {
      bodyTilt = TrafficSignalBodyTilt.fromNBT(compound.getInteger(BODY_TILT_KEY));
    }

    // Get the visor type
    if (compound.hasKey(VISOR_TYPE_KEY)) {
      visorType = TrafficSignalVisorType.fromNBT(compound.getInteger(VISOR_TYPE_KEY));
    }
  }

  /**
   * Returns the NBT tag compound with the tile entity's NBT data.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   *
   * @since 2.0
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    // Set the paint color
    compound.setInteger(PAINT_COLOR_KEY, paintColor.toNBT());

    // Set the body tilt
    compound.setInteger(BODY_TILT_KEY, bodyTilt.toNBT());

    // Set the visor type
    compound.setInteger(VISOR_TYPE_KEY, visorType.toNBT());

    // Return the compound
    return compound;
  }

  /**
   * Gets the current paint color.
   *
   * @return the current paint color.
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getPaintColor() {
    return paintColor;
  }

  /**
   * Gets the current body tilt.
   *
   * @return the current body tilt.
   *
   * @since 1.0
   */
  public TrafficSignalBodyTilt getBodyTilt() {
    return bodyTilt;
  }

  /**
   * Gets the current visor type.
   *
   * @return the current visor type.
   *
   * @since 1.0
   */
  public TrafficSignalVisorType getVisorType() {
    return visorType;
  }

  /**
   * Sets the body paint color.
   *
   * @param paintColor the new body paint color
   *
   * @since 1.0
   */
  public void setPaintColor(
      TrafficSignalBodyColor paintColor) {
    this.paintColor = paintColor;
    System.err.println(
        "Paint color set to [" + (world.isRemote ? "client" : "server") + "]: " + paintColor);
    markDirtySync(world, pos, true);
  }

  /**
   * Sets the body tilt.
   *
   * @param bodyTilt the new body tilt
   *
   * @since 1.0
   */
  public void setBodyTilt(
      TrafficSignalBodyTilt bodyTilt) {
    this.bodyTilt = bodyTilt;
    markDirtySync(world, pos, true);
  }

  /**
   * Sets the visor type.
   *
   * @param visorType the new visor type
   *
   * @since 1.0
   */
  public void setVisorType(
      TrafficSignalVisorType visorType) {
    this.visorType = visorType;
    markDirtySync(world, pos, true);
  }

  /**
   * Gets the next paint color in the sequence.
   *
   * @return the next paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextPaintColor() {
    TrafficSignalBodyColor nextPaintColor = paintColor.getNextColor();
    setPaintColor(nextPaintColor);
    return getPaintColor();
  }

  /**
   * Gets the next body tilt in the sequence.
   *
   * @return the next body tilt in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyTilt getNextBodyTilt() {
    TrafficSignalBodyTilt nextBodyTilt = bodyTilt.getNextTilt();
    setBodyTilt(nextBodyTilt);
    return getBodyTilt();
  }

  /**
   * Gets the next visor type in the sequence.
   *
   * @return the next visor type in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalVisorType getNextVisorType() {
    TrafficSignalVisorType nextVisorType = visorType.getNextVisorType();
    setVisorType(nextVisorType);
    return getVisorType();
  }
}
