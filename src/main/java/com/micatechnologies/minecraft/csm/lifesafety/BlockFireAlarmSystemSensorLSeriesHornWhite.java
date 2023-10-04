package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmSystemSensorLSeriesHornWhite extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmsystemsensorlserieshornwhite";
    }

    @Override
    public String getSoundResourceName( IBlockState blockState ) {
        return "csm:spectralert";
    }

    @Override
    public int getSoundTickLen( IBlockState blockState ) {
        return 60;
    }
}
