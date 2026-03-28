package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;

public class TrafficSignalSectionInfo {

  private static final TrafficSignalBodyColor DEFAULT_BODY_PAINT_COLOR =
      TrafficSignalBodyColor.FLAT_BLACK;
  private static final TrafficSignalBodyColor DEFAULT_DOOR_PAINT_COLOR =
      TrafficSignalBodyColor.FLAT_BLACK;
  private static final TrafficSignalBodyColor DEFAULT_VISOR_PAINT_COLOR =
      TrafficSignalBodyColor.FLAT_BLACK;
  private static final TrafficSignalVisorType DEFAULT_VISOR_TYPE = TrafficSignalVisorType.TUNNEL;
  private static final TrafficSignalBulbStyle DEFAULT_BULB_STYLE = TrafficSignalBulbStyle.LED;
  private static final TrafficSignalBulbType DEFAULT_BULB_TYPE = TrafficSignalBulbType.BALL;
  private static final TrafficSignalBulbColor DEFAULT_BULB_COLOR = TrafficSignalBulbColor.GREEN;
  private static final boolean DEFAULT_BULB_LIT = false;
  private static final boolean DEFAULT_BULB_FLASHING = false;


  private static final String KEY_BODY_COLOR = "bodyColor";
  private static final String KEY_DOOR_COLOR = "doorColor";
  private static final String KEY_VISOR_COLOR = "visorColor";
  private static final String KEY_VISOR_TYPE = "visorType";
  private static final String KEY_BULB_STYLE = "bulbStyle";
  private static final String KEY_BULB_TYPE = "bulbType";
  private static final String KEY_BULB_COLOR = "bulbColor";
  private static final String KEY_BULB_CUSTOM_COLOR = "bulbCustomColor";
  private static final String KEY_BULB_LIT = "bulbLit";
  private static final String KEY_BULB_FLASHING = "bulbFlashing";

  private TrafficSignalBodyColor bodyColor;
  private TrafficSignalBodyColor doorColor;
  private TrafficSignalBodyColor visorColor;
  private TrafficSignalVisorType visorType;
  private TrafficSignalBulbStyle bulbStyle;
  private TrafficSignalBulbType bulbType;
  private TrafficSignalBulbColor bulbColor;
  private TrafficSignalBulbColor bulbCustomColor;
  private boolean bulbLit;
  private boolean bulbFlashing;

  public TrafficSignalSectionInfo() {
    this.bodyColor = DEFAULT_BODY_PAINT_COLOR;
    this.doorColor = DEFAULT_DOOR_PAINT_COLOR;
    this.visorColor = DEFAULT_VISOR_PAINT_COLOR;
    this.visorType = DEFAULT_VISOR_TYPE;
    this.bulbStyle = DEFAULT_BULB_STYLE;
    this.bulbType = DEFAULT_BULB_TYPE;
    this.bulbColor = DEFAULT_BULB_COLOR;
    this.bulbCustomColor = DEFAULT_BULB_COLOR;
    this.bulbLit = DEFAULT_BULB_LIT;
    this.bulbFlashing = DEFAULT_BULB_FLASHING;
  }

  public TrafficSignalSectionInfo(TrafficSignalBodyColor bodyColor,
      TrafficSignalBodyColor doorColor,
      TrafficSignalBodyColor visorColor, TrafficSignalVisorType visorType,
      TrafficSignalBulbStyle bulbStyle, TrafficSignalBulbType bulbType,
      TrafficSignalBulbColor bulbColor,boolean bulbFlashing) {
    this.bodyColor = bodyColor;
    this.doorColor = doorColor;
    this.visorColor = visorColor;
    this.visorType = visorType;
    this.bulbStyle = bulbStyle;
    this.bulbType = bulbType;
    this.bulbColor = bulbColor;
    this.bulbCustomColor = bulbColor;
    this.bulbLit = DEFAULT_BULB_LIT;
    this.bulbFlashing = bulbFlashing;
  }

