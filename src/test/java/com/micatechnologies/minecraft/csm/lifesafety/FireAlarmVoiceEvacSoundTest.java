package com.micatechnologies.minecraft.csm.lifesafety;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the volume calculation logic extracted from {@link FireAlarmVoiceEvacSound}. Tests
 * the package-private static {@code calculateVolume} method directly, which does not require a
 * running Minecraft client.
 */
class FireAlarmVoiceEvacSoundTest {

  private static final float MAX_VOLUME = 1.0f;
  private static final float MIN_VOLUME = 0.05f;
  private static final float TOLERANCE = 0.001f;

  // region: At speaker position (distance ~0)

  @Test
  void volumeAtSpeakerPositionShouldBeMaximum() {
    // Player at center of block (0,0,0) => speaker center is (0.5, 0.5, 0.5)
    List<BlockPos> speakers = Collections.singletonList(new BlockPos(0, 0, 0));
    float vol = FireAlarmVoiceEvacSound.calculateVolume(0.5, 0.5, 0.5, speakers, 48.0f);
    assertEquals(MAX_VOLUME, vol, TOLERANCE);
  }

  // endregion

  // region: Beyond hearing range

  @Test
  void volumeBeyondHearingRangeShouldBeMinimum() {
    List<BlockPos> speakers = Collections.singletonList(new BlockPos(0, 0, 0));
    // Player far away (1000 blocks), hearing range 48
    float vol = FireAlarmVoiceEvacSound.calculateVolume(1000.0, 0.5, 0.5, speakers, 48.0f);
    assertEquals(MIN_VOLUME, vol, TOLERANCE);
  }

  @Test
  void volumeExactlyAtHearingRangeBoundaryShouldBeMinimum() {
    // Hearing range 10, speaker at (0,0,0) center (0.5,0.5,0.5), player 10 blocks away on X axis
    List<BlockPos> speakers = Collections.singletonList(new BlockPos(0, 0, 0));
    float vol = FireAlarmVoiceEvacSound.calculateVolume(10.5, 0.5, 0.5, speakers, 10.0f);
    assertEquals(MIN_VOLUME, vol, TOLERANCE);
  }

  // endregion

  // region: Distance falloff

  @Test
  void volumeDecreasesWithDistance() {
    List<BlockPos> speakers = Collections.singletonList(new BlockPos(0, 0, 0));
    float hearingRange = 48.0f;

    float volNear = FireAlarmVoiceEvacSound.calculateVolume(5.5, 0.5, 0.5, speakers, hearingRange);
    float volMid = FireAlarmVoiceEvacSound.calculateVolume(20.5, 0.5, 0.5, speakers, hearingRange);
    float volFar = FireAlarmVoiceEvacSound.calculateVolume(40.5, 0.5, 0.5, speakers, hearingRange);

    assertTrue(volNear > volMid, "Near volume should be greater than mid volume");
    assertTrue(volMid > volFar, "Mid volume should be greater than far volume");
    assertTrue(volFar >= MIN_VOLUME, "Far volume should be at least MIN_VOLUME");
  }

  @Test
  void volumeFollowsQuadraticFalloff() {
    List<BlockPos> speakers = Collections.singletonList(new BlockPos(0, 0, 0));
    float hearingRange = 100.0f;
    // Player at distance 50 from speaker center (0.5,0.5,0.5)
    double playerX = 50.5;
    float vol = FireAlarmVoiceEvacSound.calculateVolume(playerX, 0.5, 0.5, speakers, hearingRange);

    // Expected: linear = 1 - 50/100 = 0.5, ratio = 0.25
    // volume = 0.05 + 0.95 * 0.25 = 0.2875
    assertEquals(0.2875f, vol, 0.01f);
  }

  // endregion

  // region: Multiple speakers

  @Test
  void volumeUsesNearestSpeaker() {
    List<BlockPos> speakers = Arrays.asList(
        new BlockPos(100, 0, 0),  // Far speaker
        new BlockPos(0, 0, 0)     // Near speaker
    );
    float hearingRange = 48.0f;

    // Player near the second speaker
    float vol = FireAlarmVoiceEvacSound.calculateVolume(1.5, 0.5, 0.5, speakers, hearingRange);

    // Should be close to max since player is ~1 block from nearest speaker
    assertTrue(vol > 0.9f, "Volume should be high when near the nearest speaker");
  }

  // endregion

  // region: Empty speaker list

  @Test
  void volumeWithEmptySpeakerListShouldBeMinimum() {
    List<BlockPos> speakers = Collections.emptyList();
    // With no speakers, minDistSq stays at MAX_VALUE => beyond hearing range
    float vol = FireAlarmVoiceEvacSound.calculateVolume(0, 0, 0, speakers, 48.0f);
    assertEquals(MIN_VOLUME, vol, TOLERANCE);
  }

  // endregion

  // region: Volume bounds

  @Test
  void volumeNeverExceedsMax() {
    List<BlockPos> speakers = Arrays.asList(
        new BlockPos(0, 0, 0),
        new BlockPos(0, 0, 1),
        new BlockPos(1, 0, 0));
    // Player right at center of speaker 0
    float vol = FireAlarmVoiceEvacSound.calculateVolume(0.5, 0.5, 0.5, speakers, 48.0f);
    assertTrue(vol <= MAX_VOLUME, "Volume should not exceed MAX_VOLUME");
  }

  @Test
  void volumeNeverBelowMin() {
    List<BlockPos> speakers = Collections.singletonList(new BlockPos(0, 0, 0));
    // Player extremely far away
    float vol =
        FireAlarmVoiceEvacSound.calculateVolume(999999.0, 999999.0, 999999.0, speakers, 1.0f);
    assertTrue(vol >= MIN_VOLUME, "Volume should not go below MIN_VOLUME");
  }

  // endregion

  // region: Hearing range edge cases

  @Test
  void zeroHearingRangeShouldReturnMinVolume() {
    List<BlockPos> speakers = Collections.singletonList(new BlockPos(0, 0, 0));
    // hearingRange 0 means hearingRangeSq is 0, any non-zero distance > 0
    float vol = FireAlarmVoiceEvacSound.calculateVolume(1.0, 0.0, 0.0, speakers, 0.0f);
    assertEquals(MIN_VOLUME, vol, TOLERANCE);
  }

  // endregion
}
