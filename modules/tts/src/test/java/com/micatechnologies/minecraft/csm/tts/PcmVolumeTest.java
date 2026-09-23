package com.micatechnologies.minecraft.csm.tts;

import static org.junit.jupiter.api.Assertions.*;

import javax.sound.sampled.AudioFormat;
import org.junit.jupiter.api.Test;

class PcmVolumeTest {

  private static final AudioFormat LITTLE =
      new AudioFormat(16000F, 16, 1, true, false);
  private static final AudioFormat BIG =
      new AudioFormat(16000F, 16, 1, true, true);

  private static short sampleAt(byte[] b, int i, boolean bigEndian) {
    int hi = bigEndian ? b[i] : b[i + 1];
    int lo = bigEndian ? b[i + 1] : b[i];
    return (short) ((hi << 8) | (lo & 0xFF));
  }

  private static byte[] samples(boolean bigEndian, short... values) {
    byte[] b = new byte[values.length * 2];
    for (int i = 0; i < values.length; i++) {
      byte hi = (byte) (values[i] >> 8);
      byte lo = (byte) values[i];
      b[2 * i] = bigEndian ? hi : lo;
      b[2 * i + 1] = bigEndian ? lo : hi;
    }
    return b;
  }

  @Test
  void halfGainHalvesEverySampleIncludingNegativeOnes() {
    for (boolean bigEndian : new boolean[]{false, true}) {
      byte[] b = samples(bigEndian, (short) 1000, (short) -1000, Short.MAX_VALUE, Short.MIN_VALUE);
      PcmVolume.apply(b, b.length, bigEndian ? BIG : LITTLE, 0.5F);
      assertEquals(500, sampleAt(b, 0, bigEndian));
      assertEquals(-500, sampleAt(b, 2, bigEndian));
      assertEquals(16384, sampleAt(b, 4, bigEndian));
      assertEquals(-16384, sampleAt(b, 6, bigEndian));
    }
  }

  @Test
  void zeroGainIsSilence() {
    byte[] b = samples(false, (short) 12345, (short) -32000);
    PcmVolume.apply(b, b.length, LITTLE, 0.0F);
    assertEquals(0, sampleAt(b, 0, false));
    assertEquals(0, sampleAt(b, 2, false));
  }

  @Test
  void fullGainAndOtherFormatsAreUntouched() {
    byte[] b = samples(false, (short) 12345);
    byte[] before = b.clone();
    PcmVolume.apply(b, b.length, LITTLE, 1.0F);
    assertArrayEquals(before, b);

    AudioFormat eightBit = new AudioFormat(16000F, 8, 1, true, false);
    byte[] c = {100, -100};
    PcmVolume.apply(c, c.length, eightBit, 0.5F);
    assertArrayEquals(new byte[]{100, -100}, c);
  }

  @Test
  void onlyTheBytesReadAreScaled() {
    byte[] b = samples(false, (short) 1000, (short) 1000);
    PcmVolume.apply(b, 2, LITTLE, 0.5F);
    assertEquals(500, sampleAt(b, 0, false));
    assertEquals(1000, sampleAt(b, 2, false));
  }
}
