package com.micatechnologies.minecraft.csm.block;

import net.minecraft.block.state.IBlockState;

public abstract class AbstractBlockFireAlarmSounderVoiceEvac extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getSoundResourceName( IBlockState blockState ) {
        return null;
    }

    @Override
    public int getSoundTickLen( IBlockState blockState ) {
        return 0;
    }
}