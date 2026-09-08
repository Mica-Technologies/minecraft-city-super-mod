package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Appearance state for an {@link BlockControllableRrfb}: what colour the housing is painted and
 * whether it carries lamps on both faces or only the one it points at.
 *
 * <p>Neither can live in the block's metadata, which facing and colour state already use in
 * full, so the block reads both back out of here in {@code getActualState} and the blockstate
 * picks the model and textures from them. Nothing here affects how the controller drives the
 * beacon — this is finish only.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TileEntityRrfb extends AbstractTileEntity {

  /** Short NBT keys, matching the convention the other signal tile entities use. */
  private static final String KEY_HOUSING_COLOR = "hsC";
  private static final String KEY_DOUBLE_SIDED = "dbS";

  private TrafficSignalBodyColor housingColor = TrafficSignalBodyColor.FLAT_BLACK;

  /**
   * Double-sided by default: a crossing is normally signed from both approaches, and it is
   * less surprising to turn a face off than to discover one missing.
   */
  private boolean doubleSided = true;

  public TrafficSignalBodyColor getHousingColor() {
    return housingColor;
  }

  public void setHousingColor(TrafficSignalBodyColor color) {
    this.housingColor = color;
    markDirtySync(world, pos, true);
  }

  /**
   * Advances the housing paint to the next colour and returns it. Invoked by the config GUI.
   *
   * @return the colour now applied
   */
  public TrafficSignalBodyColor getNextHousingColor() {
    setHousingColor(housingColor.getNextColor());
    return housingColor;
  }

  public boolean isDoubleSided() {
    return doubleSided;
  }

  public void setDoubleSided(boolean doubleSided) {
    this.doubleSided = doubleSided;
    markDirtySync(world, pos, true);
  }

  /**
   * Flips between lamps on both faces and lamps on the facing side only.
   *
   * @return the state now applied
   */
  public boolean toggleDoubleSided() {
    setDoubleSided(!doubleSided);
    return doubleSided;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    housingColor = TrafficSignalBodyColor.fromNBT(compound.getInteger(KEY_HOUSING_COLOR));
    // Absent means a beacon saved before the option existed, and those were all double-sided.
    doubleSided = !compound.hasKey(KEY_DOUBLE_SIDED) || compound.getBoolean(KEY_DOUBLE_SIDED);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_HOUSING_COLOR, housingColor.toNBT());
    compound.setBoolean(KEY_DOUBLE_SIDED, doubleSided);
    return compound;
  }
}
