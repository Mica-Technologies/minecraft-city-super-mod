package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmESTIntegrityHornStrobeWhite extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmestintegrityhornstrobewhite";
    }

    @Override
    public String getSoundResourceName( IBlockState blockState ) {
        return "csm:est_integrity";
    }

    @Override
    public int getSoundTickLen( IBlockState blockState ) {
        return 70;
    }
}
