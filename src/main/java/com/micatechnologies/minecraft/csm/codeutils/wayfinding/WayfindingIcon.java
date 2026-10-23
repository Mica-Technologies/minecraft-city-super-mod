package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

/**
 * A wayfinding panel's icons: the panel's pictogram ({@code p}) and each line's icons
 * ({@code il1}..{@code il4}, {@code ir1}..{@code ir4}) are all one of these. Most are a cell of the
 * icon sheet ({@link WayfindingSheet}); the eight {@code arrow_*} icons are the arrow sprite
 * turned, so a line can carry its own arrow.
 *
 * <p>Saved by {@link #getId() id}, never by ordinal, so the list is in the order the editor's
 * picker shows it and may be reordered or grown. Every id the airport and hospital panels ever
 * saved is here. An id that is not known (a sign from a later version) reads as {@link #NONE}.</p>
 *
 * @since 2026.10
 */
public enum WayfindingIcon {
  NONE("none", null),
  ARROW_LEFT("arrow_left", WayfindingArrow.LEFT),
  ARROW_RIGHT("arrow_right", WayfindingArrow.RIGHT),
  ARROW_UP("arrow_up", WayfindingArrow.UP),
  ARROW_DOWN("arrow_down", WayfindingArrow.DOWN),
  ARROW_UP_LEFT("arrow_up_left", WayfindingArrow.UP_LEFT),
  ARROW_UP_RIGHT("arrow_up_right", WayfindingArrow.UP_RIGHT),
  ARROW_DOWN_LEFT("arrow_down_left", WayfindingArrow.DOWN_LEFT),
  ARROW_DOWN_RIGHT("arrow_down_right", WayfindingArrow.DOWN_RIGHT),
  INFO("info", null),
  EXIT("exit", null),
  STAIRS("stairs", null),
  ELEVATOR("elevator", null),
  ESCALATOR("escalator", null),
  ACCESSIBLE("accessible", null),
  RESTROOM("restroom", null),
  RESTROOM_MEN("restroom_men", null),
  RESTROOM_WOMEN("restroom_women", null),
  RESTROOM_FAMILY("restroom_family", null),
  BABY_CHANGE("baby_change", null),
  DRINKING_WATER("drinking_water", null),
  CAFE("cafe", null),
  FOOD("food", null),
  SHOP("shop", null),
  CASH("cash", null),
  TELEPHONE("telephone", null),
  WIFI("wifi", null),
  CHARGING("charging", null),
  LOCKERS("lockers", null),
  GALLERY("gallery", null),
  CHAPEL("chapel", null),
  SECURITY("security", null),
  FIRST_AID("first_aid", null),
  AED("aed", null),
  FIRE_EXTINGUISHER("fire_extinguisher", null),
  ASSEMBLY_POINT("assembly_point", null),
  NO_SMOKING("no_smoking", null),
  PARKING("parking", null),
  BICYCLE("bicycle", null),
  TICKET("ticket", null),
  METRO("metro", null),
  TRAIN("train", null),
  TRAM("tram", null),
  BUS("bus", null),
  TAXI("taxi", null),
  GROUND("ground", null),
  DEPART("depart", null),
  ARRIVE("arrive", null),
  CHECKIN("checkin", null),
  BAGGAGE("baggage", null),
  EMERGENCY("emergency", null),
  XRAY("xray", null),
  LAB("lab", null),
  PHARMACY("pharmacy", null),
  MATERNITY("maternity", null);

  private final String id;
  private final WayfindingArrow arrow;

  WayfindingIcon(String id, WayfindingArrow arrow) {
    this.id = id;
    this.arrow = arrow;
  }

  public String getId() {
    return id;
  }

  /** The arrow this icon draws, or {@code null} for a sheet icon (or none). */
  public WayfindingArrow getArrow() {
    return arrow;
  }

  /** Whether this icon is the arrow sprite turned rather than a cell of the icon sheet. */
  public boolean isArrow() {
    return arrow != null;
  }

  /**
   * The icon the back of a double-sided sign shows: a sideways arrow swapped, so it points the
   * same way in the world; every other icon is itself.
   */
  public WayfindingIcon mirrored() {
    return arrow == null ? this : ofArrow(arrow.mirrored());
  }

  /** The icon that draws an arrow. */
  public static WayfindingIcon ofArrow(WayfindingArrow arrow) {
    for (WayfindingIcon i : values()) {
      if (i.arrow == arrow) {
        return i;
      }
    }
    return NONE;
  }

  /** The icon with this id, {@link #NONE} for one not known. */
  public static WayfindingIcon byId(String id) {
    for (WayfindingIcon i : values()) {
      if (i.id.equals(id)) {
        return i;
      }
    }
    return NONE;
  }
}
