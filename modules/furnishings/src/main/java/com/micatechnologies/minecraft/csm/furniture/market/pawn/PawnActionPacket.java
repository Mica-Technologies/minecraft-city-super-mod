package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A button on a pawn counter's screens. The server runs it on its own thread only if the player
 * can reach the counter and, for what acts on a screen's slots, still has that counter's screen
 * open; the resale switch is for {@value PawnPermissions#NODE_ADMIN} only.
 *
 * @since 2026.10
 */
public class PawnActionPacket implements IMessage {

  /** Pawn what is in the offer slots. */
  public static final int ACCEPT = 0;
  /** Resale on or off. */
  public static final int TOGGLE_RESALE = 1;
  /** Open the resale screen. */
  public static final int OPEN_RESALE = 2;
  /** Back to the pawn screen. */
  public static final int OPEN_PAWN = 3;
  /** Buy from resale slot {@code slot}: one item, or the whole stack with {@code whole}. */
  public static final int BUY = 4;

  private BlockPos pos;
  private int action;
  private int slot;
  private boolean whole;

  public PawnActionPacket() {
  }

  public PawnActionPacket(BlockPos pos, int action, int slot, boolean whole) {
    this.pos = pos;
    this.action = action;
    this.slot = slot;
    this.whole = whole;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    action = buf.readByte();
    slot = buf.readByte();
    whole = buf.readBoolean();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(action);
    buf.writeByte(slot);
    buf.writeBoolean(whole);
  }

  /** Server side. */
  public static class Handler implements IMessageHandler<PawnActionPacket, IMessage> {

    @Override
    public IMessage onMessage(PawnActionPacket message, MessageContext ctx) {
      EntityPlayerMP player = ctx.getServerHandler().player;
      player.server.addScheduledTask(() -> handle(player, message));
      return null;
    }

    private static void handle(EntityPlayerMP player, PawnActionPacket m) {
      if (!CsmPacketUtils.canPlayerReach(player, m.pos)) {
        return;
      }
      TileEntity te = player.world.getTileEntity(m.pos);
      if (!(te instanceof TileEntityPawnCounter)) {
        return;
      }
      TileEntityPawnCounter counter = (TileEntityPawnCounter) te;
      Container open = player.openContainer;
      switch (m.action) {
        case ACCEPT:
          if (open instanceof ContainerPawnCounter
              && ((ContainerPawnCounter) open).getCounter() == counter) {
            ((ContainerPawnCounter) open).accept(player);
          }
          return;
        case TOGGLE_RESALE:
          if (PawnPermissions.isAdmin(player)) {
            counter.setResale(!counter.isResale());
            if (open instanceof ContainerPawnCounter) {
              ((ContainerPawnCounter) open).requote();
            }
          }
          return;
        case OPEN_RESALE:
          if (counter.isResale()) {
            BlockPawnCounter.open(player, m.pos, BlockPawnCounter.GUI_RESALE);
          }
          return;
        case OPEN_PAWN:
          BlockPawnCounter.open(player, m.pos, BlockPawnCounter.GUI_PAWN);
          return;
        case BUY:
          if (open instanceof ContainerPawnResale
              && ((ContainerPawnResale) open).getCounter() == counter) {
            PawnResale.buy(player, counter, m.slot, m.whole);
          }
          return;
        default:
          break;
      }
    }
  }
}
