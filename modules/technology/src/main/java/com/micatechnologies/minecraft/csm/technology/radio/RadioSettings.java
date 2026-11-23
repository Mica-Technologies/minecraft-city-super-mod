package com.micatechnologies.minecraft.csm.technology.radio;

import net.minecraft.nbt.NBTTagCompound;

/**
 * What a handheld radio is set to: on or off, its preset, volume, squelch, privacy code, scan
 * and unit ID.
 *
 * <p>The same keys hold the settings in a placed radio's tile entity and in a held radio's
 * stack, where they sit in the stack's {@code BlockEntityTag}: vanilla copies that compound into
 * the tile entity when the block is placed, so setting a radio down and picking it up again
 * needs no translation either way. Values are stored for every make (a radio with no privacy
 * code stores 0), so a setting a make gains later is already there.</p>
 *
 * <p>Nothing read from NBT or from a packet is trusted: {@link #read} keeps every value inside
 * the widest range any make has, and {@link #forModel} narrows it to one make's.</p>
 *
 * @since 2026.11
 */
public final class RadioSettings {

  /** The stack compound vanilla copies into the tile entity on placement. */
  public static final String STACK_TAG = "BlockEntityTag";

  /** The loudest volume step. */
  public static final int MAX_VOLUME = 10;
  /** The tightest squelch step. */
  public static final int MAX_SQUELCH = 9;
  /** The longest unit ID, in characters. */
  public static final int MAX_UNIT_ID_LENGTH = 8;
  /** The most presets any make may have; a stored preset index past it is not kept. */
  static final int MAX_CHANNELS = 64;

  private static final String KEY_ON = "on";
  private static final String KEY_CHANNEL = "ch";
  private static final String KEY_VOLUME = "vol";
  private static final String KEY_SQUELCH = "sq";
  private static final String KEY_PRIVACY_CODE = "pc";
  private static final String KEY_SCAN = "scan";
  private static final String KEY_UNIT_ID = "id";

  private final boolean on;
  private final int channel;
  private final int volume;
  private final int squelch;
  private final int privacyCode;
  private final boolean scan;
  private final String unitId;

  /** A radio as it comes out of the box: off, first preset, middling volume and squelch. */
  public static final RadioSettings DEFAULT = new RadioSettings(false, 0, 5, 3, 0, false, "");

  /**
   * Settings from values that may have come from anywhere; each is brought into range.
   *
   * @param on          whether the radio is on
   * @param channel     the preset's index
   * @param volume      the volume step
   * @param squelch     the squelch step
   * @param privacyCode the privacy code, 0 for none
   * @param scan        whether the radio is scanning
   * @param unitId      the unit ID
   */
  public RadioSettings(boolean on, int channel, int volume, int squelch, int privacyCode,
      boolean scan, String unitId) {
    this.on = on;
    this.channel = channel >= 0 && channel < MAX_CHANNELS ? channel : 0;
    this.volume = clamp(volume, 0, MAX_VOLUME);
    this.squelch = clamp(squelch, 0, MAX_SQUELCH);
    this.privacyCode = clamp(privacyCode, 0, RadioModel.MAX_PRIVACY_CODE);
    this.scan = scan;
    this.unitId = cleanUnitId(unitId);
  }

  private static int clamp(int value, int low, int high) {
    return value < low ? low : Math.min(value, high);
  }

