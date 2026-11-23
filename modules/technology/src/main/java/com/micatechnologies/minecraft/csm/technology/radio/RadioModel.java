package com.micatechnologies.minecraft.csm.technology.radio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The makes of handheld radio: each one's presets, which settings it has, and the colours of the
 * screen its settings are shown on. Every brand is invented.
 *
 * <p>The two public-safety radios share one list of talkgroups, and the dual-band radio's
 * memories include the consumer radio's first channels, so the makes have channels in common
 * (see {@link RadioChannel}).</p>
 *
 * @since 2026.11
 */
public enum RadioModel {

  /** The cheap dual-band handheld: a list of memories shown as frequencies. */
  BOWFENG_BF5("BOWFENG", "BF-5", dualBandMemories(), true, true, false,
      0xFF17181A, 0xFF2A2C30, 0xFF2F55E8, 0xFFF4F7FF, 0xFFE8641E),

  /** The grey-railed multiband public-safety portable with a colour screen. */
  HARROW_XG100("HARROW", "UNISON XG-100", talkgroups(), false, true, true,
      0xFF8C9096, 0xFF1A1B1E, 0xFF0C1A3C, 0xFFEFF4FF, 0xFFF0B428),

  /** The blister-pack consumer radio: 22 numbered channels and a privacy code. */
  MICAROLLA_EM1000("MICAROLLA", "TALKMATE EM-1000", consumerChannels(), true, false, false,
      0xFF1B1C1F, 0xFFB9BDC2, 0xFFB7C2AE, 0xFF161A14, 0xFFC8201E),

  /** The rugged police and fire portable, in black or high-visibility yellow-green. */
  MICAROLLA_APEX8000("MICAROLLA", "APEX 8000", talkgroups(), false, true, true,
      0xFF1A1B1D, 0xFF2B2D30, 0xFFD9E0D2, 0xFF12140F, 0xFFE8641E);

  /** The highest privacy code a radio with codes has; 0 is no code. */
  public static final int MAX_PRIVACY_CODE = 38;

  private final String brand;
  private final String product;
  private final List<RadioChannel> channels;
  private final boolean privacyCode;
  private final boolean scan;
  private final boolean unitId;
  private final int bodyColor;
  private final int bezelColor;
  private final int screenColor;
  private final int screenTextColor;
  private final int accentColor;

  RadioModel(String brand, String product, List<RadioChannel> channels, boolean privacyCode,
      boolean scan, boolean unitId, int bodyColor, int bezelColor, int screenColor,
      int screenTextColor, int accentColor) {
    this.brand = brand;
    this.product = product;
    this.channels = Collections.unmodifiableList(channels);
    this.privacyCode = privacyCode;
    this.scan = scan;
    this.unitId = unitId;
    this.bodyColor = bodyColor;
    this.bezelColor = bezelColor;
    this.screenColor = screenColor;
    this.screenTextColor = screenTextColor;
    this.accentColor = accentColor;
  }

  /** The maker's name as the radio carries it. */
  public String getBrand() {
    return brand;
  }

  /** The product's name as the radio carries it. */
  public String getProduct() {
    return product;
  }

  /** The radio's presets, in the order its channel keys step through them. */
  public List<RadioChannel> getChannels() {
    return channels;
  }

  /** Whether the radio has a privacy code (a sub-audible tone number). */
  public boolean hasPrivacyCode() {
    return privacyCode;
  }

  /** Whether the radio can scan its presets. */
  public boolean hasScan() {
    return scan;
  }

  /** Whether the radio carries a unit ID. */
  public boolean hasUnitId() {
    return unitId;
  }

  /** The body's colour on the settings screen, ARGB. */
  public int getBodyColor() {
    return bodyColor;
  }

  /** The colour round the display on the settings screen, ARGB. */
  public int getBezelColor() {
    return bezelColor;
  }

  /** The lit display's colour, ARGB. */
  public int getScreenColor() {
    return screenColor;
  }

  /** The colour of the display's writing, ARGB. */
  public int getScreenTextColor() {
    return screenTextColor;
  }

  /** The colour of the radio's one bright key, ARGB. */
  public int getAccentColor() {
    return accentColor;
  }

  private static RadioChannel channel(String name, int frequencyHz) {
    return new RadioChannel(name, frequencyHz, false);
  }

  /** The 22 licence-free consumer channels, at their real frequencies. */
  private static List<RadioChannel> consumerChannels() {
    List<RadioChannel> list = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      list.add(channel("CH " + (i + 1), 462_562_500 + i * 25_000));
    }
    for (int i = 0; i < 7; i++) {
      list.add(channel("CH " + (i + 8), 467_562_500 + i * 25_000));
    }
    for (int i = 0; i < 8; i++) {
      list.add(channel("CH " + (i + 15), 462_550_000 + i * 25_000));
    }
    return list;
  }

  /** The dual-band radio's memories: a hobbyist's mix, three of them the consumer channels. */
  private static List<RadioChannel> dualBandMemories() {
    List<RadioChannel> list = new ArrayList<>();
    list.add(channel("MR 001", 462_562_500));
    list.add(channel("MR 002", 462_587_500));
    list.add(channel("MR 003", 462_612_500));
    list.add(channel("MR 004", 146_520_000));
    list.add(channel("MR 005", 446_000_000));
    list.add(channel("MR 006", 151_820_000));
    list.add(channel("MR 007", 151_880_000));
    list.add(channel("MR 008", 151_940_000));
    list.add(channel("MR 009", 154_570_000));
    list.add(channel("MR 010", 154_600_000));
    list.add(new RadioChannel("MR 011", 162_550_000, true));
    list.add(new RadioChannel("MR 012", 162_400_000, true));
    return list;
  }

  /** A city's talkgroups, the same on both public-safety radios. */
  private static List<RadioChannel> talkgroups() {
    List<RadioChannel> list = new ArrayList<>();
    list.add(channel("DISPATCH", 851_012_500));
    list.add(channel("TAC 1", 851_512_500));
    list.add(channel("TAC 2", 852_012_500));
    list.add(channel("TAC 3", 852_512_500));
    list.add(channel("FIRE", 853_012_500));
    list.add(channel("FIREGROUND", 853_512_500));
    list.add(channel("EMS", 854_012_500));
    list.add(channel("PUBLIC WORKS", 855_012_500));
    list.add(channel("TRANSIT", 856_012_500));
    list.add(channel("EVENTS", 857_012_500));
    return list;
  }
}
