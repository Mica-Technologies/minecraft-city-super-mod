package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * What one mile marker says, exactly as it will be drawn: the mile number, the tenth (".2", or
 * empty), and on an enhanced plate the direction, the route shield and the route number.
 *
 * <p>Immutable and compared by value, because it is the key the baked model caches its quads
 * under: a line of markers counting up a highway bakes each number once. It is always built
 * through {@link #of}, which clamps every value to what the plate can carry, so the model never
 * has to cope with a number it has no room for.</p>
 *
 * @since 2026.9
 */
public final class MileMarkerLegend {

  /** Direction words, in the order the glyph sheet holds them. */
  public static final String[] DIRECTIONS = {"North", "South", "East", "West"};

  /** Longest route number an enhanced plate's shield takes. */
  public static final int MAX_ROUTE_LENGTH = 3;

  private final String mile;
  private final String tenth;
  @Nullable
  private final GuideSignShieldType shield;
  private final String route;
  private final int direction;

  private MileMarkerLegend(String mile, String tenth, @Nullable GuideSignShieldType shield,
      String route, int direction) {
    this.mile = mile;
    this.tenth = tenth;
    this.shield = shield;
    this.route = route;
    this.direction = direction;
  }

  /**
   * The legend a plate shows for the given settings, each clamped to what the plate carries.
   *
   * @param layout    the plate
   * @param mile      the mile number; below zero for the plate's default
   * @param tenth     tenths of a mile, 0 to 9 (ignored on a plate without a tenth panel)
   * @param shield    the route shield (ignored unless enhanced); null for the default
   * @param route     the route number (ignored unless enhanced)
   * @param direction 0 to 3, north, south, east, west (ignored unless enhanced)
   *
   * @return the legend
   */
  public static MileMarkerLegend of(MileMarkerLayout layout, int mile, int tenth,
      @Nullable GuideSignShieldType shield, @Nullable String route, int direction) {
    int m = mile < 0 ? layout.getDefaultMile()
        : Math.max(layout.getMinMile(), Math.min(layout.getMaxMile(), mile));
    String t = layout.hasTenth() ? "." + Math.max(0, Math.min(9, tenth)) : "";
    if (!layout.isEnhanced()) {
      return new MileMarkerLegend(Integer.toString(m), t, null, "", -1);
    }
    return new MileMarkerLegend(Integer.toString(m), t,
        shield == null ? GuideSignShieldType.US_ROUTE : shield, clampRoute(route),
        Math.max(0, Math.min(DIRECTIONS.length - 1, direction)));
  }

  /** The legend a newly placed marker (and the marker in the inventory) shows. */
  public static MileMarkerLegend defaultFor(MileMarkerLayout layout) {
    return of(layout, -1, 2, GuideSignShieldType.US_ROUTE, "12", 3);
  }

  /**
   * A route number as a shield will carry it: digits only, at most {@link #MAX_ROUTE_LENGTH}.
   * The glyph sheet holds the guide sign font's numerals and nothing else.
   *
   * @param value the raw value
   *
   * @return the value the shield will print
   */
  public static String clampRoute(@Nullable String value) {
    if (value == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(MAX_ROUTE_LENGTH);
    for (int i = 0; i < value.length() && out.length() < MAX_ROUTE_LENGTH; i++) {
      char c = value.charAt(i);
      if (c >= '0' && c <= '9') {
        out.append(c);
      }
    }
    return out.toString();
  }

  public String getMile() {
    return mile;
  }

  /** The tenth panel's legend (".2"), or empty on a plate without one. */
  public String getTenth() {
    return tenth;
  }

  /** The route shield, or null on a plate that has none. */
  @Nullable
  public GuideSignShieldType getShield() {
    return shield;
  }

  public String getRoute() {
    return route;
  }

  /** 0 to 3 (north, south, east, west), or -1 on a plate that has none. */
  public int getDirection() {
    return direction;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MileMarkerLegend)) {
      return false;
    }
    MileMarkerLegend other = (MileMarkerLegend) o;
    return direction == other.direction && shield == other.shield && mile.equals(other.mile)
        && tenth.equals(other.tenth) && route.equals(other.route);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mile, tenth, shield, route, direction);
  }

  @Override
  public String toString() {
    return "mile=" + mile + tenth + (shield == null ? ""
        : ",shield=" + shield.getName() + ",route=" + route + ",dir=" + direction);
  }
}
