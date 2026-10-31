package com.micatechnologies.minecraft.csm.venues.bowling;

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
 * A pair of lanes' scoring monitors: up to three bowlers a lane, each a line in score sheet
 * notation ({@link BowlingScore}), the left lane's number and the screens' colour. Edited in the
 * venue text editor or by NBT; it does not tick.
 *
 * <p>NBT ({@code /blockdata} on the master):</p>
 * <ul>
 *   <li>{@code b0}, {@code b1}, {@code b2}: the left lane's bowlers, {@code b3}, {@code b4},
 *   {@code b5} the right lane's, each {@code "NAME: rolls"} ({@code "ALEX: X 9/ 8-"}), up to 40
 *   characters;</li>
 *   <li>{@code ln}: the left lane's number, 1-98 (the right lane is the next); 1;</li>
 *   <li>{@code c}: the screens' colour ({@link VenueColours}; royal blue);</li>
 *   <li>{@code ua}, {@code ub}: the bowler up on the left and right lane, 0 none or 1-3.</li>
 * </ul>
 *
 * @since 2026.10
 */
public class TileEntityBowlingMonitor extends AbstractTileEntity implements IVenueTextHolder {

  /** Bowlers a lane. */
  public static final int PER_LANE = 3;
  static final int LINE_MAX = 40;

  private final String[] bowlers = {"", "", "", "", "", ""};
  private int lane = 1;
  private int colour = VenueColours.ROYAL_BLUE;
  private int upLeft;
  private int upRight;

  private int textVersion;
  /** The text version the renderer's list was compiled for (client). */
  int renderedVersion = -1;
  /** The bowlers worked out, for the renderer (client), with the version they were made for. */
  private BowlingScore[] scores;
  private int scoredVersion = -1;

  // --- reading -------------------------------------------------------------------------------

  /** The left lane's number. */
  public int getLane() {
    return lane;
  }

  public int getColour() {
    return colour;
  }

  /** The bowler up on a lane (0 left, 1 right): 0 none, 1-3. */
  public int up(int side) {
    return side == 0 ? upLeft : upRight;
  }

  public int getTextVersion() {
    return textVersion;
  }

  /** The six bowlers, worked out: 0-2 the left lane's, 3-5 the right's. */
  public BowlingScore[] scores() {
    if (scores == null || scoredVersion != textVersion) {
      BowlingScore[] out = new BowlingScore[bowlers.length];
      for (int i = 0; i < out.length; i++) {
        out[i] = BowlingScore.parse(bowlers[i]);
      }
      scores = out;
      scoredVersion = textVersion;
    }
    return scores;
  }

  EnumFacing facing() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockBowlingMonitor) {
        return s.getValue(BlockVenue.FACING);
      }
    }
    return EnumFacing.NORTH;
  }

  BlockBowlingMonitor block() {
    if (world == null) {
      return null;
    }
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockBowlingMonitor ? (BlockBowlingMonitor) s.getBlock() : null;
  }

  // --- IVenueTextHolder ----------------------------------------------------------------------

  @Override
  public int lineCount() {
    return 7;
  }

  @Override
  public int maxLength(int i) {
    return i < 6 ? LINE_MAX : 2;
  }

  @Override
  public String getLine(int i) {
    return i < 6 ? bowlers[i] : Integer.toString(lane);
  }

  @Override
  public String lineCaption(int i) {
    return "gui.csm.venue_text.bowling." + i;
  }

  @Override
  public int choiceCount() {
    return 3;
  }

  @Override
  public int choiceRange(int i) {
    return i == 0 ? VenueColours.COUNT : PER_LANE + 1;
  }

  @Override
  public int getChoice(int i) {
    return i == 0 ? colour : i == 1 ? upLeft : upRight;
  }

  @Override
  public String choiceCaption(int i) {
    return i == 0 ? "gui.csm.venue_text.choice.screen" : i == 1
        ? "gui.csm.venue_text.choice.up_left" : "gui.csm.venue_text.choice.up_right";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return i == 0 ? VenueColours.key(v) : "csm.venues.bowling.up." + Math.max(0, Math.min(3, v));
  }

  @Override
  public void applyText(String[] lines, int[] choices) {
    for (int i = 0; i < lines.length && i < lineCount(); i++) {
      if (i < 6) {
        bowlers[i] = TileEntityScoreboard.clean(lines[i], LINE_MAX);
      } else {
        lane = parseLane(lines[i], lane);
      }
    }
    if (choices.length > 0) {
      colour = VenueColours.clamp(choices[0]);
    }
    if (choices.length > 1) {
      upLeft = clampUp(choices[1]);
    }
    if (choices.length > 2) {
      upRight = clampUp(choices[2]);
    }
    textVersion++;
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  private static int clampUp(int v) {
    return Math.max(0, Math.min(PER_LANE, v));
  }

  private static int parseLane(String s, int old) {
    String digits = s == null ? "" : s.replaceAll("[^0-9]", "");
    if (digits.isEmpty()) {
      return old;
    }
    try {
      return Math.max(1, Math.min(98, Integer.parseInt(digits.length() > 2
          ? digits.substring(0, 2) : digits)));
    } catch (NumberFormatException e) {
      return old;
    }
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    String old = key();
    for (int i = 0; i < bowlers.length; i++) {
      if (c.hasKey("b" + i)) {
        bowlers[i] = TileEntityScoreboard.clean(c.getString("b" + i), LINE_MAX);
      }
    }
    if (c.hasKey("ln")) {
      lane = Math.max(1, Math.min(98, c.getInteger("ln")));
    }
    if (c.hasKey("c")) {
      colour = VenueColours.clamp(c.getInteger("c"));
    }
    if (c.hasKey("ua")) {
      upLeft = clampUp(c.getInteger("ua"));
    }
    if (c.hasKey("ub")) {
      upRight = clampUp(c.getInteger("ub"));
    }
    if (!old.equals(key())) {
      textVersion++;
    }
  }

  private String key() {
    return String.join("\u0000", bowlers) + '\u0000' + lane + ',' + colour + ',' + upLeft + ','
        + upRight;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    for (int i = 0; i < bowlers.length; i++) {
      c.setString("b" + i, bowlers[i]);
    }
    c.setInteger("ln", lane);
    c.setInteger("c", colour);
    c.setInteger("ua", upLeft);
    c.setInteger("ub", upRight);
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockBowlingMonitor b = block();
    if (b == null) {
      return new AxisAlignedBB(pos);
    }
    return b.worldOutline(pos, facing());
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(96.0 * 96.0);
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityBowlingMonitorRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityBowlingMonitorRenderer.release(pos);
    }
  }
}
