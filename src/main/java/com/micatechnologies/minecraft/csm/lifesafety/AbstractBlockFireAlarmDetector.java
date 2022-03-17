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

        // Build search corner positions
        int corner1X = blockPos.getX() - RADIUS_AROUND_BLOCKS_CHECK;
        int corner1Y = blockPos.getY() - VERT_BELOW_BLOCKS_CHECK;
        int corner1Z = blockPos.getZ() - RADIUS_AROUND_BLOCKS_CHECK;
        int corner2X = blockPos.getX() + RADIUS_AROUND_BLOCKS_CHECK;
        int corner2Y = blockPos.getY();
        int corner2Z = blockPos.getZ() + RADIUS_AROUND_BLOCKS_CHECK;
        BlockPos corner1 = new BlockPos( corner1X, corner1Y, corner1Z );
        BlockPos corner2 = new BlockPos( corner2X, corner2Y, corner2Z );

        // Get blocks within search area
        Iterable< BlockPos > blockPosListInSearchArea = BlockPos.getAllInBox( corner1, corner2 );

        // Search for fire within area
        for ( BlockPos blockPosInSearchArea : blockPosListInSearchArea ) {
            IBlockState searchBlockState = world.getBlockState( blockPosInSearchArea );
            Block searchBlock = searchBlockState.getBlock();
            if ( searchBlock == Blocks.FIRE || searchBlockState.getMaterial() == Material.FIRE ) {
                foundFire = true;
                MinecraftServer minecraftServer = FMLCommonHandler.instance().getMinecraftServerInstance();
                if ( minecraftServer != null ) {
                    minecraftServer.sendMessage( new TextComponentString( "A fire has been detected at [" +
                                                                                  blockPosInSearchArea.getX() +
                                                                                  "," +
                                                                                  blockPosInSearchArea.getY() +
                                                                                  "," +
                                                                                  blockPosInSearchArea.getZ() +
                                                                                  "]" ) );
                }
                break;
            }
        }

        // If fire found, activate linked panel
        if ( foundFire ) {
            activateLinkedPanel( world, blockPos, null );
            onFire( world, blockPos, blockState );
        }
    }

    abstract public void onFire( World world, BlockPos blockPos, IBlockState blockState );
}
