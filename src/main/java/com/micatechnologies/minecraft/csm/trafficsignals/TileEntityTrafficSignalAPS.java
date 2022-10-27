package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.Sounds;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

public class TileEntityTrafficSignalAPS extends TileEntityTrafficSignalTickableRequester
{
    /**
     * The NBT key used to store the current crosswalk sound index.
     */
    private static final String CROSSWALK_SOUND_INDEX_NBT_KEY = "CrosswalkSoundIndex";

    /**
     * The NBT key used to store the current crosswalk sound last played time.
     */
    private static final String CROSSWALK_SOUND_LAST_PLAY_TIME_NBT_KEY = "CrosswalkSoundLastPlayTime";

    /**
     * The NBT key used to store the current crosswalk last pressed time.
     */
    private static final String CROSSWALK_LAST_PRESS_TIME_NBT_KEY = "CrosswalkLastPressTime";

    /**
     * The current crosswalk sound index.
     */
    private int crosswalkSoundIndex = 0;

    /**
     * The current crosswalk sound last played time.
     */
    private long crosswalkSoundLastPlayedTime = 0;

    /**
     * The current crosswalk last pressed time.
     */
    private long crosswalkLastPressTime = 0;

    /**
     * Writes to the NBT tag compound with the tile entity's NBT data and returns the compound.
     *
     * @param compound the NBT tag compound to write the tile entity's NBT data to
     *
     * @return the NBT tag compound with the tile entity's NBT data
     */
    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {
        // Write crosswalk sound to NBT
        compound.setInteger( CROSSWALK_SOUND_INDEX_NBT_KEY, crosswalkSoundIndex );

        // Write crosswalk sound last played time to NBT
        compound.setLong( CROSSWALK_SOUND_LAST_PLAY_TIME_NBT_KEY, crosswalkSoundLastPlayedTime );

        // Write crosswalk last pressed time to NBT
        compound.setLong( CROSSWALK_LAST_PRESS_TIME_NBT_KEY, crosswalkLastPressTime );

        // Call super
        return super.writeNBT( compound );
    }

    /**
     * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
     *
     * @param compound the NBT tag compound to read the tile entity's NBT data from
     */
    @Override
    public void readNBT( NBTTagCompound compound ) {
        // Read crosswalk sound from NBT and check validity
        if ( compound.hasKey( CROSSWALK_SOUND_INDEX_NBT_KEY ) ) {
            int storedCrosswalkSoundIndex = compound.getInteger( CROSSWALK_SOUND_INDEX_NBT_KEY );
            if ( storedCrosswalkSoundIndex >= 0 && storedCrosswalkSoundIndex < CrosswalkSound.values().length ) {
                crosswalkSoundIndex = storedCrosswalkSoundIndex;
            }
            else {
                crosswalkSoundIndex = 0;
                System.err.println( "Invalid crosswalk sound index: " +
                                            crosswalkSoundIndex +
                                            " for crosswalk button tile entity " +
                                            "at [X: " +
                                            pos.getX() +
                                            ", Y: " +
                                            pos.getY() +
                                            pos.getY() +
                                            ", Z: " +
                                            pos.getZ() +
                                            "]. Reverting to default (0)." );
            }
        }

        // Read crosswalk sound last played time from NBT and check validity
        if ( compound.hasKey( CROSSWALK_SOUND_LAST_PLAY_TIME_NBT_KEY ) ) {
            long storedCrosswalkSoundLastPlayedTime = compound.getLong( CROSSWALK_SOUND_LAST_PLAY_TIME_NBT_KEY );
            if ( storedCrosswalkSoundLastPlayedTime >= 0 ) {
                crosswalkSoundLastPlayedTime = storedCrosswalkSoundLastPlayedTime;
            }
            else {
                crosswalkSoundLastPlayedTime = 0;
                System.err.println( "Invalid crosswalk sound last played time: " +
                                            crosswalkSoundLastPlayedTime +
                                            " for crosswalk button tile entity " +
                                            "at [X: " +
                                            pos.getX() +
                                            ", Y: " +
                                            pos.getY() +
                                            pos.getY() +
                                            ", Z: " +
                                            pos.getZ() +
                                            "]. Reverting to default (0)." );
            }
        }

        // Read crosswalk last pressed time from NBT and check validity
        if ( compound.hasKey( CROSSWALK_LAST_PRESS_TIME_NBT_KEY ) ) {
            long storedCrosswalkLastPressTime = compound.getLong( CROSSWALK_LAST_PRESS_TIME_NBT_KEY );
            if ( storedCrosswalkLastPressTime >= 0 ) {
                crosswalkLastPressTime = storedCrosswalkLastPressTime;
            }
            else {
                crosswalkLastPressTime = 0;
                System.err.println( "Invalid crosswalk last pressed time: " +
                                            crosswalkLastPressTime +
                                            " for crosswalk button tile entity " +
                                            "at [X: " +
                                            pos.getX() +
                                            ", Y: " +
                                            pos.getY() +
                                            pos.getY() +
                                            ", Z: " +
                                            pos.getZ() +
                                            "]. Reverting to default (0)." );
            }
        }

        // Call super
        super.readNBT( compound );
    }

