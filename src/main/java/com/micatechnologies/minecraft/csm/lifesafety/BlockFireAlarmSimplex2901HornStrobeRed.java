package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmSimplex2901HornStrobeRed extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmsimplex2901hornstrobered";
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
