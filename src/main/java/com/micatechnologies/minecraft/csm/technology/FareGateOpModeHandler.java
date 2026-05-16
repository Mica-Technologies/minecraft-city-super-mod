package com.micatechnologies.minecraft.csm.technology;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link FareGateOpModePacket}. Applies the new mode to the
 * targeted {@link TileEntityFareGate} on the next server tick. Validates that the packet
 * sender is within ~6 blocks of the gate (basic anti-replay distance check) and that the
 * targeted block is actually a fare gate.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class FareGateOpModeHandler
    implements IMessageHandler<FareGateOpModePacket, IMessage> {

  private static final double MAX_INTERACT_DIST_SQ = 36.0; // 6 blocks

  @Override
  public IMessage onMessage(FareGateOpModePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> handle(message, player));
    return null;
  }

  private static void handle(FareGateOpModePacket message, EntityPlayerMP player) {
    World world = player.world;
    if (player.getDistanceSq(message.getPos()) > MAX_INTERACT_DIST_SQ) {
      return;
    }
    if (!(world.getBlockState(message.getPos()).getBlock() instanceof BlockFareGate)) {
      return;
    }
    TileEntity te = world.getTileEntity(message.getPos());
    if (!(te instanceof TileEntityFareGate)) {
      return;
    }
    FareGateOpMode mode = FareGateOpMode.fromOrdinal(message.getModeOrdinal());
    ((TileEntityFareGate) te).setOpMode(mode);
  }
}
