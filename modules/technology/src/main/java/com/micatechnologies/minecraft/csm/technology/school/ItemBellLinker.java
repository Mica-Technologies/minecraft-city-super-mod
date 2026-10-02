package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.technology.TileEntitySpeaker;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Links speakers and hallway bells to a bell schedule controller.
 *
 * <ul>
 *   <li><b>Right-click a controller</b>: select it (remembered on this item stack, so two players
 *   linking two schools do not trip over each other).</li>
 *   <li><b>Right-click a speaker or bell</b>: link it to the selected controller, or unlink it if
 *   it is linked already. Any speaker in the tab counts, and a clock/speaker panel's either
 *   half (its speaker is linked).</li>
 *   <li><b>Sneak + right-click a controller</b>: clear all its links.</li>
 * </ul>
 *
 * <p>Handled in {@code onItemUseFirst}, before the block's own right-click, so a speaker's
 * ambient-sound cycling does not take the click.</p>
 *
 * @since 2026.10
 */
public class ItemBellLinker extends AbstractItem {

  /** How far from its controller a device may be linked, in blocks. */
  private static final double MAX_LINK_DISTANCE = 128.0;

  private static final String TAG_X = "bcX";
  private static final String TAG_Y = "bcY";
  private static final String TAG_Z = "bcZ";
  private static final String TAG_DIM = "bcD";

  public ItemBellLinker() {
    super(0, 1);
  }

  @Override
  public String getItemRegistryName() {
    return "school_bell_linker";
  }

  @Override
  public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos clicked,
      EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
    BlockPos pos = clicked;
    IBlockState state = world.getBlockState(pos);
    if (state.getBlock() instanceof BlockSchoolClockSpeakerPanel) {
      pos = ((BlockSchoolClockSpeakerPanel) state.getBlock()).speakerHalf(pos, state);
      state = world.getBlockState(pos);
    }
    TileEntity te = world.getTileEntity(pos);
    boolean controller = te instanceof TileEntityBellController;
    boolean device = te instanceof TileEntitySpeaker
        || state.getBlock() instanceof BlockSchoolBell;
    if (!controller && !device) {
      return EnumActionResult.PASS;
    }
    if (world.isRemote) {
      return EnumActionResult.SUCCESS;
    }
    ItemStack stack = player.getHeldItem(hand);
    if (controller) {
      TileEntityBellController c = (TileEntityBellController) te;
      if (player.isSneaking()) {
        int n = c.clearLinks();
        tell(player, "§aCleared " + n + " linked speaker(s) and bell(s)");
      } else {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound()
            : new NBTTagCompound();
        tag.setInteger(TAG_X, pos.getX());
        tag.setInteger(TAG_Y, pos.getY());
        tag.setInteger(TAG_Z, pos.getZ());
        tag.setInteger(TAG_DIM, world.provider.getDimension());
        stack.setTagCompound(tag);
        tell(player, "§bSelected bell controller (" + c.getLinks().size()
            + " linked). Right-click speakers and bells to link or unlink them.");
      }
      return EnumActionResult.SUCCESS;
    }

    BlockPos selected = selected(stack, world);
    if (selected == null) {
      tell(player, "§eRight-click a bell schedule controller first.");
      return EnumActionResult.SUCCESS;
    }
    if (!world.isBlockLoaded(selected)) {
      tell(player, "§eThe selected controller is too far away (its chunk is not loaded).");
      return EnumActionResult.SUCCESS;
    }
    TileEntity selectedTe = world.getTileEntity(selected);
    if (!(selectedTe instanceof TileEntityBellController)) {
      stack.getTagCompound().removeTag(TAG_DIM);
      tell(player, "§cThe selected controller is gone. Right-click a controller first.");
      return EnumActionResult.SUCCESS;
    }
    if (selected.distanceSq(pos) > MAX_LINK_DISTANCE * MAX_LINK_DISTANCE) {
      tell(player, "§cToo far from the controller (more than "
          + (int) MAX_LINK_DISTANCE + " blocks).");
      return EnumActionResult.SUCCESS;
    }
    TileEntityBellController c = (TileEntityBellController) selectedTe;
    String what = state.getBlock() instanceof BlockSchoolBell ? "bell" : "speaker";
    if (c.isLinked(pos)) {
      c.unlink(pos);
      tell(player, "§aUnlinked " + what + " (" + c.getLinks().size() + " linked)");
    } else if (c.link(pos)) {
      tell(player, "§aLinked " + what + " (" + c.getLinks().size() + " linked)");
    } else {
      tell(player, "§cThe controller is full (" + TileEntityBellController.MAX_LINKS
          + " links).");
    }
    return EnumActionResult.SUCCESS;
  }

  @Nullable
  private static BlockPos selected(ItemStack stack, World world) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null || !tag.hasKey(TAG_DIM)
        || tag.getInteger(TAG_DIM) != world.provider.getDimension()) {
      return null;
    }
    return new BlockPos(tag.getInteger(TAG_X), tag.getInteger(TAG_Y), tag.getInteger(TAG_Z));
  }

  private static void tell(EntityPlayer player, String message) {
    player.sendMessage(new TextComponentString(message));
  }

  @Override
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add("Links speakers and hallway bells to a bell schedule controller");
    tooltip.add("§7Right-click a controller, then each speaker or bell");
    tooltip.add("§7Click a linked one again to unlink it");
    tooltip.add("§7Sneak + right-click a controller to clear its links");
  }
}
