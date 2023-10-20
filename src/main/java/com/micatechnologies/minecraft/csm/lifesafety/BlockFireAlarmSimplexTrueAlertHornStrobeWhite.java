package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmSimplexTrueAlertHornStrobeWhite extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmsimplextruealerthornstrobewhite";
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
