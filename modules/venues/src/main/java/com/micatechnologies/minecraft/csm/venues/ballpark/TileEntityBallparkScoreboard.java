package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.av.IVenueTextHolder;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboard;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A ballpark scoreboard's tile entity: the line score and the count, edited in the venue text
 * editor or by NBT. It does not tick; nothing on the board moves unless it is told to.
 *
 * <p>NBT ({@code /blockdata} on the master):</p>
 * <ul>
 *   <li>{@code away}, {@code home}: the teams, up to 10 printable characters (AWAY, HOME);</li>
 *   <li>{@code ai}, {@code hi}: each team's runs inning by inning, numbers between spaces, up to
 *   twelve innings (an X for a home half not played, a dash for an empty square):
 *   {@code "0 1 0 0 2"}; R is their sum;</li>
 *   <li>{@code ah}, {@code ae}, {@code hh}, {@code he}: hits and errors, 0-99;</li>
 *   <li>{@code b}, {@code s}, {@code o}: the count, 0-3 balls, 0-2 strikes and outs;</li>
 *   <li>{@code mph}: the last pitch's speed, 0-199 (0 shows nothing);</li>
 *   <li>{@code ac}, {@code hc}: the teams' colours ({@link VenueColours}: navy, crimson);</li>
 *   <li>{@code bat}: who is batting, 0 away, 1 home (a lamp beside the name).</li>
 * </ul>
 *
 * @since 2026.10
 */
public class TileEntityBallparkScoreboard extends AbstractTileEntity implements IVenueTextHolder {

  static final int NAME_MAX = 10;
  static final int INNINGS_MAX = 12;

  private String away = "AWAY";
  private String home = "HOME";
  private String awayInnings = "";
  private String homeInnings = "";
  private int awayHits;
  private int awayErrors;
  private int homeHits;
  private int homeErrors;
  private int balls;
  private int strikes;
  private int outs;
  private int mph;
  private int awayColour = VenueColours.NAVY;
  private int homeColour = VenueColours.CRIMSON;
  private int batting;

  private int textVersion;
  /** The text version the renderer's list was compiled for (client). */
  int renderedVersion = -1;

  // --- reading -------------------------------------------------------------------------------

  public String getAway() {
    return away;
  }

  public String getHome() {
    return home;
  }

  /** A team's innings, one square each: a number, X, or "" for an empty square. */
  public List<String> innings(boolean homeTeam) {
    List<String> out = new ArrayList<>();
    String s = homeTeam ? homeInnings : awayInnings;
    if (!s.isEmpty()) {
      for (String t : s.split(" ")) {
        out.add("-".equals(t) ? "" : t);
      }
    }
    return out;
  }

  /** A team's runs: the sum of its innings. */
  public int runs(boolean homeTeam) {
    int r = 0;
    for (String t : innings(homeTeam)) {
      try {
        r += Integer.parseInt(t);
      } catch (NumberFormatException ignored) {
        // X or an empty square
      }
    }
    return r;
  }

  public int hits(boolean homeTeam) {
    return homeTeam ? homeHits : awayHits;
  }

  public int errors(boolean homeTeam) {
    return homeTeam ? homeErrors : awayErrors;
  }

  public int getBalls() {
    return balls;
  }

  public int getStrikes() {
    return strikes;
  }

  public int getOuts() {
    return outs;
  }

  public int getMph() {
    return mph;
  }

  public int colour(boolean homeTeam) {
    return homeTeam ? homeColour : awayColour;
  }

  /** Whether the home team is batting. */
  public boolean homeBatting() {
    return batting == 1;
  }

  public int getTextVersion() {
    return textVersion;
  }

  /** The board's block, or null before it is there. */
  BlockBallparkScoreboard block() {
    if (world == null) {
      return null;
    }
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockBallparkScoreboard ? (BlockBallparkScoreboard) s.getBlock()
        : null;
  }

  boolean small() {
    BlockBallparkScoreboard b = block();
    return b != null && b.isSmall();
  }

