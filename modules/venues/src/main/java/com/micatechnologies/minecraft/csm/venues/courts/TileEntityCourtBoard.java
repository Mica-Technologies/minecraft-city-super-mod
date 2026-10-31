package com.micatechnologies.minecraft.csm.venues.courts;

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
 * The tile entity of a courtside scoreboard ({@code court_scoreboard}) or a track's lap counter
 * ({@code track_lap_counter}), which one by its block. Edited in the venue text editor or by NBT;
 * nothing ticks.
 *
 * <p>The scoreboard: two players (or teams), each with the games they won in each set, the
 * current set last, and the points in the game; a serve lamp; each player's colour. NBT:
 * {@code p1}, {@code p2} (12 characters; HOME, AWAY), {@code g1}, {@code g2} (games by set, up to
 * five, {@code "6 4 2"}), {@code pt1}, {@code pt2} (the points, 3 characters: 0, 15, 30, 40, AD),
 * {@code srv} (0 none, 1, 2), {@code c1}, {@code c2} (venue colours: navy, crimson).</p>
 *
 * <p>The lap counter: {@code laps}, the laps to go (0-99; 25 by default); its bell rings when it
 * is turned to 1, the bell lap. A click turns it down a lap.</p>
 *
 * @since 2026.10
 */
public class TileEntityCourtBoard extends AbstractTileEntity implements IVenueTextHolder {

  static final int NAME_MAX = 12;
  static final int SETS = 5;

  private String p1 = "HOME";
  private String p2 = "AWAY";
  private final int[] g1 = {-1, -1, -1, -1, -1};
  private final int[] g2 = {-1, -1, -1, -1, -1};
  private String pt1 = "0";
  private String pt2 = "0";
  private int serve = 1;
  private int c1 = VenueColours.NAVY;
  private int c2 = VenueColours.CRIMSON;
  private int laps = 25;

  private int textVersion;
  /** The text version the renderer's list was compiled for (client). */
  int renderedVersion = -1;

  /** The lap counter rather than the scoreboard. */
  public boolean lapCounter() {
    BlockCourtBoard b = block();
    return b != null && b.isLapCounter();
  }

  BlockCourtBoard block() {
    if (world == null) {
      return null;
    }
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockCourtBoard ? (BlockCourtBoard) s.getBlock() : null;
  }

