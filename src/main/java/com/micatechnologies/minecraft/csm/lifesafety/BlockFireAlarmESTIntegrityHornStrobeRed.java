package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;

public class BlockFireAlarmESTIntegrityHornStrobeRed extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmestintegrityhornstrobered";
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
