package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;

/**
 * An exit pay kiosk's settings (#302): its owner (whoever placed it), who with an op may change
 * the hourly rate, and the rate itself, emeralds an hour or as much money through SUM's economy
 * (0 is free). Nothing ticks. NBT: {@code ow} the owner, {@code rt} the rate.
 *
 * @since 2026.10
 */
public class TileEntityParkingKiosk extends AbstractTileEntity {

  /** The rates the owner cycles through, per hour. */
  static final int[] RATES = {1, 2, 3, 5, 10, 0};

  @Nullable
  private UUID owner;
  private int rate = 1;

  @Nullable
  public UUID getOwner() {
    return owner;
  }

  public void setOwner(UUID owner) {
    this.owner = owner;
    markDirty();
  }

  public int getRate() {
    return rate;
  }

  /** The next rate in {@link #RATES}. */
  public int cycleRate() {
    int at = 0;
    for (int i = 0; i < RATES.length; i++) {
      if (RATES[i] == rate) {
        at = i;
      }
    }
    rate = RATES[(at + 1) % RATES.length];
    markDirty();
    return rate;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    owner = compound.hasUniqueId("ow") ? compound.getUniqueId("ow") : null;
    rate = compound.hasKey("rt") ? Math.max(0, compound.getInteger("rt")) : 1;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (owner != null) {
      compound.setUniqueId("ow", owner);
    }
    compound.setInteger("rt", rate);
    return compound;
  }

  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
