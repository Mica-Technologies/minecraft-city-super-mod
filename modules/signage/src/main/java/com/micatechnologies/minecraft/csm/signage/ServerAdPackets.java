package com.micatechnologies.minecraft.csm.signage;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * The three packets that carry a server's ads to its clients.
 *
 * <ol>
 *   <li>{@link Catalogue}, server to client on joining: what the server offers -- id, name, hash,
 *       size -- and nothing else. Always sent, empty included, so a client never keeps the last
 *       server's list.</li>
 *   <li>{@link Request}, client to server: send me ad {@code n}. A client asks only when a board
 *       on its screen shows that ad and its cache does not already hold it, one at a time.</li>
 *   <li>{@link Chunk}, server to client: a piece of that ad's file, in order. An empty chunk with
 *       a total of 0 means refused.</li>
 * </ol>
 *
 * <p>Every read is bounded before it allocates, as PERFORMANCE_AND_SECURITY.md requires: FML
 * decodes a packet on the server even when it is meant for the client, so a hostile client could
 * send any of the three.</p>
 */
public final class ServerAdPackets {

  private ServerAdPackets() {
  }

  static void writeString(ByteBuf buf, String s) {
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    buf.writeInt(bytes.length);
    buf.writeBytes(bytes);
  }

  /** What a server offers. */
  public static class Catalogue implements IMessage {

    List<ServerAds.Ad> ads = new ArrayList<>();

    public Catalogue() {
    }

    Catalogue(List<ServerAds.Ad> ads) {
      this.ads = ads;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
      // Minimum encoding per ad: two length ints, the hash, two shorts and an int.
      int n = CsmPacketUtils.readBoundedCount(buf, ServerAds.MAX_ADS, 4 + 4 + 32 + 2 + 2 + 4);
      ads = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        String id = CsmPacketUtils.readBoundedString(buf, ServerAds.MAX_ID);
        String name = CsmPacketUtils.readBoundedString(buf, ServerAds.MAX_NAME * 4);
        byte[] hash = new byte[32];
        buf.readBytes(hash);
        int w = buf.readUnsignedShort();
        int h = buf.readUnsignedShort();
        int length = buf.readInt();
        ads.add(new ServerAds.Ad(id, ServerAds.clean(name), hash, w, h, length, null));
      }
    }

    @Override
    public void toBytes(ByteBuf buf) {
      buf.writeInt(ads.size());
      for (ServerAds.Ad ad : ads) {
        writeString(buf, ad.id);
        writeString(buf, ad.name);
        buf.writeBytes(ad.hash);
        buf.writeShort(ad.width);
        buf.writeShort(ad.height);
        buf.writeInt(ad.length);
      }
    }
  }

  /** Handles a catalogue on the client. */
  public static class CatalogueHandler implements IMessageHandler<Catalogue, IMessage> {

    @Override
    public IMessage onMessage(Catalogue message, MessageContext ctx) {
      Minecraft.getMinecraft().addScheduledTask(() -> ServerAdImages.onCatalogue(message.ads));
      return null;
    }
  }

  /** A client asking for an ad. */
  public static class Request implements IMessage {

    int index;

    public Request() {
    }

    Request(int index) {
      this.index = index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
      index = buf.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
      buf.writeByte(index);
    }
  }

  /** Answers a request on the server. */
  public static class RequestHandler implements IMessageHandler<Request, IMessage> {

    @Override
    public IMessage onMessage(Request message, MessageContext ctx) {
      EntityPlayerMP player = ctx.getServerHandler().player;
      player.server.addScheduledTask(() -> ServerAdSync.serve(player, message.index));
      return null;
    }
  }

  /** A piece of an ad's file. */
  public static class Chunk implements IMessage {

    int index;
    int offset;
    int total;
    byte[] bytes = new byte[0];

    public Chunk() {
    }

    Chunk(int index, int offset, int total, byte[] bytes) {
      this.index = index;
      this.offset = offset;
      this.total = total;
      this.bytes = bytes;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
      index = buf.readUnsignedByte();
      offset = buf.readInt();
      total = buf.readInt();
      bytes = CsmPacketUtils.readBoundedBytes(buf, ServerAds.CHUNK);
    }

    @Override
    public void toBytes(ByteBuf buf) {
      buf.writeByte(index);
      buf.writeInt(offset);
      buf.writeInt(total);
      buf.writeInt(bytes.length);
      buf.writeBytes(bytes);
    }
  }

  /** Takes a chunk on the client. */
  public static class ChunkHandler implements IMessageHandler<Chunk, IMessage> {

    @Override
    public IMessage onMessage(Chunk message, MessageContext ctx) {
      Minecraft.getMinecraft().addScheduledTask(() -> ServerAdImages.onChunk(message.index,
          message.offset, message.total, message.bytes));
      return null;
    }
  }
}
