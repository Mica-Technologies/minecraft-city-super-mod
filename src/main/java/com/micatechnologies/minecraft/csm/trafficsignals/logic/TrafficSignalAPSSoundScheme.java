package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.CsmSounds;

/**
 * The {@link TrafficSignalAPSSoundScheme} enum defines the various sound schemes which can be used by traffic signal
 * APS devices using the {@link AbstractBlockTrafficSignalAPS} block interface.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see AbstractBlockTrafficSignalAPS
 * @since 2023.2.0
 */
public enum TrafficSignalAPSSoundScheme
{

    /**
     * Enum value for the Campbell/PedSafety (Male) sound scheme.
     *
     * @since 1.0
     */
    MALE_CAMPBELL( "Campbell/PedSafety (Male)", CsmSounds.SOUND.MALE_BEEP, CsmSounds.SOUND.MALE_WAIT, 30,
                   CsmSounds.SOUND.MALE_WAIT, 15, CsmSounds.SOUND.MALE_CROSSWALK_ON, 140 ),
    /**
     * Enum value for the Campbell/PedSafety (Female) sound scheme.
     *
     * @since 1.0
     */
    FEMALE_CAMPBELL( "Campbell/PedSafety (Female)", CsmSounds.SOUND.FEMALE_BEEP, CsmSounds.SOUND.FEMALE_WAIT, 30,
                     CsmSounds.SOUND.FEMALE_WAIT, 15, CsmSounds.SOUND.MALE_CROSSWALK_ON, 140 ),
    /**
     * Enum value for the Campbell/PedSafety (Automated/Male) sound scheme.
     *
     * @since 1.0
     */
    MALE_CAMPBELL_AUTOMATED( "Campbell/PedSafety (Automated/Male)", CsmSounds.SOUND.MALE_BEEP, CsmSounds.SOUND.MALE_WAIT, 30,
                             CsmSounds.SOUND.FEMALE_AUTOMATED, 80, CsmSounds.SOUND.MALE_CROSSWALK_ON, 140 ),
    /**
     * Enum value for the Campbell/PedSafety (Automated/Female) sound scheme.
     *
     * @since 1.0
     */
    FEMALE_CAMPBELL_AUTOMATED( "Campbell/PedSafety (Automated/Female)", CsmSounds.SOUND.FEMALE_BEEP,
                               CsmSounds.SOUND.FEMALE_WAIT, 30, CsmSounds.SOUND.FEMALE_AUTOMATED, 80,
                               CsmSounds.SOUND.MALE_CROSSWALK_ON, 140 ),
    /**
     * Enum value for the Campbell/PedSafety (Male with Audible Tick) sound scheme.
     *
     * @since 1.0
     */
    MALE_CAMPBELL_WITH_TICK( "Campbell/PedSafety (Male with Audible Tick)", CsmSounds.SOUND.MALE_BEEP,
                             CsmSounds.SOUND.MALE_WAIT, 30, CsmSounds.SOUND.MALE_WAIT, 15,
                             CsmSounds.SOUND._8TICK_PER_SECOND_CROSSWALK, 45 ),
    /**
     * Enum value for the Campbell/PedSafety (Female with Audible Tick) sound scheme.
     *
     * @since 1.0
     */
    FEMALE_CAMPBELL_WITH_TICK( "Campbell/PedSafety (Female with Audible Tick)", CsmSounds.SOUND.FEMALE_BEEP,
                               CsmSounds.SOUND.FEMALE_WAIT, 30, CsmSounds.SOUND.FEMALE_WAIT, 15,
                               CsmSounds.SOUND._8TICK_PER_SECOND_CROSSWALK, 45 ),
    /**
     * Enum value for the Campbell/PedSafety (Automated/Male with Audible Tick) sound scheme.
     *
     * @since 1.0
     */
    MALE_CAMPBELL_AUTOMATED_WITH_TICK( "Campbell/PedSafety (Automated/Male with Audible Tick)", CsmSounds.SOUND.MALE_BEEP,
                                       CsmSounds.SOUND.MALE_WAIT, 30, CsmSounds.SOUND.FEMALE_AUTOMATED, 80,
                                       CsmSounds.SOUND._8TICK_PER_SECOND_CROSSWALK, 45 ),
    /**
     * Enum value for the Campbell/PedSafety (Automated/Female with Audible Tick) sound scheme.
     *
     * @since 1.0
     */
    FEMALE_CAMPBELL_AUTOMATED_WITH_TICK( "Campbell/PedSafety (Automated/Female with Audible Tick)",
                                         CsmSounds.SOUND.FEMALE_BEEP, CsmSounds.SOUND.FEMALE_WAIT, 30,
                                         CsmSounds.SOUND.FEMALE_AUTOMATED, 80, CsmSounds.SOUND._8TICK_PER_SECOND_CROSSWALK,
                                         45 );

    /**
     * The name for the APS sound scheme.
     *
     * @since 1.0
     */
    final String name;

    /**
     * The locator sound for the APS sound scheme.
     *
     * @since 1.0
     */
    final CsmSounds.SOUND locateSound;

    /**
     * The length of the locator sound for the APS sound scheme.
     *
     * @since 1.0
     */
    int lenOfLocateSound = 20;

    /**
     * The wait sound for the APS sound scheme.
     *
     * @since 1.0
     */
    final CsmSounds.SOUND waitSound;

    /**
     * The length of the wait sound for the APS sound scheme.
     *
     * @since 1.0
     */
    final int lenOfWaitSound;

    /**
     * The press sound for the APS sound scheme.
     *
     * @since 1.0
     */
    final CsmSounds.SOUND pressSound;

