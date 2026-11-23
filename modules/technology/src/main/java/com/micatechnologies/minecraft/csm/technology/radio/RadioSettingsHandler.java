package com.micatechnologies.minecraft.csm.technology.radio;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link RadioSettingsPacket}: stores the settings on the radio the
 * sender is holding, or on the one standing within their reach, narrowed to what that make of
 * radio can hold. A packet that names anything else changes nothing.
 *
 * @since 2026.11
 */
public class RadioSettingsHandler implements IMessageHandler<RadioSettingsPacket, IMessage> {

  @Override
  public IMessage onMessage(RadioSettingsPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> apply(player, message));
    return null;
  }

  private static void apply(EntityPlayerMP player, RadioSettingsPacket message) {
    if (message.isHeld()) {
      ItemStack stack = player.getHeldItem(message.getHand());
      if (!stack.isEmpty() && stack.getItem() instanceof ItemBlockRadio) {
        RadioModel model = ((ItemBlockRadio) stack.getItem()).getRadioModel();
        ItemBlockRadio.setSettings(stack, message.getSettings().forModel(model));
      }
      return;
    }
    BlockPos pos = message.getPos();
    if (!CsmPacketUtils.canPlayerReach(player, pos)) {
      return;
    }
    Block block = player.world.getBlockState(pos).getBlock();
    TileEntity tileEntity = player.world.getTileEntity(pos);
    if (block instanceof BlockRadio && tileEntity instanceof TileEntityRadio) {
      TileEntityRadio radio = (TileEntityRadio) tileEntity;
      radio.setSettings(message.getSettings().forModel(((BlockRadio) block).getRadioModel()));
      radio.markDirtySync(player.world, pos, true);
    }
  }
}
