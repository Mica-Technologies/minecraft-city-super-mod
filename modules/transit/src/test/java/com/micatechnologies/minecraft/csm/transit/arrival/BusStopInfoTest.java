package com.micatechnologies.minecraft.csm.transit.arrival;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.transit.stop.TransitRoute;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * A board's data must survive a save and the wire unchanged, take its defaults from an empty
 * compound, and hold its caps whichever way it arrives: a hostile packet can claim a huge string,
 * a bad agency, or text the screen's font cannot draw.
 */
class BusStopInfoTest {

  private static BusStopInfo live() {
    BusStopInfo info = new BusStopInfo();
    info.setAgency(BoardAgency.CITYLINE);
    info.setStopName("Riverside");
    info.setDirection("Northbound");
    info.setRoutes("B 30");
    return info;
  }

  @Test
  void defaultsFromAnEmptyCompound() {
    BusStopInfo info = new BusStopInfo();
    info.readFrom(new NBTTagCompound());
    assertEquals(BoardAgency.CITYLINE, info.getAgency());
    assertEquals("", info.getStopName());
    assertEquals("", info.getDirection());
    assertEquals("", info.getRoutes());
    assertEquals(0, info.getRouteKeys().length);
    assertEquals("", info.getDestination(0));
    assertEquals("", info.getDestination(99));
    assertFalse(info.isClosed());
    assertEquals(BusStopInfo.DEFAULT_CUSTOM_GROUND, info.getCustomGround());
    assertEquals(BusStopInfo.DEFAULT_CUSTOM_LEGEND, info.getCustomLegend());
    // two-sided follows the mount until it is set
    assertFalse(info.isTwoSidedSet());
    assertTrue(info.isTwoSided(false));
    assertFalse(info.isTwoSided(true));
  }

  @Test
  void nbtRoundTrip() {
    BusStopInfo info = live();
    info.setAgency(BoardAgency.CUSTOM);
    info.setCustomName("Harbour Bus");
    info.setCustomGround(0x123456);
    info.setCustomLegend(0xFEDCBA);
    info.setDestination(0, "Upper West Side");
    info.setDestination(3, "Harbor");
    info.setClosed(true);
    info.setTwoSided(false);
    NBTTagCompound nbt = new NBTTagCompound();
    info.writeTo(nbt);
    assertEquals(BoardAgency.CUSTOM.ordinal(), nbt.getByte(BusStopInfo.KEY_AGENCY));
    assertEquals("Riverside", nbt.getString(BusStopInfo.KEY_STOP_NAME));

    BusStopInfo back = new BusStopInfo();
    back.readFrom(nbt);
    assertSame(info, back);
    assertTrue(back.isTwoSidedSet());
    assertFalse(back.isTwoSided(false));
  }

  @Test
  void unsetTwoSidedIsNotWritten() {
    NBTTagCompound nbt = new NBTTagCompound();
    live().writeTo(nbt);
    assertFalse(nbt.hasKey(BusStopInfo.KEY_TWO_SIDED));
  }

  @Test
  void unknownAgencyReadsAsCityline() {
    NBTTagCompound nbt = new NBTTagCompound();
    nbt.setByte(BusStopInfo.KEY_AGENCY, (byte) 99);
    BusStopInfo info = new BusStopInfo();
    info.setAgency(BoardAgency.METRO);
    info.readFrom(nbt);
    assertEquals(BoardAgency.CITYLINE, info.getAgency());
  }

  @Test
  void capsHoldFromASave() {
    NBTTagCompound nbt = new NBTTagCompound();
    nbt.setString(BusStopInfo.KEY_STOP_NAME, repeat('x', 200));
    nbt.setString(BusStopInfo.KEY_DIRECTION, repeat('d', 200));
    nbt.setString(BusStopInfo.KEY_ROUTES, repeat('r', 200));
    nbt.setString(BusStopInfo.KEY_CUSTOM_NAME, repeat('n', 200));
    nbt.setInteger(BusStopInfo.KEY_CUSTOM_GROUND, 0xFF123456);
    BusStopInfo info = new BusStopInfo();
    info.readFrom(nbt);
    assertEquals(BusStopInfo.MAX_STOP_NAME, info.getStopName().length());
    assertEquals(BusStopInfo.MAX_DIRECTION, info.getDirection().length());
    assertEquals(BusStopInfo.MAX_ROUTES, info.getRoutes().length());
    assertEquals(BusStopInfo.MAX_AGENCY_NAME, info.getCustomName().length());
    assertEquals(0x123456, info.getCustomGround());
  }

