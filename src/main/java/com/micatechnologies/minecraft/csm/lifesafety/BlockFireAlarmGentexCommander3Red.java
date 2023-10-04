package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmGentexCommander3Red extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmgentexcommander3red";
    }

    @Override
    public String getSoundResourceName( IBlockState blockState ) {
        return "csm:gentex_gos_code3";
    }

    @Override
    public int getSoundTickLen( IBlockState blockState ) {
        return 70;
    }
}