  EnumFacing facing() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockCourtBoard) {
        return s.getValue(BlockVenue.FACING);
      }
    }
    return EnumFacing.NORTH;
  }

  public String name(int p) {
    return p == 1 ? p1 : p2;
  }

  /** Games won in set s (0-4) by player p, -1 where none. */
  public int games(int p, int s) {
    return (p == 1 ? g1 : g2)[s];
  }

  /** How many sets have a score. */
  public int setsShown() {
    int n = 0;
    for (int s = 0; s < SETS; s++) {
      if (g1[s] >= 0 || g2[s] >= 0) {
        n = s + 1;
      }
    }
    return Math.max(1, n);
  }

  public String points(int p) {
    return p == 1 ? pt1 : pt2;
  }

  public int getServe() {
    return serve;
  }

  public int colour(int p) {
    return p == 1 ? c1 : c2;
  }

  public int getLaps() {
    return laps;
  }

  public int getTextVersion() {
    return textVersion;
  }

  /** Turns the counter down a lap (to 0); returns the laps left. Server side. */
  public int lapDown() {
    laps = Math.max(0, laps - 1);
    sync();
    return laps;
  }

  private void sync() {
    textVersion++;
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  private static void games(String s, int[] into) {
    java.util.Arrays.fill(into, -1);
    int i = 0;
    for (String t : TileEntityScoreboard.clean(s, 32).trim().split("\\s+")) {
      if (t.isEmpty() || i >= SETS) {
        continue;
      }
      if (t.equals("-")) {
        i++;
        continue;
      }
      try {
        into[i++] = Math.max(0, Math.min(99, Integer.parseInt(t.length() > 2 ? t.substring(0, 2)
            : t)));
      } catch (NumberFormatException ignored) {
        i++;
      }
    }
  }

  private static String gamesText(int[] g) {
    StringBuilder b = new StringBuilder();
    int last = -1;
    for (int s = 0; s < SETS; s++) {
      if (g[s] >= 0) {
        last = s;
      }
    }
    for (int s = 0; s <= last; s++) {
      if (b.length() > 0) {
        b.append(' ');
      }
      b.append(g[s] >= 0 ? Integer.toString(g[s]) : "-");
    }
    return b.toString();
  }

  // --- IVenueTextHolder ----------------------------------------------------------------------

  @Override
  public int lineCount() {
    return lapCounter() ? 1 : 6;
  }

  @Override
  public int maxLength(int i) {
    if (lapCounter()) {
      return 2;
    }
    return i < 2 ? NAME_MAX : i < 4 ? 20 : 3;
  }

  @Override
  public String getLine(int i) {
    if (lapCounter()) {
      return Integer.toString(laps);
    }
    switch (i) {
      case 0:
        return p1;
      case 1:
        return p2;
      case 2:
        return gamesText(g1);
      case 3:
        return gamesText(g2);
      case 4:
        return pt1;
      default:
        return pt2;
    }
  }

  @Override
  public String lineCaption(int i) {
    return lapCounter() ? "gui.csm.venue_text.laps.0" : "gui.csm.venue_text.tennis." + i;
  }

  @Override
  public int choiceCount() {
    return lapCounter() ? 0 : 3;
  }

  @Override
  public int choiceRange(int i) {
    return i == 0 ? 3 : VenueColours.COUNT;
  }

  @Override
  public int getChoice(int i) {
    return i == 0 ? serve : i == 1 ? c1 : c2;
  }

  @Override
  public String choiceCaption(int i) {
    return i == 0 ? "gui.csm.venue_text.choice.serve" : i == 1
        ? "gui.csm.venue_text.choice.player1" : "gui.csm.venue_text.choice.player2";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return i == 0 ? "csm.venues.courts.serve." + Math.max(0, Math.min(2, v)) : VenueColours.key(v);
  }

  @Override
  public void applyText(String[] lines, int[] choices) {
    if (lapCounter()) {
      if (lines.length > 0 && lines[0] != null) {
        try {
          laps = Math.max(0, Math.min(99, Integer.parseInt(lines[0].trim())));
        } catch (NumberFormatException ignored) {
          // keep the laps
        }
      }
      sync();
      return;
    }
    for (int i = 0; i < lines.length && i < 6; i++) {
      String s = lines[i] == null ? "" : lines[i];
      switch (i) {
        case 0:
          p1 = TileEntityScoreboard.clean(s, NAME_MAX);
          break;
        case 1:
          p2 = TileEntityScoreboard.clean(s, NAME_MAX);
          break;
        case 2:
          games(s, g1);
          break;
        case 3:
          games(s, g2);
          break;
        case 4:
          pt1 = TileEntityScoreboard.clean(s, 3);
          break;
        default:
          pt2 = TileEntityScoreboard.clean(s, 3);
          break;
      }
    }
    if (choices.length > 0) {
      serve = Math.max(0, Math.min(2, choices[0]));
    }
    if (choices.length > 1) {
      c1 = VenueColours.clamp(choices[1]);
    }
    if (choices.length > 2) {
      c2 = VenueColours.clamp(choices[2]);
    }
    sync();
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    if (c.hasKey("p1")) {
      p1 = TileEntityScoreboard.clean(c.getString("p1"), NAME_MAX);
    }
    if (c.hasKey("p2")) {
      p2 = TileEntityScoreboard.clean(c.getString("p2"), NAME_MAX);
    }
    if (c.hasKey("g1")) {
      games(c.getString("g1"), g1);
    }
    if (c.hasKey("g2")) {
      games(c.getString("g2"), g2);
    }
    if (c.hasKey("pt1")) {
      pt1 = TileEntityScoreboard.clean(c.getString("pt1"), 3);
    }
    if (c.hasKey("pt2")) {
      pt2 = TileEntityScoreboard.clean(c.getString("pt2"), 3);
    }
    serve = c.hasKey("srv") ? Math.max(0, Math.min(2, c.getInteger("srv"))) : serve;
    c1 = c.hasKey("c1") ? VenueColours.clamp(c.getInteger("c1")) : c1;
    c2 = c.hasKey("c2") ? VenueColours.clamp(c.getInteger("c2")) : c2;
    laps = c.hasKey("laps") ? Math.max(0, Math.min(99, c.getInteger("laps"))) : laps;
    textVersion++;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    c.setString("p1", p1);
    c.setString("p2", p2);
    c.setString("g1", gamesText(g1));
    c.setString("g2", gamesText(g2));
    c.setString("pt1", pt1);
    c.setString("pt2", pt2);
    c.setInteger("srv", serve);
    c.setInteger("c1", c1);
    c.setInteger("c2", c2);
    c.setInteger("laps", laps);
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockCourtBoard b = block();
    if (b == null) {
      return new AxisAlignedBB(pos);
    }
    return b.worldOutline(pos, facing());
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(128.0 * 128.0);
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityCourtBoardRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityCourtBoardRenderer.release(pos);
    }
  }
}
