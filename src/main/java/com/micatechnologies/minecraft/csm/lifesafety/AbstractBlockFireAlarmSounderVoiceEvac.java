package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

/**
 * Abstract base class for fire alarm voice evacuation speaker blocks. Returns a null sound
 * resource so that audio is managed externally via the voice evacuation channel system.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public abstract class AbstractBlockFireAlarmSounderVoiceEvac extends AbstractBlockFireAlarmSounder {

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return null;
  }
}