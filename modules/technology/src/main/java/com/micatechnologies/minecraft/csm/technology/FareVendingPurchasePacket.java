package com.micatechnologies.minecraft.csm.technology;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client → server packet sent when the player presses a purchase button in the
 * {@link FareVendingGui}. Carries the vending-machine block position (so the server can
 * verify the player is still nearby) and the ordinal of the {@link FareVendingPurchase}
 * the player selected.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class FareVendingPurchasePacket implements IMessage {

  private BlockPos vendingPos;
  private int purchaseOrdinal;

  public FareVendingPurchasePacket() {
    // Required by Forge
  }

  public FareVendingPurchasePacket(BlockPos vendingPos, FareVendingPurchase purchase) {
    this.vendingPos = vendingPos;
    this.purchaseOrdinal = purchase.ordinal();
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.vendingPos = BlockPos.fromLong(buf.readLong());
    this.purchaseOrdinal = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(vendingPos.toLong());
    buf.writeInt(purchaseOrdinal);
  }

  public BlockPos getVendingPos() {
    return vendingPos;
  }

  public int getPurchaseOrdinal() {
    return purchaseOrdinal;
  }
}
