package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Strings messenger cable between two span wire anchors.
 *
 * <p>Click one anchor to pick it up, click the other to string the span. The hanger mounts
 * standing between them are found automatically -- a builder places their mounts where they want
 * signals and the cable is fitted to them, rather than the other way round.
 *
 * <p>The pending first click is held per player, following the same pattern as the signal link
 * tool. It lives only for the session; a player who logs out mid-link simply starts again.
 */
public class ItemSpanWireTool extends AbstractItem {

  private final Map<UUID, BlockPos> pendingAnchorMap = new HashMap<>();

  /**
   * Whether the next span this player strings carries a lower tether. Held per player alongside
   * the pending anchor, and toggled by sneak-clicking anything that is not part of a span --
   * the same "click something else to change the mode" idiom the signal link tool uses.
   */
  private final Map<UUID, Boolean> boxSpanModeMap = new HashMap<>();

  @Override
  public EnumActionResult onItemUse(EntityPlayer player,
      World worldIn,
      BlockPos pos,
      EnumHand hand,
      EnumFacing facing,
      float hitX,
      float hitY,
      float hitZ) {
    if (worldIn.isRemote) {
      return EnumActionResult.SUCCESS;
    }

    final TileEntity tileEntity = worldIn.getTileEntity(pos);

    if (tileEntity instanceof TileEntitySpanWireAnchor) {
      if (player.isSneaking()) {
        return unlinkAt(player, worldIn, pos);
      }
      return linkAt(player, worldIn, pos);
    }

    if (tileEntity instanceof TileEntitySpanWireHanger) {
      return describeHanger(player, (TileEntitySpanWireHanger) tileEntity, pos);
    }

    if (worldIn.getBlockState(pos).getBlock() instanceof BlockSpanWireGuyAnchor) {
      return player.isSneaking()
          ? detachGuy(player, worldIn, pos)
          : attachGuy(player, worldIn, pos);
    }

    if (player.isSneaking()) {
      final boolean boxSpan = !isBoxSpanMode(player);
      boxSpanModeMap.put(player.getUniqueID(), boxSpan);
      player.sendMessage(new TextComponentString(boxSpan
          ? "Next span will be a box span: a lower tether ties the signal bottoms together."
          : "Next span will be free-swinging: messenger only, no tether."));
      return EnumActionResult.SUCCESS;
    }

    // Clicking anything else clears a half-finished link, so a stale first anchor cannot sit
    // around waiting to surprise the player several minutes later.
    if (pendingAnchorMap.remove(player.getUniqueID()) != null) {
      player.sendMessage(new TextComponentString("Span wire selection cleared."));
    }
    return EnumActionResult.SUCCESS;
  }

  private EnumActionResult linkAt(EntityPlayer player, World world, BlockPos pos) {
    final BlockPos pending = pendingAnchorMap.get(player.getUniqueID());

    if (pending == null || pending.equals(pos)) {
      pendingAnchorMap.put(player.getUniqueID(), pos);
      player.sendMessage(new TextComponentString(
          "Span wire anchored at " + describe(pos) + ". Click the far anchor to string it."));
      return EnumActionResult.SUCCESS;
    }

    final SpanWireManager.LinkResult result = SpanWireManager.link(
        world, pending, pos, SpanWireCatenary.DEFAULT_SLACK, isBoxSpanMode(player));
    player.sendMessage(new TextComponentString(result.getMessage()));

    // Only a successful link consumes the pending anchor. A rejected one leaves it selected so
    // the player can correct the far end without re-picking the near one.
    if (result.isSuccess()) {
      pendingAnchorMap.remove(player.getUniqueID());
    }
    return EnumActionResult.SUCCESS;
  }

