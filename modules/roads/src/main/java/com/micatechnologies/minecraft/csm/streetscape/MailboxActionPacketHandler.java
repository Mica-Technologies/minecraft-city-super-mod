package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Claims, assigns or frees a mailbox compartment, if the sender may and can reach the box, then
 * reopens the sender's screen in whatever mode now applies (a claim turns a free compartment
 * into the player's own).
 *
 * <p>Claiming needs the compartment free. Assigning and freeing need the placer or an
 * operator. A name is looked up among players online, then among players the server has seen;
 * a name the server has never seen is refused rather than stored, since it could not be tied
 * to a player.</p>
 *
 * @version 1.0
 */
public class MailboxActionPacketHandler implements IMessageHandler<MailboxActionPacket, IMessage> {

  @Override
  public IMessage onMessage(MailboxActionPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> handle(player, message));
    return null;
  }

  private static void handle(EntityPlayerMP player, MailboxActionPacket message) {
    BlockPos pos = message.getPos();
    if (!CsmPacketUtils.canPlayerReach(player, pos)) {
      return;
    }
    TileEntity te = player.world.getTileEntity(pos);
    if (!(te instanceof TileEntityMailbox)) {
      return;
    }
    TileEntityMailbox box = (TileEntityMailbox) te;
    int i = message.getCompartment();
    if (i < 0 || i >= box.getCompartmentCount()) {
      return;
    }
    switch (message.getAction()) {
      case MailboxActionPacket.CLAIM:
        if (box.getOwner(i) != null) {
          return;
        }
        box.setOwner(i, player.getUniqueID(), player.getName());
        break;
      case MailboxActionPacket.ASSIGN: {
        if (!BlockMailbox.mayManage(player, box)) {
          return;
        }
        GameProfile profile = lookUp(player, message.getName());
        if (profile == null) {
          player.sendStatusMessage(new TextComponentString("No player called "
              + message.getName() + " has been on this server"), true);
          return;
        }
        box.setOwner(i, profile.getId(), profile.getName());
        break;
      }
      case MailboxActionPacket.FREE:
        if (!BlockMailbox.mayManage(player, box)) {
          return;
        }
        box.setOwner(i, null, "");
        break;
      default:
        return;
    }
    box.markDirtySync(player.world, pos, true);
    BlockMailbox.openCompartment(player, player.world, pos, i, box);
  }

  private static GameProfile lookUp(EntityPlayerMP player, String name) {
    EntityPlayerMP online = player.server.getPlayerList().getPlayerByUsername(name);
    if (online != null) {
      return online.getGameProfile();
    }
    // Only a name already in the cache: asking the cache for any other name would look it up
    // with Mojang, on the server thread.
    for (String known : player.server.getPlayerProfileCache().getUsernames()) {
      if (known.equalsIgnoreCase(name)) {
        return player.server.getPlayerProfileCache().getGameProfileForUsername(known);
      }
    }
    return null;
  }
}
