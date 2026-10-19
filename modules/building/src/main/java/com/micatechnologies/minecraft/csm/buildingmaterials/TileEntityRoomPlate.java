package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A room plate's legend: its room number and room name. Data only: never ticked, and drawn by
 * the plate's baked model ({@link RoomPlateBakedModel}), not a renderer.
 *
 * <p>NBT: {@code num} (int, 0 to 9999, or -1 for no number) and {@code name} (byte, an index into
 * {@link RoomPlateGlyphs#NAMES}, 0 for none). Both are set with {@code /blockdata}; a click cycles
 * the name and a sneak-click steps the number.</p>
 *
 * @version 1.0
 * @since 2026.10
 */
public class TileEntityRoomPlate extends AbstractTileEntity {

  private static final String KEY_NUMBER = "num";
  private static final String KEY_NAME = "name";

  private int number = 101;
  private int name = 1;

  public int getNumber() {
    return number;
  }

  public int getName() {
    return name;
  }

  void set(int number, int name) {
    this.number = RoomPlateGlyphs.clampNumber(number);
    this.name = RoomPlateGlyphs.clampName(name);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    number = compound.hasKey(KEY_NUMBER) ? RoomPlateGlyphs.clampNumber(compound.getInteger(
        KEY_NUMBER)) : 101;
    name = compound.hasKey(KEY_NAME) ? RoomPlateGlyphs.clampName(compound.getInteger(KEY_NAME))
        : 1;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_NUMBER, number);
    compound.setByte(KEY_NAME, (byte) name);
    return compound;
  }

  /** Only the legend is drawn from this, so a sync that leaves it alone rebuilds nothing. */
  @Override
  protected long getBakedModelKey() {
    return number * 16L + name;
  }
}
