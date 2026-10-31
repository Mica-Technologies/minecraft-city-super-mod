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
 * The end zone lettering's words and colours, edited in the venue text editor or by NBT. It does
 * not tick.
 *
 * <p>NBT ({@code /blockdata}): {@code txt} the words (up to 24 printable characters, default
 * HOME), {@code h} the letters' height in blocks (1-9, default 5), {@code w} the widest the words
 * may run, in blocks (4-48, default 40; they shrink to fit), {@code fc} their colour and
 * {@code oc} an outline's ({@link VenueColours}, white and none: 16 is no outline).</p>
 *
 * @since 2026.10
 */
public class TileEntityEndZoneLettering extends AbstractTileEntity implements IVenueTextHolder {

  static final int MAX_TEXT = 24;
  /** {@code oc}: no outline. */
  public static final int NO_OUTLINE = VenueColours.COUNT;

  private String text = "HOME";
  private int height = 5;
  private int width = 40;
  private int colour = VenueColours.WHITE;
  private int outline = NO_OUTLINE;

  public String getText() {
    return text;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public int getColour() {
    return colour;
  }

  /** The outline's colour index, or {@link #NO_OUTLINE}. */
  public int getOutline() {
    return outline;
  }

  EnumFacing facing() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockEndZoneLettering) {
        return s.getValue(BlockVenue.FACING);
      }
    }
    return EnumFacing.NORTH;
  }

  private static int number(String s, int min, int max, int old) {
    try {
      return Math.max(min, Math.min(max, Integer.parseInt(s.trim())));
    } catch (NumberFormatException e) {
      return old;
    }
  }

  // --- IVenueTextHolder ----------------------------------------------------------------------

  @Override
  public int lineCount() {
    return 3;
  }

  @Override
  public int maxLength(int i) {
    return i == 0 ? MAX_TEXT : 2;
  }

  @Override
  public String getLine(int i) {
    return i == 0 ? text : Integer.toString(i == 1 ? height : width);
  }

  @Override
  public String lineCaption(int i) {
    return "gui.csm.venue_text.lettering." + i;
  }

  @Override
  public int choiceCount() {
    return 2;
  }

  @Override
  public int choiceRange(int i) {
    return i == 0 ? VenueColours.COUNT : VenueColours.COUNT + 1;
  }

  @Override
  public int getChoice(int i) {
    return i == 0 ? colour : outline;
  }

  @Override
  public String choiceCaption(int i) {
    return i == 0 ? "gui.csm.venue_text.choice.lettering" : "gui.csm.venue_text.choice.outline";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return i == 1 && v >= VenueColours.COUNT ? "csm.venues.colour.none" : VenueColours.key(v);
  }

  @Override
  public void applyText(String[] lines, int[] choices) {
    if (lines.length > 0) {
      text = TileEntityScoreboard.clean(lines[0], MAX_TEXT);
    }
    if (lines.length > 1) {
      height = number(lines[1], 1, 9, height);
    }
    if (lines.length > 2) {
      width = number(lines[2], 4, 48, width);
    }
    if (choices.length > 0) {
      colour = VenueColours.clamp(choices[0]);
    }
    if (choices.length > 1) {
      outline = Math.max(0, Math.min(NO_OUTLINE, choices[1]));
    }
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    if (c.hasKey("txt")) {
      text = TileEntityScoreboard.clean(c.getString("txt"), MAX_TEXT);
    }
    height = c.hasKey("h") ? Math.max(1, Math.min(9, c.getInteger("h"))) : height;
    width = c.hasKey("w") ? Math.max(4, Math.min(48, c.getInteger("w"))) : width;
    colour = c.hasKey("fc") ? VenueColours.clamp(c.getInteger("fc")) : colour;
    outline = c.hasKey("oc") ? Math.max(0, Math.min(NO_OUTLINE, c.getInteger("oc"))) : outline;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    c.setString("txt", text);
    c.setInteger("h", height);
    c.setInteger("w", width);
    c.setInteger("fc", colour);
    c.setInteger("oc", outline);
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos).grow(25, 0, 25);
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(192.0 * 192.0);
  }
}
