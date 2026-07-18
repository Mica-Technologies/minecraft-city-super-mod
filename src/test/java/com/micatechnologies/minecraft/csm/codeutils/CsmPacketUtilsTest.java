package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the length-prefixed decode helpers in {@link CsmPacketUtils}
 * ({@link CsmPacketUtils#readBoundedBytes}, {@link CsmPacketUtils#readBoundedString},
 * {@link CsmPacketUtils#readBoundedCount}). These guard against allocation attacks: a forged
 * packet whose claimed length is negative, larger than the caller's cap, or larger than the bytes
 * actually present must be rejected <em>before</em> any array/collection is allocated. Rejection is
 * signalled by {@link IndexOutOfBoundsException}, which netty/FML treats as a malformed packet.
 *
 * <p>The validation helpers ({@code canPlayerReach}, {@code isOperatorOrCreative}) are not covered
 * here because they require a live Minecraft server/player and belong to integration testing.</p>
 */
class CsmPacketUtilsTest {

  /** Writes an int-length-prefixed byte payload the way the encode side does. */
  private static ByteBuf prefixed(byte[] payload) {
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(payload.length);
    buf.writeBytes(payload);
    return buf;
  }

  // region readBoundedBytes

  @Test
  void readBoundedBytesRoundTripsValidPayload() {
    byte[] data = {1, 2, 3, 4, 5};
    ByteBuf buf = prefixed(data);

    byte[] out = CsmPacketUtils.readBoundedBytes(buf, 16);

    assertArrayEquals(data, out);
    assertEquals(0, buf.readableBytes(), "all bytes consumed");
  }

  @Test
  void readBoundedBytesAllowsZeroLength() {
    ByteBuf buf = prefixed(new byte[0]);
    assertArrayEquals(new byte[0], CsmPacketUtils.readBoundedBytes(buf, 16));
  }

  @Test
  void readBoundedBytesAllowsExactMax() {
    byte[] data = {7, 7, 7};
    assertArrayEquals(data, CsmPacketUtils.readBoundedBytes(prefixed(data), 3));
  }

  @Test
  void readBoundedBytesRejectsNegativeLength() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(-1);
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedBytes(buf, 16));
  }

  @Test
  void readBoundedBytesRejectsLengthOverMax() {
    byte[] data = {1, 2, 3, 4, 5};
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedBytes(prefixed(data), 4));
  }

  @Test
  void readBoundedBytesRejectsLyingLengthPrefix() {
    // Claims 100 bytes but only 8 follow — must be rejected before allocating a 100-byte array.
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(100);
    buf.writeBytes(new byte[8]);
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedBytes(buf, Integer.MAX_VALUE));
  }

  @Test
  void readBoundedBytesRejectsIntMaxLength() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(Integer.MAX_VALUE);
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedBytes(buf, Integer.MAX_VALUE));
  }

  // endregion

  // region readBoundedString

  @Test
  void readBoundedStringRoundTripsUtf8() {
    byte[] data = "héllo".getBytes(StandardCharsets.UTF_8);
    // Sanity: the length prefix is a BYTE count, not a char count (é is two UTF-8 bytes).
    assertEquals(6, data.length);
    assertEquals("héllo", CsmPacketUtils.readBoundedString(prefixed(data), 16));
  }

  @Test
  void readBoundedStringAllowsEmpty() {
    assertEquals("", CsmPacketUtils.readBoundedString(prefixed(new byte[0]), 16));
  }

  @Test
  void readBoundedStringRejectsOverMax() {
    byte[] data = "abcdef".getBytes(StandardCharsets.UTF_8);
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedString(prefixed(data), 3));
  }

  @Test
  void readBoundedStringRejectsLyingLengthPrefix() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(1000);
    buf.writeBytes("short".getBytes(StandardCharsets.UTF_8));
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedString(buf, Integer.MAX_VALUE));
  }

  // endregion

  // region readBoundedCount

  @Test
  void readBoundedCountReturnsValidCount() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(3);
    buf.writeBytes(new byte[3 * 4]); // 3 elements * 4 bytes each
    assertEquals(3, CsmPacketUtils.readBoundedCount(buf, 8, 4));
  }

  @Test
  void readBoundedCountAllowsZero() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(0);
    assertEquals(0, CsmPacketUtils.readBoundedCount(buf, 8, 4));
  }

  @Test
  void readBoundedCountRejectsNegative() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(-5);
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedCount(buf, 8, 4));
  }

  @Test
  void readBoundedCountRejectsOverMaxCount() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(9);
    buf.writeBytes(new byte[9 * 4]);
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedCount(buf, 8, 4));
  }

  @Test
  void readBoundedCountRejectsCountExceedingRemainingBytes() {
    // Count fits under maxCount but claims more payload than is actually present.
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(5);
    buf.writeBytes(new byte[4]); // only room for 1 element at 4 bytes each
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedCount(buf, 100, 4));
  }

  @Test
  void readBoundedCountDoesNotOverflowOnLargeCount() {
    // count * bytesPerElement overflows a 32-bit int (would wrap negative and wrongly pass the
    // remaining-bytes check). The helper casts to long, so this must still be rejected.
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(Integer.MAX_VALUE);
    buf.writeBytes(new byte[16]);
    assertThrows(IndexOutOfBoundsException.class,
        () -> CsmPacketUtils.readBoundedCount(buf, Integer.MAX_VALUE, 2));
  }

  // endregion
}
