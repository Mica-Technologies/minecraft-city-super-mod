package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The centre-hung scoreboard's tile entity, on its master: what the screens show. It does not
 * tick. The game clock is worked out from the world's time: while it runs, the time left is the
 * time left when it was started ({@code clk}) less the ticks since ({@code t0}), so it needs no
 * update to count down and every client sees the same second.
 *
 * <p>NBT, all settable with {@code /blockdata} on the master:</p>
 * <ul>
 *   <li>{@code home}, {@code away}: team names, up to 12 characters</li>
 *   <li>{@code hs}, {@code as}: scores, 0-999; {@code hsh}, {@code ash}: shots, 0-999</li>
 *   <li>{@code per}: the period, up to 4 characters ({@code 1}, {@code OT}, {@code Q4})</li>
 *   <li>{@code clk}: the clock in tenths of a second (12000 is 20:00), the time left when it
 *       was last started or stopped; {@code len}: a period's length, what Reset sets</li>
 *   <li>{@code run}: 1b while the clock runs; {@code t0}: the world time it started at, written
 *       only while it runs (set {@code run:1b} without one and it starts now)</li>
 *   <li>{@code mode}: 0 the score while the clock runs and the idle video while it is stopped,
 *       1 always the score, 2 always the video (with a score strip along its foot)</li>
 *   <li>{@code hc}, {@code ac}: the teams' colours, {@link VenueColours} indices</li>
 * </ul>
 *
 * @since 2026.10
 */
public class TileEntityScoreboard extends AbstractTileEntity {

  /** Longest team name. */
  public static final int MAX_NAME = 12;
  /** Longest period text. */
  public static final int MAX_PERIOD = 4;
  /** Highest score or shot count. */
  public static final int MAX_COUNT = 999;
  /** The longest clock, tenths of a second (99:59.9). */
  public static final int MAX_CLOCK = 59_999;
  /** A period's length unless set, tenths (20:00). */
  public static final int DEFAULT_LENGTH = 12_000;

  public static final int MODE_AUTO = 0;
  public static final int MODE_SCORE = 1;
  public static final int MODE_VIDEO = 2;

  private static final String K_HOME = "home";
  private static final String K_AWAY = "away";
  private static final String K_HS = "hs";
  private static final String K_AS = "as";
  private static final String K_HSH = "hsh";
  private static final String K_ASH = "ash";
  private static final String K_PER = "per";
  private static final String K_CLK = "clk";
  private static final String K_LEN = "len";
  private static final String K_RUN = "run";
  private static final String K_T0 = "t0";
  private static final String K_MODE = "mode";
  private static final String K_HC = "hc";
  private static final String K_AC = "ac";
  private static final String K_PW = "pw";

  private String home = "HOME";
  private String away = "AWAY";
  private int homeScore;
  private int awayScore;
  private int homeShots;
  private int awayShots;
  private String period = "1";
  private int clock = DEFAULT_LENGTH;
  private int length = DEFAULT_LENGTH;
  private boolean running;
  private long startTime;
  private int mode = MODE_AUTO;
  private int homeColour = VenueColours.NAVY;
  private int awayColour = VenueColours.CRIMSON;
  private boolean powered;

  /** Bumped on the client whenever something the screens' text list draws changes. */
  private int textVersion;
  /** The text version the renderer's lists were compiled for (client). */
  int renderedVersion = -1;
  private EnumFacing facing;

  // --- reading -------------------------------------------------------------------------------

  public String getHome() {
    return home;
  }

  public String getAway() {
    return away;
  }

  public int getHomeScore() {
    return homeScore;
  }

  public int getAwayScore() {
    return awayScore;
  }

  public int getHomeShots() {
    return homeShots;
  }

  public int getAwayShots() {
    return awayShots;
  }

  public String getPeriod() {
    return period;
  }

  public int getLength() {
    return length;
  }

  public int getMode() {
    return mode;
  }

  public int getHomeColour() {
    return homeColour;
  }

  public int getAwayColour() {
    return awayColour;
  }

  /** Changes whenever the screens' words or colours do. */
  public int getTextVersion() {
    return textVersion;
  }

  /** Whether the clock was started and has not been stopped (it may have run out). */
  public boolean isClockStarted() {
    return running;
  }

