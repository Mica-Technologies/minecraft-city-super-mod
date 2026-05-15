package com.micatechnologies.minecraft.csm.technology;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link ComputerNotepadPacket}. Stores the notepad text and, if the
 * shutdown flag is set, flips the block's POWERED property off. Runs on the next server tick
 * so we don't mutate world state from the netty thread.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class ComputerNotepadHandler
    implements IMessageHandler<ComputerNotepadPacket, IMessage> {

  @Override
  public IMessage onMessage(ComputerNotepadPacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World world = ctx.getServerHandler().player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (te instanceof TileEntityComputer) {
        ((TileEntityComputer) te).setNotepadText(message.getNotepadText());
      }
      if (message.isShutdown()) {
        IBlockState state = world.getBlockState(message.getPos());
        if (state.getBlock() instanceof AbstractBlockPoweredComputer) {
          world.setBlockState(message.getPos(),
              state.withProperty(AbstractBlockPoweredComputer.POWERED, false), 3);
        }
      }
    });
    return null;
  }
}
