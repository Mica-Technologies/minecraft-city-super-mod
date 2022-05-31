package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.sun.speech.freetts.*;

import javax.annotation.Nullable;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityRedstoneTTS extends TileEntity
{
    private static final String TTS_STRING_KEY     = "ttsString";
    private static final String TTS_VOICE_NAME_KEY = "ttsVoice";

    private static final String TTS_VOICE_DURATION_SRETCH_KEY = "ttsVoiceDurationStretch";
    private              String ttsString                     = "Setup is Required!";
    private              String ttsVoiceName                  = "kevin16";
    private              float  ttsVoiceDurationStretch       = 1f;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );
        if ( p_readFromNBT_1_.hasKey( TTS_STRING_KEY ) ) {
            ttsString = p_readFromNBT_1_.getString( TTS_STRING_KEY );
        }
        if ( p_readFromNBT_1_.hasKey( TTS_VOICE_NAME_KEY ) ) {
            ttsVoiceName = p_readFromNBT_1_.getString( TTS_VOICE_NAME_KEY );
        }
        if ( p_readFromNBT_1_.hasKey( TTS_VOICE_DURATION_SRETCH_KEY ) ) {
            ttsVoiceDurationStretch = p_readFromNBT_1_.getFloat( TTS_VOICE_DURATION_SRETCH_KEY );
        }
    }

    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound p_writeToNBT_1_ ) {
        p_writeToNBT_1_.setString( TTS_STRING_KEY, ttsString );
        p_writeToNBT_1_.setString( TTS_VOICE_NAME_KEY, ttsVoiceName );
        p_writeToNBT_1_.setFloat( TTS_VOICE_DURATION_SRETCH_KEY, ttsVoiceDurationStretch );
        return super.writeToNBT( p_writeToNBT_1_ );
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
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        writeToNBT( nbtTagCompound );
        int metadata = getBlockMetadata();
        return new SPacketUpdateTileEntity( this.pos, metadata, nbtTagCompound );
    }

    @Override
    public void onDataPacket( NetworkManager networkManager, SPacketUpdateTileEntity pkt ) {
        readFromNBT( pkt.getNbtCompound() );
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        writeToNBT( nbtTagCompound );
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag( NBTTagCompound nbtTagCompound )
    {
        this.readFromNBT( nbtTagCompound );
    }

    public void readTtsString() {
        Thread ttsThread = new Thread( () -> {
            try {
                // Get voice
                Voice ttsVoice = VoiceManager.getInstance().getVoice( ttsVoiceName );

                // Allocate any required data for the voice
                ttsVoice.allocate();

                // This part actually reads the text
                ttsVoice.setVolume( Minecraft.getMinecraft().gameSettings.getSoundLevel( SoundCategory.BLOCKS ) );
                //ttsVoice.setDurationStretch( ttsVoiceDurationStretch );
                ttsVoice.speak( ttsString );

                // Deallocate the data
                ttsVoice.deallocate();
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        } );
        ttsThread.start();
    }

    public String getTtsString() {
        return ttsString;
    }

    public void setTtsString( String ttsString ) {
        this.ttsString = ttsString;
        System.err.println("Trig time:"+System.currentTimeMillis());
        markDirty();
        world.scheduleBlockUpdate( pos, this.getBlockType(), 0, 0 );
        world.markAndNotifyBlock( pos, null, world.getBlockState( pos ),world.getBlockState( pos ),3 );
    }

    public float getTtsVoiceDurationStretch() {
        return ttsVoiceDurationStretch;
    }

    public void setTtsVoiceDurationStretch( float ttsVoiceDurationStretch ) {
        this.ttsVoiceDurationStretch = ttsVoiceDurationStretch;
        markDirty();
        world.scheduleBlockUpdate( pos, this.getBlockType(), 0, 0 );
        world.markAndNotifyBlock( pos, null, world.getBlockState( pos ),world.getBlockState( pos ),3 );
    }
}