    /**
     * Gets the current crosswalk sound.
     *
     * @return The current crosswalk sound.
     */
    public CrosswalkSound getCrosswalkSound() {
        return CrosswalkSound.values()[ crosswalkSoundIndex ];
    }

    /**
     * Switches the crosswalk sound to the next sound in the list and returns the new sound name.
     *
     * @return The name of the new crosswalk sound.
     */
    public String switchSound() {
        // Increment crosswalk sound index and check validity
        crosswalkSoundIndex++;
        if ( crosswalkSoundIndex >= CrosswalkSound.values().length ) {
            crosswalkSoundIndex = 0;
        }

        // Mark the tile entity as dirty
        markDirtySync( world, pos );

        // Return the new crosswalk sound name
        return getCrosswalkSound().name;
    }

    /**
     * Returns the tick rate of the tile entity.
     *
     * @return the tick rate of the tile entity
     */
    @Override
    public long getTickRate() {
        return getCrosswalkSound().lenOfLocateSound;
    }

    /**
     * Handles the tick event of the crosswalk button.
     */
    @Override
    public void onTick() {
        // Get block color value
        int blockColor = world.getBlockState( pos )
                              .getValue( BlockControllableCrosswalkButtonAudible.BlockCustom.COLOR );

        // Handle for each color state
        if ( blockColor == BlockControllableCrosswalkButtonAudible.BlockCustom.SIGNAL_OFF ) {
            // Do nothing if no power/turned off
        }
        else if ( blockColor == BlockControllableCrosswalkButtonAudible.BlockCustom.SIGNAL_GREEN ) {
            // Play walk sound if it's time (not still playing)
            boolean isWalkSoundAlreadyPlaying = ( crosswalkSoundLastPlayedTime + getCrosswalkSound().lenOfWalkSound ) >
                    world.getTotalWorldTime();
            boolean isPressSoundAlreadyPlaying = ( crosswalkLastPressTime + getCrosswalkSound().lenOfPressSound ) >
                    world.getTotalWorldTime();
            if ( !isWalkSoundAlreadyPlaying && !isPressSoundAlreadyPlaying ) {
                world.playSound( null, pos.getX(), pos.getY(), pos.getZ(),
                                 getCrosswalkSound().walkSound.getSoundEvent(), SoundCategory.NEUTRAL,
                                 getCrosswalkSound().volume, getCrosswalkSound().pitch );
                crosswalkSoundLastPlayedTime = world.getTotalWorldTime();
            }
        }
        else if ( blockColor == BlockControllableCrosswalkButtonAudible.BlockCustom.SIGNAL_YELLOW ) {
            // Play locate sound (future: countdown when controller updated)
            world.playSound( null, pos.getX(), pos.getY(), pos.getZ(), getCrosswalkSound().locateSound.getSoundEvent(),
                             SoundCategory.NEUTRAL, getCrosswalkSound().volume, getCrosswalkSound().pitch );
        }
        else if ( blockColor == BlockControllableCrosswalkButtonAudible.BlockCustom.SIGNAL_RED ) {
            // Play locate sound
            world.playSound( null, pos.getX(), pos.getY(), pos.getZ(), getCrosswalkSound().locateSound.getSoundEvent(),
                             SoundCategory.NEUTRAL, getCrosswalkSound().volume, getCrosswalkSound().pitch );
        }
        else {
            System.err.println( "Invalid block color value: " +
                                        blockColor +
                                        " for crosswalk button tile entity at [X: " +
                                        pos.getX() +
                                        ", Y: " +
                                        pos.getY() +
                                        pos.getY() +
                                        ", Z: " +
                                        pos.getZ() +
                                        "]" );
        }
    }

    /**
     * Handles the press event of the crosswalk button.
     */
    public void onPress() {
        // Play press sound if it's time (not still playing)
        boolean isPressSoundAlreadyPlaying = ( crosswalkLastPressTime + getCrosswalkSound().lenOfPressSound ) >
                world.getTotalWorldTime();
        if ( !isPressSoundAlreadyPlaying ) {
            world.playSound( null, pos.getX(), pos.getY(), pos.getZ(), getCrosswalkSound().pressSound.getSoundEvent(),
                             SoundCategory.NEUTRAL, getCrosswalkSound().volume, getCrosswalkSound().pitch );
            crosswalkLastPressTime = world.getTotalWorldTime();
        }
    }

