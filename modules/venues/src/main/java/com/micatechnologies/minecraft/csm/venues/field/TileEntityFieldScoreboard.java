package com.micatechnologies.minecraft.csm.venues.field;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.av.IVenueTextHolder;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboard;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A football or soccer scoreboard's tile entity: the teams, the score, the period and a clock,
 * and football's down, distance, ball on, time-outs and possession or soccer's added time. Edited
 * in the venue text editor or by NBT. It does not tick: a running clock is worked out from the
 * world's time each frame, as the centre-hung scoreboard's is (football's counts down, soccer's
 * up).
 *
 * <p>NBT ({@code /blockdata} on the master):</p>
 * <ul>
 *   <li>{@code home}, {@code away}: up to 10 printable characters (HOME, AWAY);</li>
 *   <li>{@code hs}, {@code as}: the scores, 0-999;</li>
 *   <li>{@code per}: the quarter or half, up to 3 characters ({@code 1}, {@code OT});</li>
 *   <li>{@code clk}: the clock in tenths of a second when last set or stopped (football's time
 *   left, default 9000 = 15:00; soccer's time played, default 0); {@code run} {@code 1b} while
 *   running (set without {@code t0} it starts now); {@code t0} the world time it started;</li>
 *   <li>football: {@code dn} the down, 0-4 (0 shows none); {@code dist} the distance, up to 5
 *   characters ({@code 10}, {@code GOAL}); {@code ball} the ball on, up to 6 ({@code 35},
 *   {@code OWN 20}); {@code hto}, {@code ato} time-outs left, 0-3; {@code pos} possession, 0
 *   none, 1 home, 2 away;</li>
 *   <li>soccer: {@code add} the added minutes shown, 0-30 (0 shows none);</li>
 *   <li>{@code hc}, {@code ac}: the teams' colours ({@link VenueColours}: navy, crimson).</li>
 * </ul>
 *
 * @since 2026.10
 */
public class TileEntityFieldScoreboard extends AbstractTileEntity implements IVenueTextHolder {

  static final int NAME_MAX = 10;
  private static final int CLOCK_MAX = 59_999;

  private String home = "HOME";
  private String away = "AWAY";
  private int homeScore;
  private int awayScore;
  private String period = "1";
  private int clock = -1;
  private boolean running;
  private long startTime;
  private int down = 1;
  private String distance = "10";
  private String ballOn = "25";
  private int homeTimeouts = 3;
  private int awayTimeouts = 3;
  private int possession;
  private int added;
  private int homeColour = VenueColours.NAVY;
  private int awayColour = VenueColours.CRIMSON;
  private boolean powered;

  private int textVersion;
  /** The text version the renderer's list was compiled for (client). */
  int renderedVersion = -1;

  // --- reading -------------------------------------------------------------------------------

  /** The soccer board rather than the football one. */
  public boolean soccer() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockFieldScoreboard) {
        return ((BlockFieldScoreboard) s.getBlock()).isSoccer();
      }
    }
    return false;
  }

  private int storedClock() {
    return clock >= 0 ? clock : soccer() ? 0 : 9000;
  }

  /** The clock now, in tenths: football's time left, soccer's time played. */
  public int clockNow(long worldTime, float partial) {
    int c = storedClock();
    if (!running) {
      return c;
    }
    double elapsed = (worldTime - startTime + partial) / 2.0;
    if (soccer()) {
      return (int) Math.min(CLOCK_MAX, Math.floor(c + elapsed));
    }
    return (int) Math.max(0, Math.ceil(c - elapsed));
  }

  /** The clock now as the board shows it. */
  public String clockText(long worldTime, float partial) {
    return BlockFieldScoreboard.clock(clockNow(worldTime, partial), soccer());
  }

  public String getHome() {
    return home;
  }

  public String getAway() {
    return away;
  }

  public int score(boolean homeTeam) {
    return homeTeam ? homeScore : awayScore;
  }

  public String getPeriod() {
    return period;
  }

  public int getDown() {
    return down;
  }

  public String getDistance() {
    return distance;
  }

  public String getBallOn() {
    return ballOn;
  }

  public int timeouts(boolean homeTeam) {
    return homeTeam ? homeTimeouts : awayTimeouts;
  }

  /** 0 none, 1 home, 2 away. */
  public int getPossession() {
    return possession;
  }

  public int getAdded() {
    return added;
  }

  public int colour(boolean homeTeam) {
    return homeTeam ? homeColour : awayColour;
  }

  public int getTextVersion() {
    return textVersion;
  }

  /** The board's block, or null before it is there. */
  BlockFieldScoreboard block() {
    if (world == null) {
      return null;
    }
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockFieldScoreboard ? (BlockFieldScoreboard) s.getBlock()
        : null;
  }

  EnumFacing facing() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockFieldScoreboard) {
        return s.getValue(BlockVenue.FACING);
      }
    }
    return EnumFacing.NORTH;
  }

  // --- the clock (server) --------------------------------------------------------------------

  /** Starts a stopped clock or stops a running one; returns whether it runs now. */
  public boolean toggleClock() {
    if (world == null) {
      return running;
    }
    long now = world.getTotalWorldTime();
    if (running) {
      clock = clockNow(now, 0);
      running = false;
    } else if (soccer() || storedClock() > 0) {
      clock = storedClock();
      startTime = now;
      running = true;
    }
    sync();
    return running;
  }

  /** A redstone level at one of the board's cells: a rising edge starts or stops the clock. */
  public void redstone(boolean on) {
    if (on && !powered) {
      powered = true;
      toggleClock();
    } else if (!on && powered) {
      powered = false;
      markDirty();
    }
  }

  private void sync() {
    textVersion++;
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  // --- cleaning ------------------------------------------------------------------------------

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

  private void setDownLine(String s) {
    String[] t = TileEntityScoreboard.clean(s, 24).trim().split("\\s+", 3);
    if (t.length > 0 && !t[0].isEmpty()) {
      try {
        down = Math.max(0, Math.min(4, Integer.parseInt(t[0])));
      } catch (NumberFormatException ignored) {
        // keep the down
      }
    }
    distance = t.length > 1 ? TileEntityScoreboard.clean(t[1], 5) : "";
    ballOn = t.length > 2 ? TileEntityScoreboard.clean(t[2], 6) : "";
  }

  // --- IVenueTextHolder ----------------------------------------------------------------------

  @Override
  public int lineCount() {
    return soccer() ? 6 : 7;
  }

  @Override
  public int maxLength(int i) {
    switch (i) {
      case 0:
      case 1:
        return NAME_MAX;
      case 3:
        return 3;
      default:
        return 16;
    }
  }

  @Override
  public String getLine(int i) {
    switch (i) {
      case 0:
        return home;
      case 1:
        return away;
      case 2:
        return homeScore + " " + awayScore;
      case 3:
        return period;
      case 4:
        return world == null ? "" : clockText(world.getTotalWorldTime(), 0);
      case 5:
        if (soccer()) {
          return Integer.toString(added);
        }
        return (down + " " + distance + " " + ballOn).trim();
      default:
        return homeTimeouts + " " + awayTimeouts;
    }
  }

  @Override
  public String lineCaption(int i) {
    return "gui.csm.venue_text." + (soccer() ? "soccer." : "football.") + i;
  }

  @Override
  public int choiceCount() {
    return soccer() ? 2 : 3;
  }

  @Override
  public int choiceRange(int i) {
    return i == 2 ? 3 : VenueColours.COUNT;
  }

  @Override
  public int getChoice(int i) {
    return i == 0 ? homeColour : i == 1 ? awayColour : possession;
  }

  @Override
  public String choiceCaption(int i) {
    return i == 0 ? "gui.csm.venue_text.choice.home" : i == 1 ? "gui.csm.venue_text.choice.away"
        : "gui.csm.venue_text.choice.possession";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return i == 2 ? "csm.venues.football.possession." + Math.max(0, Math.min(2, v))
        : VenueColours.key(v);
  }

  @Override
  public void applyText(String[] lines, int[] choices) {
    boolean soccer = soccer();
    for (int i = 0; i < lines.length && i < lineCount(); i++) {
      String s = lines[i] == null ? "" : lines[i];
      switch (i) {
        case 0:
          home = TileEntityScoreboard.clean(s, NAME_MAX);
          break;
        case 1:
          away = TileEntityScoreboard.clean(s, NAME_MAX);
          break;
        case 2: {
          int[] n = numbers(s, 2);
          homeScore = clamp(n[0], 999, homeScore);
          awayScore = clamp(n[1], 999, awayScore);
          break;
        }
        case 3:
          period = TileEntityScoreboard.clean(s, 3);
          break;
        case 4:
          // a running clock is not set from the editor, or opening it would set the clock back
          // to when the screen opened: stop it first
          if (!running) {
            int t = TileEntityScoreboard.parseClock(s);
            if (t >= 0) {
              clock = t;
            }
          }
          break;
        case 5:
          if (soccer) {
            int[] n = numbers(s, 1);
            added = clamp(n[0], 30, added);
          } else {
            setDownLine(s);
          }
          break;
        default: {
          int[] n = numbers(s, 2);
          homeTimeouts = clamp(n[0], 3, homeTimeouts);
          awayTimeouts = clamp(n[1], 3, awayTimeouts);
          break;
        }
      }
    }
    if (choices.length > 0) {
      homeColour = VenueColours.clamp(choices[0]);
    }
    if (choices.length > 1) {
      awayColour = VenueColours.clamp(choices[1]);
    }
    if (choices.length > 2 && !soccer) {
      possession = Math.max(0, Math.min(2, choices[2]));
    }
    sync();
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    if (c.hasKey("home")) {
      home = TileEntityScoreboard.clean(c.getString("home"), NAME_MAX);
    }
    if (c.hasKey("away")) {
      away = TileEntityScoreboard.clean(c.getString("away"), NAME_MAX);
    }
    homeScore = c.hasKey("hs") ? clamp(c.getInteger("hs"), 999, 0) : homeScore;
    awayScore = c.hasKey("as") ? clamp(c.getInteger("as"), 999, 0) : awayScore;
    if (c.hasKey("per")) {
      period = TileEntityScoreboard.clean(c.getString("per"), 3);
    }
    clock = c.hasKey("clk") ? Math.max(0, Math.min(CLOCK_MAX, c.getInteger("clk"))) : clock;
    boolean wasRunning = running;
    running = c.getBoolean("run");
    if (c.hasKey("t0")) {
      startTime = c.getLong("t0");
    } else if (running && !wasRunning && world != null) {
      startTime = world.getTotalWorldTime();
    }
    down = c.hasKey("dn") ? Math.max(0, Math.min(4, c.getInteger("dn"))) : down;
    if (c.hasKey("dist")) {
      distance = TileEntityScoreboard.clean(c.getString("dist"), 5);
    }
    if (c.hasKey("ball")) {
      ballOn = TileEntityScoreboard.clean(c.getString("ball"), 6);
    }
    homeTimeouts = c.hasKey("hto") ? clamp(c.getInteger("hto"), 3, 0) : homeTimeouts;
    awayTimeouts = c.hasKey("ato") ? clamp(c.getInteger("ato"), 3, 0) : awayTimeouts;
    possession = c.hasKey("pos") ? Math.max(0, Math.min(2, c.getInteger("pos"))) : possession;
    added = c.hasKey("add") ? clamp(c.getInteger("add"), 30, 0) : added;
    homeColour = c.hasKey("hc") ? VenueColours.clamp(c.getInteger("hc")) : homeColour;
    awayColour = c.hasKey("ac") ? VenueColours.clamp(c.getInteger("ac")) : awayColour;
    powered = c.getBoolean("pw");
    textVersion++;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    c.setString("home", home);
    c.setString("away", away);
    c.setInteger("hs", homeScore);
    c.setInteger("as", awayScore);
    c.setString("per", period);
    if (clock >= 0) {
      c.setInteger("clk", clock);
    }
    c.setBoolean("run", running);
    if (running) {
      c.setLong("t0", startTime);
    }
    c.setInteger("dn", down);
    c.setString("dist", distance);
    c.setString("ball", ballOn);
    c.setInteger("hto", homeTimeouts);
    c.setInteger("ato", awayTimeouts);
    c.setInteger("pos", possession);
    c.setInteger("add", added);
    c.setInteger("hc", homeColour);
    c.setInteger("ac", awayColour);
    if (powered) {
      c.setBoolean("pw", true);
    }
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockFieldScoreboard b = block();
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
      TileEntityFieldScoreboardRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityFieldScoreboardRenderer.release(pos);
    }
  }
}
