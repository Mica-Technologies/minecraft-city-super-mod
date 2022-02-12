package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
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
public class TileEntityFireAlarmControlPanel extends TileEntity
{
    private static final String   soundIndexKey          = "soundIndex";
    private static final String   alarmKey               = "alarm";
    private static final String   alarmStormKey          = "alarmStorm";
    private static final String   connectedAppliancesKey = "connectedAppliances";
    private static final String[] SOUND_RESOURCE_NAMES   = { "csm:svenew",
                                                             "csm:sveold",
                                                             "csm:simplex_voice_evac_old_alt",
                                                             "csm:mills_firealarm",
                                                             "csm:lms_voice_evac",
                                                             "csm:notifier_voice_evac",
                                                             "csm:notifier_voice_evac_alt",
                                                             "csm:notifier_voice_evac_alt2",
                                                             "csm:awful_notifier_ve",
                                                             "csm:mclalsve" };
    private static final int[]    SOUND_LENGTHS          = { 2100, 560, 560, 755, 700, 520, 520, 440, 460, 600 };
    private static final String[] SOUND_NAMES            = { "Simplex Voice Evac 1",
                                                             "Simplex Voice Evac 2",
                                                             "Simplex Voice Evac 3",
                                                             "Notifier Voice Evac 1",
                                                             "Notifier Voice Evac 2",
                                                             "Notifier Voice Evac 3",
                                                             "Notifier Voice Evac 4",
                                                             "Notifier Voice Evac 5",
                                                             "Notifier Voice Evac 6",
                                                             "Mica Voice Evac 1" };
    private static final String   STORM_SOUND_NAME       = "csm:notifier_tornado_voice_evac";
    private static final int      STORM_SOUND_LENGTH     = 460;

    private int                        soundIndex;
    private boolean                    alarm;
    private boolean                    alarmStorm;
    private int                        alarmStormSoundTracking = 0;
    private HashMap< String, Integer > alarmSoundTracking      = null;
    private ArrayList< BlockPos >      connectedAppliances     = new ArrayList<>();

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        // Read sound index
        try {
            soundIndex = p_readFromNBT_1_.getInteger( soundIndexKey );
        }
        catch ( Exception e ) {
            soundIndex = 0;
        }

        // Read alarm state
        try {
            alarm = p_readFromNBT_1_.getBoolean( alarmKey );
        }
        catch ( Exception e ) {
            alarm = false;
        }

        // Read alarm storm state
        try {
            alarmStorm = p_readFromNBT_1_.getBoolean( alarmStormKey );
        }
        catch ( Exception e ) {
            alarmStorm = false;
        }

        // Read connected appliance locations
        connectedAppliances.clear();
        if ( p_readFromNBT_1_.hasKey( connectedAppliancesKey ) ) {
            // Split into each block position
            String[] positions = p_readFromNBT_1_.getString( connectedAppliancesKey ).split( "\n" );
            for ( String position : positions ) {
                String[] coordinates = position.split( " " );
                if ( coordinates.length == 3 ) {
                    connectedAppliances.add(
                            new BlockPos( Integer.parseInt( coordinates[ 0 ] ), Integer.parseInt( coordinates[ 1 ] ),
                                          Integer.parseInt( coordinates[ 2 ] ) ) );
                }
            }
        }