  @Test
  void textIsPrintableAscii() {
    assertEquals("Upper West", BusStopInfo.clean("  Upper\u0000 Westé\n ", 32));
    assertEquals("", BusStopInfo.clean(null, 32));
    assertEquals("abc", BusStopInfo.clean("abc   def", 4));
  }

  @Test
  void routesParseIncludingLetters() {
    BusStopInfo info = live();
    assertArrayEquals(new int[]{TransitRoute.parse("B"), 30}, info.getRouteKeys());
  }

  @Test
  void versionMovesOnlyOnChange() {
    BusStopInfo info = live();
    int v = info.getVersion();
    info.setStopName("Riverside");
    info.setClosed(false);
    assertEquals(v, info.getVersion());
    info.setClosed(true);
    assertNotEquals(v, info.getVersion());
  }

  @Test
  void wireRoundTrip() {
    BusStopInfo info = live();
    info.setAgency(BoardAgency.METRO);
    info.setDestination(1, "Lakeside");
    info.setTwoSided(true);
    ByteBuf buf = Unpooled.buffer();
    info.writeTo(buf);
    BusStopInfo back = BusStopInfo.readFrom(buf);
    assertSame(info, back);
    assertEquals(0, buf.readableBytes());

    // unset two-sided stays unset over the wire
    ByteBuf buf2 = Unpooled.buffer();
    live().writeTo(buf2);
    assertFalse(BusStopInfo.readFrom(buf2).isTwoSidedSet());
  }

  @Test
  void wireRejectsABadAgency() {
    ByteBuf buf = Unpooled.buffer();
    live().writeTo(buf);
    buf.setByte(0, 6);
    assertThrows(IndexOutOfBoundsException.class, () -> BusStopInfo.readFrom(buf));
  }

