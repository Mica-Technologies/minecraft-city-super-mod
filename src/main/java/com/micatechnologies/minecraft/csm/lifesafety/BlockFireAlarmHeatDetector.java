package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockFireAlarmHeatDetector extends AbstractBlockFireAlarmDetector
{
    @Override
    public String getBlockRegistryName() {
        return "firealarmheatdetector";
    }

    @Override
    public void onFire( World world, BlockPos blockPos, IBlockState blockState ) {
    }
}
