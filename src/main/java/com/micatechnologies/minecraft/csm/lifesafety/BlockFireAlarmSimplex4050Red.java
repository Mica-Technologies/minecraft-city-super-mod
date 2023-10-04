package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmSimplex4050Red extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmsimplex4050red";
    }

    @Override
    public String getSoundResourceName( IBlockState blockState ) {
        return "csm:2910calcode";
    }

    @Override
    public int getSoundTickLen( IBlockState blockState ) {
        return 360;
    }
}
