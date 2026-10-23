package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A wayfinding panel's whole sign, sent from its editor on every change, as the sign's own NBT
 * ({@link WayfindingSign#write}). The server reads it as it reads a {@code /blockdata}: every
 * string cleaned, every enum by id, every number clamped. Core's channel.
 *
 * @since 2026.10
 */
public class WayfindingPanelPacket implements IMessage {

  /** The most a sign's NBT may weigh on the wire, bytes: four full lines and every key. */
  private static final long MAX_NBT = 8192;

  private BlockPos pos;
  private NBTTagCompound sign;

  public WayfindingPanelPacket() {
  }

  /**
   * A sign.
   *
   * @param pos  the cell the editor was opened on
   * @param sign the sign
   */
  public WayfindingPanelPacket(BlockPos pos, WayfindingSign sign) {
    this.pos = pos;
    this.sign = sign.write(new NBTTagCompound());
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    try {
      sign = ByteBufUtils.readTag(buf);
    } catch (RuntimeException e) {
      sign = null;
    }
    if (sign != null && buf.readerIndex() > MAX_NBT + 8) {
      sign = null;
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    ByteBufUtils.writeTag(buf, sign);
  }

  /**
   * Applies the edit, if the player can reach the cell named and may change blocks there (not in
   * adventure mode, not inside spawn protection). The sign is written to every cell of the panel
   * ({@link WayfindingPanels#applyToPanel}).
   */
  public static class Handler implements IMessageHandler<WayfindingPanelPacket, IMessage> {

    @Override
    public IMessage onMessage(WayfindingPanelPacket message, MessageContext ctx) {
      EntityPlayerMP player = ctx.getServerHandler().player;
      NBTTagCompound nbt = message.sign;
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
        IBlockState state = world.getBlockState(target);
        TileEntity te = world.getTileEntity(target);
        if (!(state.getBlock() instanceof AbstractBlockWayfindingPanel)
            || !(te instanceof AbstractTileEntityWayfindingPanel)) {
          return;
        }
        WayfindingScheme fallback = ((AbstractTileEntityWayfindingPanel) te).defaultSign()
            .getScheme();
        WayfindingPanels.applyToPanel(world, target, WayfindingSign.read(nbt, fallback));
      });
      return null;
    }
  }
}
