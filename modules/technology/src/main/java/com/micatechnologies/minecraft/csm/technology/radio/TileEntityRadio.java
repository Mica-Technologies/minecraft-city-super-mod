package com.micatechnologies.minecraft.csm.technology.radio;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A handheld radio stood on a surface: holds its {@link RadioSettings} while it is a block. It
 * does not tick and nothing is drawn from it.
 *
 * @since 2026.11
 */
public class TileEntityRadio extends AbstractTileEntity {

  private RadioSettings settings = RadioSettings.DEFAULT;

  @Override
  public void readNBT(NBTTagCompound compound) {
    settings = RadioSettings.read(compound);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    return settings.write(compound);
  }

  /** The radio's settings, in the widest range; narrow them with {@code forModel}. */
  public RadioSettings getSettings() {
    return settings;
  }

  /**
   * Replaces the radio's settings.
   *
   * @param settings the settings to hold
   */
  public void setSettings(RadioSettings settings) {
    this.settings = settings == null ? RadioSettings.DEFAULT : settings;
  }
}
