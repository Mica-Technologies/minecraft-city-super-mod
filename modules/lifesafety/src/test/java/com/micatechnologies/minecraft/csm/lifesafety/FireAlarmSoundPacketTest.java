package com.micatechnologies.minecraft.csm.lifesafety;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FireAlarmSoundPacket}, covering factory methods, byte serialization
 * round-trips, and edge cases.
 */
class FireAlarmSoundPacketTest {

  // region: Factory method tests

  @Test
  void stopFactoryCreatesNonStartPacketWithChannel() {
    FireAlarmSoundPacket pkt = FireAlarmSoundPacket.stop("channel1");
    assertFalse(pkt.isStart());
    assertEquals("channel1", pkt.getChannel());
    assertEquals("", pkt.getSoundResource());
    assertEquals(0f, pkt.getHearingRange());
    assertTrue(pkt.getSpeakerPositions().isEmpty());
  }

  @Test
  void stopAllFactoryCreatesEmptyChannelStopPacket() {
    FireAlarmSoundPacket pkt = FireAlarmSoundPacket.stopAll();
    assertFalse(pkt.isStart());
    assertEquals("", pkt.getChannel());
  }

  @Test
  void startFactoryCreatesStartPacketWithAllFields() {
    List<BlockPos> speakers = Arrays.asList(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6));
    FireAlarmSoundPacket pkt =
        FireAlarmSoundPacket.start("ch1", "csm:alarm_sound", 48.0f, speakers);

    assertTrue(pkt.isStart());
    assertEquals("ch1", pkt.getChannel());
    assertEquals("csm:alarm_sound", pkt.getSoundResource());
    assertEquals(48.0f, pkt.getHearingRange());
    assertEquals(2, pkt.getSpeakerPositions().size());
    assertEquals(new BlockPos(1, 2, 3), pkt.getSpeakerPositions().get(0));
    assertEquals(new BlockPos(4, 5, 6), pkt.getSpeakerPositions().get(1));
  }

  // endregion

  // region: toBytes/fromBytes round-trip

  @Test
  void startPacketRoundTrip() {
    List<BlockPos> speakers = Arrays.asList(
        new BlockPos(100, 64, -200),
        new BlockPos(-50, 128, 300));
    FireAlarmSoundPacket original =
        FireAlarmSoundPacket.start("fire_panel_1", "csm:voice_evac", 64.0f, speakers);

    ByteBuf buf = Unpooled.buffer();
    original.toBytes(buf);

    FireAlarmSoundPacket deserialized = new FireAlarmSoundPacket();
    deserialized.fromBytes(buf);

    assertTrue(deserialized.isStart());
    assertEquals("fire_panel_1", deserialized.getChannel());
    assertEquals("csm:voice_evac", deserialized.getSoundResource());
    assertEquals(64.0f, deserialized.getHearingRange());
    assertEquals(2, deserialized.getSpeakerPositions().size());
    assertEquals(new BlockPos(100, 64, -200), deserialized.getSpeakerPositions().get(0));
    assertEquals(new BlockPos(-50, 128, 300), deserialized.getSpeakerPositions().get(1));

    buf.release();
  }

  @Test
  void stopPacketRoundTrip() {
    FireAlarmSoundPacket original = FireAlarmSoundPacket.stop("panel_2");

    ByteBuf buf = Unpooled.buffer();
    original.toBytes(buf);

    FireAlarmSoundPacket deserialized = new FireAlarmSoundPacket();
    deserialized.fromBytes(buf);

    assertFalse(deserialized.isStart());
    assertEquals("panel_2", deserialized.getChannel());
    assertEquals("", deserialized.getSoundResource());
    assertEquals(0f, deserialized.getHearingRange());
    assertTrue(deserialized.getSpeakerPositions().isEmpty());

    buf.release();
  }

  @Test
  void stopAllPacketRoundTrip() {
    FireAlarmSoundPacket original = FireAlarmSoundPacket.stopAll();

    ByteBuf buf = Unpooled.buffer();
    original.toBytes(buf);

    FireAlarmSoundPacket deserialized = new FireAlarmSoundPacket();
    deserialized.fromBytes(buf);

    assertFalse(deserialized.isStart());
    assertEquals("", deserialized.getChannel());

    buf.release();
  }

  // endregion

  // region: Edge cases

  @Test
  void roundTripWithEmptyChannel() {
    FireAlarmSoundPacket original =
        FireAlarmSoundPacket.start("", "csm:test", 10.0f, Collections.emptyList());

    ByteBuf buf = Unpooled.buffer();
    original.toBytes(buf);

    FireAlarmSoundPacket deserialized = new FireAlarmSoundPacket();
    deserialized.fromBytes(buf);

    assertEquals("", deserialized.getChannel());
    assertTrue(deserialized.isStart());

    buf.release();
  }

  @Test
  void roundTripWithEmptySpeakerList() {
    FireAlarmSoundPacket original =
        FireAlarmSoundPacket.start("ch", "csm:sound", 32.0f, Collections.emptyList());

    ByteBuf buf = Unpooled.buffer();
    original.toBytes(buf);

    FireAlarmSoundPacket deserialized = new FireAlarmSoundPacket();
    deserialized.fromBytes(buf);

    assertTrue(deserialized.getSpeakerPositions().isEmpty());

    buf.release();
  }

  @Test
  void roundTripWithLargeSpeakerList() {
    List<BlockPos> speakers = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      speakers.add(new BlockPos(i, i * 2, i * 3));
    }
    FireAlarmSoundPacket original =
        FireAlarmSoundPacket.start("big_channel", "csm:big_sound", 128.0f, speakers);

    ByteBuf buf = Unpooled.buffer();
    original.toBytes(buf);

    FireAlarmSoundPacket deserialized = new FireAlarmSoundPacket();
    deserialized.fromBytes(buf);

    assertEquals(500, deserialized.getSpeakerPositions().size());
    for (int i = 0; i < 500; i++) {
      BlockPos expected = new BlockPos(i, i * 2, i * 3);
      assertEquals(expected, deserialized.getSpeakerPositions().get(i),
          "Mismatch at speaker index " + i);
    }

    buf.release();
  }

  @Test
  void roundTripWithUnicodeChannel() {
    FireAlarmSoundPacket original =
        FireAlarmSoundPacket.start("ch\u00e9nel\u2605", "csm:s", 1.0f, Collections.emptyList());

    ByteBuf buf = Unpooled.buffer();
    original.toBytes(buf);

    FireAlarmSoundPacket deserialized = new FireAlarmSoundPacket();
    deserialized.fromBytes(buf);

    assertEquals("ch\u00e9nel\u2605", deserialized.getChannel());

    buf.release();
  }

  // endregion
}
