package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.tabs.CsmTabTechnology;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import com.sun.speech.freetts.*;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityRedstoneTTS extends AbstractTileEntity
{
    private static final String TTS_STRING_KEY     = "ttsString";
    private static final String TTS_VOICE_NAME_KEY = "ttsVoice";

    private static final String TTS_VOICE_DURATION_SRETCH_KEY = "ttsVoiceDurationStretch";
    private              String ttsString                     = "Setup is Required!";
    private              String ttsVoiceName                  = "kevin16";
    private              float  ttsVoiceDurationStretch       = 1f;

    /**
     * Returns the NBT tag compound with the tile entity's NBT data.
     *
     * @param compound the NBT tag compound to write the tile entity's NBT data to
     *
     * @return the NBT tag compound with the tile entity's NBT data
     */
    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {
        compound.setString( TTS_STRING_KEY, ttsString );
        compound.setString( TTS_VOICE_NAME_KEY, ttsVoiceName );
        compound.setFloat( TTS_VOICE_DURATION_SRETCH_KEY, ttsVoiceDurationStretch );
        return compound;
    }

    /**
     * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
     *
     * @param compound the NBT tag compound to read the tile entity's NBT data from
     */
    @Override
    public void readNBT( NBTTagCompound compound ) {
        if ( compound.hasKey( TTS_STRING_KEY ) ) {
            ttsString = compound.getString( TTS_STRING_KEY );
        }
        if ( compound.hasKey( TTS_VOICE_NAME_KEY ) ) {
            ttsVoiceName = compound.getString( TTS_VOICE_NAME_KEY );
        }
        if ( compound.hasKey( TTS_VOICE_DURATION_SRETCH_KEY ) ) {
            ttsVoiceDurationStretch = compound.getFloat( TTS_VOICE_DURATION_SRETCH_KEY );
        }
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
                ttsVoice.setDurationStretch( ttsVoiceDurationStretch );
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
        System.err.println( "Trig time:" + System.currentTimeMillis() );
        markDirtySync( getWorld(), getPos(),true );
        System.out.println( "setTtsString (remote: " +
                                    getWorld().isRemote +
                                    ") at pos " +
                                    getPos() +
                                    " with string: " +
                                    ttsString );
    }

    public float getTtsVoiceDurationStretch() {
        return ttsVoiceDurationStretch;
    }

    public void setTtsVoiceDurationStretch( float ttsVoiceDurationStretch ) {
        this.ttsVoiceDurationStretch = ttsVoiceDurationStretch;
        markDirtySync( getWorld(), getPos(),true );
    }
}