    /**
     * Returns a boolean indicating if the tile entity should also tick on the client side. By default, the tile entity
     * will always tick on the server side, and in the event of singleplayer/local mode, the host client is considered
     * the server. This implementation always returns true to ensure that sound is played on the client side.
     *
     * @return a boolean indicating if the tile entity should also tick on the client side. This implementation always
     *         returns true to ensure that sound is played on the client side.
     */
    @Override
    public boolean doClientTick() {
        return true;
    }

    /**
     * Returns a boolean indicating if the tile entity ticking should be paused. If the tile entity is paused, the tick
     * event will not be called. This implementation always returns false as the tile entity should always tick.
     *
     * @return a boolean indicating if the tile entity ticking should be paused. This implementation always returns
     *         false as the tile entity should always tick.
     */
    @Override
    public boolean pauseTicking() {
        return false;
    }

    public enum CrosswalkSound
    {
        MALE_CAMPBELL( "Campbell/PedSafety (Male)", Sounds.SOUND.MALE_BEEP, Sounds.SOUND.MALE_WAIT, 30,
                       Sounds.SOUND.MALE_WAIT, 15, Sounds.SOUND.MALE_CROSSWALK_ON, 140 ),
        FEMALE_CAMPBELL( "Campbell/PedSafety (Female)", Sounds.SOUND.FEMALE_BEEP, Sounds.SOUND.FEMALE_WAIT, 30,
                         Sounds.SOUND.FEMALE_WAIT, 15, Sounds.SOUND.MALE_CROSSWALK_ON, 140 ),
        MALE_CAMPBELL_AUTOMATED( "Campbell/PedSafety (Automated/Male)", Sounds.SOUND.MALE_BEEP, Sounds.SOUND.MALE_WAIT,
                                 30, Sounds.SOUND.FEMALE_AUTOMATED, 80, Sounds.SOUND.MALE_CROSSWALK_ON, 140 ),
        FEMALE_CAMPBELL_AUTOMATED( "Campbell/PedSafety (Automated/Female)", Sounds.SOUND.FEMALE_BEEP,
                                   Sounds.SOUND.FEMALE_WAIT, 30, Sounds.SOUND.FEMALE_AUTOMATED, 80,
                                   Sounds.SOUND.MALE_CROSSWALK_ON, 140 ),
        MALE_CAMPBELL_WITH_TICK( "Campbell/PedSafety (Male with Audible Tick)", Sounds.SOUND.MALE_BEEP,
                                 Sounds.SOUND.MALE_WAIT, 30, Sounds.SOUND.MALE_WAIT, 15,
                                 Sounds.SOUND._8TICK_PER_SECOND_CROSSWALK, 45 ),
        FEMALE_CAMPBELL_WITH_TICK( "Campbell/PedSafety (Female with Audible Tick)", Sounds.SOUND.FEMALE_BEEP,
                                   Sounds.SOUND.FEMALE_WAIT, 30, Sounds.SOUND.FEMALE_WAIT, 15,
                                   Sounds.SOUND._8TICK_PER_SECOND_CROSSWALK, 45 ),
        MALE_CAMPBELL_AUTOMATED_WITH_TICK( "Campbell/PedSafety (Automated/Male with Audible Tick)",
                                           Sounds.SOUND.MALE_BEEP, Sounds.SOUND.MALE_WAIT, 30,
                                           Sounds.SOUND.FEMALE_AUTOMATED, 80, Sounds.SOUND._8TICK_PER_SECOND_CROSSWALK,
                                           45 ),
        FEMALE_CAMPBELL_AUTOMATED_WITH_TICK( "Campbell/PedSafety (Automated/Female with Audible Tick)",
                                             Sounds.SOUND.FEMALE_BEEP, Sounds.SOUND.FEMALE_WAIT, 30,
                                             Sounds.SOUND.FEMALE_AUTOMATED, 80,
                                             Sounds.SOUND._8TICK_PER_SECOND_CROSSWALK, 45 );

        final String       name;
        final Sounds.SOUND locateSound;

        final int          lenOfLocateSound = 20;
        final Sounds.SOUND waitSound;
        final int          lenOfWaitSound;
        final Sounds.SOUND pressSound;
        final int          lenOfPressSound;
        final Sounds.SOUND walkSound;
        final int          lenOfWalkSound;

        final float volume = 1;
        final float pitch  = 1;

        CrosswalkSound( String name,
                        Sounds.SOUND locateSound,
                        Sounds.SOUND waitSound,
                        int lenOfWaitSound,
                        Sounds.SOUND pressSound,
                        int lenOfPressSound,
                        Sounds.SOUND walkSound,
                        int lenOfWalkSound )
        {
            this.name = name;
            this.locateSound = locateSound;
            this.waitSound = waitSound;
            this.lenOfWaitSound = lenOfWaitSound;
            this.pressSound = pressSound;
            this.lenOfPressSound = lenOfPressSound;
            this.walkSound = walkSound;
            this.lenOfWalkSound = lenOfWalkSound;
        }
    }
}
