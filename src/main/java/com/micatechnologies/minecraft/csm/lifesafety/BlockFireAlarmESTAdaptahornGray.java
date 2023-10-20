package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmESTAdaptahornGray extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmestadaptahorngray";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:edwards_adaptahorn_code44";
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    return 170;
  }
}