        super.readFromNBT( p_readFromNBT_1_ );
    }

    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound p_writeToNBT_1_ ) {
        // Write sound index
        p_writeToNBT_1_.setInteger( soundIndexKey, soundIndex );

        // Write alarm state
        p_writeToNBT_1_.setBoolean( alarmKey, alarm );

        // Write alarm storm state
        p_writeToNBT_1_.setBoolean( alarmStormKey, alarmStorm );

        // Write connected appliance locations
        StringBuilder connectedAppliancesString = new StringBuilder();
        for ( BlockPos bp : connectedAppliances ) {
            connectedAppliancesString.append( bp.getX() )
                                     .append( " " )
                                     .append( bp.getY() )
                                     .append( " " )
                                     .append( bp.getZ() )
                                     .append( "\n" );
        }
        p_writeToNBT_1_.setString( connectedAppliancesKey, connectedAppliancesString.toString() );
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

    public void switchSound() {
        soundIndex++;
        if ( soundIndex >= SOUND_RESOURCE_NAMES.length ) {
            soundIndex = 0;
        }
        markDirty();
    }

    public synchronized boolean addLinkedAlarm( BlockPos blockPos ) {
        if ( !connectedAppliances.contains( blockPos ) ) {
            connectedAppliances.add( blockPos );
            markDirty();
            return true;
        }
        return false;
    }

    public boolean getAlarmState() {
        return alarm;
    }

    public void setAlarmState( boolean alarmState ) {
        alarm = alarmState;
        markDirty();
    }

    public void setAlarmStormState( boolean alarmStormState ) {
        alarmStorm = alarmStormState;
        markDirty();
    }

    public String getCurrentSoundName() {
        return SOUND_NAMES[ soundIndex ];
    }

    public synchronized void updateTick( World world,
                                         BlockPos p_updateTick_2_,
                                         BlockFireAlarmControlPanel.BlockCustom fireAlarmControlPanel )
    {
        if ( alarm ) {
            // Reset storm alarm (fire alarm overrrides storm)
            if ( alarmStormSoundTracking > 0 ) {
                alarmStormSoundTracking = 0;
            }

            // Alarm is starting
            if ( alarmSoundTracking == null ) {
                alarmSoundTracking = new HashMap<>();
                MinecraftServer mcserv = FMLCommonHandler.instance().getMinecraftServerInstance();
                if ( mcserv != null ) {
                    mcserv.getPlayerList()
                          .sendMessage( new TextComponentString( "The fire alarm at [" +
                                                                         p_updateTick_2_.getX() +
                                                                         "," +
                                                                         p_updateTick_2_.getY() +
                                                                         "," +
                                                                         p_updateTick_2_.getZ() +
                                                                         "] " +
                                                                         "has been activated!" ) );
                }
            }

            // Perform sound handling
            for ( BlockPos bp : connectedAppliances ) {
                // Get block at linked position
                IBlockState blockStateAtPos = world.getBlockState( bp );
                Block blockAtPos = blockStateAtPos.getBlock();

                // Check for alarm sound values at location
                String alarmSoundName = null;
                int alarmSoundLength = -1;
                if ( blockAtPos instanceof AbstractBlockFireAlarmSounderVoiceEvac ) {
                    alarmSoundName = getCurrentSoundResourceName();
                    alarmSoundLength = getCurrentSoundLength();
                }
                else if ( blockAtPos instanceof AbstractBlockFireAlarmSounder ) {
                    AbstractBlockFireAlarmSounder blockFireAlarmSounder = ( AbstractBlockFireAlarmSounder ) blockAtPos;
                    alarmSoundName = blockFireAlarmSounder.getSoundResourceName( blockStateAtPos );
                    alarmSoundLength = blockFireAlarmSounder.getSoundTickLen( blockStateAtPos );
                }

                // Handle only if alarm at location
                if ( alarmSoundName != null && alarmSoundLength != -1 ) {
                    // Add sound tracker if does not exist and reset sound tracker to 0 if sound length reached (or
                    // greater)
                    if ( !alarmSoundTracking.containsKey( alarmSoundName ) ||
                            alarmSoundTracking.get( alarmSoundName ) > alarmSoundLength ) {
                        alarmSoundTracking.put( alarmSoundName, 0 );
                    }

                    // Play sound
                    if ( alarmSoundTracking.get( alarmSoundName ) == 0 ) {
                        world.playSound( ( EntityPlayer ) null, bp.getX(), bp.getY(), bp.getZ(),
                                         ( net.minecraft.util.SoundEvent ) net.minecraft.util.SoundEvent.REGISTRY.getObject(
                                                 new ResourceLocation( alarmSoundName ) ), SoundCategory.AMBIENT,
                                         ( float ) 2, ( float ) 1 );

                    }
                }
            }

            // Increment sound trackers
            final int incrementSize = fireAlarmControlPanel.tickRate( world );
            for ( String key : alarmSoundTracking.keySet() ) {
                int valForKey = alarmSoundTracking.get( key );
                alarmSoundTracking.put( key, valForKey + incrementSize );
            }
        }
        else {
            // Alarm has ended
            if ( alarmSoundTracking != null ) {
                alarmSoundTracking = null;
                MinecraftServer mcserv = FMLCommonHandler.instance().getMinecraftServerInstance();
                if ( mcserv != null ) {
                    mcserv.getPlayerList()
                          .sendMessage( new TextComponentString( "The fire alarm at [" +
                                                                         p_updateTick_2_.getX() +
                                                                         "," +
                                                                         p_updateTick_2_.getY() +
                                                                         "," +
                                                                         p_updateTick_2_.getZ() +
                                                                         "] " +
                                                                         "has been reset." ) );
                }
            }

            // Handle storm alarm
            if ( alarmStorm ) {
                // Perform sound handling
                for ( BlockPos bp : connectedAppliances ) {
                    // Get block at linked position
                    IBlockState blockStateAtPos = world.getBlockState( bp );
                    Block blockAtPos = blockStateAtPos.getBlock();

                    // Check for alarm sound values at location
                    if ( blockAtPos instanceof AbstractBlockFireAlarmSounderVoiceEvac ) {
                        if ( alarmStormSoundTracking > STORM_SOUND_LENGTH ) {
                            alarmStormSoundTracking = 0;
                        }

                        // Play sound
                        if ( alarmStormSoundTracking == 0 ) {
                            world.playSound( ( EntityPlayer ) null, bp.getX(), bp.getY(), bp.getZ(),
                                             ( net.minecraft.util.SoundEvent ) net.minecraft.util.SoundEvent.REGISTRY.getObject(
                                                     new ResourceLocation( STORM_SOUND_NAME ) ), SoundCategory.AMBIENT,
                                             ( float ) 2, ( float ) 1 );

                        }
                    }
                }

                // Increment storm sound tracker
                final int incrementSize = fireAlarmControlPanel.tickRate( world );
                alarmStormSoundTracking += incrementSize;
            }
            else {
                // Reset storm alarm
                if ( alarmStormSoundTracking > 0 ) {
                    alarmStormSoundTracking = 0;
                }
            }
        }
    }

    public int getCurrentSoundLength() {
        return SOUND_LENGTHS[ soundIndex ];
    }

    public String getCurrentSoundResourceName() {
        return SOUND_RESOURCE_NAMES[ soundIndex ];
    }

}
