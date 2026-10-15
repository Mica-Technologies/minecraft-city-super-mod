package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A button pressed on a shop's screen: buy, sell back, or one of the owner's settings. The server
 * runs it on its own thread only if the player can reach the shop, and checks who they are for
 * every setting ({@link ShopPermissions}); prices and amounts are clamped by the shop.
 *
 * @since 2026.10
 */
public class ShopActionPacket implements IMessage {

  /** Buy one sale's worth. */
  public static final int BUY = 0;
  /** Sell one buy-back's worth. */
  public static final int SELL = 1;
  /** Set the sale: {@code count} items for {@code value}. */
  public static final int SET_SELL = 2;
  /** Set the buy-back: {@code count} items for {@code value} (zero: none). */
  public static final int SET_BUY = 3;
  /** Empty the till. */
  public static final int WITHDRAW = 4;
  /** Put {@code value} into the till. */
  public static final int DEPOSIT = 5;
  /** Decorative on ({@code count} 1) or off. */
  public static final int SET_DECORATIVE = 6;
  /** Unlimited stock on or off. */
  public static final int SET_INFINITE = 7;
  /** Server shop on or off. */
  public static final int SET_SERVER = 8;
  /** Send the status again. */
  public static final int REFRESH = 9;
  /** Open the setup screen (from the counter, for a shop's managers). */
  public static final int OPEN_SETUP = 10;

  private BlockPos pos;
  private int action;
  private int count;
  private double value;

  public ShopActionPacket() {
  }

  public ShopActionPacket(BlockPos pos, int action, int count, double value) {
    this.pos = pos;
    this.action = action;
    this.count = count;
    this.value = value;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    action = buf.readByte();
    count = buf.readInt();
    value = buf.readDouble();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(action);
    buf.writeInt(count);
    buf.writeDouble(value);
  }

  /** Server side. */
  public static class Handler implements IMessageHandler<ShopActionPacket, IMessage> {

    @Override
    public IMessage onMessage(ShopActionPacket message, MessageContext ctx) {
      EntityPlayerMP player = ctx.getServerHandler().player;
      player.server.addScheduledTask(() -> handle(player, message));
      return null;
    }

    private static void handle(EntityPlayerMP player, ShopActionPacket m) {
      if (!CsmPacketUtils.canPlayerReach(player, m.pos)) {
        return;
      }
      TileEntity te = player.world.getTileEntity(m.pos);
      if (!(te instanceof TileEntityShop)) {
        return;
      }
      TileEntityShop shop = (TileEntityShop) te;
      switch (m.action) {
        case BUY:
          ShopTrade.buy(player, shop);
          return;
        case SELL:
          ShopTrade.sell(player, shop);
          return;
        case WITHDRAW:
          ShopTrade.withdraw(player, shop);
          return;
        case DEPOSIT:
          ShopTrade.deposit(player, shop, m.value);
          return;
        case REFRESH:
          ShopStatusPacket.send(player, shop);
          return;
        case OPEN_SETUP:
          if (ShopPermissions.mayManage(player, shop)) {
            ShopBlocks.openScreen(player, shop, true);
          }
          return;
        default:
          break;
      }
      // Settings: the shop's managers only.
      if (!ShopPermissions.mayManage(player, shop)) {
        return;
      }
      switch (m.action) {
        case SET_SELL:
          shop.setSell(shop.isMoneyMode(), m.count, m.value);
          break;
        case SET_BUY:
          shop.setBuy(shop.isMoneyMode(), m.count, m.value);
          break;
        case SET_DECORATIVE:
          shop.setDecorative(m.count != 0);
          break;
        case SET_INFINITE:
          if (!ShopPermissions.canInfinite(player)) {
            return;
          }
          shop.setInfinite(m.count != 0);
          break;
        case SET_SERVER:
          if (!ShopPermissions.canServer(player)) {
            return;
          }
          if (m.count == 0 && shop.isServerShop()) {
            // A server shop handed back to a player starts with an empty till.
            shop.setServerShop(false, player);
          } else if (m.count != 0 && !shop.isServerShop()) {
            // Never move an owner's takings to whoever flips the switch: they collect first.
            if (shop.getTillMoney() > 0 || shop.getTillEmeralds() > 0) {
              ShopTrade.tell(player, "csm.shop.msg.till_not_empty");
              return;
            }
            shop.setServerShop(true, player);
          }
          break;
        default:
          return;
      }
      shop.sync();
      ShopStatusPacket.send(player, shop);
    }
  }
}
