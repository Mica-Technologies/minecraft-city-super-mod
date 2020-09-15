package com.micatechnologies.minecraft.csm.tiles;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.AbstractBlockFireAlarmSounder;
import com.micatechnologies.minecraft.csm.block.AbstractBlockFireAlarmSounderVoiceEvac;
import com.micatechnologies.minecraft.csm.block.BlockFireAlarmControlPanel;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.HashMap;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityFireAlarmSensor extends TileEntity
{
    private static final String   linkedPanelPosKey = "linkedPanelPos";
    private              BlockPos linkedPanelPos;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        if ( p_readFromNBT_1_.hasKey( linkedPanelPosKey ) ) {
            String[] split = p_readFromNBT_1_.getString( linkedPanelPosKey ).split( " " );
            if ( split.length != 3 ) {
                linkedPanelPos = null;
            }
            else {
                linkedPanelPos = new BlockPos( Integer.parseInt( split[ 0 ] ), Integer.parseInt( split[ 1 ] ),
                                               Integer.parseInt( split[ 2 ] ) );
            }
        }
        else {
            linkedPanelPos = null;
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
        if ( linkedPanelPos != null ) {
            p_writeToNBT_1_.setString( linkedPanelPosKey, linkedPanelPos.getX() +
                    " " +
                    linkedPanelPos.getY() +
                    " " +
                    linkedPanelPos.getZ() );
        }
        return super.writeToNBT( p_writeToNBT_1_ );
    }

    public BlockPos getLinkedPanelPos() {
        return linkedPanelPos;
    }

    public boolean setLinkedPanelPos( BlockPos blockPos ) {
        if ( linkedPanelPos == null ) {
            linkedPanelPos = blockPos;
            markDirty();
            return true;
        } else {
            return false;
        }
    }
}
