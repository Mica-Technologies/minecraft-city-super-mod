package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a {@link GarageKeypadPacket} on the server.
 *
 * <p>The conventions every CSM packet follows (see "Network safety" in
 * {@code assets/docs/PERFORMANCE_AND_SECURITY.md}): the work is scheduled onto the server thread,
 * its first check is that the player can reach the keypad, and the block there must really be a
 * keypad. The code is checked, and the owner for setting one, by the tile entity, which also
 * counts wrong tries; nothing the client says about who owns the keypad is believed.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class GarageKeypadPacketHandler implements IMessageHandler<GarageKeypadPacket, IMessage> {

  @Override
  public IMessage onMessage(GarageKeypadPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      IBlockState state = player.world.getBlockState(message.getPos());
      if (!(state.getBlock() instanceof BlockGarageDoorControl)
          || ((BlockGarageDoorControl) state.getBlock()).kind()
          != BlockGarageDoorControl.Kind.KEYPAD) {
        return;
      }
      TileEntity te = player.world.getTileEntity(message.getPos());
      String entry = message.getEntry();
      if (!(te instanceof TileEntityGarageDoorControl) || entry == null) {
        return;
      }
      TileEntityGarageDoorControl keypad = (TileEntityGarageDoorControl) te;
      if (message.getAction() == GarageKeypadPacket.SET) {
        keypad.setCode(player, entry);
      } else {
        keypad.enter(player, entry);
      }
    });
    return null;
  }
}
