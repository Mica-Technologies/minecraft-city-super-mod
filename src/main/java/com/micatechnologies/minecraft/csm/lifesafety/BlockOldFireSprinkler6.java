package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockOldFireSprinkler6 extends AbstractBlockFireAlarmDetector
{
    @Override
    public String getBlockRegistryName() {
        return "oldfiresprinkler6";
    }

    @Override
    public void onFire( World world, BlockPos blockPos, IBlockState blockState ) {
        int waterX = blockPos.getX();
        int waterY = blockPos.getY() - 1;
        int waterZ = blockPos.getZ();
        BlockPos waterBlockPos = new BlockPos( waterX, waterY, waterZ );
        IBlockState previousBlockState = world.getBlockState( waterBlockPos );
        world.setBlockState( waterBlockPos, Blocks.FLOWING_WATER.getDefaultState(), 3 );
        world.notifyBlockUpdate( waterBlockPos, previousBlockState, Blocks.FLOWING_WATER.getDefaultState(), 3 );
        world.notifyNeighborsOfStateChange( waterBlockPos, Blocks.FLOWING_WATER, true );
    }
}
