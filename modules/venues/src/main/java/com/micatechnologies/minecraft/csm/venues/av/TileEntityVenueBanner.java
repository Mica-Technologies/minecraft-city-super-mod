package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A banner's tile entity: its three lines and three colours. It does not tick.
 *
 * <p>NBT ({@code /blockdata} on the master): {@code l1} the top line (up to 24 characters),
 * {@code l2} the big line (up to 8: a year or a number), {@code l3} the bottom line (24);
 * {@code bc} the cloth, {@code tc} the trim and {@code fc} the lettering, {@link VenueColours}
 * indices. A championship banner starts as CHAMPIONS / 2026 in white on navy with gold trim, a
 * retired number as PLAYER / 00 in white on crimson with white trim.</p>
 *
 * @since 2026.10
 */
public class TileEntityVenueBanner extends AbstractTileEntity implements IVenueTextHolder {

  static final int[] MAX = {24, 8, 24};

  private static final String[] K_LINE = {"l1", "l2", "l3"};
  private static final String K_CLOTH = "bc";
  private static final String K_TRIM = "tc";
  private static final String K_INK = "fc";

  private final String[] lines = {"", "", ""};
  private int cloth;
  private int trim;
  private int ink = VenueColours.WHITE;

  private int textVersion;
  /** The text version the renderer's list was compiled for (client). */
  int renderedVersion = -1;

  /** A championship banner's tile entity (and what a saved one is read into). */
  public TileEntityVenueBanner() {
    this(false);
  }

  /**
   * A new banner's tile entity.
   *
   * @param retired a retired number's defaults
   */
  public TileEntityVenueBanner(boolean retired) {
    if (retired) {
      lines[0] = "PLAYER";
      lines[1] = "00";
      cloth = VenueColours.CRIMSON;
      trim = VenueColours.WHITE;
    } else {
      lines[0] = "CHAMPIONS";
      lines[1] = "2026";
      cloth = VenueColours.NAVY;
      trim = VenueColours.GOLD;
    }
  }

  public int getCloth() {
    return cloth;
  }

  public int getTrim() {
    return trim;
  }

  public int getInk() {
    return ink;
  }

  public int getTextVersion() {
    return textVersion;
  }

  /** The banner's block, or null before it is there. */
  BlockVenueBanner block() {
    if (world == null) {
      return null;
    }
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockVenueBanner ? (BlockVenueBanner) s.getBlock() : null;
  }

  /** The way the banner faces. */
  public EnumFacing getFacing() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockVenueBanner) {
        return s.getValue(BlockVenue.FACING);
      }
    }
    return EnumFacing.NORTH;
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
  public String getLine(int i) {
    return lines[i];
  }

  @Override
  public String lineCaption(int i) {
    return "gui.csm.venue_text.banner." + i;
  }

  @Override
  public int choiceCount() {
    return 3;
  }

  @Override
  public int choiceRange(int i) {
    return VenueColours.COUNT;
  }

  @Override
  public int getChoice(int i) {
    return i == 0 ? cloth : i == 1 ? trim : ink;
  }

  @Override
  public String choiceCaption(int i) {
    return i == 0 ? "gui.csm.venue_text.choice.cloth" : i == 1 ? "gui.csm.venue_text.choice.trim"
        : "gui.csm.venue_text.choice.text";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return VenueColours.key(v);
  }

  @Override
  public void applyText(String[] in, int[] choices) {
    for (int i = 0; i < 3; i++) {
      lines[i] = TileEntityScoreboard.clean(i < in.length ? in[i] : "", MAX[i]);
    }
    if (choices.length > 0) {
      cloth = VenueColours.clamp(choices[0]);
    }
    if (choices.length > 1) {
      trim = VenueColours.clamp(choices[1]);
    }
    if (choices.length > 2) {
      ink = VenueColours.clamp(choices[2]);
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
    if (c.hasKey(K_CLOTH)) {
      cloth = VenueColours.clamp(c.getInteger(K_CLOTH));
    }
    if (c.hasKey(K_TRIM)) {
      trim = VenueColours.clamp(c.getInteger(K_TRIM));
    }
    if (c.hasKey(K_INK)) {
      ink = VenueColours.clamp(c.getInteger(K_INK));
    }
    if (!old.equals(key())) {
      textVersion++;
    }
  }

  private String key() {
    return lines[0] + '\u0000' + lines[1] + '\u0000' + lines[2] + '\u0000' + ink;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    for (int i = 0; i < 3; i++) {
      c.setString(K_LINE[i], lines[i]);
    }
    c.setInteger(K_CLOTH, cloth);
    c.setInteger(K_TRIM, trim);
    c.setInteger(K_INK, ink);
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockVenueBanner b = block();
    if (b == null) {
      return new AxisAlignedBB(pos);
    }
    return b.worldOutline(pos, getFacing());
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(128.0 * 128.0);
  }

  /** The cloth and trim are tinted by the block colour handler, which reads them here. */
  @Override
  protected long getBakedModelKey() {
    return cloth | (long) trim << 8;
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityVenueBannerRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityVenueBannerRenderer.release(pos);
    }
  }
}
