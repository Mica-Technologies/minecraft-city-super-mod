package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

/**
 * Links a whole building's fire alarm to a panel with three clicks: the panel, then two opposite
 * corners of a box. Every appliance, initiating device and follower in the box is linked as a
 * {@link ItemFireAlarmLinker} click on each would link it (see {@link FireAlarmAreaLink}).
 * Sneak-clicking the second corner unlinks the box instead.
 * <p>
 * A separate item rather than a mode of the linker: the linker's clicks are scripted (a building is
 * wired by driving a client device by device), and a mode a stray click into the air could flip
 * would turn the next device click into a corner. It extends the linker so that every block which
 * steps aside for a linker in hand (the panel's GUI, pull stations, controllers) steps aside for
 * this too, and so {@link ItemFireAlarmLinker#getSelectedPanel(ItemStack)} reads its selection.
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class ItemFireAlarmAreaLinker extends ItemFireAlarmLinker {

  /** The first corner, once clicked, as {@link BlockPos#toLong()}. */
  private static final String CORNER_KEY = "AreaCorner";

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
      EnumFacing facing, float hitX, float hitY, float hitZ) {
    ItemStack stack = player.getHeldItem(hand);

    // On a panel: select it, or, sneaking, prune its missing devices as the linker does
    if (world.getBlockState(pos).getBlock() instanceof BlockFireAlarmControlPanel) {
      if (player.isSneaking()) {
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
      }
      setSelectedPanel(stack, pos);
      setCorner(stack, null);
      say(world, player, "Area linking to the fire alarm control panel at " + describe(pos)
          + ". Click two opposite corners of the building.");
      return EnumActionResult.SUCCESS;
    }

    BlockPos panelPos = getSelectedPanel(stack);
    TileEntity panelTe = panelPos == null ? null : world.getTileEntity(panelPos);
    if (!(panelTe instanceof TileEntityFireAlarmControlPanel)) {
      say(world, player, panelPos == null ? "No panel selected!"
          : "The area linker links to a fire alarm control panel; click one first");
      return EnumActionResult.SUCCESS;
    }

    BlockPos corner = getCorner(stack);
    if (corner == null) {
      setCorner(stack, pos);
      say(world, player, "First corner at " + describe(pos)
          + ". Click the opposite corner; sneak-click it to unlink instead.");
      return EnumActionResult.SUCCESS;
    }

    setCorner(stack, null);
    if (!world.isRemote) {
      TileEntityFireAlarmControlPanel panel = (TileEntityFireAlarmControlPanel) panelTe;
      try {
        FireAlarmAreaLink.Result result = player.isSneaking()
            ? FireAlarmAreaLink.unlink(world, panel, corner, pos)
            : FireAlarmAreaLink.link(world, panel, corner, pos);
        say(world, player, (player.isSneaking() ? result.describeUnlink() : result.describeLink())
            + " (" + FireAlarmAreaLink.volume(corner, pos) + " blocks from " + describe(corner)
            + " to " + describe(pos) + ")");
      } catch (IllegalArgumentException tooBig) {
        say(world, player, tooBig.getMessage());
      }
    }
    return EnumActionResult.SUCCESS;
  }

  /** Sneak-click in the air forgets a half-made box first, and only then the panel. */
  @Override
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
      EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (player.isSneaking() && getCorner(stack) != null) {
      setCorner(stack, null);
      say(world, player, "Cleared the first corner");
      return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    return super.onItemRightClick(world, player, hand);
  }

  @Override
  public void addInformation(ItemStack stack, World world, List<String> list,
      ITooltipFlag flag) {
    list.add("Link every fire alarm device in a box to a panel");
    list.add("Click a panel, then two opposite corners");
    list.add("Sneak-click the second corner to unlink the box");
    list.add("Sneak-click the air to clear the corner, then the panel");
    BlockPos selected = getSelectedPanel(stack);
    list.add(selected == null ? "No panel selected" : "Panel selected at " + describe(selected));
    BlockPos corner = getCorner(stack);
    if (corner != null) {
      list.add("First corner at " + describe(corner));
    }
  }

  private static BlockPos getCorner(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag == null || !tag.hasKey(CORNER_KEY, Constants.NBT.TAG_LONG) ? null
        : BlockPos.fromLong(tag.getLong(CORNER_KEY));
  }

  private static void setCorner(ItemStack stack, BlockPos corner) {
    NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
    if (corner == null) {
      tag.removeTag(CORNER_KEY);
    } else {
      tag.setLong(CORNER_KEY, corner.toLong());
    }
    stack.setTagCompound(tag);
  }

  private static void say(World world, EntityPlayer player, String text) {
    if (!world.isRemote) {
      player.sendMessage(new TextComponentString(text));
    }
  }

  private static String describe(BlockPos pos) {
    return "(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")";
  }

  @Override
  public String getItemRegistryName() {
    return "firealarmarealinker";
  }
}
