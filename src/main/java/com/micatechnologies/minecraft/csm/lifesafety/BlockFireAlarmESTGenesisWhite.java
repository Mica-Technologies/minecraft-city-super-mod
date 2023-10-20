package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmESTGenesisWhite extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmestgenesiswhite";
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
