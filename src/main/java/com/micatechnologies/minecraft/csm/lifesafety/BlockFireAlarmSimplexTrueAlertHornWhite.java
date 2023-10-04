package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmSimplexTrueAlertHornWhite extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmsimplextruealerthornwhite";
    }

    @Override
    public String getSoundResourceName( IBlockState blockState ) {
        return "csm:stahorn";
    }

    @Override
    public int getSoundTickLen( IBlockState blockState ) {
        return 60;
    }
}