  /**
   * Runs a back-guy from the anchor already picked up down to this ground anchor.
   *
   * <p>Deliberately a second, explicit step rather than something that happens on its own. Most
   * spans have no guy, so one appearing because a ground anchor happened to be nearby is a wire
   * the builder did not ask for.
   */
  private EnumActionResult attachGuy(EntityPlayer player, World world, BlockPos pos) {
    final BlockPos pending = pendingAnchorMap.get(player.getUniqueID());
    if (pending == null) {
      player.sendMessage(new TextComponentString(
          "Click a span wire anchor first, then this ground anchor, to run a back-guy to it."));
      return EnumActionResult.SUCCESS;
    }
    if (!(world.getTileEntity(pending) instanceof TileEntitySpanWireAnchor)) {
      pendingAnchorMap.remove(player.getUniqueID());
      player.sendMessage(new TextComponentString("That anchor is gone. Start again."));
      return EnumActionResult.SUCCESS;
    }
    if (!SpanWireGuyFinder.isInGuyRange(pending, pos)) {
      player.sendMessage(new TextComponentString(
          "That ground anchor is too far from " + describe(pending) + " to guy to. It must be "
              + SpanWireGuyFinder.describeRange() + "."));
      return EnumActionResult.SUCCESS;
    }
    ((TileEntitySpanWireAnchor) world.getTileEntity(pending)).setGuyAnchor(pos);
    pendingAnchorMap.remove(player.getUniqueID());
    player.sendMessage(new TextComponentString(
        "Back-guy run from " + describe(pending) + " down to " + describe(pos) + "."));
    return EnumActionResult.SUCCESS;
  }

  /** Drops any guy running to this ground anchor, without disturbing the span itself. */
  private EnumActionResult detachGuy(EntityPlayer player, World world, BlockPos pos) {
    SpanWireGuyFinder.clearGuysTo(world, pos);
    player.sendMessage(new TextComponentString("Back-guys to " + describe(pos) + " removed."));
    return EnumActionResult.SUCCESS;
  }

  private EnumActionResult unlinkAt(EntityPlayer player, World world, BlockPos pos) {
    final SpanWireDefinition span = SpanWireManager.getSpanAt(world, pos);
    if (span == null) {
      player.sendMessage(new TextComponentString("That anchor has no span on it."));
      return EnumActionResult.SUCCESS;
    }
    SpanWireManager.teardown(world, span);
    player.sendMessage(new TextComponentString(
        "Span removed from " + describe(span.getAnchorA()) + " to " + describe(span.getAnchorB())
            + "."));
    return EnumActionResult.SUCCESS;
  }

  private EnumActionResult describeHanger(EntityPlayer player, TileEntitySpanWireHanger hanger,
      BlockPos pos) {
    final SpanWireDefinition span = hanger.getSpan();
    if (span == null) {
      player.sendMessage(new TextComponentString(
          "This mount is not on a span. Link two anchors either side of it."));
      return EnumActionResult.SUCCESS;
    }
    final double drop = hanger.getCableDrop();
    if (drop < 0.0) {
      player.sendMessage(new TextComponentString(
          "Mount " + describe(pos) + " sits " + String.format("%.2f", -drop)
              + " blocks ABOVE the cable. Nothing hangs upward -- lower this mount."));
      return EnumActionResult.SUCCESS;
    }
    final String verdict = drop > SpanWireManager.MAX_HANGER_DROP
        ? " -- too far to reach; raise this mount."
        : ".";
    player.sendMessage(new TextComponentString(
        "Mount " + describe(pos) + " hangs " + String.format("%.2f", drop)
            + " blocks below the cable" + verdict));
    return EnumActionResult.SUCCESS;
  }

  private boolean isBoxSpanMode(EntityPlayer player) {
    return Boolean.TRUE.equals(boxSpanModeMap.get(player.getUniqueID()));
  }

  private static String describe(BlockPos pos) {
    return "(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")";
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Strings messenger cable between two span wire anchors.");
    list.add("Click one anchor, then the other. Mounts between are found automatically.");
    list.add("Sneak + click an anchor to remove its span.");
    list.add("Click an anchor then a ground anchor to run an optional back-guy.");
    list.add("Sneak + click a ground anchor to remove its back-guy.");
    list.add("Sneak + click anything else to switch between plain and box span.");
    list.add("Click a wire mount to check how it sits against the cable.");
  }

  @Override
  public String getItemRegistryName() {
    return "spanwiretool";
  }
}
