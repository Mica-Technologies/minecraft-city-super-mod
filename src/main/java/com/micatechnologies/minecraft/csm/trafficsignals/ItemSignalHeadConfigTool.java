package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.trafficaccessories.AbstractBlockSignalBackplate;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkSignalDouble;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import java.util.List;
import net.minecraft.block.Block;
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

public class ItemSignalHeadConfigTool extends AbstractItem {

  private static final String NBT_MODE_KEY = "csm_tool_mode";

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    ItemStack heldStack = player.getHeldItem(hand);

    // Resolve backplate clicks to the signal behind them
    if (worldIn.getBlockState(pos).getBlock() instanceof AbstractBlockSignalBackplate) {
      BlockPos signalPos = AbstractBlockSignalBackplate.findSignalBehind(worldIn, pos);
      if (signalPos != null) {
        pos = signalPos;
      } else if (!worldIn.isRemote && !player.isSneaking()) {
        player.sendMessage(
            new TextComponentString("Backplate not connected to a configurable signal head."));
        return EnumActionResult.FAIL;
      }
    }

    // OPEN_GUI mode must run on client side (GuiScreen has no server Container)
    if (worldIn.isRemote && !player.isSneaking()
        && getMode(heldStack) == ItemSignalHeadConfigToolMode.OPEN_GUI) {
      if (worldIn.getBlockState(pos).getBlock() instanceof AbstractBlockControllableCrosswalkSignalNew) {
        player.openGui(Csm.instance, 4, worldIn, pos.getX(), pos.getY(), pos.getZ());
        return EnumActionResult.SUCCESS;
      }
      if (worldIn.getBlockState(pos).getBlock() instanceof AbstractBlockControllableSignalHead) {
        player.openGui(Csm.instance, 1, worldIn, pos.getX(), pos.getY(), pos.getZ());
        return EnumActionResult.SUCCESS;
      }
    }
    if (!worldIn.isRemote) {
      IBlockState state = worldIn.getBlockState(pos);
      Block clickedBlock = state.getBlock();
      ItemSignalHeadConfigToolMode mode = getMode(heldStack);

      // Sneak-click to change mode
      if (player.isSneaking()) {
        switchToNextMode(heldStack);
        player.sendMessage(
            new TextComponentString("Signal Head Config Mode: " + getMode(heldStack).getFriendlyName()));
        return EnumActionResult.SUCCESS;
      }

      // Handle crosswalk signal blocks
      if (clickedBlock instanceof AbstractBlockControllableCrosswalkSignalNew) {
        TileEntity rawCwTE = worldIn.getTileEntity(pos);
        if (!(rawCwTE instanceof TileEntityCrosswalkSignalNew)) {
          player.sendMessage(new TextComponentString("Crosswalk signal tile entity missing."));
          return EnumActionResult.FAIL;
        }
        TileEntityCrosswalkSignalNew cwTe = (TileEntityCrosswalkSignalNew) rawCwTE;
        switch (mode) {
          case CYCLE_BODY_COLOR: {
            var next = cwTe.getNextBodyPaintColor();
            player.sendMessage(new TextComponentString("Body color: " + next.getFriendlyName()));
            break;
          }
          case CYCLE_VISOR_COLOR: {
            var next = cwTe.getNextVisorPaintColor();
            player.sendMessage(new TextComponentString("Visor color: " + next.getFriendlyName()));
            break;
          }
          case CYCLE_VISOR_TYPE: {
            var next = cwTe.getNextVisorType();
            player.sendMessage(new TextComponentString("Visor type: " + next.getFriendlyName()));
            break;
          }
          case CYCLE_BODY_TILT: {
            var next = cwTe.getNextBodyTilt();
            player.sendMessage(new TextComponentString("Body tilt: " + next.getFriendlyName()));
            break;
          }
          case CYCLE_MOUNT_TYPE: {
            var next = cwTe.getNextMountType();
            player.sendMessage(new TextComponentString("Mount type: " + next.getFriendlyName()));
            break;
          }
          case CYCLE_SIGNAL_COLOR: {
            worldIn.setBlockState(pos, state.cycleProperty(AbstractBlockControllableSignal.COLOR));
            int newColor = worldIn.getBlockState(pos).getValue(AbstractBlockControllableSignal.COLOR);
            player.sendMessage(new TextComponentString("Signal color set to " + colorName(newColor)));
            break;
          }
          case CYCLE_DOOR_COLOR:
            player.sendMessage(new TextComponentString("Crosswalk signals do not have a door."));
            break;
          case CYCLE_BULB_TYPE: {
            // Only applicable to double (12-inch stacked) crosswalk signals
            if (clickedBlock instanceof BlockControllableCrosswalkSignalDouble) {
              var next = cwTe.getNextBulbType();
              player.sendMessage(new TextComponentString("Bulb type: " + next.getFriendlyName()));
            } else {
              player.sendMessage(new TextComponentString("Bulb type is fixed for this signal."));
            }
            break;
          }
          case CYCLE_BULB_STYLE:
            player.sendMessage(new TextComponentString("Not applicable to crosswalk signals."));
            break;
          case TOGGLE_ALTERNATE_FLASH:
            player.sendMessage(new TextComponentString("Not applicable to crosswalk signals."));
            break;
          case OPEN_GUI:
            // Handled on client side above
            break;
        }
        return EnumActionResult.SUCCESS;
      }

      // Regular click - apply current mode to a signal head block
      if (!(clickedBlock instanceof AbstractBlockControllableSignalHead)) {
        // Special case: CYCLE_SIGNAL_COLOR works on any controllable signal
        if (mode == ItemSignalHeadConfigToolMode.CYCLE_SIGNAL_COLOR
            && clickedBlock instanceof AbstractBlockControllableSignal) {
          worldIn.setBlockState(pos, state.cycleProperty(AbstractBlockControllableSignal.COLOR));
          int newColor = worldIn.getBlockState(pos).getValue(AbstractBlockControllableSignal.COLOR);
          player.sendMessage(new TextComponentString("Signal color set to " + colorName(newColor)));
          return EnumActionResult.SUCCESS;
        }
        player.sendMessage(new TextComponentString("Not a custom rendering signal head block."));
        return EnumActionResult.FAIL;
      }

      TileEntity rawTE = worldIn.getTileEntity(pos);
      if (!(rawTE instanceof TileEntityTrafficSignalHead)) {
        player.sendMessage(new TextComponentString("Signal head tile entity missing."));
        return EnumActionResult.FAIL;
      }

      TileEntityTrafficSignalHead te = (TileEntityTrafficSignalHead) rawTE;

      switch (mode) {
        case CYCLE_BODY_COLOR: {
          var next = te.getNextBodyPaintColor();
          player.sendMessage(new TextComponentString("Body color: " + next.getFriendlyName()));
          break;
        }
        case CYCLE_DOOR_COLOR: {
          var next = te.getNextDoorPaintColor();
          player.sendMessage(new TextComponentString("Door color: " + next.getFriendlyName()));
          break;
        }
        case CYCLE_VISOR_COLOR: {
          var next = te.getNextVisorPaintColor();
          player.sendMessage(new TextComponentString("Visor color: " + next.getFriendlyName()));
          break;
        }
        case CYCLE_VISOR_TYPE: {
          var next = te.getNextVisorType();
          player.sendMessage(new TextComponentString("Visor type: " + next.getFriendlyName()));
          break;
        }
        case CYCLE_BODY_TILT: {
          var next = te.getNextBodyTilt();
          player.sendMessage(new TextComponentString("Body tilt: " + next.getFriendlyName()));
          break;
        }
        case CYCLE_BULB_STYLE: {
          var next = te.getNextBulbStyle();
          player.sendMessage(new TextComponentString("Bulb style: " + next.getFriendlyName()));
          break;
        }
        case CYCLE_BULB_TYPE: {
          var next = te.getNextBulbType();
          player.sendMessage(new TextComponentString("Bulb type: " + next.getFriendlyName()));
          break;
        }
        case CYCLE_SIGNAL_COLOR: {
          worldIn.setBlockState(pos, state.cycleProperty(AbstractBlockControllableSignal.COLOR));
          int newColor = worldIn.getBlockState(pos).getValue(AbstractBlockControllableSignal.COLOR);
          player.sendMessage(new TextComponentString("Signal color set to " + colorName(newColor)));
          break;
        }
        case TOGGLE_ALTERNATE_FLASH: {
          boolean newVal = te.toggleAlternateFlash();
          player.sendMessage(new TextComponentString("Alternate flash: " + (newVal ? "ON (wig-wag B)" : "OFF (normal)")));
          break;
        }
        case OPEN_GUI:
          // Handled on client side above
          break;
      }

      return EnumActionResult.SUCCESS;
    }
    return EnumActionResult.SUCCESS;
  }

  private static String colorName(int color) {
    switch (color) {
      case 0: return "Red";
      case 1: return "Yellow";
      case 2: return "Green";
      case 3: return "Off";
      default: return "Unknown";
    }
  }

  public static ItemSignalHeadConfigToolMode getMode(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag != null && tag.hasKey(NBT_MODE_KEY)) {
      int ordinal = tag.getInteger(NBT_MODE_KEY);
      ItemSignalHeadConfigToolMode[] values = ItemSignalHeadConfigToolMode.values();
      if (ordinal >= 0 && ordinal < values.length) {
        return values[ordinal];
      }
    }
    return ItemSignalHeadConfigToolMode.OPEN_GUI;
  }

  public static void switchToNextMode(ItemStack stack) {
    ItemSignalHeadConfigToolMode current = getMode(stack);
    int next = (current.ordinal() + 1) % ItemSignalHeadConfigToolMode.values().length;
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
    list.add("Customize signal head appearance: body color, visor,");
    list.add("door color, bulb style, tilt, and more.");
    list.add("Right-click to apply. Sneak + right-click to switch modes.");
    list.add("Current mode: " + getMode(itemstack).getFriendlyName());
  }

  @Override
  public String getItemRegistryName() {
    return "signalheadconfigtool";
  }
}
