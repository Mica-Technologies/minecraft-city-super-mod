package com.micatechnologies.minecraft.csm.tts;

import javax.sound.sampled.AudioFormat;

/**
 * Applies the player's volume to synthesized speech, which plays on a Java Sound line of its own
 * rather than through Minecraft's sound system, so no volume slider would otherwise reach it.
 *
 * <p>The samples are scaled rather than the line's {@code MASTER_GAIN} control set: not every
 * mixer offers that control, and one that does works in decibels with a floor well above
 * silence, so a slider at zero would still be heard. MaryTTS produces signed 16-bit PCM; any
 * other format is passed through untouched, as every format was before.</p>
 *
 * <p>Its own class, apart from {@link MaryTtsEngine}, so a test can load it without MaryTTS.</p>
 *
 * @since 2026.9
 */
final class PcmVolume {

  private PcmVolume() {
  }

  /**
   * Scales signed 16-bit PCM samples in place.
   *
   * @param buffer the audio bytes, whole frames (as {@code AudioInputStream.read} returns them)
   * @param length how many of them are audio
   * @param format the audio's format
   * @param gain   0 (silent) to 1 (as synthesized); anything at or above 1 leaves them alone
   */
  static void apply(byte[] buffer, int length, AudioFormat format, float gain) {
    if (gain >= 1.0F || format.getSampleSizeInBits() != 16
        || !AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())) {
      return;
    }
    float g = Math.max(0.0F, gain);
    boolean bigEndian = format.isBigEndian();
    for (int i = 0; i + 1 < length; i += 2) {
      int hi = bigEndian ? buffer[i] : buffer[i + 1];
      int lo = bigEndian ? buffer[i + 1] : buffer[i];
      short sample = (short) ((hi << 8) | (lo & 0xFF));
      int scaled = Math.round(sample * g);
      byte outHi = (byte) (scaled >> 8);
      byte outLo = (byte) scaled;
      if (bigEndian) {
        buffer[i] = outHi;
        buffer[i + 1] = outLo;
      } else {
        buffer[i] = outLo;
        buffer[i + 1] = outHi;
      }
    }
  }
}
