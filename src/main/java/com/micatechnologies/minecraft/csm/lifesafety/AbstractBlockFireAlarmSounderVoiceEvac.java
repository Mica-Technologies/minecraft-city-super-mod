package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public abstract class AbstractBlockFireAlarmSounderVoiceEvac extends AbstractBlockFireAlarmSounder {

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return null;
  }
}