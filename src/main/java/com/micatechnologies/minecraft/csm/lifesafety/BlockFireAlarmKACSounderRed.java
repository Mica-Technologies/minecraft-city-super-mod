package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmKACSounderRed extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmkacsounderred";
    }

    @Override
    public String getSoundResourceName( IBlockState blockState ) {
        return "csm:kac";
    }

    @Override
    public int getSoundTickLen( IBlockState blockState ) {
        return 315;
    }
}
