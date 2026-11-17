package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * An address plaque's words and settings ({@link AddressPlaque}, whose notes list the NBT). Data
 * only: never ticked and never drawn by a renderer; the plaque's baked model reads it.
 *
 * <p>The keys sit at the top of the tile entity's compound, so a script sets a plaque with
 * {@code /setblock ... csm:address_plaque_brass 2 replace {num:"245",st:"W 86th St"}} or a
 * {@code /blockdata} on one already placed, exactly as the editor does.</p>
 *
 * @since 2026.11
 */
public class TileEntityAddressPlaque extends AbstractTileEntity {

  private AddressPlaque plaque = AddressPlaque.DEFAULT;

  public AddressPlaque getPlaque() {
    return plaque;
  }

  /**
   * Sets the plaque, relighting a lamp box that was switched on or off.
   *
   * @return whether anything changed
   */
  public boolean setPlaque(AddressPlaque plaque) {
    boolean changed = !this.plaque.equals(plaque);
    boolean relight = this.plaque.isLit() != plaque.isLit();
    this.plaque = plaque;
    // only for a live plaque: one read as its chunk loads is lit with the chunk
    if (relight && world != null && world.isBlockLoaded(pos)
        && world.getTileEntity(pos) == this) {
      world.checkLight(pos);
    }
    return changed;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    setPlaque(AddressPlaque.isPlaque(compound) ? AddressPlaque.read(compound)
        : AddressPlaque.DEFAULT);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    return plaque.write(compound);
  }

  /** Everything the baked model draws comes from here, so a sync that changes none is free. */
  @Override
  protected long getBakedModelKey() {
    return plaque.hashCode();
  }
}
