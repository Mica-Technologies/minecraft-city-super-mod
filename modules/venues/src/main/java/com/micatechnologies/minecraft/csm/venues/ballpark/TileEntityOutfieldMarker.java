package com.micatechnologies.minecraft.csm.venues.ballpark;

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
 * An outfield distance marker's number: {@code txt} (up to five characters, default 400) and
 * {@code fc}, its colour ({@link VenueColours}, default white). It does not tick; the number is
 * {@link TileEntityOutfieldMarkerRenderer}'s.
 *
 * @since 2026.10
 */
public class TileEntityOutfieldMarker extends AbstractTileEntity implements IVenueTextHolder {

  static final int MAX = 5;

  private String text = "400";
  private int ink = VenueColours.WHITE;
  private int textVersion;
  /** The text version the renderer's list was compiled for (client). */
  int renderedVersion = -1;

  public String getText() {
    return text;
  }

  public int getInk() {
    return ink;
  }

  public int getTextVersion() {
    return textVersion;
  }

  /** The way the wall faces, and whether it is the 45 degree one. */
  EnumFacing facing() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockOutfieldWallMarker) {
        return s.getValue(BlockVenue.FACING);
      }
    }
    return EnumFacing.NORTH;
  }

  boolean diagonal() {
    if (world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockOutfieldWallMarker) {
        return s.getValue(BlockOutfieldWall.DIAGONAL);
      }
    }
    return false;
  }

  // --- IVenueTextHolder ----------------------------------------------------------------------

  @Override
  public int lineCount() {
    return 1;
  }

  @Override
  public int maxLength(int i) {
    return MAX;
  }

  @Override
  public String getLine(int i) {
    return text;
  }

  @Override
  public String lineCaption(int i) {
    return "gui.csm.venue_text.marker.0";
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
    return ink;
  }

  @Override
  public String choiceCaption(int i) {
    return "gui.csm.venue_text.choice.text";
  }

  @Override
  public String choiceValueKey(int i, int v) {
    return VenueColours.key(v);
  }

  @Override
  public void applyText(String[] lines, int[] choices) {
    text = TileEntityScoreboard.clean(lines.length > 0 ? lines[0] : "", MAX);
    if (choices.length > 0) {
      ink = VenueColours.clamp(choices[0]);
    }
    textVersion++;
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound c) {
    String old = text + '\u0000' + ink;
    if (c.hasKey("txt")) {
      text = TileEntityScoreboard.clean(c.getString("txt"), MAX);
    }
    if (c.hasKey("fc")) {
      ink = VenueColours.clamp(c.getInteger("fc"));
    }
    if (!old.equals(text + '\u0000' + ink)) {
      textVersion++;
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    c.setString("txt", text);
    c.setInteger("fc", ink);
    return c;
  }

  // --- rendering -----------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos.add(-2, 0, -2), pos.add(3, 2, 3));
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(192.0 * 192.0);
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityOutfieldMarkerRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityOutfieldMarkerRenderer.release(pos);
    }
  }
}
