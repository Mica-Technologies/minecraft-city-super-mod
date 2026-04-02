package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalAPSSoundScheme;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Tile entity class for an APS (accessible pedestrian signal) button. This class assists in
 * tracking and managing the APS' configured sounds and requests.
 *
 * @author Mica Technologies
 * @version 2.0
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
   * The maximum value for the crosswalk arrow orientation. (0, 1, 2, 3; aka left, right, both,
   * none)
   *
   * @since 1.1
   */
  public static final int CROSSWALK_ARROW_ORIENTATION_MAX = 3;

  /**
   * The possible/available sound schemes for the APS.
   *
   * @since 2.0
   */
  private final TrafficSignalAPSSoundScheme[] soundSchemes;

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
   * Tracks the last color state for which a sound was started, so we only send start/stop
   * packets on state changes rather than every tick. -1 means no sound is active.
   */
  private int lastSoundColorState = -1;

  /**
   * The walk sound resource currently playing on this channel, or null if no walk sound active.
   */
  private String currentWalkSound = null;

  /**
   * The cycle length (in ticks) for the current walk sound. The server resends the start
   * packet at this interval, giving precise loop timing independent of audio file duration.
   */
  private int currentWalkSoundLen = 0;

  /**
   * Whether the locate tone is currently active (sent per-interval for sync).
   */
  private boolean locateToneActive = false;

  /**
   * Constructor for a {@link TileEntityTrafficSignalAPS} with the specified sound schemes.
   *
   * @param soundSchemes the sound schemes for the APS
   *
   * @since 2.0
   */
  public TileEntityTrafficSignalAPS(TrafficSignalAPSSoundScheme[] soundSchemes) {
    this.soundSchemes = soundSchemes;
  }

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
      if (storedCrosswalkSoundIndex >= 0 && storedCrosswalkSoundIndex < soundSchemes.length) {
        crosswalkSoundIndex = storedCrosswalkSoundIndex;
      } else {
        System.err.println("Invalid crosswalk sound index: " + crosswalkSoundIndex
            + " for crosswalk button tile entity " + "at [X: " + pos.getX() + ", Y: " + pos.getY()
            + ", Z: " + pos.getZ() + "]. Reverting to default (0).");
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
        System.err.println(
            "Invalid crosswalk sound last played time: " + crosswalkSoundLastPlayedTime
                + " for crosswalk button tile entity " + "at [X: " + pos.getX() + ", Y: "
                + pos.getY() + ", Z: " + pos.getZ() + "]. Reverting to default (0).");
        crosswalkSoundLastPlayedTime = 0;
      }
    }

    // Read crosswalk last pressed time from NBT and check validity
    if (compound.hasKey(CROSSWALK_LAST_PRESS_TIME_NBT_KEY)) {
      long storedCrosswalkLastPressTime = compound.getLong(CROSSWALK_LAST_PRESS_TIME_NBT_KEY);
      if (storedCrosswalkLastPressTime >= 0) {
        crosswalkLastPressTime = storedCrosswalkLastPressTime;
      } else {
        System.err.println("Invalid crosswalk last pressed time: " + crosswalkLastPressTime
            + " for crosswalk button tile entity " + "at [X: " + pos.getX() + ", Y: " + pos.getY()
            + ", Z: " + pos.getZ() + "]. Reverting to default (0).");
        crosswalkLastPressTime = 0;
      }
    }

    // Read crosswalk arrow orientation from NBT and check validity
    if (compound.hasKey(CROSSWALK_ARROW_ORIENTATION_NBT_KEY)) {
      int storedCrosswalkArrowOrientation =
          compound.getInteger(CROSSWALK_ARROW_ORIENTATION_NBT_KEY);
      if (storedCrosswalkArrowOrientation >= CROSSWALK_ARROW_ORIENTATION_MIN
          && storedCrosswalkArrowOrientation <= CROSSWALK_ARROW_ORIENTATION_MAX) {
        crosswalkArrowOrientation = storedCrosswalkArrowOrientation;
      } else {
        System.err.println("Invalid crosswalk arrow orientation: " + crosswalkArrowOrientation
            + " for crosswalk button tile entity " + "at [X: " + pos.getX() + ", Y: " + pos.getY()
            + ", Z: " + pos.getZ() + "]. Reverting to default (0).");
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
    if (crosswalkSoundIndex >= soundSchemes.length) {
      crosswalkSoundIndex = 0;
    }

    // Stop any currently playing sound and reset state so the next tick
    // immediately starts the new scheme's sound for the current signal phase
    if (currentWalkSound != null) {
      stopSoundViaPacket();
      currentWalkSound = null;
      currentWalkSoundLen = 0;
    }
    lastSoundColorState = -1;

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
    return soundSchemes[crosswalkSoundIndex];
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
   * The hearing range for APS sounds in blocks. Determines the maximum distance at which
   * the sound is audible, with volume attenuating linearly as the player moves away.
   */
  private static final float APS_HEARING_RANGE = 16.0f;

  /**
   * Returns a boolean indicating if the tile entity should also tick on the client side.
   * Sound is now handled via network packets and client-side MovingSound, so client ticking
   * is no longer needed.
   *
   * @return false — sound is managed server-side via packets.
   *
   * @since 2.0
   */
  @Override
  public boolean doClientTick() {
    return false;
  }

  /**
   * Gets a unique channel name for this APS button based on its position.
   */
  private String getChannel() {
    return "aps_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
  }

  /**
   * Gets a separate channel for press/wait sounds so they don't conflict with the
   * locate tone that fires every 20 ticks on the main channel.
   */
  private String getPressChannel() {
    return "aps_press_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
  }

  /**
   * Sends an APS sound packet to all clients to start a sound with distance-based volume.
   *
   * @param channel       the channel to play on
   * @param soundResource the sound resource to play
   * @param repeat        true for looping sounds (walk), false for one-shot (locate tone)
   */
  private void playSoundOnChannel(String channel, String soundResource, boolean repeat) {
    if (world != null && !world.isRemote && soundResource != null) {
      CsmNetwork.sendToAll(APSSoundPacket.start(
          channel, soundResource, APS_HEARING_RANGE, repeat, pos));
    }
  }

  /**
   * Sends an APS sound packet to stop the current sound on the main channel.
   */
  private void stopSoundViaPacket() {
    if (world != null && !world.isRemote) {
      CsmNetwork.sendToAll(APSSoundPacket.stop(getChannel()));
    }
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
    return 1; // Tick every game tick for responsive state change detection
  }

  /**
   * Handles the tick event of the crosswalk button. Sends start/stop packets only on
   * state changes. Sounds loop on the client with silence baked into the audio files.
   *
   * @since 2.0
   */
  /**
   * The global tick interval for locate tone playback. All APS buttons send their locate
   * tone beep at worldTime % this value == 0, ensuring perfect sync across devices.
   */
  private static final long LOCATE_TONE_INTERVAL = 20L;

  @Override
  public void onTick() {
    if (world == null || world.isRemote) return;

    int blockColor =
        world.getBlockState(pos).getValue(BlockControllableCrosswalkButtonAudible.COLOR);

    // Detect state changes for walk sound management
    if (blockColor != lastSoundColorState) {
      // Stop any active walk sound when leaving the walk phase
      if (currentWalkSound != null) {
        stopSoundViaPacket();
        currentWalkSound = null;
        currentWalkSoundLen = 0;
      }
      locateToneActive = false;
      lastSoundColorState = blockColor;

      // Start walk sound on transition to GREEN (one-shot, server controls cycle timing)
      if (blockColor == BlockControllableCrosswalkButtonAudible.SIGNAL_GREEN) {
        if (getCrosswalkSound().getWalkSound() != null) {
          currentWalkSound = getCrosswalkSound().getWalkSound().getSoundLocation().toString();
          currentWalkSoundLen = getCrosswalkSound().getLenOfWalkSound();
          playSoundOnChannel(getChannel(), currentWalkSound, false);
        }
      }
    }

    // Server-controlled walk sound cycling: resend the one-shot start packet at each
    // lenOfWalkSound interval. The client handler stops the old sound (which is in its
    // baked-silence tail by now) and starts a fresh one. This gives precise loop timing
    // from the server tick counter, avoids client-side repeat timing drift, and ensures
    // newly loaded clients pick up the sound within one cycle.
    if (blockColor == BlockControllableCrosswalkButtonAudible.SIGNAL_GREEN
        && currentWalkSound != null
        && currentWalkSoundLen > 0
        && world.getTotalWorldTime() % currentWalkSoundLen == 0) {
      playSoundOnChannel(getChannel(), currentWalkSound, false);
    }

    // Per-interval locate tone: send a one-shot beep at each global 20-tick boundary.
    // All APS buttons with the same interval fire at the same world tick = perfect sync.
    // Uses repeat=false (original 0.1s beep files, no padding).
    if ((blockColor == BlockControllableCrosswalkButtonAudible.SIGNAL_YELLOW
        || blockColor == BlockControllableCrosswalkButtonAudible.SIGNAL_RED)
        && world.getTotalWorldTime() % LOCATE_TONE_INTERVAL == 0) {
      locateToneActive = true;
      if (getCrosswalkSound().getLocateSound() != null) {
        playSoundOnChannel(getChannel(),
            getCrosswalkSound().getLocateSound().getSoundLocation().toString(), false);
      }
    }
  }

  /**
   * Handles the press event of the crosswalk button. Uses packet-based MovingSound.
   *
   * @since 2.0
   */
  public void onPress() {
    if (world == null || world.isRemote) return;

    boolean isPressSoundAlreadyPlaying =
        (crosswalkLastPressTime + getCrosswalkSound().getLenOfPressSound())
            > world.getTotalWorldTime();
    if (!isPressSoundAlreadyPlaying && getCrosswalkSound().getPressSound() != null) {
      playSoundOnChannel(getPressChannel(),
          getCrosswalkSound().getPressSound().getSoundLocation().toString(), false);
      crosswalkLastPressTime = world.getTotalWorldTime();
    }
  }
}
