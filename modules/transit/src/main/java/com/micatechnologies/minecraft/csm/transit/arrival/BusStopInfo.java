package com.micatechnologies.minecraft.csm.transit.arrival;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.transit.stop.TransitRoute;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;

/**
 * What an arrival board or an info totem shows besides the countdown: its agency, the stop's name,
 * a direction line, its own routes if it has any, and three switches. Saved on the board's tile
 * entity and edited through one packet ({@link BusStopInfoPacket}); every value goes through the
 * same setters whichever way it arrives, so the caps hold for a save, a packet and the GUI alike.
 *
 * <p><b>Text.</b> Printable ASCII only (32 to 126), since the screen's font draws nothing else;
 * anything else is dropped, the ends are trimmed and the rest cut to its cap.</p>
 *
 * <p><b>NBT keys</b> (short, as {@code PERFORMANCE_AND_SECURITY.md} "Tile entity data" asks):</p>
 * <table>
 *   <caption>keys</caption>
 *   <tr><td>{@code ag}</td><td>byte</td><td>agency, a {@link BoardAgency} ordinal (CITYLINE 0 ..
 *   EMBERLINE 3, METRO 4, CUSTOM 5); an unknown value reads as CITYLINE</td></tr>
 *   <tr><td>{@code an}</td><td>string</td><td>custom agency name, at most
 *   {@value #MAX_AGENCY_NAME}</td></tr>
 *   <tr><td>{@code ac}</td><td>int</td><td>custom agency ground colour, 0xRRGGBB</td></tr>
 *   <tr><td>{@code al}</td><td>int</td><td>custom agency lettering colour, 0xRRGGBB</td></tr>
 *   <tr><td>{@code sn}</td><td>string</td><td>stop name, at most {@value #MAX_STOP_NAME}</td></tr>
 *   <tr><td>{@code dr}</td><td>string</td><td>direction line ("Northbound"), at most
 *   {@value #MAX_DIRECTION}; empty for no second page</td></tr>
 *   <tr><td>{@code rt}</td><td>string</td><td>the board's own routes as typed ("B 30"), at most
 *   {@value #MAX_ROUTES}, read with {@link TransitRoute#parseLine}; empty to list the nearest
 *   stop's</td></tr>
 *   <tr><td>{@code ds}</td><td>boolean</td><td>show each route's destination (default off)</td></tr>
 *   <tr><td>{@code cl}</td><td>boolean</td><td>closed: STATION CLOSED in place of the routes</td></tr>
 *   <tr><td>{@code ts}</td><td>boolean</td><td>two-sided; absent means "as the mount suggests":
 *   two-sided unless on a wall ({@link #isTwoSided})</td></tr>
 * </table>
 *
 * @since 2026.11
 */
public final class BusStopInfo {

  public static final int MAX_AGENCY_NAME = 16;
  public static final int MAX_STOP_NAME = 32;
  public static final int MAX_DIRECTION = 24;
  public static final int MAX_ROUTES = 24;
  /** The most bytes a string may claim on the wire: four UTF-8 bytes a character at most. */
  public static final int MAX_WIRE_BYTES = 128;

  public static final String KEY_AGENCY = "ag";
  public static final String KEY_CUSTOM_NAME = "an";
  public static final String KEY_CUSTOM_GROUND = "ac";
  public static final String KEY_CUSTOM_LEGEND = "al";
  public static final String KEY_STOP_NAME = "sn";
  public static final String KEY_DIRECTION = "dr";
  public static final String KEY_ROUTES = "rt";
  public static final String KEY_DESTINATIONS = "ds";
  public static final String KEY_CLOSED = "cl";
  public static final String KEY_TWO_SIDED = "ts";

  /** A new custom agency's colours: a slate ground and white lettering. */
  public static final int DEFAULT_CUSTOM_GROUND = 0x3A4250;
  public static final int DEFAULT_CUSTOM_LEGEND = 0xF4F4F4;

  private static final int FLAG_DESTINATIONS = 1;
  private static final int FLAG_CLOSED = 2;
  private static final int FLAG_SIDED_SET = 4;
  private static final int FLAG_TWO_SIDED = 8;

  private BoardAgency agency = BoardAgency.CITYLINE;
  private String customName = "";
  private int customGround = DEFAULT_CUSTOM_GROUND;
  private int customLegend = DEFAULT_CUSTOM_LEGEND;
  private String stopName = "";
  private String direction = "";
  private String routes = "";
  private boolean destinations;
  private boolean closed;
  /** Null: as the mount suggests. */
  @Nullable
  private Boolean twoSided;

  /** The routes parsed from {@link #routes}, kept until it changes. */
  private int[] routeKeys = new int[0];
  /** Moves on every change, so a renderer knows to compile its text again. */
  private int version;