  /**
   * A unit ID as a radio's screen can show it: capitals, digits, space and dash, at most
   * {@link #MAX_UNIT_ID_LENGTH} characters, with no space at either end.
   *
   * @param raw the text typed or stored, possibly null
   *
   * @return the unit ID to keep
   */
  public static String cleanUnitId(String raw) {
    if (raw == null) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < raw.length() && out.length() < MAX_UNIT_ID_LENGTH; i++) {
      char c = Character.toUpperCase(raw.charAt(i));
      if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '-') {
        out.append(c);
      }
    }
    return out.toString().trim();
  }

  /**
   * Reads settings from a tile entity's compound or a stack's {@link #STACK_TAG}.
   *
   * @param compound the compound, or null for a radio that has never been set
   *
   * @return the settings, every value in range
   */
  public static RadioSettings read(NBTTagCompound compound) {
    if (compound == null) {
      return DEFAULT;
    }
    return new RadioSettings(compound.getBoolean(KEY_ON),
        compound.getInteger(KEY_CHANNEL),
        compound.hasKey(KEY_VOLUME) ? compound.getInteger(KEY_VOLUME) : DEFAULT.volume,
        compound.hasKey(KEY_SQUELCH) ? compound.getInteger(KEY_SQUELCH) : DEFAULT.squelch,
        compound.getInteger(KEY_PRIVACY_CODE),
        compound.getBoolean(KEY_SCAN),
        compound.getString(KEY_UNIT_ID));
  }

  /**
   * Writes the settings into a compound.
   *
   * @param compound the compound to write into
   *
   * @return the same compound
   */
  public NBTTagCompound write(NBTTagCompound compound) {
    compound.setBoolean(KEY_ON, on);
    compound.setByte(KEY_CHANNEL, (byte) channel);
    compound.setByte(KEY_VOLUME, (byte) volume);
    compound.setByte(KEY_SQUELCH, (byte) squelch);
    compound.setByte(KEY_PRIVACY_CODE, (byte) privacyCode);
    compound.setBoolean(KEY_SCAN, scan);
    compound.setString(KEY_UNIT_ID, unitId);
    return compound;
  }

  /**
   * These settings as one make of radio can hold them: a preset it has, and nothing set that it
   * lacks.
   *
   * @param model the make
   *
   * @return the settings narrowed to it
   */
  public RadioSettings forModel(RadioModel model) {
    return new RadioSettings(on,
        channel < model.getChannels().size() ? channel : 0,
        volume,
        squelch,
        model.hasPrivacyCode() ? privacyCode : 0,
        model.hasScan() && scan,
        model.hasUnitId() ? unitId : "");
  }

  /** Whether the radio is on. */
  public boolean isOn() {
    return on;
  }

  /** The preset's index in its make's list. */
  public int getChannel() {
    return channel;
  }

  /** The volume step, 0 to {@link #MAX_VOLUME}. */
  public int getVolume() {
    return volume;
  }

  /** The squelch step, 0 to {@link #MAX_SQUELCH}. */
  public int getSquelch() {
    return squelch;
  }

  /** The privacy code, 0 for none. */
  public int getPrivacyCode() {
    return privacyCode;
  }

  /** Whether the radio is scanning its presets. */
  public boolean isScan() {
    return scan;
  }

  /** The unit ID, possibly empty. */
  public String getUnitId() {
    return unitId;
  }

  /** These settings with the radio turned on or off. */
  public RadioSettings withOn(boolean value) {
    return new RadioSettings(value, channel, volume, squelch, privacyCode, scan, unitId);
  }

  /** These settings on another preset. */
  public RadioSettings withChannel(int value) {
    return new RadioSettings(on, value, volume, squelch, privacyCode, scan, unitId);
  }

  /** These settings at another volume. */
  public RadioSettings withVolume(int value) {
    return new RadioSettings(on, channel, value, squelch, privacyCode, scan, unitId);
  }

  /** These settings at another squelch. */
  public RadioSettings withSquelch(int value) {
    return new RadioSettings(on, channel, volume, value, privacyCode, scan, unitId);
  }

  /** These settings with another privacy code. */
  public RadioSettings withPrivacyCode(int value) {
    return new RadioSettings(on, channel, volume, squelch, value, scan, unitId);
  }

  /** These settings with scan on or off. */
  public RadioSettings withScan(boolean value) {
    return new RadioSettings(on, channel, volume, squelch, privacyCode, value, unitId);
  }

  /** These settings with another unit ID. */
  public RadioSettings withUnitId(String value) {
    return new RadioSettings(on, channel, volume, squelch, privacyCode, scan, value);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof RadioSettings)) {
      return false;
    }
    RadioSettings o = (RadioSettings) other;
    return on == o.on && channel == o.channel && volume == o.volume && squelch == o.squelch
        && privacyCode == o.privacyCode && scan == o.scan && unitId.equals(o.unitId);
  }

  @Override
  public int hashCode() {
    int hash = unitId.hashCode();
    hash = hash * 31 + channel;
    hash = hash * 31 + volume;
    hash = hash * 31 + squelch;
    hash = hash * 31 + privacyCode;
    return hash * 4 + (on ? 2 : 0) + (scan ? 1 : 0);
  }
}
