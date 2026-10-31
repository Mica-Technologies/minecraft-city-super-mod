package com.micatechnologies.minecraft.csm.venues.exterior;

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
import net.minecraft.util.math.BlockPos;

/**
 * The exterior marquee board's tile entity, on its controller: the three lines of event text,
 * the mode and the colours. It does not tick.
 *
 * <p>NBT ({@code /blockdata} on the controller): {@code l1} the first line (up to 32 characters,
 * the biggest), {@code l2} the second (32), {@code l3} the third (48, the smallest);
 * {@code mode} 0 text and video in turn (eight seconds each, the default), 1 text only, 2 video
 * only; {@code fg} and {@code bg} the text and background colours ({@link VenueColours}
 * indices).</p>
 *
 * <p>On the client it also keeps the board's size, found by walking from the controller along
 * the bottom row to the reader's right and up the left column, at most once a second
 * ({@link #width()}, {@link #height()}), since a cell far across the board changing tells the
 * controller nothing.</p>
 *
 * @since 2026.10
 */
public class TileEntityMarquee extends AbstractTileEntity implements IVenueTextHolder {

  /** The widest board, blocks. */
  public static final int MAX_W = 48;
  /** The tallest board, blocks. */
  public static final int MAX_H = 24;
  /** Each line's longest. */
  static final int[] MAX = {32, 32, 48};
  /** The modes. */
  public static final int MODE_CYCLE = 0;
  public static final int MODE_TEXT = 1;
  public static final int MODE_VIDEO = 2;
  static final int MODES = 3;
  /** How long text and video each show in the cycle, in ticks. */
  static final long CYCLE_HALF = 160;

  private static final String[] K_LINE = {"l1", "l2", "l3"};
  private static final String K_MODE = "mode";
  private static final String K_FG = "fg";
  private static final String K_BG = "bg";

  private final String[] lines = {"TONIGHT 7PM", "HOME VS AWAY", "TICKETS AT THE BOX OFFICE"};
  private int mode = MODE_CYCLE;
  private int foreground = VenueColours.GOLD;
  private int background = VenueColours.BLACK;

  private int textVersion;
  /** The text version the renderer's list was compiled for (client). */
  int renderedVersion = -1;
  private int width = 1;
  private int height = 1;
  private long sizedAt = Long.MIN_VALUE;

  public String getLine(int i) {
    return lines[i];
  }

  public int getMode() {
    return mode;
  }

  public int getForeground() {
    return foreground;
  }

  public int getBackground() {
    return background;
  }

  public int getTextVersion() {
    return textVersion;
  }

  /** The controller's state, or null before it is there. */
  IBlockState state() {
    if (world == null) {
      return null;
    }
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockMarqueeBoard ? s : null;
  }

  /** The way the board faces. */
  public EnumFacing getFacing() {
    IBlockState s = state();
    return s != null ? s.getValue(BlockVenue.FACING) : EnumFacing.NORTH;
  }

  /** Whether the board has a screen on its back too. */
  public boolean isDoubleSided() {
    IBlockState s = state();
    return s != null && s.getValue(BlockMarqueeBoard.DOUBLE);
  }

  /** Whether the text shows at world time {@code now} (rather than the video). */
  public boolean showsText(long now) {
    return mode == MODE_TEXT || mode == MODE_CYCLE && (now / CYCLE_HALF) % 2 == 0;
  }

  /** The board's width in blocks, as last walked. */
  public int width() {
    measure();
    return width;
  }

  /** The board's height in blocks, as last walked. */
  public int height() {
    measure();
    return height;
  }

  /** Walks the board's bottom row and left column, at most once a second. */
  private void measure() {
    if (world == null) {
      return;
    }
    long now = world.getTotalWorldTime();
    // (a subtraction from the never-walked sentinel would overflow, so it is tested first)
    if (sizedAt != Long.MIN_VALUE && now >= sizedAt && now - sizedAt < 20) {
      return;
    }
    sizedAt = now;
    IBlockState s = state();
    if (s == null) {
      return;
    }
    EnumFacing right = s.getValue(BlockVenue.FACING).rotateYCCW();
    width = run(s, right, MAX_W);
    height = run(s, EnumFacing.UP, MAX_H);
  }

