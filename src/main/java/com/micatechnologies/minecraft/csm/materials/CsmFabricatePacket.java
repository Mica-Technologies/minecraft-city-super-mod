package com.micatechnologies.minecraft.csm.materials;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client to server packet sent when the player confirms a selection in the Fabricator picker.
 *
 * <p>Carries the Fabricator's position, so the server can verify the player is still standing at
 * it, the registry name of the block to produce, and how many to make. The block is identified by
 * registry name rather than by an index into the picker list: the list is built from a client-side
 * registry iteration and its ordering must never be something the server trusts.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public class CsmFabricatePacket implements IMessage {

  private BlockPos fabricatorPos;
  private String blockRegistryName;
  private int quantity;

  public CsmFabricatePacket() {
    // Required by Forge
  }

  public CsmFabricatePacket(BlockPos fabricatorPos, String blockRegistryName, int quantity) {
    this.fabricatorPos = fabricatorPos;
    this.blockRegistryName = blockRegistryName;
    this.quantity = quantity;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.fabricatorPos = BlockPos.fromLong(buf.readLong());
    this.blockRegistryName = ByteBufUtils.readUTF8String(buf);
    this.quantity = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(fabricatorPos.toLong());
    ByteBufUtils.writeUTF8String(buf, blockRegistryName);
    buf.writeInt(quantity);
  }

  public BlockPos getFabricatorPos() {
    return fabricatorPos;
  }

  public String getBlockRegistryName() {
    return blockRegistryName;
  }

  public int getQuantity() {
    return quantity;
  }
}
