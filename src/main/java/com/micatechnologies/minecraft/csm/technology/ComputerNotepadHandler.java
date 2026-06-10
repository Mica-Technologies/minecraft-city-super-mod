package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
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

  /** Maximum stored notepad length; text rides TE NBT into chunk syncs and world saves. */
  public static final int MAX_NOTEPAD_LENGTH = 16_000;

  @Override
  public IMessage onMessage(ComputerNotepadPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (te instanceof TileEntityComputer) {
        String text = message.getNotepadText();
        if (text != null && text.length() > MAX_NOTEPAD_LENGTH) {
          text = text.substring(0, MAX_NOTEPAD_LENGTH);
        }
        ((TileEntityComputer) te).setNotepadText(text);
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
