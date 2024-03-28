package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalAPSSoundScheme;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;

/**
 * Tile entity utility class for an APS (accessible pedestrian signal) button. This class assists in
 * tracking and managing the APS' configured sounds and requests.
 *
 * @author Mica Technologies
 * @version 1.1
 * @since 2022.1
 */
public class TileEntityTrafficSignalAPS extends TileEntityTrafficSignalTickableRequester {

  /**
   * The NBT key used to store the current crosswalk sound index.
   *
   * @since 1.0
   */
  private static final String CROSSWALK_SOUND_INDEX_NBT_KEY = "CrosswalkSoundIndex";

  /**
   * The NBT key used to store the current crosswalk sound last played time.
   *
   * @since 1.0
   */
  private static final String CROSSWALK_SOUND_LAST_PLAY_TIME_NBT_KEY = "CrosswalkSoundLastPlayTime";

  /**
   * The NBT key used to store the current crosswalk last pressed time.
   *
   * @since 1.0
   */
  private static final String CROSSWALK_LAST_PRESS_TIME_NBT_KEY = "CrosswalkLastPressTime";

  /**
   * The NBT key used to store the current crosswalk arrow orientation.
   *
   * @since 1.1
   */
  private static final String CROSSWALK_ARROW_ORIENTATION_NBT_KEY = "CrosswalkArrowOrientation";

  /**
   * The minimum value for the crosswalk arrow orientation.
   *
   * @since 1.1
   */
  public static final int CROSSWALK_ARROW_ORIENTATION_MIN = 0;

  /**
   * The maximum value for the crosswalk arrow orientation. (0, 1, 2; aka left, right, both)
   *
   * @since 1.1
   */
  public static final int CROSSWALK_ARROW_ORIENTATION_MAX = 2;

  /**
   * The current crosswalk sound index.
   *
   * @since 1.0
   */
  private int crosswalkSoundIndex = 0;

  /**
   * The current crosswalk sound last played time.
   *
   * @since 1.0
   */
  private long crosswalkSoundLastPlayedTime = 0;

  /**
   * The current crosswalk last pressed time.
   *
   * @since 1.0
   */
  private long crosswalkLastPressTime = 0;

  /**
   * The current crosswalk arrow orientation.
   *
   * @since 1.1
   */
  private int crosswalkArrowOrientation = 0;

