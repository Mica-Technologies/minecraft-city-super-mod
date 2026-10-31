package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A team store fixture's two colours: the garments' team colour and their trim (the stripes,
 * the number, a cap's brim), {@link VenueColours} indices. It does not tick and has no renderer:
 * the fixture's baked model is tinted by a block colour handler that reads these, so a colour
 * change rebuilds its chunk section ({@link #getBakedModelKey}).
 *
 * <p>NBT: {@code c} the team colour (default 0, navy), {@code t} the trim (default 14, white).</p>
 *
 * @since 2026.10
 */
public class TileEntityTeamColour extends AbstractTileEntity {

  private static final String K_COLOUR = "c";
  private static final String K_TRIM = "t";

  private int colour = VenueColours.NAVY;
  private int trim = VenueColours.WHITE;

  /** The team colour. */
  public int getColour() {
    return colour;
  }

  /** The trim colour. */
  public int getTrim() {
    return trim;
  }

  /**
   * Steps a colour on, server side, and sends it to the clients.
   *
   * @param ofTrim the trim rather than the team colour
   *
   * @return the new index
   */
  public int step(boolean ofTrim) {
    if (ofTrim) {
      trim = (trim + 1) % VenueColours.COUNT;
    } else {
      colour = (colour + 1) % VenueColours.COUNT;
    }
    if (world != null) {
      markDirtySync(world, pos, true);
    }
    return ofTrim ? trim : colour;
  }

  @Override
  public void readNBT(NBTTagCompound c) {
    if (c.hasKey(K_COLOUR)) {
      colour = VenueColours.clamp(c.getInteger(K_COLOUR));
    }
    if (c.hasKey(K_TRIM)) {
      trim = VenueColours.clamp(c.getInteger(K_TRIM));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    c.setInteger(K_COLOUR, colour);
    c.setInteger(K_TRIM, trim);
    return c;
  }

  /** The colours are tinted into the baked model, so a change of either rebuilds the section. */
  @Override
  protected long getBakedModelKey() {
    return colour | (long) trim << 8;
  }
}
