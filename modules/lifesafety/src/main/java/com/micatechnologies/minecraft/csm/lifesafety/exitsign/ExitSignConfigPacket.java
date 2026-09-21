package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client to server: set the exit sign at {@code pos} to a new setup, sent whole each time the
 * player changes an option in {@link GuiExitSign}. The setup travels as
 * {@link ExitSignConfig#pack}, eighteen bits in one int, so there is nothing to bound on decode.
 *
 * @since 2026.9
 */
public class ExitSignConfigPacket implements IMessage {

  private BlockPos pos;
  private int packedConfig;

  public ExitSignConfigPacket() {
  }

  public ExitSignConfigPacket(BlockPos pos, ExitSignConfig config) {
    this.pos = pos;
    this.packedConfig = config.pack();
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.packedConfig = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeInt(packedConfig);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getPackedConfig() {
    return packedConfig;
  }
}