  // --- values ---------------------------------------------------------------------------------

  public BoardAgency getAgency() {
    return agency;
  }

  /** Sets the agency; null keeps CITYLINE. */
  public void setAgency(@Nullable BoardAgency agency) {
    BoardAgency a = agency == null ? BoardAgency.CITYLINE : agency;
    if (a != this.agency) {
      this.agency = a;
      version++;
    }
  }

  public String getCustomName() {
    return customName;
  }

  public void setCustomName(@Nullable String name) {
    String s = clean(name, MAX_AGENCY_NAME);
    if (!s.equals(customName)) {
      customName = s;
      version++;
    }
  }

  public int getCustomGround() {
    return customGround;
  }

  public void setCustomGround(int rgb) {
    int c = rgb & 0xFFFFFF;
    if (c != customGround) {
      customGround = c;
      version++;
    }
  }

  public int getCustomLegend() {
    return customLegend;
  }

  public void setCustomLegend(int rgb) {
    int c = rgb & 0xFFFFFF;
    if (c != customLegend) {
      customLegend = c;
      version++;
    }
  }

  public String getStopName() {
    return stopName;
  }

  public void setStopName(@Nullable String name) {
    String s = clean(name, MAX_STOP_NAME);
    if (!s.equals(stopName)) {
      stopName = s;
      version++;
    }
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(@Nullable String text) {
    String s = clean(text, MAX_DIRECTION);
    if (!s.equals(direction)) {
      direction = s;
      version++;
    }
  }

  /** The routes as typed. */
  public String getRoutes() {
    return routes;
  }

  public void setRoutes(@Nullable String text) {
    String s = clean(text, MAX_ROUTES);
    if (!s.equals(routes)) {
      routes = s;
      routeKeys = TransitRoute.parseLine(s);
      version++;
    }
  }

  /**
   * The board's own routes, parsed. Shared, not a copy: read it, never change it.
   *
   * @return the route keys ({@link TransitRoute}), empty to list the nearest stop's
   */
  public int[] getRouteKeys() {
    return routeKeys;
  }

  public boolean isDestinations() {
    return destinations;
  }

  public void setDestinations(boolean on) {
    if (on != destinations) {
      destinations = on;
      version++;
    }
  }

  public boolean isClosed() {
    return closed;
  }

  public void setClosed(boolean on) {
    if (on != closed) {
      closed = on;
      version++;
    }
  }

  /**
   * Whether the board shows its face on both sides.
   *
   * @param wallMounted whether it is set flat on a wall
   *
   * @return as set, or if never set, true unless it is on a wall
   */
  public boolean isTwoSided(boolean wallMounted) {
    return twoSided != null ? twoSided : !wallMounted;
  }

  /** Whether two-sided was ever set, rather than following the mount. */
  public boolean isTwoSidedSet() {
    return twoSided != null;
  }

  /** Sets two-sided; null to follow the mount again. */
  public void setTwoSided(@Nullable Boolean on) {
    if (!Objects.equals(on, twoSided)) {
      twoSided = on;
      version++;
    }
  }

  /** Moves on every change. */
  public int getVersion() {
    return version;
  }

  /**
   * The header line: the agency's name, then the stop's ("CITYLINE: Riverside"), or whichever of
   * the two there is.
   *
   * @return the header, possibly empty (a custom agency with no name and no stop name)
   */
  public String header() {
    String title = agency.title(this);
    if (stopName.isEmpty()) {
      return title;
    }
    return title.isEmpty() ? stopName : title + ": " + stopName;
  }

  /**
   * Copies every value of another, through the setters (so through their caps).
   *
   * @param other the values to take
   */
  public void copyFrom(BusStopInfo other) {
    setAgency(other.agency);
    setCustomName(other.customName);
    setCustomGround(other.customGround);
    setCustomLegend(other.customLegend);
    setStopName(other.stopName);
    setDirection(other.direction);
    setRoutes(other.routes);
    setDestinations(other.destinations);
    setClosed(other.closed);
    setTwoSided(other.twoSided);
  }

  // --- NBT ------------------------------------------------------------------------------------

  /**
   * Reads the values from a tile entity's NBT; an absent key reads as its default.
   *
   * @param nbt the compound
   */
  public void readFrom(NBTTagCompound nbt) {
    setAgency(BoardAgency.byOrdinal(nbt.getByte(KEY_AGENCY)));
    setCustomName(nbt.getString(KEY_CUSTOM_NAME));
    setCustomGround(nbt.hasKey(KEY_CUSTOM_GROUND) ? nbt.getInteger(KEY_CUSTOM_GROUND)
        : DEFAULT_CUSTOM_GROUND);
    setCustomLegend(nbt.hasKey(KEY_CUSTOM_LEGEND) ? nbt.getInteger(KEY_CUSTOM_LEGEND)
        : DEFAULT_CUSTOM_LEGEND);
    setStopName(nbt.getString(KEY_STOP_NAME));
    setDirection(nbt.getString(KEY_DIRECTION));
    setRoutes(nbt.getString(KEY_ROUTES));
    setDestinations(nbt.getBoolean(KEY_DESTINATIONS));
    setClosed(nbt.getBoolean(KEY_CLOSED));
    setTwoSided(nbt.hasKey(KEY_TWO_SIDED) ? nbt.getBoolean(KEY_TWO_SIDED) : null);
  }

  /**
   * Writes the values to a tile entity's NBT.
   *
   * @param nbt the compound
   */
  public void writeTo(NBTTagCompound nbt) {
    nbt.setByte(KEY_AGENCY, (byte) agency.ordinal());
    nbt.setString(KEY_CUSTOM_NAME, customName);
    nbt.setInteger(KEY_CUSTOM_GROUND, customGround);
    nbt.setInteger(KEY_CUSTOM_LEGEND, customLegend);
    nbt.setString(KEY_STOP_NAME, stopName);
    nbt.setString(KEY_DIRECTION, direction);
    nbt.setString(KEY_ROUTES, routes);
    nbt.setBoolean(KEY_DESTINATIONS, destinations);
    nbt.setBoolean(KEY_CLOSED, closed);
    if (twoSided != null) {
      nbt.setBoolean(KEY_TWO_SIDED, twoSided);
    } else {
      nbt.removeTag(KEY_TWO_SIDED);
    }
  }

  // --- wire -----------------------------------------------------------------------------------

  /**
   * Writes the values to a packet.
   *
   * @param buf the buffer
   */
  public void writeTo(ByteBuf buf) {
    buf.writeByte(agency.ordinal());
    writeString(buf, customName);
    buf.writeInt(customGround);
    buf.writeInt(customLegend);
    writeString(buf, stopName);
    writeString(buf, direction);
    writeString(buf, routes);
    int flags = (destinations ? FLAG_DESTINATIONS : 0) | (closed ? FLAG_CLOSED : 0)
        | (twoSided != null ? FLAG_SIDED_SET : 0)
        | (twoSided != null && twoSided ? FLAG_TWO_SIDED : 0);
    buf.writeByte(flags);
  }

  /**
   * Reads the values from a packet. Every string is a bounded read ({@link CsmPacketUtils}),
   * rejected before allocating if its length lies; an agency no ordinal names is rejected too.
   * The values then go through the setters' caps.
   *
   * @param buf the buffer
   *
   * @return the values
   *
   * @throws IndexOutOfBoundsException for a bad length or agency, which disconnects the sender
   */
  public static BusStopInfo readFrom(ByteBuf buf) {
    BusStopInfo info = new BusStopInfo();
    BoardAgency agency = BoardAgency.byOrdinal(buf.readUnsignedByte());
    if (agency == null) {
      throw new IndexOutOfBoundsException("Bad bus stop agency");
    }
    info.setAgency(agency);
    info.setCustomName(CsmPacketUtils.readBoundedString(buf, MAX_WIRE_BYTES));
    info.setCustomGround(buf.readInt());
    info.setCustomLegend(buf.readInt());
    info.setStopName(CsmPacketUtils.readBoundedString(buf, MAX_WIRE_BYTES));
    info.setDirection(CsmPacketUtils.readBoundedString(buf, MAX_WIRE_BYTES));
    info.setRoutes(CsmPacketUtils.readBoundedString(buf, MAX_WIRE_BYTES));
    int flags = buf.readUnsignedByte();
    info.setDestinations((flags & FLAG_DESTINATIONS) != 0);
    info.setClosed((flags & FLAG_CLOSED) != 0);
    info.setTwoSided((flags & FLAG_SIDED_SET) != 0 ? (flags & FLAG_TWO_SIDED) != 0 : null);
    return info;
  }

  /** An int-prefixed UTF-8 string, as {@link CsmPacketUtils#readBoundedString} reads it. */
  static void writeString(ByteBuf buf, String s) {
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    buf.writeInt(bytes.length);
    buf.writeBytes(bytes);
  }

  /**
   * Text as the board keeps it: printable ASCII only, ends trimmed, at most {@code max}
   * characters.
   *
   * @param text the text
   * @param max  its cap
   *
   * @return the cleaned text, never null
   */
  public static String clean(@Nullable String text, int max) {
    if (text == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(Math.min(text.length(), max + 8));
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= 32 && c < 127) {
        out.append(c);
      }
    }
    String s = out.toString().trim();
    return s.length() > max ? s.substring(0, max).trim() : s;
  }
}
