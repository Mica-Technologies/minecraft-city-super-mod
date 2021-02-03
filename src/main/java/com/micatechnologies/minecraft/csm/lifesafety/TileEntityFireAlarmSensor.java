package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityFireAlarmSensor extends TileEntity
{
    private static final String linkedPanelPosXKey = "lpX";
    private static final String linkedPanelPosYKey = "lpY";
    private static final String linkedPanelPosZKey = "lpZ";
    private              int    linkedPanelX;
    private              int    linkedPanelY       = -500;
    private              int    linkedPanelZ;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        if ( p_readFromNBT_1_.hasKey( linkedPanelPosXKey ) &&
                p_readFromNBT_1_.hasKey( linkedPanelPosYKey ) &&
                p_readFromNBT_1_.hasKey( linkedPanelPosZKey ) ) {
            linkedPanelX = p_readFromNBT_1_.getInteger( linkedPanelPosXKey );
            linkedPanelY = p_readFromNBT_1_.getInteger( linkedPanelPosYKey );
            linkedPanelZ = p_readFromNBT_1_.getInteger( linkedPanelPosZKey );
        }
        else {
            linkedPanelY = -500;
        }
        super.readFromNBT( p_readFromNBT_1_ );
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
        p_writeToNBT_1_.setInteger( linkedPanelPosXKey, linkedPanelX );
        p_writeToNBT_1_.setInteger( linkedPanelPosYKey, linkedPanelY );
        p_writeToNBT_1_.setInteger( linkedPanelPosZKey, linkedPanelZ );

        return super.writeToNBT( p_writeToNBT_1_ );
    }

    public BlockPos getLinkedPanelPos( World world ) {
        return new BlockPos( linkedPanelX, linkedPanelY, linkedPanelZ );
    }

    public boolean setLinkedPanelPos( BlockPos blockPos, EntityPlayer player ) {
        if ( linkedPanelY == -500 ) {
            linkedPanelX = blockPos.getX();
            linkedPanelY = blockPos.getY();
            linkedPanelZ = blockPos.getZ();
            markDirty();
            return true;
        }
        else {
            return false;
        }
    }
}
