package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

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
}
