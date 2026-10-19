package com.micatechnologies.minecraft.csm.healthcare.nursecall;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The nurse call master panel's room lamps: which of its six rooms are calling, one bit each
 * ({@code rooms}, NBT byte). It does not tick; the network sets it when a call starts or ends,
 * and the panel's baked model reads it through the block's actual state.
 *
 * @since 2026.10
 */
public class TileEntityNurseCallPanel extends AbstractTileEntity {

  private static final String KEY_ROOMS = "rooms";

  private int rooms;

  /**
   * The room lamps lit, one bit a room.
   *
   * @return the mask
   */
  public int getRooms() {
    return rooms;
  }

  /**
   * Sets the room lamps and sends them to the clients watching.
   *
   * @param rooms the mask
   */
  public void setRooms(int rooms) {
    this.rooms = rooms & 0x3F;
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    rooms = compound.getByte(KEY_ROOMS) & 0x3F;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setByte(KEY_ROOMS, (byte) rooms);
    return compound;
  }

  @Override
  protected long getBakedModelKey() {
    return rooms;
  }
}
