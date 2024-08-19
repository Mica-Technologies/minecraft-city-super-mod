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
   * The key used to store the body paint color in NBT data.
   *
   * @since 1.0
   */
  private static final String BODY_PAINT_COLOR_KEY = "bodyPaintColor";

  /**
   * The key used to store the body paint color in NBT data.
   *
   * @since 1.0
   */
  private static final String DOOR_PAINT_COLOR_KEY = "doorPaintColor";

  /**
   * The key used to store the body paint color in NBT data.
   *
   * @since 1.0
   */
  private static final String VISOR_PAINT_COLOR_KEY = "visorPaintColor";

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
  private TrafficSignalBodyColor bodyPaintColor = TrafficSignalBodyColor.FLAT_BLACK;

  /**
   * The current door paint color.
   *
   * @since 1.0
   */
  private TrafficSignalBodyColor doorPaintColor = TrafficSignalBodyColor.FLAT_BLACK;

  /**
   * The current visor paint color.
   *
   * @since 1.0
   */
  private TrafficSignalBodyColor visorPaintColor = TrafficSignalBodyColor.FLAT_BLACK;

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
    // Get the body paint color
    if (compound.hasKey(BODY_PAINT_COLOR_KEY)) {
      bodyPaintColor = TrafficSignalBodyColor.fromNBT(compound.getInteger(BODY_PAINT_COLOR_KEY));
    }

    // Get the door paint color
    if (compound.hasKey(DOOR_PAINT_COLOR_KEY)) {
      doorPaintColor = TrafficSignalBodyColor.fromNBT(compound.getInteger(DOOR_PAINT_COLOR_KEY));
    }

    // Get the visor paint color
    if (compound.hasKey(VISOR_PAINT_COLOR_KEY)) {
      visorPaintColor = TrafficSignalBodyColor.fromNBT(compound.getInteger(VISOR_PAINT_COLOR_KEY));
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
    // Set the body paint color
    compound.setInteger(BODY_PAINT_COLOR_KEY, bodyPaintColor.toNBT());

    // Set the door paint color
    compound.setInteger(DOOR_PAINT_COLOR_KEY, doorPaintColor.toNBT());

    // Set the visor paint color
    compound.setInteger(VISOR_PAINT_COLOR_KEY, visorPaintColor.toNBT());

    // Set the body tilt
    compound.setInteger(BODY_TILT_KEY, bodyTilt.toNBT());

    // Set the visor type
    compound.setInteger(VISOR_TYPE_KEY, visorType.toNBT());

    // Return the compound
    return compound;
  }

  /**
   * Gets the current body paint color.
   *
   * @return the current body paint color.
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getBodyPaintColor() {
    return bodyPaintColor;
  }

  /**
   * Gets the current door paint color.
   *
   * @return the current door paint color.
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getDoorPaintColor() {
    return doorPaintColor;
  }

  /**
   * Gets the current visor paint color.
   *
   * @return the current visor paint color.
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getVisorPaintColor() {
    return visorPaintColor;
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
   * @param bodyPaintColor the new body paint color
   *
   * @since 1.0
   */
  public void setBodyPaintColor(TrafficSignalBodyColor bodyPaintColor) {
    this.bodyPaintColor = bodyPaintColor;
    System.err.println(
        "Body paint color set to [" + (world.isRemote ? "client" : "server") + "]: " + bodyPaintColor);
    markDirtySync(world, pos, true);
  }

  /**
   * Sets the door paint color.
   *
   * @param paintColor the new door paint color
   *
   * @since 1.0
   */
  public void setDoorPaintColor(TrafficSignalBodyColor paintColor) {
    this.doorPaintColor = paintColor;
    markDirtySync(world, pos, true);
  }

  /**
   * Sets the visor paint color.
   *
   * @param paintColor the new visor paint color
   *
   * @since 1.0
   */
  public void setVisorPaintColor(TrafficSignalBodyColor paintColor) {
    this.visorPaintColor = paintColor;
    markDirtySync(world, pos, true);
  }

  /**
   * Sets the body tilt.
   *
   * @param bodyTilt the new body tilt
   *
   * @since 1.0
   */
  public void setBodyTilt(TrafficSignalBodyTilt bodyTilt) {
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
  public void setVisorType(TrafficSignalVisorType visorType) {
    this.visorType = visorType;
    markDirtySync(world, pos, true);
  }

  /**
   * Gets the next body paint color in the sequence.
   *
   * @return the next body paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextBodyPaintColor() {
    TrafficSignalBodyColor nextPaintColor = bodyPaintColor.getNextColor();
    setBodyPaintColor(nextPaintColor);
    return getBodyPaintColor();
  }

  /**
   * Gets the next door paint color in the sequence.
   *
   * @return the next door paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextDoorPaintColor() {
    TrafficSignalBodyColor nextPaintColor = doorPaintColor.getNextColor();
    setDoorPaintColor(nextPaintColor);
    return getDoorPaintColor();
  }

  /**
   * Gets the next visor paint color in the sequence.
   *
   * @return the next visor paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextVisorPaintColor() {
    TrafficSignalBodyColor nextPaintColor = visorPaintColor.getNextColor();
    setVisorPaintColor(nextPaintColor);
    return getVisorPaintColor();
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
