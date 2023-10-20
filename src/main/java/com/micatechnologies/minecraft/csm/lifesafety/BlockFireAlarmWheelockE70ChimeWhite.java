package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmWheelockE70ChimeWhite extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmwheelocke70chimewhite";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:et70_chime";
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    return 140;
  }
}
