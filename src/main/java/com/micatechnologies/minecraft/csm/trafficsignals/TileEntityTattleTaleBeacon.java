package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityTattleTaleBeacon extends TileEntity
{
    private static final int    LEFT_VAL        = 0;
    private static final int    AHEAD_VAL       = 1;
    private static final int    RIGHT_VAL       = 2;
    private static final int    PA_VAL          = 3;
    private static final int    HYBRID_LEFT_VAL = 4;
    private static final String VAL_KEY         = "modeval";

    private int currVal;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );

        if ( p_readFromNBT_1_.hasKey( VAL_KEY ) ) {
            currVal = p_readFromNBT_1_.getInteger( VAL_KEY );
        }
        else {
            currVal = 0;
        }
    }

    @Override
    public boolean shouldRefresh( World p_shouldRefresh_1_,
                                  BlockPos p_shouldRefresh_2_,
                                  IBlockState p_shouldRefresh_3_,
                                  IBlockState p_shouldRefresh_4_ )
    {
        return false;
    }

    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound p_writeToNBT_1_ ) {
        p_writeToNBT_1_.setInteger( VAL_KEY, currVal );
        return super.writeToNBT( p_writeToNBT_1_ );
    }

    public AbstractBlockControllableSignal.SIGNAL_SIDE getSignalSide() {
        AbstractBlockControllableSignal.SIGNAL_SIDE side = null;
        if ( currVal == LEFT_VAL ) {
            side = AbstractBlockControllableSignal.SIGNAL_SIDE.LEFT;
        }
        else if ( currVal == AHEAD_VAL ) {
            side = AbstractBlockControllableSignal.SIGNAL_SIDE.THROUGH;
        }
        else if ( currVal == RIGHT_VAL ) {
            side = AbstractBlockControllableSignal.SIGNAL_SIDE.RIGHT;
        }
        else if ( currVal == PA_VAL ) {
            side = AbstractBlockControllableSignal.SIGNAL_SIDE.PROTECTED;
        }
        else if ( currVal == HYBRID_LEFT_VAL ) {
            side = AbstractBlockControllableSignal.SIGNAL_SIDE.FLASHING_LEFT;
        }
        return side;
    }

    public void cycleMode( EntityPlayer player ) {
        if ( currVal == LEFT_VAL ) {
            currVal = HYBRID_LEFT_VAL;
            if ( player != null && !world.isRemote ) {
                player.sendStatusMessage(
                        new TextComponentString( "Linked to HYBRID_LEFT (Flashing Yellow Left Arrow) signals" ),
                        ( true ) );
            }
        }
        else if ( currVal == HYBRID_LEFT_VAL ) {
            currVal = AHEAD_VAL;
            if ( player != null && !world.isRemote ) {
                player.sendStatusMessage( new TextComponentString( "Linked to AHEAD signals" ), ( true ) );
            }
        }
        else if ( currVal == AHEAD_VAL ) {
            currVal = RIGHT_VAL;
            if ( player != null && !world.isRemote ) {
                player.sendStatusMessage( new TextComponentString( "Linked to RIGHT signals" ), ( true ) );
            }
        }
        else if ( currVal == RIGHT_VAL ) {
            currVal = PA_VAL;
            if ( player != null && !world.isRemote ) {
                player.sendStatusMessage( new TextComponentString( "Linked to PROTECTED_AHEAD signals" ), ( true ) );
            }
        }
        else if ( currVal == PA_VAL ) {
            currVal = LEFT_VAL;
            if ( player != null && !world.isRemote ) {
                player.sendStatusMessage( new TextComponentString( "Linked to LEFT signals" ), ( true ) );
            }
        }
    }
}
