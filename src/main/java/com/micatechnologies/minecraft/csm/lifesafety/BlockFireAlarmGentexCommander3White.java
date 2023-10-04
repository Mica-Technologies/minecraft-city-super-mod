package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmGentexCommander3White extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmgentexcommander3white";
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