  /**
   * Writes to the NBT tag compound with the tile entity's NBT data and returns the compound.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   *
   * @since 1.0
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    // Write crosswalk sound to NBT
    compound.setInteger(CROSSWALK_SOUND_INDEX_NBT_KEY, crosswalkSoundIndex);

    // Write crosswalk sound last played time to NBT
    compound.setLong(CROSSWALK_SOUND_LAST_PLAY_TIME_NBT_KEY, crosswalkSoundLastPlayedTime);

    // Write crosswalk last pressed time to NBT
    compound.setLong(CROSSWALK_LAST_PRESS_TIME_NBT_KEY, crosswalkLastPressTime);

    // Write crosswalk arrow orientation to NBT
    compound.setInteger(CROSSWALK_ARROW_ORIENTATION_NBT_KEY, crosswalkArrowOrientation);

    // Return the NBT tag compound
    return compound;
  }

  /**
   * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   *
   * @since 1.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    // Read crosswalk sound from NBT and check validity
    if (compound.hasKey(CROSSWALK_SOUND_INDEX_NBT_KEY)) {
      int storedCrosswalkSoundIndex = compound.getInteger(CROSSWALK_SOUND_INDEX_NBT_KEY);
      if (storedCrosswalkSoundIndex >= 0 &&
          storedCrosswalkSoundIndex < TrafficSignalAPSSoundScheme.values().length) {
        crosswalkSoundIndex = storedCrosswalkSoundIndex;
      } else {
        System.err.println("Invalid crosswalk sound index: " +
            crosswalkSoundIndex +
            " for crosswalk button tile entity " +
            "at [X: " +
            pos.getX() +
            ", Y: " +
            pos.getY() +
            ", Z: " +
            pos.getZ() +
            "]. Reverting to default (0).");
        crosswalkSoundIndex = 0;
      }
    }

    // Read crosswalk sound last played time from NBT and check validity
    if (compound.hasKey(CROSSWALK_SOUND_LAST_PLAY_TIME_NBT_KEY)) {
      long storedCrosswalkSoundLastPlayedTime =
          compound.getLong(CROSSWALK_SOUND_LAST_PLAY_TIME_NBT_KEY);
      if (storedCrosswalkSoundLastPlayedTime >= 0) {
        crosswalkSoundLastPlayedTime = storedCrosswalkSoundLastPlayedTime;
      } else {
        System.err.println("Invalid crosswalk sound last played time: " +
            crosswalkSoundLastPlayedTime +
            " for crosswalk button tile entity " +
            "at [X: " +
            pos.getX() +
            ", Y: " +
            pos.getY() +
            ", Z: " +
            pos.getZ() +
            "]. Reverting to default (0).");
        crosswalkSoundLastPlayedTime = 0;
      }
    }

    // Read crosswalk last pressed time from NBT and check validity
    if (compound.hasKey(CROSSWALK_LAST_PRESS_TIME_NBT_KEY)) {
      long storedCrosswalkLastPressTime = compound.getLong(CROSSWALK_LAST_PRESS_TIME_NBT_KEY);
      if (storedCrosswalkLastPressTime >= 0) {
        crosswalkLastPressTime = storedCrosswalkLastPressTime;
      } else {
        System.err.println("Invalid crosswalk last pressed time: " +
            crosswalkLastPressTime +
            " for crosswalk button tile entity " +
            "at [X: " +
            pos.getX() +
            ", Y: " +
            pos.getY() +
            ", Z: " +
            pos.getZ() +
            "]. Reverting to default (0).");
        crosswalkLastPressTime = 0;
      }
    }

    // Read crosswalk arrow orientation from NBT and check validity
    if (compound.hasKey(CROSSWALK_ARROW_ORIENTATION_NBT_KEY)) {
      int storedCrosswalkArrowOrientation =
          compound.getInteger(CROSSWALK_ARROW_ORIENTATION_NBT_KEY);
      if (storedCrosswalkArrowOrientation >= CROSSWALK_ARROW_ORIENTATION_MIN &&
          storedCrosswalkArrowOrientation <= CROSSWALK_ARROW_ORIENTATION_MAX) {
        crosswalkArrowOrientation = storedCrosswalkArrowOrientation;
      } else {
        System.err.println("Invalid crosswalk arrow orientation: " +
            crosswalkArrowOrientation +
            " for crosswalk button tile entity " +
            "at [X: " +
            pos.getX() +
            ", Y: " +
            pos.getY() +
            ", Z: " +
            pos.getZ() +
            "]. Reverting to default (0).");
        crosswalkArrowOrientation = 0;
      }
    }
  }

  /**
   * Switches the crosswalk sound to the next sound in the list and returns the new sound name.
   *
   * @return The name of the new crosswalk sound.
   *
   * @since 1.0
   */
  public String switchSound() {
    // Increment crosswalk sound index and check validity
    crosswalkSoundIndex++;
    if (crosswalkSoundIndex >= TrafficSignalAPSSoundScheme.values().length) {
      crosswalkSoundIndex = 0;
    }

    // Mark the tile entity as dirty
    markDirtySync(world, pos, true);

    // Return the new crosswalk sound name
    return getCrosswalkSound().getName();
  }

  /**
   * Gets the current crosswalk sound.
   *
   * @return The current crosswalk sound.
   *
   * @since 1.0
   */
  public TrafficSignalAPSSoundScheme getCrosswalkSound() {
    return TrafficSignalAPSSoundScheme.values()[crosswalkSoundIndex];
  }

  /**
   * Gets the current crosswalk arrow orientation.
   *
   * @return The current crosswalk arrow orientation.
   *
   * @since 1.1
   */
  public int getCrosswalkArrowOrientation() {
    return crosswalkArrowOrientation;
  }

  /**
   * Increments the current crosswalk arrow orientation and returns the new orientation.
   *
   * @return The new crosswalk arrow orientation.
   *
   * @since 1.1
   */
  public int incrementCrosswalkArrowOrientation(IBlockState state) {
    int oldCrosswalkArrowOrientation = crosswalkArrowOrientation;
    crosswalkArrowOrientation++;
    if (crosswalkArrowOrientation > CROSSWALK_ARROW_ORIENTATION_MAX) {
      crosswalkArrowOrientation = CROSSWALK_ARROW_ORIENTATION_MIN;
    }

    // Mark the tile entity as dirty
    markDirtySync(world, pos, state, true);

    return crosswalkArrowOrientation;
  }

