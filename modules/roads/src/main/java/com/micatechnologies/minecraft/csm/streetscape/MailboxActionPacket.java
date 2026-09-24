package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Something a player did on a mailbox compartment's screen: claim a free compartment, or (the
 * placer or an operator) hand it to a named player or free it. The server decides whether the
 * player may; see {@link MailboxActionPacketHandler}.
 *
 * @version 1.0
 */
public class MailboxActionPacket implements IMessage {

  public static final int CLAIM = 0;
  public static final int ASSIGN = 1;
  public static final int FREE = 2;

  /** A Minecraft player name is at most 16 characters. */
  private static final int MAX_NAME = 16;

  private BlockPos pos;
  private int compartment;
  private int action;
  private String name;

  public MailboxActionPacket() {
  }

  public MailboxActionPacket(BlockPos pos, int compartment, int action, String name) {
    this.pos = pos;
    this.compartment = compartment;
    this.action = action;
    this.name = name == null ? "" : name;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    compartment = buf.readUnsignedByte();
    action = buf.readByte();
    name = CsmPacketUtils.readBoundedString(buf, MAX_NAME * 4);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(compartment);
    buf.writeByte(action);
    byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
    buf.writeInt(bytes.length);
    buf.writeBytes(bytes);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getCompartment() {
    return compartment;
  }

  public int getAction() {
    return action;
  }

  public String getName() {
    return name;
  }
}
