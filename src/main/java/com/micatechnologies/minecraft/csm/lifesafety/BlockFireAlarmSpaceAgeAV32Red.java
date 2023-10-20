package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmSpaceAgeAV32Red extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmspaceageav32red";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:sae_marchtime";
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    return 30;
  }
}