  EnumFacing facing() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockBallparkScoreboard) {
        return s.getValue(BlockVenue.FACING);
      }
    }
    return EnumFacing.NORTH;
  }

  // --- cleaning ------------------------------------------------------------------------------

  private static String cleanInnings(String s) {
    StringBuilder b = new StringBuilder();
    int n = 0;
    for (String t : TileEntityScoreboard.clean(s, 64).split("\\s+")) {
      if (t.isEmpty() || n >= INNINGS_MAX) {
        continue;
      }
      String v;
      if ("x".equalsIgnoreCase(t)) {
        v = "X";
      } else if ("-".equals(t)) {
        v = "-";
      } else {
        try {
          v = Integer.toString(Math.max(0, Math.min(99, Integer.parseInt(t))));
        } catch (NumberFormatException e) {
          continue;
        }
      }
      if (b.length() > 0) {
        b.append(' ');
      }
      b.append(v);
      n++;
    }
    return b.toString();
  }

  /** The whole numbers in a line, in order. */
  private static int[] numbers(String s, int n) {
    int[] out = new int[n];
    java.util.Arrays.fill(out, -1);
    int i = 0;
    for (String t : TileEntityScoreboard.clean(s, 64).split("[^0-9]+")) {
      if (t.isEmpty() || i >= n) {
        continue;
      }
      try {
        out[i++] = Integer.parseInt(t.length() > 3 ? t.substring(0, 3) : t);
      } catch (NumberFormatException ignored) {
        // not a number
      }
    }
    return out;
  }

  private static int clamp(int v, int max, int old) {
    return v < 0 ? old : Math.min(max, v);
  }

  // --- IVenueTextHolder ----------------------------------------------------------------------

  @Override
  public int lineCount() {
    return small() ? 2 : 7;
  }

  /** The line shown at editor line {@code i} (the count board's lines are the last two). */
  private int line(int i) {
    return small() ? i + 5 : i;
  }

  @Override
  public int maxLength(int i) {
    switch (line(i)) {
      case 0:
      case 1:
        return NAME_MAX;
      case 2:
      case 3:
        return 40;
      default:
        return 16;
    }
  }

  @Override
  public String getLine(int i) {
    switch (line(i)) {
      case 0:
        return away;
      case 1:
        return home;
      case 2:
        return awayInnings;
      case 3:
        return homeInnings;
      case 4:
        return awayHits + " " + awayErrors + " " + homeHits + " " + homeErrors;
      case 5:
        return balls + " " + strikes + " " + outs;
      default:
        return mph > 0 ? Integer.toString(mph) : "";
    }
  }

  @Override
  public String lineCaption(int i) {
    return "gui.csm.venue_text.ballpark." + line(i);
  }

  @Override
  public int choiceCount() {
    return small() ? 0 : 3;
  }

  @Override
  public int choiceRange(int i) {
    return i == 2 ? 2 : VenueColours.COUNT;
  }

  @Override
  public int getChoice(int i) {
    return i == 0 ? awayColour : i == 1 ? homeColour : batting;
  }

  @Override
  public String choiceCaption(int i) {
    return i == 0 ? "gui.csm.venue_text.choice.away" : i == 1 ? "gui.csm.venue_text.choice.home"
        : "gui.csm.venue_text.choice.batting";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return i == 2 ? "csm.venues.ballpark.batting." + (v == 1 ? 1 : 0) : VenueColours.key(v);
  }

  @Override
  public void applyText(String[] lines, int[] choices) {
    for (int i = 0; i < lines.length && i < lineCount(); i++) {
      setLine(line(i), lines[i]);
    }
    if (!small()) {
      if (choices.length > 0) {
        awayColour = VenueColours.clamp(choices[0]);
      }
      if (choices.length > 1) {
        homeColour = VenueColours.clamp(choices[1]);
      }
      if (choices.length > 2) {
        batting = choices[2] == 1 ? 1 : 0;
      }
    }
    textVersion++;
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  private void setLine(int line, String s) {
    switch (line) {
      case 0:
        away = TileEntityScoreboard.clean(s, NAME_MAX);
        break;
      case 1:
        home = TileEntityScoreboard.clean(s, NAME_MAX);
        break;
      case 2:
        awayInnings = cleanInnings(s);
        break;
      case 3:
        homeInnings = cleanInnings(s);
        break;
      case 4: {
        int[] n = numbers(s, 4);
        awayHits = clamp(n[0], 99, awayHits);
        awayErrors = clamp(n[1], 99, awayErrors);
        homeHits = clamp(n[2], 99, homeHits);
        homeErrors = clamp(n[3], 99, homeErrors);
        break;
      }
      case 5: {
        int[] n = numbers(s, 3);
        balls = clamp(n[0], 3, balls);
        strikes = clamp(n[1], 2, strikes);
        outs = clamp(n[2], 2, outs);
        break;
      }
      default: {
        int[] n = numbers(s, 1);
        mph = n[0] < 0 ? 0 : Math.min(199, n[0]);
        break;
      }
    }
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    String old = key();
    if (c.hasKey("away")) {
      away = TileEntityScoreboard.clean(c.getString("away"), NAME_MAX);
    }
    if (c.hasKey("home")) {
      home = TileEntityScoreboard.clean(c.getString("home"), NAME_MAX);
    }
    if (c.hasKey("ai")) {
      awayInnings = cleanInnings(c.getString("ai"));
    }
    if (c.hasKey("hi")) {
      homeInnings = cleanInnings(c.getString("hi"));
    }
    awayHits = c.hasKey("ah") ? clamp(c.getInteger("ah"), 99, 0) : awayHits;
    awayErrors = c.hasKey("ae") ? clamp(c.getInteger("ae"), 99, 0) : awayErrors;
    homeHits = c.hasKey("hh") ? clamp(c.getInteger("hh"), 99, 0) : homeHits;
    homeErrors = c.hasKey("he") ? clamp(c.getInteger("he"), 99, 0) : homeErrors;
    balls = c.hasKey("b") ? clamp(c.getInteger("b"), 3, 0) : balls;
    strikes = c.hasKey("s") ? clamp(c.getInteger("s"), 2, 0) : strikes;
    outs = c.hasKey("o") ? clamp(c.getInteger("o"), 2, 0) : outs;
    mph = c.hasKey("mph") ? clamp(c.getInteger("mph"), 199, 0) : mph;
    awayColour = c.hasKey("ac") ? VenueColours.clamp(c.getInteger("ac")) : awayColour;
    homeColour = c.hasKey("hc") ? VenueColours.clamp(c.getInteger("hc")) : homeColour;
    batting = c.hasKey("bat") ? (c.getInteger("bat") == 1 ? 1 : 0) : batting;
    if (!old.equals(key())) {
      textVersion++;
    }
  }

  private String key() {
    return away + '\u0000' + home + '\u0000' + awayInnings + '\u0000' + homeInnings + '\u0000'
        + awayHits + ',' + awayErrors + ',' + homeHits + ',' + homeErrors + ',' + balls + ','
        + strikes + ',' + outs + ',' + mph + ',' + awayColour + ',' + homeColour + ',' + batting;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    c.setString("away", away);
    c.setString("home", home);
    c.setString("ai", awayInnings);
    c.setString("hi", homeInnings);
    c.setInteger("ah", awayHits);
    c.setInteger("ae", awayErrors);
    c.setInteger("hh", homeHits);
    c.setInteger("he", homeErrors);
    c.setInteger("b", balls);
    c.setInteger("s", strikes);
    c.setInteger("o", outs);
    c.setInteger("mph", mph);
    c.setInteger("ac", awayColour);
    c.setInteger("hc", homeColour);
    c.setInteger("bat", batting);
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockBallparkScoreboard b = block();
    if (b == null) {
      return new AxisAlignedBB(pos);
    }
    return b.worldOutline(pos, facing());
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(192.0 * 192.0);
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityBallparkScoreboardRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityBallparkScoreboardRenderer.release(pos);
    }
  }
}