    /**
     * The length of the press sound for the APS sound scheme.
     *
     * @since 1.0
     */
    final int lenOfPressSound;

    /**
     * The walk sound for the APS sound scheme.
     *
     * @since 1.0
     */
    final CsmSounds.SOUND walkSound;

    /**
     * The length of the walk sound for the APS sound scheme.
     *
     * @since 1.0
     */
    final int lenOfWalkSound;

    /**
     * The volume for the APS sound scheme.
     *
     * @since 1.0
     */
    final float volume = 1;

    /**
     * The pitch for the APS sound scheme.
     *
     * @since 1.0
     */
    final float pitch = 1;

    /**
     * The constructor for the {@link TrafficSignalAPSSoundScheme} enum with the default locator sound length.
     *
     * @param name            The name for the APS sound scheme.
     * @param locateSound     The locator sound for the APS sound scheme.
     * @param waitSound       The wait sound for the APS sound scheme.
     * @param lenOfWaitSound  The length of the wait sound for the APS sound scheme.
     * @param pressSound      The press sound for the APS sound scheme.
     * @param lenOfPressSound The length of the press sound for the APS sound scheme.
     * @param walkSound       The walk sound for the APS sound scheme.
     * @param lenOfWalkSound  The length of the walk sound for the APS sound scheme.
     *
     * @since 1.0
     */
    TrafficSignalAPSSoundScheme( String name,
                                 CsmSounds.SOUND locateSound,
                                 CsmSounds.SOUND waitSound,
                                 int lenOfWaitSound,
                                 CsmSounds.SOUND pressSound,
                                 int lenOfPressSound,
                                 CsmSounds.SOUND walkSound,
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

    /**
     * The constructor for the {@link TrafficSignalAPSSoundScheme} enum with a specified locator sound length.
     *
     * @param name             The name for the APS sound scheme.
     * @param locateSound      The locator sound for the APS sound scheme.
     * @param lenOfLocateSound The length of the locator sound for the APS sound scheme.
     * @param waitSound        The wait sound for the APS sound scheme.
     * @param lenOfWaitSound   The length of the wait sound for the APS sound scheme.
     * @param pressSound       The press sound for the APS sound scheme.
     * @param lenOfPressSound  The length of the press sound for the APS sound scheme.
     * @param walkSound        The walk sound for the APS sound scheme.
     * @param lenOfWalkSound   The length of the walk sound for the APS sound scheme.
     *
     * @since 1.0
     */
    TrafficSignalAPSSoundScheme( String name,
                                 CsmSounds.SOUND locateSound,
                                 int lenOfLocateSound,
                                 CsmSounds.SOUND waitSound,
                                 int lenOfWaitSound,
                                 CsmSounds.SOUND pressSound,
                                 int lenOfPressSound,
                                 CsmSounds.SOUND walkSound,
                                 int lenOfWalkSound )
    {
        this.name = name;
        this.locateSound = locateSound;
        this.lenOfLocateSound = lenOfLocateSound;
        this.waitSound = waitSound;
        this.lenOfWaitSound = lenOfWaitSound;
        this.pressSound = pressSound;
        this.lenOfPressSound = lenOfPressSound;
        this.walkSound = walkSound;
        this.lenOfWalkSound = lenOfWalkSound;
    }

    /**
     * Gets the name for the APS sound scheme.
     *
     * @return The name for the APS sound scheme.
     *
     * @since 1.0
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the locator sound for the APS sound scheme.
     *
     * @return The locator sound for the APS sound scheme.
     *
     * @since 1.0
     */
    public CsmSounds.SOUND getLocateSound() {
        return locateSound;
    }

    /**
     * Gets the length of the locator sound for the APS sound scheme.
     *
     * @return The length of the locator sound for the APS sound scheme.
     *
     * @since 1.0
     */
    public int getLenOfLocateSound() {
        return lenOfLocateSound;
    }

    /**
     * Gets the wait sound for the APS sound scheme.
     *
     * @return The wait sound for the APS sound scheme.
     *
     * @since 1.0
     */
    public CsmSounds.SOUND getWaitSound() {
        return waitSound;
    }

    /**
     * Gets the length of the wait sound for the APS sound scheme.
     *
     * @return The length of the wait sound for the APS sound scheme.
     *
     * @since 1.0
     */
    public int getLenOfWaitSound() {
        return lenOfWaitSound;
    }

    /**
     * Gets the press sound for the APS sound scheme.
     *
     * @return The press sound for the APS sound scheme.
     *
     * @since 1.0
     */
    public CsmSounds.SOUND getPressSound() {
        return pressSound;
    }

    /**
     * Gets the length of the press sound for the APS sound scheme.
     *
     * @return The length of the press sound for the APS sound scheme.
     *
     * @since 1.0
     */
    public int getLenOfPressSound() {
        return lenOfPressSound;
    }

    /**
     * Gets the walk sound for the APS sound scheme.
     *
     * @return The walk sound for the APS sound scheme.
     *
     * @since 1.0
     */
    public CsmSounds.SOUND getWalkSound() {
        return walkSound;
    }

    /**
     * Gets the length of the walk sound for the APS sound scheme.
     *
     * @return The length of the walk sound for the APS sound scheme.
     *
     * @since 1.0
     */
    public int getLenOfWalkSound() {
        return lenOfWalkSound;
    }

    /**
     * Gets the volume for the APS sound scheme.
     *
     * @return The volume for the APS sound scheme.
     *
     * @since 1.0
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Gets the pitch for the APS sound scheme.
     *
     * @return The pitch for the APS sound scheme.
     *
     * @since 1.0
     */
    public float getPitch() {
        return pitch;
    }
}
