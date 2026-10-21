package com.micatechnologies.minecraft.csm.technology.rack;

/**
 * The rack-mount units: what each is called, how many U it takes, how deep it is and where its
 * face is on the unit sheet.
 *
 * <p><b>Written by {@code dev-env-utils/scripts/gen_technology_rack.py}; edit it there.</b> The
 * ordinals are saved in every rack ({@link RackContents}), so units are only ever appended, and
 * each unit's band on the sheet follows from the ones above it.</p>
 *
 * @since 2026.10
 */
public enum RackUnitType {

  /** 1U Rack Server. */
  SERVER_1U("rack_unit_server_1u", 1, 11.0f, 0, 0),
  /** 2U Rack Server. */
  SERVER_2U("rack_unit_server_2u", 2, 11.0f, 6, 0),
  /** 4U Storage Array (SAN). */
  STORAGE_4U("rack_unit_storage_4u", 4, 12.0f, 18, 0),
  /** Micafi 24-Port Rack Switch. */
  SWITCH_24_PORT("rack_unit_switch_24_port", 1, 4.0f, 42, 1),
  /** Micafi 48-Port Rack Switch. */
  SWITCH_48_PORT("rack_unit_switch_48_port", 1, 5.0f, 48, 1),
  /** 24-Port Patch Panel. */
  PATCH_PANEL("rack_unit_patch_panel", 1, 2.0f, 54, 0),
  /** 1U Firewall Appliance. */
  FIREWALL("rack_unit_firewall", 1, 5.0f, 60, 0),
  /** 2U Rack UPS. */
  UPS_2U("rack_unit_ups_2u", 2, 10.0f, 66, 0),
  /** 1U Rack PDU. */
  PDU("rack_unit_pdu", 1, 3.0f, 78, 3),
  /** 1U KVM Console Drawer. */
  KVM_DRAWER("rack_unit_kvm_drawer", 1, 10.0f, 84, 0),
  /** 1U Cable Manager. */
  CABLE_MANAGER("rack_unit_cable_manager", 1, 2.0f, 90, 0),
  /** 1U Blanking Plate. */
  BLANKING_PLATE("rack_unit_blanking_plate", 1, 0.5f, 96, 0),
  /** Micafi Cloud Gateway (1U). */
  MICAFI_GATEWAY("rack_unit_micafi_gateway", 1, 4.0f, 102, 2),
  /** 2U GPU Server. */
  GPU_SERVER_2U("rack_unit_gpu_server_2u", 2, 12.0f, 108, 0);

  /** The unit sheet: every unit's face, and its sides' colour, on one animated sprite. */
  public static final String SHEET = "csm:blocks/technology/rack/rack_units";

  /** The sheet's size in texels, a frame. */
  public static final int SHEET_TEXELS = 128;

  /** Texels a U down the sheet. */
  public static final int TEXELS_PER_U = 6;

  /** The first row of the colour swatches, each 32 texels across and 8 down. */
  public static final int SWATCH_ROW = 120;

  private final String registryName;
  private final int heightU;
  private final float depth;
  private final int bandRow;
  private final int swatch;

  RackUnitType(String registryName, int heightU, float depth, int bandRow, int swatch) {
    this.registryName = registryName;
    this.heightU = heightU;
    this.depth = depth;
    this.bandRow = bandRow;
    this.swatch = swatch;
  }

  /** The unit's item's registry name. */
  public String getRegistryName() {
    return registryName;
  }

  /** How many U the unit takes. */
  public int getHeightU() {
    return heightU;
  }

  /** How deep the unit is, in model pixels (a sixteenth of a block). */
  public float getDepth() {
    return depth;
  }

  /** The sheet row its face starts on; it runs {@code heightU * TEXELS_PER_U} rows. */
  public int getBandRow() {
    return bandRow;
  }

  /** Which colour swatch its sides take: 0 black, 1 silver, 2 white, 3 graphite. */
  public int getSwatch() {
    return swatch;
  }

  /**
   * The unit with this ordinal, or {@code null} for one this version does not know (a rack
   * saved by a later version).
   *
   * @param ordinal the saved ordinal
   * @return the unit, or {@code null}
   */
  public static RackUnitType fromOrdinal(int ordinal) {
    RackUnitType[] all = values();
    return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
  }
}
