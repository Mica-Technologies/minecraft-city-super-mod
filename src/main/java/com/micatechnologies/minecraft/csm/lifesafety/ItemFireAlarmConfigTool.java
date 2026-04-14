package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import net.minecraft.block.Block;
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
 * Hand-held item used to configure fire alarm control panels. Supports multiple modes
 * including opening the config GUI, audible silence, panel reset, and voice evac sound cycling.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class ItemFireAlarmConfigTool extends AbstractItem {

  private static final String NBT_MODE_KEY = "csm_tool_mode";

  @Override
  public EnumActionResult onItemUse(EntityPlayer player,
      World worldIn,
      BlockPos pos,
      EnumHand hand,
      EnumFacing facing,
      float hitX,
      float hitY,
      float hitZ) {
    ItemStack heldStack = player.getHeldItem(hand);

    // OPEN_GUI mode must run on client side (GuiScreen has no server Container)
    if (worldIn.isRemote && !player.isSneaking()
        && getMode(heldStack) == ItemFireAlarmConfigToolMode.OPEN_GUI
        && worldIn.getBlockState(pos).getBlock() instanceof BlockFireAlarmControlPanel) {
      player.openGui(Csm.instance, 3, worldIn, pos.getX(), pos.getY(), pos.getZ());
      return EnumActionResult.SUCCESS;
    }

    if (!worldIn.isRemote) {
      Block clickedBlock = worldIn.getBlockState(pos).getBlock();
      ItemFireAlarmConfigToolMode mode = getMode(heldStack);

      // Sneak-click to change mode
      if (player.isSneaking()) {
        switchToNextMode(heldStack);
        player.sendMessage(
            new TextComponentString("Mode changed to: " + getMode(heldStack).getFriendlyName()));
        return EnumActionResult.SUCCESS;
      }

      // All other modes require clicking on a fire alarm control panel
      if (!(clickedBlock instanceof BlockFireAlarmControlPanel)) {
        player.sendMessage(new TextComponentString("Not a fire alarm control panel."));
        return EnumActionResult.FAIL;
      }

      TileEntity te = worldIn.getTileEntity(pos);
      if (!(te instanceof TileEntityFireAlarmControlPanel)) {
        player.sendMessage(new TextComponentString("Panel tile entity missing."));
        return EnumActionResult.FAIL;
      }

      TileEntityFireAlarmControlPanel panel = (TileEntityFireAlarmControlPanel) te;

      switch (mode) {
        case AUDIBLE_SILENCE:
          if (panel.getAlarmState()) {
            panel.setAudibleSilence(true);
            player.sendMessage(new TextComponentString("Panel audible silence activated."));
          } else {
            player.sendMessage(
                new TextComponentString("Panel is not in alarm. Cannot audible silence."));
          }
          break;
        case RESET_PANEL:
          if (panel.getAlarmState()) {
            panel.setAlarmState(false);
            player.sendMessage(new TextComponentString("Panel alarm status has been reset!"));
          } else {
            player.sendMessage(new TextComponentString("Panel is not in alarm."));
          }
          break;
        case CYCLE_VOICE_EVAC_SOUND:
          panel.switchSound();
          player.sendMessage(new TextComponentString(
              "Switching alarm panel sound to " + panel.getCurrentSoundName()));
          break;
        case OPEN_GUI:
          // Handled on client side above
          break;
      }

      return EnumActionResult.SUCCESS;
    }

    return EnumActionResult.SUCCESS;
  }

  public static ItemFireAlarmConfigToolMode getMode(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag != null && tag.hasKey(NBT_MODE_KEY)) {
      int ordinal = tag.getInteger(NBT_MODE_KEY);
      ItemFireAlarmConfigToolMode[] values = ItemFireAlarmConfigToolMode.values();
      if (ordinal >= 0 && ordinal < values.length) {
        return values[ordinal];
      }
    }
    return ItemFireAlarmConfigToolMode.OPEN_GUI;
  }

  public static void switchToNextMode(ItemStack stack) {
    ItemFireAlarmConfigToolMode current = getMode(stack);
    int next = (current.ordinal() + 1) % ItemFireAlarmConfigToolMode.values().length;
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null) {
      tag = new NBTTagCompound();
      stack.setTagCompound(tag);
    }
    tag.setInteger(NBT_MODE_KEY, next);
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Configure fire alarm panel: audible silence,");
    list.add("reset, voice evac sound selection, and more.");
    list.add("Right-click to apply. Sneak + right-click to switch modes.");
    list.add("Current mode: " + getMode(itemstack).getFriendlyName());
  }

  @Override
  public String getItemRegistryName() {
    return "firealarmconfigtool";
  }
}
