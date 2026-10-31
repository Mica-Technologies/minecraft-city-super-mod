package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The ribbon board text piece's tile entity: the text and how it is shown. It does not tick.
 *
 * <p>NBT ({@code /blockdata}): {@code txt} the text, up to 64 characters of printable ASCII;
 * {@code fg} and {@code bg} the text and band colours ({@link VenueColours} indices);
 * {@code spd} the speed, 0 still (centred on the run), 1 slow, 2 medium, 3 fast.</p>
 *
 * <p>On the client it also keeps how long its run is, found by walking along the band and kept
 * for a second ({@link #runCells}), since a cell far down the band changing tells it nothing.</p>
 *
 * @since 2026.10
 */
public class TileEntityRibbonText extends AbstractTileEntity implements IVenueTextHolder {

  /** Longest text. */
  public static final int MAX_TEXT = 64;
  /** Longest run the text crosses, blocks. */
  public static final int MAX_RUN = 256;
  /** The speeds, pixels (sixteenths of a block) a second. */
  static final float[] SPEEDS = {0f, 16f, 32f, 64f};

  private static final String K_TXT = "txt";
  private static final String K_FG = "fg";
  private static final String K_BG = "bg";
  private static final String K_SPD = "spd";

  private String text = "WELCOME TO THE ARENA";
  private int foreground = VenueColours.GOLD;
  private int background = VenueColours.BLACK;
  private int speed = 2;

  private int textVersion;
  /** The text version the renderer's list was compiled for (client). */
  int renderedVersion = -1;
  private int run = 1;
  private long runCheckedAt = Long.MIN_VALUE;

  public String getText() {
    return text;
  }

  public int getForeground() {
    return foreground;
  }

  public int getBackground() {
    return background;
  }

  public int getSpeed() {
    return speed;
  }

  public int getTextVersion() {
    return textVersion;
  }

  /** The way the band faces. */
  public EnumFacing getFacing() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockLedRibbonText) {
        return s.getValue(BlockVenue.FACING);
      }
    }
    return EnumFacing.NORTH;
  }

  /**
   * How many cells the text crosses: this one and the straights carrying on to the right, the
   * same way round, up to the first other text piece. Walked again at most once a second.
   */
  public int runCells() {
    if (world == null) {
      return run;
    }
    long now = world.getTotalWorldTime();
    // (a subtraction from the never-walked sentinel would overflow, so it is tested first)
    if (runCheckedAt != Long.MIN_VALUE && now >= runCheckedAt && now - runCheckedAt < 20) {
      return run;
    }
    runCheckedAt = now;
    EnumFacing facing = getFacing();
    EnumFacing right = facing.rotateYCCW();
    int n = 1;
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos);
    while (n < MAX_RUN) {
      at.move(right);
      if (!world.isBlockLoaded(at)) {
        break;
      }
      IBlockState s = world.getBlockState(at);
      if (!(s.getBlock() instanceof BlockLedRibbon) || s.getBlock() instanceof BlockLedRibbonText
          || s.getValue(BlockVenue.FACING) != facing) {
        break;
      }
      n++;
    }
    run = n;
    return n;
  }

  // --- IVenueTextHolder ----------------------------------------------------------------------

  @Override
  public int lineCount() {
    return 1;
  }

  @Override
  public int maxLength(int i) {
    return MAX_TEXT;
  }

  @Override
  public String getLine(int i) {
    return text;
  }

  @Override
  public String lineCaption(int i) {
    return "gui.csm.venue_text.ribbon.0";
  }

  @Override
  public int choiceCount() {
    return 3;
  }

  @Override
  public int choiceRange(int i) {
    return i == 2 ? SPEEDS.length : VenueColours.COUNT;
  }

  @Override
  public int getChoice(int i) {
    return i == 0 ? foreground : i == 1 ? background : speed;
  }

  @Override
  public String choiceCaption(int i) {
    return i == 0 ? "gui.csm.venue_text.choice.text" : i == 1 ? "gui.csm.venue_text.choice.back"
        : "gui.csm.venue_text.choice.speed";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return i == 2 ? "csm.venues.speed." + v : VenueColours.key(v);
  }

  @Override
  public void applyText(String[] lines, int[] choices) {
    text = TileEntityScoreboard.clean(lines.length > 0 ? lines[0] : "", MAX_TEXT);
    if (choices.length > 0) {
      foreground = VenueColours.clamp(choices[0]);
    }
    if (choices.length > 1) {
      background = VenueColours.clamp(choices[1]);
    }
    if (choices.length > 2) {
      speed = Math.max(0, Math.min(SPEEDS.length - 1, choices[2]));
    }
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    String old = text + '\u0000' + foreground;
    if (c.hasKey(K_TXT)) {
      text = TileEntityScoreboard.clean(c.getString(K_TXT), MAX_TEXT);
    }
    if (c.hasKey(K_FG)) {
      foreground = VenueColours.clamp(c.getInteger(K_FG));
    }
    if (c.hasKey(K_BG)) {
      background = VenueColours.clamp(c.getInteger(K_BG));
    }
    if (c.hasKey(K_SPD)) {
      speed = Math.max(0, Math.min(SPEEDS.length - 1, c.getInteger(K_SPD)));
    }
    if (!old.equals(text + '\u0000' + foreground)) {
      textVersion++;
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    c.setString(K_TXT, text);
    c.setInteger(K_FG, foreground);
    c.setInteger(K_BG, background);
    c.setInteger(K_SPD, speed);
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  /** The run, from this cell to the far end, whatever it was when last walked. */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    EnumFacing right = getFacing().rotateYCCW();
    BlockPos end = pos.offset(right, Math.max(0, run - 1));
    return new AxisAlignedBB(Math.min(pos.getX(), end.getX()), pos.getY(),
        Math.min(pos.getZ(), end.getZ()), Math.max(pos.getX(), end.getX()) + 1, pos.getY() + 1,
        Math.max(pos.getZ(), end.getZ()) + 1);
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(128.0 * 128.0);
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
      TileEntityRibbonTextRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityRibbonTextRenderer.release(pos);
    }
  }
}