  public TrafficSignalSectionInfo(TrafficSignalBodyColor bodyColor,
      TrafficSignalBodyColor doorColor,
      TrafficSignalBodyColor visorColor, TrafficSignalVisorType visorType,
      TrafficSignalBulbStyle bulbStyle, TrafficSignalBulbType bulbType,
      TrafficSignalBulbColor bulbColor,boolean bulbLit,boolean bulbFlashing) {
    this.bodyColor = bodyColor;
    this.doorColor = doorColor;
    this.visorColor = visorColor;
    this.visorType = visorType;
    this.bulbStyle = bulbStyle;
    this.bulbType = bulbType;
    this.bulbColor = bulbColor;
    this.bulbCustomColor = bulbColor;
    this.bulbLit = bulbLit;
    this.bulbFlashing = bulbFlashing;
  }

  public TrafficSignalSectionInfo(TrafficSignalBodyColor bodyColor,
      TrafficSignalBodyColor doorColor,
      TrafficSignalBodyColor visorColor, TrafficSignalVisorType visorType,
      TrafficSignalBulbStyle bulbStyle, TrafficSignalBulbType bulbType,
      TrafficSignalBulbColor bulbColor,TrafficSignalBulbColor bulbCustomColor,boolean bulbLit,boolean bulbFlashing) {
    this.bodyColor = bodyColor;
    this.doorColor = doorColor;
    this.visorColor = visorColor;
    this.visorType = visorType;
    this.bulbStyle = bulbStyle;
    this.bulbType = bulbType;
    this.bulbColor = bulbColor;
    this.bulbCustomColor = bulbCustomColor;
    this.bulbLit = bulbLit;
    this.bulbFlashing = bulbFlashing;
  }

  public TrafficSignalBodyColor getBodyColor() {
    return bodyColor;
  }

  public TrafficSignalBodyColor getDoorColor() {
    return doorColor;
  }

  public TrafficSignalBodyColor getVisorColor() {
    return visorColor;
  }

  public TrafficSignalVisorType getVisorType() {
    return visorType;
  }

  public TrafficSignalBulbStyle getBulbStyle() {
    return bulbStyle;
  }

  public TrafficSignalBulbType getBulbType() {
    return bulbType;
  }

  public TrafficSignalBulbColor getBulbColor() {
    return bulbColor;
  }

  public TrafficSignalBulbColor getBulbCustomColor() {
    return bulbCustomColor;
  }

  public boolean isBulbLit() {
    return bulbLit;
  }

  public boolean isBulbFlashing() {
    return bulbFlashing;
  }

  public void setBodyColor(TrafficSignalBodyColor bodyColor) {
    this.bodyColor = bodyColor;
  }

  public void setDoorColor(TrafficSignalBodyColor doorColor) {
    this.doorColor = doorColor;
  }

  public void setVisorColor(TrafficSignalBodyColor visorColor) {
    this.visorColor = visorColor;
  }

  public void setVisorType(TrafficSignalVisorType visorType) {
    this.visorType = visorType;
  }

  public void setBulbStyle(TrafficSignalBulbStyle bulbStyle) {
    this.bulbStyle = bulbStyle;
  }

  public void setBulbType(TrafficSignalBulbType bulbType) {
    this.bulbType = bulbType;
  }

  public void setBulbColor(TrafficSignalBulbColor bulbColor) {
    this.bulbColor = bulbColor;
  }

  public void setBulbCustomColor(TrafficSignalBulbColor bulbCustomColor) {
    this.bulbCustomColor = bulbCustomColor;
  }

  public void setBulbLit(boolean bulbLit) {
    this.bulbLit = bulbLit;
  }

  public void setBulbFlashing(boolean bulbFlashing) {
    this.bulbFlashing = bulbFlashing;
  }

