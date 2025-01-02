package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.CsmSounds;

/**
 * The
 *
 * @author Mica Technologies
 * @version 1.0
 * @see AbstractBlockTrafficSignalAPS
 * @since 2023.2.0
 */
public class TrafficSignalAPSSoundScheme {

  /**
   * The name for the APS sound scheme.
   *
   * @since 1.0
   */
  private final String name;

  /**
   * The locator sound for the APS sound scheme.
   *
   * @since 1.0
   */
  private final CsmSounds.SOUND locateSound;

  /**
   * The wait sound for the APS sound scheme.
   *
   * @since 1.0
   */
  private final CsmSounds.SOUND waitSound;

  /**
   * The length of the wait sound for the APS sound scheme.
   *
   * @since 1.0
   */
  private final int lenOfWaitSound;

  /**
   * The press sound for the APS sound scheme.
   *
   * @since 1.0
   */
  private final CsmSounds.SOUND pressSound;

  /**
   * The length of the press sound for the APS sound scheme.
   *
   * @since 1.0
   */
  private final int lenOfPressSound;

  /**
   * The walk sound for the APS sound scheme.
   *
   * @since 1.0
   */
  private final CsmSounds.SOUND walkSound;

  /**
   * The length of the walk sound for the APS sound scheme.
   *
   * @since 1.0
   */
  private final int lenOfWalkSound;

  /**
   * The volume for the APS sound scheme.
   *
   * @since 1.0
   */
  private final float volume;

  /**
   * The pitch for the APS sound scheme.
   *
   * @since 1.0
   */
  private final float pitch;

  /**
   * The length of the locator sound for the APS sound scheme.
   *
   * @since 1.0
   */
  private final int lenOfLocateSound;

  /**
   * The constructor for an {@link TrafficSignalAPSSoundScheme} instance with the default locator
   * sound length.
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
  TrafficSignalAPSSoundScheme(String name, CsmSounds.SOUND locateSound,
      CsmSounds.SOUND waitSound, int lenOfWaitSound, CsmSounds.SOUND pressSound,
      int lenOfPressSound, CsmSounds.SOUND walkSound, int lenOfWalkSound) {
    this.name = name;
    this.locateSound = locateSound;
    this.lenOfLocateSound = 20;
    this.waitSound = waitSound;
    this.lenOfWaitSound = lenOfWaitSound;
    this.pressSound = pressSound;
    this.lenOfPressSound = lenOfPressSound;
    this.walkSound = walkSound;
    this.lenOfWalkSound = lenOfWalkSound;
    this.volume = 1;
    this.pitch = 1;
  }

  /**
   * The constructor for an {@link TrafficSignalAPSSoundScheme} instance with a specified locator
   * sound length.
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
  TrafficSignalAPSSoundScheme(String name, CsmSounds.SOUND locateSound,
      int lenOfLocateSound,
      CsmSounds.SOUND waitSound, int lenOfWaitSound, CsmSounds.SOUND pressSound,
      int lenOfPressSound, CsmSounds.SOUND walkSound, int lenOfWalkSound) {
    this.name = name;
    this.locateSound = locateSound;
    this.lenOfLocateSound = lenOfLocateSound;
    this.waitSound = waitSound;
    this.lenOfWaitSound = lenOfWaitSound;
    this.pressSound = pressSound;
    this.lenOfPressSound = lenOfPressSound;
    this.walkSound = walkSound;
    this.lenOfWalkSound = lenOfWalkSound;
    this.volume = 1;
    this.pitch = 1;
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