  private int run(IBlockState s, EnumFacing step, int max) {
    int n = 1;
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos);
    while (n < max) {
      at.move(step);
      if (!world.isBlockLoaded(at) || !BlockMarqueeBoard.joins(world, at, s)) {
        break;
      }
      n++;
    }
    return n;
  }

  // --- IVenueTextHolder ----------------------------------------------------------------------

  @Override
  public int lineCount() {
    return 3;
  }

  @Override
  public int maxLength(int i) {
    return MAX[i];
  }

  @Override
  public String lineCaption(int i) {
    return "gui.csm.venue_text.marquee." + i;
  }

  @Override
  public int choiceCount() {
    return 3;
  }

  @Override
  public int choiceRange(int i) {
    return i == 0 ? MODES : VenueColours.COUNT;
  }

  @Override
  public int getChoice(int i) {
    return i == 0 ? mode : i == 1 ? foreground : background;
  }

  @Override
  public String choiceCaption(int i) {
    return i == 0 ? "gui.csm.venue_text.choice.mode" : i == 1 ? "gui.csm.venue_text.choice.text"
        : "gui.csm.venue_text.choice.back";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return i == 0 ? "csm.venues.marquee.mode." + v : VenueColours.key(v);
  }

  @Override
  public void applyText(String[] in, int[] choices) {
    for (int i = 0; i < 3; i++) {
      lines[i] = TileEntityScoreboard.clean(i < in.length ? in[i] : "", MAX[i]);
    }
    if (choices.length > 0) {
      mode = Math.max(0, Math.min(MODES - 1, choices[0]));
    }
    if (choices.length > 1) {
      foreground = VenueColours.clamp(choices[1]);
    }
    if (choices.length > 2) {
      background = VenueColours.clamp(choices[2]);
    }
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    String old = key();
    for (int i = 0; i < 3; i++) {
      if (c.hasKey(K_LINE[i])) {
        lines[i] = TileEntityScoreboard.clean(c.getString(K_LINE[i]), MAX[i]);
      }
    }
    if (c.hasKey(K_MODE)) {
      mode = Math.max(0, Math.min(MODES - 1, c.getInteger(K_MODE)));
    }
    if (c.hasKey(K_FG)) {
      foreground = VenueColours.clamp(c.getInteger(K_FG));
    }
    if (c.hasKey(K_BG)) {
      background = VenueColours.clamp(c.getInteger(K_BG));
    }
    if (!old.equals(key())) {
      textVersion++;
    }
  }

  private String key() {
    return lines[0] + '\u0000' + lines[1] + '\u0000' + lines[2] + '\u0000' + foreground;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    for (int i = 0; i < 3; i++) {
      c.setString(K_LINE[i], lines[i]);
    }
    c.setInteger(K_MODE, mode);
    c.setInteger(K_FG, foreground);
    c.setInteger(K_BG, background);
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  /** The board, as last walked: from the controller to the far top corner. */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    EnumFacing right = getFacing().rotateYCCW();
    BlockPos end = pos.offset(right, Math.max(0, width - 1)).up(Math.max(0, height - 1));
    return new AxisAlignedBB(Math.min(pos.getX(), end.getX()), pos.getY(),
        Math.min(pos.getZ(), end.getZ()), Math.max(pos.getX(), end.getX()) + 1, end.getY() + 1,
        Math.max(pos.getZ(), end.getZ()) + 1);
  }

  /** The long range plus the board's own size: the game measures to the controller alone. */
  @Override
  public double getMaxRenderDistanceSquared() {
    double r = 192.0 + Math.max(width, height);
    return CsmPerformance.capRenderDistanceSq(r * r);
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityMarqueeRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityMarqueeRenderer.release(pos);
    }
  }
}