  /**
   * Converts the TrafficSignalSectionInfo to an NBTTagCompound for NBT storage.
   *
   * @return An NBTTagCompound containing the traffic signal section data.
   */
  public NBTTagCompound toNBT() {
    NBTTagCompound nbt = new NBTTagCompound();
    nbt.setInteger(KEY_BODY_COLOR, bodyColor.toNBT());
    nbt.setInteger(KEY_DOOR_COLOR, doorColor.toNBT());
    nbt.setInteger(KEY_VISOR_COLOR, visorColor.toNBT());
    nbt.setInteger(KEY_VISOR_TYPE, visorType.toNBT());
    nbt.setInteger(KEY_BULB_STYLE, bulbStyle.toNBT());
    nbt.setInteger(KEY_BULB_TYPE, bulbType.toNBT());
    nbt.setInteger(KEY_BULB_COLOR, bulbColor.toNBT());
    nbt.setInteger(KEY_BULB_CUSTOM_COLOR, bulbCustomColor.toNBT());
    nbt.setBoolean(KEY_BULB_LIT, bulbLit);
    nbt.setBoolean(KEY_BULB_FLASHING, bulbFlashing);
    return nbt;
  }

  /**
   * Converts the TrafficSignalSectionInfo to an array of integers for NBT storage.
   *
   * @return An array of integers representing the traffic signal section data.
   */
  public int[] toNBTArray() {
    return new int[] {
        bodyColor.toNBT(),
        doorColor.toNBT(),
        visorColor.toNBT(),
        visorType.toNBT(),
        bulbStyle.toNBT(),
        bulbType.toNBT(),
        bulbColor.toNBT(),
        bulbCustomColor.toNBT(),
        bulbLit ? 1 : 0, // Convert boolean to int (1 for true, 0 for false)
        bulbFlashing ? 1 : 0 // Convert boolean to int (1 for true, 0 for false)
    };
  }

  /**
   * Creates a TrafficSignalSectionInfo from NBT data.
   *
   * @param nbt The NBTTagCompound containing the traffic signal section data.
   * @return A TrafficSignalSectionInfo object populated with the data from the NBT.
   */
  public static TrafficSignalSectionInfo fromNBT(NBTTagCompound nbt) {
    TrafficSignalSectionInfo info = new TrafficSignalSectionInfo();
    info.bodyColor = TrafficSignalBodyColor.fromNBT(nbt.getInteger(KEY_BODY_COLOR));
    info.doorColor = TrafficSignalBodyColor.fromNBT(nbt.getInteger(KEY_DOOR_COLOR));
    info.visorColor = TrafficSignalBodyColor.fromNBT(nbt.getInteger(KEY_VISOR_COLOR));
    info.visorType = TrafficSignalVisorType.fromNBT(nbt.getInteger(KEY_VISOR_TYPE));
    info.bulbStyle = TrafficSignalBulbStyle.fromNBT(nbt.getInteger(KEY_BULB_STYLE));
    info.bulbType = TrafficSignalBulbType.fromNBT(nbt.getInteger(KEY_BULB_TYPE));
    info.bulbColor = TrafficSignalBulbColor.fromNBT(nbt.getInteger(KEY_BULB_COLOR));
    info.bulbCustomColor = TrafficSignalBulbColor.fromNBT(nbt.getInteger(KEY_BULB_CUSTOM_COLOR));
    info.bulbLit = nbt.getBoolean(KEY_BULB_LIT);
    info.bulbFlashing = nbt.getBoolean(KEY_BULB_FLASHING);
    return info;
  }

  /**
   * Creates a TrafficSignalSectionInfo from an array of integers.
   *
   * @param data An array of integers representing the traffic signal section data.
   * @return A TrafficSignalSectionInfo object populated with the data from the array.
   */
  public static TrafficSignalSectionInfo fromNBTArray(int[] data) {
    if (data == null || data.length < 10) {
      return new TrafficSignalSectionInfo();
    }
    return new TrafficSignalSectionInfo(
        TrafficSignalBodyColor.fromNBT(data[0]),
        TrafficSignalBodyColor.fromNBT(data[1]),
        TrafficSignalBodyColor.fromNBT(data[2]),
        TrafficSignalVisorType.fromNBT(data[3]),
        TrafficSignalBulbStyle.fromNBT(data[4]),
        TrafficSignalBulbType.fromNBT(data[5]),
        TrafficSignalBulbColor.fromNBT(data[6]),
        TrafficSignalBulbColor.fromNBT(data[7]),
        data[8] == 1, // Convert int to boolean (1 for true, 0 for false)
        data[9] == 1 // Convert int to boolean (1 for true, 0 for false)
    );
  }
}
