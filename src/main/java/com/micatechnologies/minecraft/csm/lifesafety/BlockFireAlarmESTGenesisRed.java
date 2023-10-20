package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmESTGenesisRed extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmestgenesisred";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:est_genesis";
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    return 70;
  }
}
