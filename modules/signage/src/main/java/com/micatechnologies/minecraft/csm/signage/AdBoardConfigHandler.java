package com.micatechnologies.minecraft.csm.signage;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a board's screen on the server. Nothing from the client is trusted: the player must be
 * able to reach the block the screen was opened from, the controller is found from that block
 * here, the size is clamped to the board's kind, an ad or category the library does not have is
 * replaced, and every enum comes back through its range-checked {@code fromOrdinal}.
 */
public class AdBoardConfigHandler implements IMessageHandler<AdBoardConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(AdBoardConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> apply(player, message));
    return null;
  }

  private static void apply(EntityPlayerMP player, AdBoardConfigPacket message) {
    if (!CsmPacketUtils.canPlayerReach(player, message.clicked)) {
      return;
    }
    World world = player.world;
    BlockPos controller = AdBoards.findController(world, message.clicked);
    if (controller == null) {
      return;
    }
    TileEntity te = world.getTileEntity(controller);
    if (!(te instanceof TileEntityAdBoard)) {
      return;
    }
    TileEntityAdBoard board = (TileEntityAdBoard) te;
    AdLibrary library = AdLibrary.get();
    String adId = library.find(message.adId) != null ? message.adId : AdLibrary.HOUSE_AD;
    String category = library.inCategory(message.category).isEmpty() ? "" : message.category;
    AdRotation rotation = AdRotation.fromOrdinal(message.rotation);
    if (rotation.usesCategory() && category.isEmpty()) {
      rotation = rotation.isShuffled() ? AdRotation.ALL_SHUFFLED : AdRotation.ALL_IN_ORDER;
    }
    board.setAds(adId, rotation, category, message.interval, AdFit.fromOrdinal(message.fit),
        AdLight.fromOrdinal(message.light));
    IBlockState state = world.getBlockState(controller);
    boolean cabinet = state.getBlock() instanceof AbstractBlockAdBoard
        && ((AbstractBlockAdBoard) state.getBlock()).kind().isCabinet();
    board.setBack(cabinet ? AdBack.fromOrdinal(message.back) : AdBack.NONE);

    ITextComponent problem = AdBoards.resize(world, controller, player, message.width,
        message.height, AdBoardAlign.fromOrdinal(message.align));
    if (problem != null) {
      player.sendStatusMessage(problem, false);
    }
    // resize syncs when it changes the size; the ads changed either way.
    board.markDirtySync(world, controller, true);
  }
}