  /**
   * The time left, in tenths of a second.
   *
   * @param worldTime the world's total time
   * @param partial   the part of a tick since, for a smooth tenths digit
   *
   * @return the time left, 0 when it has run out
   */
  public int remaining(long worldTime, float partial) {
    if (!running) {
      return clock;
    }
    double elapsed = (worldTime - startTime + partial) / 2.0;
    return (int) Math.max(0, Math.ceil(clock - elapsed));
  }

  /** Whether the clock is counting: started and not run out. */
  public boolean isCounting(long worldTime) {
    return running && remaining(worldTime, 0) > 0;
  }

  /** Whether the screens show the score rather than the video, now. */
  public boolean showsScore(long worldTime) {
    return mode == MODE_SCORE || mode == MODE_AUTO && isCounting(worldTime);
  }

  /**
   * A clock reading as a scoreboard shows it: {@code M:SS} from a minute up, {@code S.T} under.
   *
   * @param tenths tenths of a second
   *
   * @return the text
   */
  public static String clockText(int tenths) {
    int t = Math.max(0, tenths);
    if (t >= 600) {
      int seconds = t / 10;
      return (seconds / 60) + ":" + pad2(seconds % 60);
    }
    return (t / 10) + "." + (t % 10);
  }

  private static String pad2(int v) {
    return v < 10 ? "0" + v : Integer.toString(v);
  }

