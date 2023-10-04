package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockFireAlarmSystemSensorAdvanceHornStrobeOutdoorRed extends AbstractBlockFireAlarmSounder
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmsystemsensoradvancehornstrobeoutdoorred";
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
