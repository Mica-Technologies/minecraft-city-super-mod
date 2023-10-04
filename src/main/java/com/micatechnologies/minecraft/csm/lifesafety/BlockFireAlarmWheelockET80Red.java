package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmWheelockET80Red extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmwheelocket80red";
    }

    @Override
    public String getSoundResourceName( IBlockState blockState ) {
        return "csm:simplex_4051_marchtime";
    }

    @Override
    public int getSoundTickLen( IBlockState blockState ) {
        return 130;
    }
}
