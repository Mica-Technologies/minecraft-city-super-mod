package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.SignalVisibilityArea;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * The tool that tells a signal head where it should be seen from.
 *
 * <p>Real programmable visibility heads (3M, McCain) are masked on site by a technician who stands
 * where the driver will be and blanks out everything else. This is that job: right-click the head,
 * then right-click the road blocks that outline the area (up to {@value SignalVisibilityArea#MAX_POINTS}
 * of them, in order round the shape; two make a rectangle), then right-click the head again to
 * apply (sneak and right-click anywhere also applies). Sneak and right-click the head itself to
 * clear its area.</p>
 *
 * <p>The same area aims a horizontal louvered visor: the head reads how far below it the nearest
 * and farthest points sit and tilts its slats to pass exactly that band. See
 * {@link com.micatechnologies.minecraft.csm.trafficsignals.logic.SignalVisibility}.</p>
 *
 * <p>Per-player selection state lives on the server side of this item, keyed by player, the same
 * way the sensor zone tool keeps its corners; nothing is written to the head until the area is
 * applied, so walking away mid-programming changes nothing.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class ItemSignalVisibilityProgrammer extends AbstractItem {

  /**
   * The farthest a point may be from its head, in blocks. Generous -- a long approach is the
   * whole point -- but it stops a mis-click on the far side of a city from being accepted.
   */
  private static final double MAX_RANGE_BLOCKS = 96.0;

  private final Map<UUID, BlockPos> headPosMap = new HashMap<>();
  private final Map<UUID, List<int[]>> pointsMap = new HashMap<>();

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (worldIn.isRemote) {
      return EnumActionResult.SUCCESS;
    }
    UUID id = player.getUniqueID();
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof TileEntityTrafficSignalHead) {
      TileEntityTrafficSignalHead head = (TileEntityTrafficSignalHead) te;
      if (player.isSneaking()) {
        if (head.hasVisibilityArea()) {
          head.setVisibilityArea(null);
          say(player, "Cleared the visibility area of the signal head at " + describe(pos) + ".");
        } else {
          say(player, "The signal head at " + describe(pos) + " has no visibility area to clear.");
        }
        headPosMap.remove(id);
        pointsMap.remove(id);
        return EnumActionResult.SUCCESS;
      }
      // Clicking the head being programmed again, with enough points down, applies them. This is
      // the apply gesture that needs no held key; sneak + right-click anywhere is the other.
      List<int[]> pending = pointsMap.get(id);
      if (pos.equals(headPosMap.get(id)) && pending != null
          && pending.size() >= SignalVisibilityArea.MIN_POINTS) {
        apply(player, worldIn);
        return EnumActionResult.SUCCESS;
      }
      headPosMap.put(id, pos);
      pointsMap.put(id, new ArrayList<>());
      say(player, "Programming the signal head at " + describe(pos) + ". Right-click up to "
          + SignalVisibilityArea.MAX_POINTS + " road blocks in order around the area it should be"
          + " seen from (two make a rectangle), then right-click the head again to apply.");
      return EnumActionResult.SUCCESS;
    }

    BlockPos headPos = headPosMap.get(id);
    if (headPos == null) {
      say(player, "Right-click a signal head first to start programming its visibility area.");
      return EnumActionResult.SUCCESS;
    }
    if (player.isSneaking()) {
      apply(player, worldIn);
      return EnumActionResult.SUCCESS;
    }

    List<int[]> points = pointsMap.computeIfAbsent(id, k -> new ArrayList<>());
    if (points.size() >= SignalVisibilityArea.MAX_POINTS) {
      say(player, "That is already " + SignalVisibilityArea.MAX_POINTS
          + " points. Sneak + right-click to apply, or right-click the head to start over.");
      return EnumActionResult.SUCCESS;
    }
    points.add(new int[]{pos.getX(), pos.getY(), pos.getZ()});
    String next = points.size() >= SignalVisibilityArea.MIN_POINTS
        ? " Right-click the head to apply, or keep adding points."
        : " Add at least one more.";
    say(player, "Point " + points.size() + " set at " + describe(pos) + "." + next);
    return EnumActionResult.SUCCESS;
  }

  @Override
  public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn,
      EnumHand handIn) {
    // Sneak + right-click on air applies too, so the last click does not have to land on a block.
    if (!worldIn.isRemote && playerIn.isSneaking()
        && headPosMap.containsKey(playerIn.getUniqueID())) {
      apply(playerIn, worldIn);
      return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
    }
    return new ActionResult<>(EnumActionResult.PASS, playerIn.getHeldItem(handIn));
  }

  /**
   * Writes the collected points to the selected head as its area, if they make one.
   *
   * @param player the programming player
   * @param world  the world
   */
  private void apply(EntityPlayer player, World world) {
    UUID id = player.getUniqueID();
    BlockPos headPos = headPosMap.get(id);
    List<int[]> points = pointsMap.get(id);
    if (headPos == null || points == null) {
      say(player, "Right-click a signal head first to start programming its visibility area.");
      return;
    }
    if (points.size() < SignalVisibilityArea.MIN_POINTS) {
      say(player, "Set at least " + SignalVisibilityArea.MIN_POINTS
          + " points before applying (" + points.size() + " so far).");
      return;
    }
    TileEntity te = world.getTileEntity(headPos);
    if (!(te instanceof TileEntityTrafficSignalHead)) {
      say(player, "The signal head at " + describe(headPos) + " is gone. Nothing was programmed.");
      headPosMap.remove(id);
      pointsMap.remove(id);
      return;
    }
    SignalVisibilityArea area = SignalVisibilityArea.of(points);
    if (area == null) {
      say(player, "Those points do not make an area. Nothing was programmed.");
      return;
    }
    if (!area.isWithinRange(headPos.getX() + 0.5, headPos.getZ() + 0.5, MAX_RANGE_BLOCKS)) {
      say(player, "Every point must be within " + (int) MAX_RANGE_BLOCKS
          + " blocks of the head. Nothing was programmed.");
      return;
    }
    TileEntityTrafficSignalHead head = (TileEntityTrafficSignalHead) te;
    boolean replaced = head.hasVisibilityArea();
    head.setVisibilityArea(area);
    say(player, "Programmed the signal head at " + describe(headPos) + " with a "
        + area.pointCount() + "-point visibility area" + (replaced ? " (replaced the previous one)."
        : "."));
    headPosMap.remove(id);
    pointsMap.remove(id);
  }

  private static void say(EntityPlayer player, String message) {
    player.sendMessage(new TextComponentString(message));
  }

  private static String describe(BlockPos pos) {
    return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Programs where a signal head is seen from: the mask of a programmable");
    list.add("visibility visor and the aim of a horizontal louvered visor.");
    list.add("Right-click the head, then up to " + SignalVisibilityArea.MAX_POINTS
        + " road blocks around the area, then the head again to apply.");
    list.add("Sneak + right-click the head to clear its area.");
  }

  @Override
  public String getItemRegistryName() {
    return "signalvisibilityprogrammer";
  }
}
