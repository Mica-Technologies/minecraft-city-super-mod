package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmSimplexTrueAlertLEDRed extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmsimplextruealertledred";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:4030code44";
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    return 115;
  }
}
