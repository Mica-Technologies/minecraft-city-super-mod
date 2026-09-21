package com.micatechnologies.minecraft.csm.signage;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ServerAdsTest {

  private static byte[] png(int w, int h) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "png", out);
    return out.toByteArray();
  }

  @Test
  void theHeaderGivesTheSizeOfARealPng() throws Exception {
    assertArrayEquals(new int[]{40, 20}, ServerAds.pngSize(png(40, 20)));
    assertArrayEquals(new int[]{1024, 1024}, ServerAds.pngSize(png(1024, 1024)));
  }

  @Test
  void aTinyFileClaimingAHugeImageIsRefusedBeforeDecoding() throws Exception {
    byte[] bytes = png(4, 4);
    // Rewrite the IHDR width to 100,000 pixels: a decompression bomb's header.
    bytes[16] = 0;
    bytes[17] = 1;
    bytes[18] = (byte) 0x86;
    bytes[19] = (byte) 0xA0;
    assertNull(ServerAds.pngSize(bytes));
    assertNull(ServerAds.pngSize(png(1025, 8)));
  }

  @Test
  void anythingThatIsNotAPngIsRefused() {
    assertNull(ServerAds.pngSize(null));
    assertNull(ServerAds.pngSize(new byte[10]));
    assertNull(ServerAds.pngSize("GIF89a not a png at all, not at all....".getBytes()));
  }

  @Test
  void idsAreMadeSafeFromFileNames() {
    assertEquals("server_my_ad", ServerAds.idFor("My Ad!.PNG"));
    assertEquals("server_evil", ServerAds.idFor("../../evil.png"));
    assertEquals("server_ad", ServerAds.idFor("!!!.png"));
    assertEquals(ServerAds.MAX_ID, ServerAds.idFor(repeat('a', 200) + ".png").length());
    assertTrue(ServerAds.validId(ServerAds.idFor("Cafe Deluxe.png")));
    assertFalse(ServerAds.validId("cube_burger"));
    assertFalse(ServerAds.validId("server_../x"));
    assertFalse(ServerAds.validId("server_"));
  }

  @Test
  void namesLoseFormattingCodesAndControlCharacters() {
    assertEquals("Red text", ServerAds.clean("§cRed\u0000 text"));
    assertEquals(ServerAds.MAX_NAME, ServerAds.clean(repeat('x', 500)).length());
    assertEquals("Joe's Diner", ServerAds.nameFor("Joe's Diner.png"));
  }

  @Test
  void aCatalogueSurvivesTheWire() {
    byte[] hash = new byte[32];
    Arrays.fill(hash, (byte) 7);
    ServerAds.Ad ad = new ServerAds.Ad("server_diner", "Diner", hash, 512, 256, 1234, null);
    ByteBuf buf = Unpooled.buffer();
    new ServerAdPackets.Catalogue(Collections.singletonList(ad)).toBytes(buf);
    ServerAdPackets.Catalogue back = new ServerAdPackets.Catalogue();
    back.fromBytes(buf);
    assertEquals(1, back.ads.size());
    assertEquals("server_diner", back.ads.get(0).id);
    assertEquals(512, back.ads.get(0).width);
    assertEquals(1234, back.ads.get(0).length);
    assertArrayEquals(hash, back.ads.get(0).hash);
  }

  @Test
  void aChunkClaimingMoreThanItsLimitIsRejectedBeforeAllocating() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeByte(0);
    buf.writeInt(0);
    buf.writeInt(100);
    buf.writeInt(ServerAds.CHUNK + 1);
    assertThrows(IndexOutOfBoundsException.class, () -> new ServerAdPackets.Chunk().fromBytes(buf));

    ByteBuf count = Unpooled.buffer();
    count.writeInt(ServerAds.MAX_ADS + 1);
    assertThrows(IndexOutOfBoundsException.class,
        () -> new ServerAdPackets.Catalogue().fromBytes(count));
  }

  @Test
  void aServerAdTakesTheShapeNearestItsOwn() {
    ServerAds.Ad wide = new ServerAds.Ad("server_wide", "Wide", new byte[32], 1000, 280, 10, null);
    AdEntry entry = AdEntry.server(wide);
    assertTrue(entry.isServer());
    assertEquals(Collections.singleton(AdShape.BULLETIN), entry.getShapes());
    assertEquals("csm:server_ads/" + wide.hex(), entry.texture(AdShape.SQUARE).toString());
  }

  private static String repeat(char c, int n) {
    char[] chars = new char[n];
    Arrays.fill(chars, c);
    return new String(chars);
  }
}
