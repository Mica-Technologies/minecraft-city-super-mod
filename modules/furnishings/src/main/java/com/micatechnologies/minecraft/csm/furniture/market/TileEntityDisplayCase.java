package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityResidentialStorage;
import net.minecraft.nbt.NBTTagCompound;

/**
 * What a two-block refrigerated display (the reach-in cooler and freezer, the open dairy case)
 * holds, in its lower half: the 27 slots of a {@link TileEntityResidentialStorage}, and whether
 * redstone powered the case when it last looked. The metadata has no room left for that (facing,
 * half and lights), so, like the fireplace's power, it is kept here, and only a change of power
 * switches the lights.
 *
 * @since 2026.9
 */
public class TileEntityDisplayCase extends TileEntityResidentialStorage {

  private static final String KEY_POWERED = "p";

  private boolean powered;

  /** Constructs an empty one, as the game does before loading a saved one. */
  public TileEntityDisplayCase() {
    this(27);
  }

  /**
   * Constructs one with {@code slots} slots.
   *
   * @param slots how many slots
   */
  public TileEntityDisplayCase(int slots) {
    super(slots);
  }

  /**
   * Whether redstone powered the case when it was last looked at.
   *
   * @return whether it was powered
   */
  public boolean wasPowered() {
    return powered;
  }

  /**
   * Remembers whether redstone powers the case.
   *
   * @param powered whether it is powered
   */
  public void setPowered(boolean powered) {
    this.powered = powered;
    markDirty();
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    powered = compound.getBoolean(KEY_POWERED);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    compound.setBoolean(KEY_POWERED, powered);
    return compound;
  }
}
