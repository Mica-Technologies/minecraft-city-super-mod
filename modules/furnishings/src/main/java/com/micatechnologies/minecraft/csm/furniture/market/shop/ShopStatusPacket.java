package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.codeutils.economy.CsmEmeralds;
import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import io.netty.buffer.ByteBuf;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What one player's shop screen shows that the shop's own sync does not carry, sent to that
 * player when the screen opens and after each trade: their balance (money or emeralds held), the
 * exact stock, the till, and which of the setup screen's switches they may use. Nothing here is
 * trusted back: the server checks everything again when a button is pressed.
 *
 * @since 2026.10
 */
public class ShopStatusPacket implements IMessage {

  private static final int CAN_MANAGE = 1;
  private static final int CAN_INFINITE = 2;
  private static final int CAN_SERVER = 4;

  private BlockPos pos;
  /** The player's money, or NaN if it cannot be read (or emerald mode). */
  private double balance;
  private int emeralds;
  private int stock;
  private double tillMoney;
  private long tillEmeralds;
  private int flags;

  public ShopStatusPacket() {
  }

  /** Sends {@code player} the status of {@code shop}. */
  public static void send(EntityPlayerMP player, TileEntityShop shop) {
    ShopStatusPacket p = new ShopStatusPacket();
    p.pos = shop.getPos();
    OptionalDouble wallet = ShopEconomy.isMoneyMode() ? ShopEconomy.SUM.balance(player)
        : OptionalDouble.empty();
    p.balance = wallet.isPresent() ? wallet.getAsDouble() : Double.NaN;
    p.emeralds = CsmEmeralds.count(player);
    p.stock = shop.isInfinite() ? Integer.MAX_VALUE : shop.countStock();
    boolean manage = ShopPermissions.mayManage(player, shop);
    p.tillMoney = manage ? shop.getTillMoney() : 0;
    p.tillEmeralds = manage ? shop.getTillEmeralds() : 0;
    p.flags = (manage ? CAN_MANAGE : 0)
        | (ShopPermissions.canInfinite(player) ? CAN_INFINITE : 0)
        | (ShopPermissions.canServer(player) ? CAN_SERVER : 0);
    CsmFurnishings.NETWORK.sendTo(p, player);
    // A trade changes the player's inventory while a shop screen is open, and vanilla syncs only
    // the open container (the counter's has no slots), so send the inventory's own.
    player.inventoryContainer.detectAndSendChanges();
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    balance = buf.readDouble();
    emeralds = buf.readInt();
    stock = buf.readInt();
    tillMoney = buf.readDouble();
    tillEmeralds = buf.readLong();
    flags = buf.readByte();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeDouble(balance);
    buf.writeInt(emeralds);
    buf.writeInt(stock);
    buf.writeDouble(tillMoney);
    buf.writeLong(tillEmeralds);
    buf.writeByte(flags);
  }

  public BlockPos getPos() {
    return pos;
  }

  public double getBalance() {
    return balance;
  }

  public int getEmeralds() {
    return emeralds;
  }

  public int getStock() {
    return stock;
  }

  public double getTillMoney() {
    return tillMoney;
  }

  public long getTillEmeralds() {
    return tillEmeralds;
  }

  public boolean canManage() {
    return (flags & CAN_MANAGE) != 0;
  }

  public boolean canInfinite() {
    return (flags & CAN_INFINITE) != 0;
  }

  public boolean canServer() {
    return (flags & CAN_SERVER) != 0;
  }

  /** Client side: keeps the latest status for the open screen. */
  public static class Handler implements IMessageHandler<ShopStatusPacket, IMessage> {

    @Override
    public IMessage onMessage(ShopStatusPacket message, MessageContext ctx) {
      Minecraft.getMinecraft().addScheduledTask(() -> receive(message));
      return null;
    }

    @SideOnly(Side.CLIENT)
    private static void receive(ShopStatusPacket message) {
      ShopClientStatus.set(message);
    }
  }
}
