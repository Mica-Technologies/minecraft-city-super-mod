package com.micatechnologies.minecraft.csm.technology;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client → server packet sent when the player closes the computer GUI. Carries the notepad
 * text and a {@code shutdown} flag — when set, the server flips the block's POWERED property
 * to false on top of saving the text, giving the GUI's "Shutdown" button a single round trip
 * instead of two.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class ComputerNotepadPacket implements IMessage {

  private BlockPos pos;
  private String notepadText;
  private boolean shutdown;

  public ComputerNotepadPacket() {
    // Required by Forge
  }

  public ComputerNotepadPacket(BlockPos pos, String notepadText, boolean shutdown) {
    this.pos = pos;
    this.notepadText = notepadText == null ? "" : notepadText;
    this.shutdown = shutdown;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.notepadText = ByteBufUtils.readUTF8String(buf);
    this.shutdown = buf.readBoolean();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    ByteBufUtils.writeUTF8String(buf, notepadText);
    buf.writeBoolean(shutdown);
  }

  public BlockPos getPos() {
    return pos;
  }

  public String getNotepadText() {
    return notepadText;
  }

  public boolean isShutdown() {
    return shutdown;
  }
}
