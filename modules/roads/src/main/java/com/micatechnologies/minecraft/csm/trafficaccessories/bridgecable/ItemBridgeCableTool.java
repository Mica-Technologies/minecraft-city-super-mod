package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Strings a bridge cable by hand: click the anchor that is to own it, then whatever it runs to
 * (another anchor, or any block). The span wire tool's pattern: the first click is held per
 * player for the session, a successful link consumes it.
 *
 * <p>The tool strings one of a few presets, cycled by sneak-clicking anything that is not an
 * anchor (or the air). Everything else a cable can be -- thickness, colour, stripes, LED strip,
 * an exact sag -- is set by {@code /blockdata} on the owning anchor afterwards; the docs carry
 * the keys.
 */
public class ItemBridgeCableTool extends AbstractItem {

  /** What the next cable a player strings will be. */
  enum Preset {
    STAY("stay cable", BridgeCableStyle.STAY, 0),
    MAIN("main cable (10% sag)", BridgeCableStyle.MAIN, 0),
    MAIN_HANGERS("main cable with hangers every 5 blocks", BridgeCableStyle.MAIN, 5),
    HANGER("hanger", BridgeCableStyle.HANGER, 0),
    ROPE("rope (4% sag)", BridgeCableStyle.ROPE, 0);

    final String description;
    final BridgeCableStyle style;
    final int hangers;

    Preset(String description, BridgeCableStyle style, int hangers) {
      this.description = description;
      this.style = style;
      this.hangers = hangers;
    }

    Preset next() {
      return values()[(ordinal() + 1) % values().length];
    }
  }

  private final Map<UUID, BlockPos> pendingAnchorMap = new HashMap<>();
  private final Map<UUID, Preset> presetMap = new HashMap<>();

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (worldIn.isRemote) {
      return EnumActionResult.SUCCESS;
    }
    final TileEntity tileEntity = worldIn.getTileEntity(pos);
    final boolean anchor = tileEntity instanceof TileEntityBridgeCableAnchor;

    if (player.isSneaking()) {
      if (anchor) {
        final int removed = ((TileEntityBridgeCableAnchor) tileEntity).clearLinks();
        pendingAnchorMap.remove(player.getUniqueID());
        say(player, removed == 0 ? "That anchor owns no cables."
            : "Removed " + removed + " cable" + (removed == 1 ? "" : "s") + " from "
                + describe(pos) + ".");
      } else {
        cyclePreset(player);
      }
      return EnumActionResult.SUCCESS;
    }

    final BlockPos pending = pendingAnchorMap.get(player.getUniqueID());
    if (pending == null || pending.equals(pos)) {
      if (!anchor) {
        say(player, "Click a bridge cable anchor first: it owns and draws the cable.");
        return EnumActionResult.SUCCESS;
      }
      pendingAnchorMap.put(player.getUniqueID(), pos);
      say(player, "Cable from " + describe(pos) + " (" + preset(player).description
          + "). Click the anchor or block it runs to.");
      return EnumActionResult.SUCCESS;
    }

    final TileEntity owner = worldIn.getTileEntity(pending);
    if (!(owner instanceof TileEntityBridgeCableAnchor)) {
      pendingAnchorMap.remove(player.getUniqueID());
      say(player, "The first anchor is gone. Start again.");
      return EnumActionResult.SUCCESS;
    }

    // One end draws a cable. If the far anchor already owns this cable the other way round,
    // move it here rather than draw it twice.
    if (anchor) {
      ((TileEntityBridgeCableAnchor) tileEntity).removeLinkTo(pending);
    }
    final Preset preset = preset(player);
    BridgeCableLink link = BridgeCableLink.of(pending, pos, preset.style);
    if (preset.hangers > 0) {
      link = link.withHangers(preset.hangers);
    }
    if (!((TileEntityBridgeCableAnchor) owner).putLink(link)) {
      say(player, "That anchor already holds " + BridgeCableLink.MAX_CABLES
          + " cables. Remove some first (sneak-click it).");
      return EnumActionResult.SUCCESS;
    }
    pendingAnchorMap.remove(player.getUniqueID());
    say(player, "Strung a " + preset.description + " from " + describe(pending) + " to "
        + describe(pos) + ", " + String.format("%.1f", Math.sqrt(pending.distanceSq(pos)))
        + " blocks.");
    return EnumActionResult.SUCCESS;
  }

  @Override
  public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player,
      EnumHand hand) {
    final ItemStack stack = player.getHeldItem(hand);
    if (!worldIn.isRemote) {
      if (player.isSneaking()) {
        cyclePreset(player);
      } else if (pendingAnchorMap.remove(player.getUniqueID()) != null) {
        say(player, "Bridge cable selection cleared.");
      }
    }
    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
  }

  private void cyclePreset(EntityPlayer player) {
    final Preset next = preset(player).next();
    presetMap.put(player.getUniqueID(), next);
    say(player, "Next cable: " + next.description + ".");
  }

  private Preset preset(EntityPlayer player) {
    final Preset preset = presetMap.get(player.getUniqueID());
    return preset == null ? Preset.STAY : preset;
  }

  private static void say(EntityPlayer player, String message) {
    player.sendMessage(new TextComponentString(message));
  }

  private static String describe(BlockPos pos) {
    return "(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")";
  }

  @Override
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add("Strings a bridge cable from an anchor.");
    tooltip.add("Click the owning anchor, then the anchor or block it runs to.");
    tooltip.add("Sneak + click anything else (or the air) to change cable type.");
    tooltip.add("Sneak + click an anchor to remove its cables.");
    tooltip.add("Thickness, colour, sag, stripes, LED: /blockdata on the anchor.");
  }

  @Override
  public String getItemRegistryName() {
    return "bridge_cable_tool";
  }
}
