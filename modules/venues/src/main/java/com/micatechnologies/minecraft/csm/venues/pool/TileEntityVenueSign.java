package com.micatechnologies.minecraft.csm.venues.pool;

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
 * The words on a {@link BlockVenueSign}: a pool's scoreboard or a golf hole's tee sign, which by
 * its block's {@link BlockVenueSign.Style}. Up to seven lines and one colour, edited in the venue
 * text editor or by NBT ({@code l0} ... {@code l6}, {@code col}, a venue colour); nothing ticks.
 * What each line means is the style's (see {@code VENUES_SYSTEM.md}).
 *
 * @since 2026.10
 */
public class TileEntityVenueSign extends AbstractTileEntity implements IVenueTextHolder {

  private final String[] lines = new String[MAX_LINES];
  private int colour = -1;
  private int textVersion;
  /** The text version the renderer's list was compiled for (client). */
  int renderedVersion = -1;

  /** Its block's style, or null when the block is not a sign. */
  BlockVenueSign.Style style() {
    BlockVenueSign b = block();
    return b == null ? null : b.style();
  }

  BlockVenueSign block() {
    if (world == null) {
      return null;
    }
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockVenueSign ? (BlockVenueSign) s.getBlock() : null;
  }

  EnumFacing facing() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockVenueSign) {
        return s.getValue(BlockVenue.FACING);
      }
    }
    return EnumFacing.NORTH;
  }

  /** Line {@code i}, or the style's default where it was never set. */
  public String line(int i) {
    if (i < 0 || i >= MAX_LINES) {
      return "";
    }
    if (lines[i] != null) {
      return lines[i];
    }
    BlockVenueSign.Style st = style();
    return st == null || i >= st.defaults.length ? "" : st.defaults[i];
  }

  /** The sign's colour (a venue colour). */
  public int colour() {
    BlockVenueSign.Style st = style();
    return colour >= 0 ? colour : st == null ? VenueColours.NAVY : st.colour;
  }

  public int getTextVersion() {
    return textVersion;
  }

  private void sync() {
    textVersion++;
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  // --- IVenueTextHolder ----------------------------------------------------------------------

  @Override
  public int lineCount() {
    BlockVenueSign.Style st = style();
    return st == null ? 0 : st.lengths.length;
  }

  @Override
  public int maxLength(int i) {
    BlockVenueSign.Style st = style();
    return st == null || i >= st.lengths.length ? 0 : st.lengths[i];
  }

  @Override
  public String getLine(int i) {
    return line(i);
  }

  @Override
  public String lineCaption(int i) {
    BlockVenueSign.Style st = style();
    return "gui.csm.venue_text." + (st == null ? "pool" : st.key) + "." + i;
  }

  @Override
  public int choiceCount() {
    return 1;
  }

  @Override
  public int choiceRange(int i) {
    return VenueColours.COUNT;
  }

  @Override
  public int getChoice(int i) {
    return colour();
  }

  @Override
  public String choiceCaption(int i) {
    return "gui.csm.venue_text.choice.colour";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return VenueColours.key(v);
  }

  @Override
  public void applyText(String[] in, int[] choices) {
    int n = lineCount();
    for (int i = 0; i < in.length && i < n; i++) {
      lines[i] = TileEntityScoreboard.clean(in[i], maxLength(i));
    }
    if (choices.length > 0) {
      colour = VenueColours.clamp(choices[0]);
    }
    sync();
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    for (int i = 0; i < MAX_LINES; i++) {
      if (c.hasKey("l" + i)) {
        lines[i] = TileEntityScoreboard.clean(c.getString("l" + i), 128);
      }
    }
    if (c.hasKey("col")) {
      colour = VenueColours.clamp(c.getInteger("col"));
    }
    textVersion++;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    for (int i = 0; i < MAX_LINES; i++) {
      if (lines[i] != null) {
        c.setString("l" + i, lines[i]);
      }
    }
    if (colour >= 0) {
      c.setInteger("col", colour);
    }
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockVenueSign b = block();
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
      TileEntityVenueSignRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityVenueSignRenderer.release(pos);
    }
  }
}
