package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A simple non-ticking tile entity that stores a sound selection index. Used by fire alarm
 * sounder blocks that need more than 2 sound options (which exceeds the 4-bit block meta
 * capacity when combined with 6-direction rotation).
 */
public class TileEntityFireAlarmSoundIndex extends AbstractTileEntity {

  private static final String SOUND_INDEX_KEY = "sIx";
  private static final String LEGACY_SOUND_INDEX_KEY = "soundIndex";
  private int soundIndex = 0;

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(SOUND_INDEX_KEY)) {
      soundIndex = compound.getInteger(SOUND_INDEX_KEY);
    } else if (compound.hasKey(LEGACY_SOUND_INDEX_KEY)) {
      soundIndex = compound.getInteger(LEGACY_SOUND_INDEX_KEY);
    }
    compound.removeTag(LEGACY_SOUND_INDEX_KEY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(SOUND_INDEX_KEY, soundIndex);
    return compound;
  }

  public int getSoundIndex() {
    return soundIndex;
  }

  public void setSoundIndex(int index) {
    soundIndex = index;
    markDirty();
  }

  public void cycleSoundIndex(int maxSounds) {
    soundIndex = (soundIndex + 1) % maxSounds;
    markDirty();
  }

  /**
   * Returns a render bounding box sized for the strobe flash quad + a 1-block margin, so
   * vanilla frustum culling can skip the shared strobe TESR when the device is offscreen.
   * This TE is bound to {@code TileEntityFireAlarmStrobeRenderer} for the Gentex Commander
   * 3 line, which renders the same flash effect that {@link TileEntityFireAlarmStrobe}
   * does — the bounds match for consistent culling.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0,
        pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
  }
}
