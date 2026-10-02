package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Server to client: a bell controller's announcement, to be spoken through Core's speech service.
 * The text is bounded on decode (it drives the client's speech synthesiser, and a packet class is
 * decoded on whichever side receives it) and the handler speaks at most one a second.
 *
 * @since 2026.10
 */
public class BellAnnouncePacket implements IMessage {

  /** Bytes of text a packet may carry: the schedule's limit in the widest UTF-8. */
  static final int MAX_TEXT_BYTES = BellSchedule.MAX_TEXT * 4;

  private String text;

  public BellAnnouncePacket() {
    // Required by Forge
  }

  public BellAnnouncePacket(String text) {
    this.text = BellSchedule.cleanText(text);
  }

  public String getText() {
    return text;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    text = BellSchedule.cleanText(CsmPacketUtils.readBoundedString(buf, MAX_TEXT_BYTES));
  }

  @Override
  public void toBytes(ByteBuf buf) {
    byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
    buf.writeInt(bytes.length);
    buf.writeBytes(bytes);
  }
}
