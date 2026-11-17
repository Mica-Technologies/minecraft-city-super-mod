package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * An address plaque's whole value, sent from its editor on every change, as the plaque's own NBT
 * ({@link AddressPlaque#write}). The server reads it as it reads a {@code /blockdata}: every
 * string cleaned, every enum by id, every number clamped. The Building module's channel.
 *
 * @since 2026.11
 */
public class AddressPlaquePacket implements IMessage {

  /** The most a plaque's NBT may weigh on the wire, bytes: both lines full and every key. */
  private static final int MAX_NBT = 1024;

  private BlockPos pos;
  private NBTTagCompound plaque;

  public AddressPlaquePacket() {
  }

  public AddressPlaquePacket(BlockPos pos, AddressPlaque plaque) {
    this.pos = pos;
    this.plaque = plaque.write(new NBTTagCompound());
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    if (buf.readableBytes() > MAX_NBT) {
      throw new IllegalArgumentException("address plaque packet too large");
    }
    try {
      plaque = ByteBufUtils.readTag(buf);
    } catch (RuntimeException e) {
      plaque = null;
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    ByteBufUtils.writeTag(buf, plaque);
  }

  /**
   * Applies the edit, if the player can reach the plaque and may change blocks there (not in
   * adventure mode, not inside spawn protection).
   */
  public static class Handler implements IMessageHandler<AddressPlaquePacket, IMessage> {

    @Override
    public IMessage onMessage(AddressPlaquePacket message, MessageContext ctx) {
      EntityPlayerMP player = ctx.getServerHandler().player;
      NBTTagCompound nbt = message.plaque;
      if (nbt == null || message.pos == null) {
        return null;
      }
      player.server.addScheduledTask(() -> {
        BlockPos target = message.pos;
        if (!CsmPacketUtils.canPlayerReach(player, target) || !player.capabilities.allowEdit) {
          return;
        }
        World world = player.world;
        if (!world.isBlockModifiable(player, target)) {
          return;
        }
        TileEntity te = world.getTileEntity(target);
        if (!(world.getBlockState(target).getBlock() instanceof BlockAddressPlaque)
            || !(te instanceof TileEntityAddressPlaque)) {
          return;
        }
        TileEntityAddressPlaque plaque = (TileEntityAddressPlaque) te;
        if (plaque.setPlaque(AddressPlaque.read(nbt))) {
          plaque.markDirtySync(world, target, world.getBlockState(target), true);
        }
      });
      return null;
    }
  }
}