  /**
   * Returns a boolean indicating if the tile entity should also tick on the client side. By
   * default, the tile entity will always tick on the server side, and in the event of
   * singleplayer/local mode, the host client is considered the server. This implementation always
   * returns true to ensure that sound is played on the client side.
   *
   * @return a boolean indicating if the tile entity should also tick on the client side. This
   *     implementation always returns true to ensure that sound is played on the client side.
   *
   * @since 1.0
   */
  @Override
  public boolean doClientTick() {
    return true;
  }

  /**
   * Returns a boolean indicating if the tile entity ticking should be paused. If the tile entity is
   * paused, the tick event will not be called. This implementation always returns false as the tile
   * entity should always tick.
   *
   * @return a boolean indicating if the tile entity ticking should be paused. This implementation
   *     always returns false as the tile entity should always tick.
   *
   * @since 1.0
   */
  @Override
  public boolean pauseTicking() {
    return false;
  }

  /**
   * Returns the tick rate of the tile entity.
   *
   * @return the tick rate of the tile entity
   *
   * @since 1.0
   */
  @Override
  public long getTickRate() {
    return getCrosswalkSound().getLenOfLocateSound();
  }

  /**
   * Handles the tick event of the crosswalk button.
   *
   * @since 1.0
   */
  @Override
  public void onTick() {
    // Get block color value
    int blockColor = world.getBlockState(pos)
        .getValue(BlockControllableCrosswalkButtonAudible.COLOR);

    // Handle for each color state
    if (blockColor == BlockControllableCrosswalkButtonAudible.SIGNAL_OFF) {
      // Do nothing if no power/turned off
    } else if (blockColor == BlockControllableCrosswalkButtonAudible.SIGNAL_GREEN) {
      // Play walk sound if it's time (not still playing)
      boolean isWalkSoundAlreadyPlaying = (crosswalkSoundLastPlayedTime +
          getCrosswalkSound().getLenOfWalkSound()) > world.getTotalWorldTime();
      boolean isPressSoundAlreadyPlaying =
          (crosswalkLastPressTime + getCrosswalkSound().getLenOfPressSound()) >
              world.getTotalWorldTime();
      if (!isWalkSoundAlreadyPlaying && !isPressSoundAlreadyPlaying) {
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
            getCrosswalkSound().getWalkSound().getSoundEvent(), SoundCategory.NEUTRAL,
            getCrosswalkSound().getVolume(), getCrosswalkSound().getPitch());
        crosswalkSoundLastPlayedTime = world.getTotalWorldTime();
      }
    } else if (blockColor == BlockControllableCrosswalkButtonAudible.SIGNAL_YELLOW) {
      // Play locate sound (future: countdown when controller updated)
      world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
          getCrosswalkSound().getLocateSound().getSoundEvent(), SoundCategory.NEUTRAL,
          getCrosswalkSound().getVolume(), getCrosswalkSound().getPitch());
    } else if (blockColor == BlockControllableCrosswalkButtonAudible.SIGNAL_RED) {
      // Play locate sound
      world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
          getCrosswalkSound().getLocateSound().getSoundEvent(), SoundCategory.NEUTRAL,
          getCrosswalkSound().getVolume(), getCrosswalkSound().getPitch());
    } else {
      System.err.println("Invalid block color value: " +
          blockColor +
          " for crosswalk button tile entity at [X: " +
          pos.getX() +
          ", Y: " +
          pos.getY() +
          pos.getY() +
          ", Z: " +
          pos.getZ() +
          "]");
    }
  }

  /**
   * Handles the press event of the crosswalk button.
   *
   * @since 1.0
   */
  public void onPress() {
    // Play press sound if it's time (not still playing)
    boolean isPressSoundAlreadyPlaying =
        (crosswalkLastPressTime + getCrosswalkSound().getLenOfPressSound()) >
            world.getTotalWorldTime();
    if (!isPressSoundAlreadyPlaying) {
      world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
          getCrosswalkSound().getPressSound().getSoundEvent(), SoundCategory.NEUTRAL,
          getCrosswalkSound().getVolume(), getCrosswalkSound().getPitch());
      crosswalkLastPressTime = world.getTotalWorldTime();
    }
  }
}
