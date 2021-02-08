package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

public abstract class AbstractBlockFireAlarmDetector extends AbstractBlockFireAlarmActivator
{
    public static final int VERT_BELOW_BLOCKS_CHECK    = 30;
    public static final int RADIUS_AROUND_BLOCKS_CHECK = 15;

    @Override
    public int getBlockTickRate() {
        // Check for fires every 25 seconds. (25 secs x 20 ticks per second = 500 ticks)
        return 500;
    }

    @Override
    public void onTick( World world, BlockPos blockPos, IBlockState blockState ) {
        boolean foundFire = false;

        // Search for fire
        int deviceX = blockPos.getX();
        int deviceY = blockPos.getY();
        int deviceZ = blockPos.getZ();
        for ( int searchX = deviceX - RADIUS_AROUND_BLOCKS_CHECK;
              searchX <= deviceX + RADIUS_AROUND_BLOCKS_CHECK;
              searchX++ ) {
            for ( int searchZ = deviceZ - RADIUS_AROUND_BLOCKS_CHECK;
                  searchZ <= deviceZ + RADIUS_AROUND_BLOCKS_CHECK;
                  searchZ++ ) {
                for ( int searchY = deviceY - VERT_BELOW_BLOCKS_CHECK; searchY <= deviceY; searchY++ ) {
                    BlockPos searchBlockPos = new BlockPos( searchX, searchY, searchZ );
                    IBlockState searchBlockState = world.getBlockState( searchBlockPos );
                    Block searchBlock = searchBlockState.getBlock();
                    if ( searchBlock == Blocks.FIRE || searchBlockState.getMaterial() == Material.FIRE ) {
                        foundFire = true;
                        MinecraftServer mcserv = FMLCommonHandler.instance().getMinecraftServerInstance();
                        if ( mcserv != null ) {
                            mcserv.sendMessage( new TextComponentString(
                                    "A fire has been detected at [" + searchX + "," + searchY + "," + searchZ + "]" ) );
                        }
                        onFire( world, blockPos, blockState );
                        break;
                    }
                }
            }
        }

        // If fire found, activate linked panel
        if ( foundFire ) {
            activateLinkedPanel( world, blockPos, null );
        }
    }

    abstract public void onFire( World world, BlockPos blockPos, IBlockState blockState );
}