  /**
   * Reads a clock typed as {@code M:SS}, {@code M:SS.T}, {@code SS} or {@code SS.T}.
   *
   * @param text the text
   *
   * @return tenths of a second, or -1 if it does not read
   */
  public static int parseClock(String text) {
    String s = text == null ? "" : text.trim();
    if (s.isEmpty()) {
      return -1;
    }
    try {
      int minutes = 0;
      String rest = s;
      int colon = s.indexOf(':');
      if (colon >= 0) {
        minutes = Integer.parseInt(s.substring(0, colon).trim());
        rest = s.substring(colon + 1).trim();
      }
      double seconds = rest.isEmpty() ? 0 : Double.parseDouble(rest);
      if (minutes < 0 || seconds < 0 || colon >= 0 && seconds >= 60) {
        return -1;
      }
      long tenths = Math.round((minutes * 60 + seconds) * 10);
      return (int) Math.min(MAX_CLOCK, tenths);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  // --- changing (server) ---------------------------------------------------------------------

  /**
   * Sets everything the screen editor edits, cleaned. The clock is set only if {@code clock}
   * is 0 or more; set while running, it runs on from there.
   */
  public void apply(String home, String away, int homeScore, int awayScore, int homeShots,
      int awayShots, String period, int clock, int length, int mode, int homeColour,
      int awayColour) {
    this.home = clean(home, MAX_NAME);
    this.away = clean(away, MAX_NAME);
    this.homeScore = count(homeScore);
    this.awayScore = count(awayScore);
    this.homeShots = count(homeShots);
    this.awayShots = count(awayShots);
    this.period = clean(period, MAX_PERIOD);
    if (length > 0) {
      this.length = Math.min(MAX_CLOCK, length);
    }
    if (clock >= 0) {
      this.clock = Math.min(MAX_CLOCK, clock);
      if (running && world != null) {
        startTime = world.getTotalWorldTime();
      }
    }
    this.mode = mode < MODE_AUTO || mode > MODE_VIDEO ? MODE_AUTO : mode;
    this.homeColour = VenueColours.clamp(homeColour);
    this.awayColour = VenueColours.clamp(awayColour);
    sync();
  }

  /**
   * Starts a stopped clock or stops a running one. A clock that has run out stops at 0.0.
   *
   * @return whether it is running now
   */
  public boolean toggleClock() {
    if (world == null) {
      return running;
    }
    long now = world.getTotalWorldTime();
    if (running) {
      clock = remaining(now, 0);
      running = false;
    } else if (clock > 0) {
      startTime = now;
      running = true;
    }
    sync();
    return running;
  }

  /** Stops the clock and sets it back to a period's length. */
  public void resetClock() {
    running = false;
    clock = length;
    sync();
  }

  /**
   * A redstone level at one of the board's cells: a rising edge starts or stops the clock.
   *
   * @param on whether that cell is powered
   */
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
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  /** Printable ASCII only (the screens' font has nothing else), trimmed and cut to length. */
  static String clean(String s, int max) {
    if (s == null) {
      return "";
    }
    StringBuilder b = new StringBuilder(Math.min(s.length(), max));
    for (int i = 0; i < s.length() && b.length() < max; i++) {
      char c = s.charAt(i);
      if (c >= 32 && c < 127) {
        b.append(c);
      }
    }
    return b.toString().trim();
  }

  private static int count(int v) {
    return Math.max(0, Math.min(MAX_COUNT, v));
  }

  // --- the board -----------------------------------------------------------------------------

  /** The board's size, from its block (large if the block is not there yet). */
  public ScoreboardSize getSize() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockScoreboard) {
        return ((BlockScoreboard) s.getBlock()).getSize();
      }
    }
    return ScoreboardSize.LARGE;
  }

  /**
   * The way the board's model faces, looked up once (not before the block is there, and
   * forgotten when its state changes in place).
   */
  public EnumFacing getFacing() {
    if (facing == null && world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockScoreboard) {
        facing = s.getValue(BlockVenue.FACING);
      }
    }
    return facing == null ? EnumFacing.NORTH : facing;
  }

  @Override
  public void updateContainingBlockInfo() {
    super.updateContainingBlockInfo();
    facing = null;
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    String oldText = textKey();
    if (c.hasKey(K_HOME)) {
      home = clean(c.getString(K_HOME), MAX_NAME);
    }
    if (c.hasKey(K_AWAY)) {
      away = clean(c.getString(K_AWAY), MAX_NAME);
    }
    homeScore = count(c.getInteger(K_HS));
    awayScore = count(c.getInteger(K_AS));
    homeShots = count(c.getInteger(K_HSH));
    awayShots = count(c.getInteger(K_ASH));
    if (c.hasKey(K_PER)) {
      period = clean(c.getString(K_PER), MAX_PERIOD);
    }
    if (c.hasKey(K_LEN)) {
      length = Math.max(1, Math.min(MAX_CLOCK, c.getInteger(K_LEN)));
    }
    if (c.hasKey(K_CLK)) {
      clock = Math.max(0, Math.min(MAX_CLOCK, c.getInteger(K_CLK)));
    }
    boolean wasRunning = running;
    running = c.getBoolean(K_RUN);
    if (running) {
      if (c.hasKey(K_T0)) {
        startTime = c.getLong(K_T0);
      } else if (!wasRunning || startTime == 0) {
        // started by /blockdata: from now
        startTime = world != null ? world.getTotalWorldTime() : 0;
      }
    }
    int m = c.getInteger(K_MODE);
    mode = m < MODE_AUTO || m > MODE_VIDEO ? MODE_AUTO : m;
    if (c.hasKey(K_HC)) {
      homeColour = VenueColours.clamp(c.getInteger(K_HC));
    }
    if (c.hasKey(K_AC)) {
      awayColour = VenueColours.clamp(c.getInteger(K_AC));
    }
    powered = c.getBoolean(K_PW);
    if (!oldText.equals(textKey())) {
      textVersion++;
    }
  }

  /** Everything the text list draws, so a change can be told. */
  private String textKey() {
    return home + '\u0000' + away + '\u0000' + homeScore + ',' + awayScore + ',' + homeShots
        + ',' + awayShots + '\u0000' + period + '\u0000' + homeColour + ',' + awayColour;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    c.setString(K_HOME, home);
    c.setString(K_AWAY, away);
    c.setInteger(K_HS, homeScore);
    c.setInteger(K_AS, awayScore);
    c.setInteger(K_HSH, homeShots);
    c.setInteger(K_ASH, awayShots);
    c.setString(K_PER, period);
    c.setInteger(K_CLK, clock);
    c.setInteger(K_LEN, length);
    c.setBoolean(K_RUN, running);
    if (running) {
      c.setLong(K_T0, startTime);
    }
    c.setInteger(K_MODE, mode);
    c.setInteger(K_HC, homeColour);
    c.setInteger(K_AC, awayColour);
    c.setBoolean(K_PW, powered);
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    ScoreboardSize s = getSize();
    AxisAlignedBB local = new AxisAlignedBB(0.5 - s.width / 2, 0, 0.5 - s.depth / 2,
        0.5 + s.width / 2, s.top + s.rig, 0.5 + s.depth / 2);
    return RotationUtils.rotateBoundingBoxByFacing(local, getFacing()).offset(pos);
  }

  /** The screens are read from the far side of an arena. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(160.0 * 160.0);
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  /** Releases the screens' lists when the board goes. */
  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityScoreboardRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityScoreboardRenderer.release(pos);
    }
  }
}