  @Test
  void wireRejectsAnOverlongString() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeByte(0);
    byte[] big = repeat('x', BusStopInfo.MAX_WIRE_BYTES + 1).getBytes(StandardCharsets.UTF_8);
    buf.writeInt(big.length);
    buf.writeBytes(big);
    assertThrows(IndexOutOfBoundsException.class, () -> BusStopInfo.readFrom(buf));
  }

  @Test
  void wireRejectsALyingLength() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeByte(0);
    buf.writeInt(100);   // claims 100 bytes, sends 3
    buf.writeBytes(new byte[]{'a', 'b', 'c'});
    assertThrows(IndexOutOfBoundsException.class, () -> BusStopInfo.readFrom(buf));
    ByteBuf neg = Unpooled.buffer();
    neg.writeByte(0);
    neg.writeInt(-1);
    assertThrows(IndexOutOfBoundsException.class, () -> BusStopInfo.readFrom(neg));
  }

  @Test
  void oldShowDestinationsSwitchIsIgnoredAndDropped() {
    NBTTagCompound nbt = new NBTTagCompound();
    nbt.setBoolean(BusStopInfo.LEGACY_KEY_SHOW_DESTINATIONS, true);
    nbt.setString(BusStopInfo.KEY_STOP_NAME, "Riverside");
    BusStopInfo info = new BusStopInfo();
    info.readFrom(nbt);
    assertEquals("Riverside", info.getStopName());
    assertEquals("", info.getDestination(0));
    NBTTagCompound out = new NBTTagCompound();
    out.setBoolean(BusStopInfo.LEGACY_KEY_SHOW_DESTINATIONS, true);
    info.writeTo(out);
    assertFalse(out.hasKey(BusStopInfo.LEGACY_KEY_SHOW_DESTINATIONS));
    assertFalse(out.hasKey(BusStopInfo.KEY_DESTINATIONS));
  }

  @Test
  void destinationsAreCappedFromASave() {
    NBTTagCompound nbt = new NBTTagCompound();
    net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
    for (int i = 0; i < 10; i++) {
      list.appendTag(new net.minecraft.nbt.NBTTagString(repeat('d', 40)));
    }
    nbt.setTag(BusStopInfo.KEY_DESTINATIONS, list);
    BusStopInfo info = new BusStopInfo();
    info.readFrom(nbt);
    assertEquals(BusStopInfo.MAX_DESTINATION, info.getDestination(0).length());
    assertEquals(BusStopInfo.MAX_DESTINATION,
        info.getDestination(TransitRoute.MAX_PER_LINE - 1).length());
  }

  @Test
  void wireRejectsTooManyDestinations() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeByte(0);
    BusStopInfo.writeString(buf, "");
    buf.writeInt(0);
    buf.writeInt(0);
    BusStopInfo.writeString(buf, "");
    BusStopInfo.writeString(buf, "");
    BusStopInfo.writeString(buf, "");
    buf.writeByte(TransitRoute.MAX_PER_LINE + 1);
    assertThrows(IndexOutOfBoundsException.class, () -> BusStopInfo.readFrom(buf));
  }

  @Test
  void wireCapsADestination() {
    BusStopInfo info = live();
    ByteBuf buf = Unpooled.buffer();
    info.writeTo(buf);
    BusStopInfo back = BusStopInfo.readFrom(buf);
    back.setDestination(0, repeat('x', 40));
    ByteBuf buf2 = Unpooled.buffer();
    back.writeTo(buf2);
    assertEquals(BusStopInfo.MAX_DESTINATION, BusStopInfo.readFrom(buf2).getDestination(0)
        .length());
  }

  @Test
  void wireCapsTextWithinTheByteBound() {
    // 100 characters fit the wire's 128 bytes but not the stop name's cap: cut on arrival
    ByteBuf buf = Unpooled.buffer();
    buf.writeByte(BoardAgency.VERDANT.ordinal());
    BusStopInfo.writeString(buf, "");
    buf.writeInt(0);
    buf.writeInt(0);
    BusStopInfo.writeString(buf, repeat('s', 100));
    BusStopInfo.writeString(buf, "");
    BusStopInfo.writeString(buf, "");
    buf.writeByte(0);   // no destinations
    buf.writeByte(0);   // flags
    BusStopInfo info = BusStopInfo.readFrom(buf);
    assertEquals(BusStopInfo.MAX_STOP_NAME, info.getStopName().length());
    assertEquals(BoardAgency.VERDANT, info.getAgency());
  }

  @Test
  void copyFromReappliesCaps() {
    BusStopInfo from = live();
    BusStopInfo to = new BusStopInfo();
    to.copyFrom(from);
    assertSame(from, to);
  }

  @Test
  void customScreenColourIsTheBrighter() {
    BusStopInfo info = new BusStopInfo();
    info.setAgency(BoardAgency.CUSTOM);
    info.setCustomGround(0x102030);
    info.setCustomLegend(0xF0E0D0);
    assertEquals(0xF0E0D0, BoardAgency.CUSTOM.screenColour(info));
    assertEquals(0xF4F4F4, BoardAgency.METRO.screenColour(info));
    assertEquals("METRO: Riverside", headerOf(BoardAgency.METRO, "Riverside"));
  }

  private static String headerOf(BoardAgency agency, String stop) {
    BusStopInfo info = new BusStopInfo();
    info.setAgency(agency);
    info.setStopName(stop);
    return info.header();
  }

  private static void assertSame(BusStopInfo a, BusStopInfo b) {
    assertEquals(a.getAgency(), b.getAgency());
    assertEquals(a.getCustomName(), b.getCustomName());
    assertEquals(a.getCustomGround(), b.getCustomGround());
    assertEquals(a.getCustomLegend(), b.getCustomLegend());
    assertEquals(a.getStopName(), b.getStopName());
    assertEquals(a.getDirection(), b.getDirection());
    assertEquals(a.getRoutes(), b.getRoutes());
    assertArrayEquals(a.getRouteKeys(), b.getRouteKeys());
    for (int i = 0; i < TransitRoute.MAX_PER_LINE; i++) {
      assertEquals(a.getDestination(i), b.getDestination(i));
    }
    assertEquals(a.isClosed(), b.isClosed());
    assertEquals(a.isTwoSidedSet(), b.isTwoSidedSet());
    assertEquals(a.isTwoSided(false), b.isTwoSided(false));
  }

  private static String repeat(char c, int n) {
    StringBuilder s = new StringBuilder(n);
    for (int i = 0; i < n; i++) {
      s.append(c);
    }
    return s.toString();
  }
}
