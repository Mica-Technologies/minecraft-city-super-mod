package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal.SIGNAL_SIDE;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import net.minecraft.block.Block;
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
  private static final String SECTION_INFOS_KEY = "sectionInfos";

  /**
   * The key used to store the body tilt in NBT data.
   *
   * @since 1.0
   */
  private static final String BODY_TILT_KEY = "bodyTilt";

  /**
   * The key used to store the section count in the section info NBT data.
   *
   * @since 1.0
   */
  private static final String SECTION_INFO_COMPOUND_COUNT_KEY = "count";

  /**
   * The key prefix used to store each section info in the section info NBT data.
   *
   * @since 1.0
   */
  private static final String SECTION_INFO_COMPOUND_KEY_PREFIX = "s_";

  /**
   * The current visor type.
   *
   * @since 1.0
   */
  private TrafficSignalSectionInfo[] sectionInfos = {};


  /**
   * The current body tilt.
   *
   * @since 1.0
   */
  private TrafficSignalBodyTilt bodyTilt = TrafficSignalBodyTilt.NONE;

  /**
   * Constructs a new TileEntityTrafficSignalHead instance.
   *
   * @since 1.0
   */
  public TileEntityTrafficSignalHead() {
    super();

    // Initialize the section infos with default values
    sectionInfos= new TrafficSignalSectionInfo[]{};
  }

  public TileEntityTrafficSignalHead(TrafficSignalSectionInfo[] sectionInfos) {
    super();

    // Initialize the section infos with the provided values
    this.sectionInfos = sectionInfos;
  }

  /**
   * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   *
   * @since 2.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    // Get the traffic signal section infos
    readSectionInfo(compound);

    // Get the body tilt
    if (compound.hasKey(BODY_TILT_KEY)) {
      bodyTilt = TrafficSignalBodyTilt.fromNBT(compound.getInteger(BODY_TILT_KEY));
    }
  }


  private void readSectionInfo(NBTTagCompound compound) {
    if (compound.hasKey(SECTION_INFOS_KEY)) {
      NBTTagCompound sectionInfoCompound = compound.getCompoundTag(SECTION_INFOS_KEY);
      sectionInfos = new TrafficSignalSectionInfo[sectionInfoCompound.getInteger(SECTION_INFO_COMPOUND_COUNT_KEY)];
      for (int i = 0; i < sectionInfos.length; i++) {
        int[] sectionData = sectionInfoCompound.getIntArray(SECTION_INFO_COMPOUND_KEY_PREFIX + i);
        sectionInfos[i] = TrafficSignalSectionInfo.fromNBTArray(sectionData);
      }
    }
  }

  private void writeSectionInfo(NBTTagCompound compound) {
    NBTTagCompound sectionInfoCompound = new NBTTagCompound();
    sectionInfoCompound.setInteger(SECTION_INFO_COMPOUND_COUNT_KEY, sectionInfos.length);
    for (int i = 0; i < sectionInfos.length; i++) {
      sectionInfoCompound.setIntArray(SECTION_INFO_COMPOUND_KEY_PREFIX + i, sectionInfos[i].toNBTArray());
    }
    compound.setTag(SECTION_INFOS_KEY, sectionInfoCompound);
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
    // Write the section infos to the compound
    writeSectionInfo(compound);

    // Set the body tilt
    compound.setInteger(BODY_TILT_KEY, bodyTilt.toNBT());

    // Return the compound
    return compound;
  }

  /**
   * Gets the current section infos (non-live, meaning the bulb colors are not
   * updated based on the current bulb color).
   *
   * @return the current section infos (non-live).
   *
   * @since 1.0
   */
  public TrafficSignalSectionInfo[] getSectionInfos() {
    return sectionInfos;
  }

  /**
   * Gets the current section infos.
   *
   * @return the current section infos.
   *
   * @since 1.0
   */
  public TrafficSignalSectionInfo[] getSectionInfos(int currentBulbColor) {
    // Red:0, Yellow:1, Green:2, Off:3
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      if (currentBulbColor==0&&sectionInfo.getBulbColor()== TrafficSignalBulbColor.RED){
        sectionInfo.setBulbLit(true);
      } else if (currentBulbColor==1&&sectionInfo.getBulbColor()== TrafficSignalBulbColor.YELLOW){
        sectionInfo.setBulbLit(true);
      } else if (currentBulbColor==2&&sectionInfo.getBulbColor()== TrafficSignalBulbColor.GREEN){
        sectionInfo.setBulbLit(true);
      } else if (currentBulbColor==3){
        sectionInfo.setBulbLit(false);
      } else {
        sectionInfo.setBulbLit(false);
      }
    }

    // Loop again, and if the bulb is lit and set to flashing, handle the flashing logic
    long blinkInterval = 500; // ms
    boolean firstHalfOfSecond = (System.currentTimeMillis() % (blinkInterval * 2)) < blinkInterval;
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      if (sectionInfo.isBulbLit() && sectionInfo.isBulbFlashing()&&firstHalfOfSecond) {
        sectionInfo.setBulbLit(false);
      }
    }

    return sectionInfos;
  }

  /**
   * Sets the section infos.
   *
   * @param sectionInfos the new section infos
   *
   * @since 1.0
   */
  public void setSectionInfos(TrafficSignalSectionInfo[] sectionInfos) {
    this.sectionInfos = sectionInfos;
    markDirtySync(world, pos, true);
  }

  public int getSectionCount() {
    return sectionInfos.length;
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
   * Gets the next body paint color in the sequence.
   *
   * @return the next body paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextBodyPaintColor() {
    if (sectionInfos.length == 0) {
      System.err.println("No section infos available to get the next body paint color.");
      return TrafficSignalBodyColor.FLAT_BLACK; // Default fallback color
    }
    TrafficSignalBodyColor nextPaintColor = sectionInfos[0].getBodyColor().getNextColor();
    // Update the body color for all sections
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setBodyColor(nextPaintColor);
    }
    return nextPaintColor;
  }

  /**
   * Gets the next door paint color in the sequence.
   *
   * @return the next door paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextDoorPaintColor() {
    TrafficSignalBodyColor nextPaintColor = sectionInfos[0].getDoorColor().getNextColor();
    // Update the door color for all sections
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setDoorColor(nextPaintColor);
    }
    return nextPaintColor;
  }

  /**
   * Gets the next visor paint color in the sequence.
   *
   * @return the next visor paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextVisorPaintColor() {
    TrafficSignalBodyColor nextPaintColor = sectionInfos[0].getVisorColor().getNextColor();
    // Update the visor color for all sections
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setVisorColor(nextPaintColor);
    }
    return nextPaintColor;
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
    TrafficSignalVisorType nextVisorType = sectionInfos[0].getVisorType().getNextVisorType();
    // Update the visor type for all sections
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setVisorType(nextVisorType);
    }
    return nextVisorType;
  }
}
