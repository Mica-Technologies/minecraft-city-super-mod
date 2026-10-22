package com.micatechnologies.minecraft.csm.technology.network;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Holds a wall-arm device's {@link MicafiAim}, and nothing else: it does not tick and has no
 * renderer. The block's metadata is full, so this is where the aim has to live; the baked model
 * reads it through the block's actual state.
 *
 * @since 2026.10
 */
public class TileEntityMicafiAim extends AbstractTileEntity {

  /** NBT key: the aim's ordinal. */
  private static final String KEY_AIM = "a";

  private MicafiAim aim = MicafiAim.CENTRE;

  /**
   * @return the head's aim
   */
  public MicafiAim getAim() {
    return aim;
  }

  /**
   * @param aim the head's aim
   */
  public void setAim(MicafiAim aim) {
    this.aim = aim;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    aim = MicafiAim.fromOrdinal(compound.getByte(KEY_AIM));
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setByte(KEY_AIM, (byte) aim.ordinal());
    return compound;
  }

  /** Only the aim reaches a baked model. */
  @Override
  protected long getBakedModelKey() {
    return aim.ordinal();
  }
}
