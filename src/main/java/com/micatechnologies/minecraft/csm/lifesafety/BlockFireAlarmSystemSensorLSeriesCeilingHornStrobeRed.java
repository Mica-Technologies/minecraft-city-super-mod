package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmSystemSensorLSeriesCeilingHornStrobeRed extends
    AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmsystemsensorlseriesceilinghornstrobered";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:spectralert";
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    return 60;
  }
}
