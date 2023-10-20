package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmSimplexTrueAlertHornRed extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmsimplextruealerthornred";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:stahorn";
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    return 60;
  }
}
