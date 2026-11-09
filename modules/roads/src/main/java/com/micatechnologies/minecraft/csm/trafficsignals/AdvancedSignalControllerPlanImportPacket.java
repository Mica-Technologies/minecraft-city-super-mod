package com.micatechnologies.minecraft.csm.trafficsignals;

import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client&rarr;server packet carrying a whole ADVANCED plan as JSON (SPATT's {@code csm-asc3-plan}
 * format), pasted into the CSM ASC-3 panel. The text travels gzip-compressed: a plan is a few
 * kilobytes of JSON and compresses about tenfold, so one packet comfortably stays under the
 * 32 KiB a client-to-server custom payload may carry ({@link #MAX_COMPRESSED_BYTES}); a plan that
 * would not is refused on the client with a message rather than sent in pieces.
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class AdvancedSignalControllerPlanImportPacket implements IMessage {

  /**
   * The most compressed bytes one packet carries, leaving room under the 32767-byte payload cap
   * for the position and length fields and the channel framing.
   */
  public static final int MAX_COMPRESSED_BYTES = 30000;

  /** The most plan text a packet may inflate to, in bytes: the JSON reader's own cap. */
  static final int MAX_INFLATED_BYTES =
      com.micatechnologies.minecraft.csm.trafficsignals.logic.AdvancedPlanJson.MAX_LENGTH * 4;

  private BlockPos pos;
  private byte[] compressed = new byte[0];

  public AdvancedSignalControllerPlanImportPacket() {
  }

  /**
   * @param pos        the controller
   * @param compressed the plan text, gzip-compressed ({@link #compress})
   */
  public AdvancedSignalControllerPlanImportPacket(BlockPos pos, byte[] compressed) {
    this.pos = pos;
    this.compressed = compressed;
  }

  /**
   * Compresses plan text for this packet.
   *
   * @param json the plan text
   *
   * @return the gzip bytes, or {@code null} if they would not fit one packet
   */
  public static byte[] compress(String json) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
      gz.write(json.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      return null;
    }
    byte[] bytes = out.toByteArray();
    return bytes.length <= MAX_COMPRESSED_BYTES ? bytes : null;
  }

  /**
   * Inflates this packet's plan text.
   *
   * @return the plan text, or {@code null} if it is not gzip or inflates past the cap
   */
  public String inflate() {
    try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[4096];
      int total = 0;
      int n;
      while ((n = in.read(buf)) > 0) {
        total += n;
        if (total > MAX_INFLATED_BYTES) {
          return null;
        }
        out.write(buf, 0, n);
      }
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    int length = buf.readInt();
    if (length < 0 || length > MAX_COMPRESSED_BYTES || length > buf.readableBytes()) {
      this.compressed = new byte[0];
      return;
    }
    this.compressed = new byte[length];
    buf.readBytes(this.compressed);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.compressed.length);
    buf.writeBytes(this.compressed);
  }

  public BlockPos getPos() {
    return pos;
  }
}
