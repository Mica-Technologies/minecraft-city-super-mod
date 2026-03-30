package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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

public class ItemSignalHeadConfigTool extends AbstractItem {

  private final Map<UUID, ItemSignalHeadConfigToolMode> modeMap = new HashMap<>();

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    // OPEN_GUI mode must run on client side (GuiScreen has no server Container)
    if (worldIn.isRemote && !player.isSneaking()
        && getMode(player) == ItemSignalHeadConfigToolMode.OPEN_GUI
        && worldIn.getBlockState(pos).getBlock() instanceof AbstractBlockControllableSignalHead) {
      player.openGui(Csm.instance, 1, worldIn, pos.getX(), pos.getY(), pos.getZ());
      return EnumActionResult.SUCCESS;
    }
    if (!worldIn.isRemote) {
      IBlockState state = worldIn.getBlockState(pos);
      Block clickedBlock = state.getBlock();
      ItemSignalHeadConfigToolMode mode = getMode(player);

      // Sneak-click to change mode
      if (player.isSneaking()) {
        switchToNextMode(player);
        player.sendMessage(
            new TextComponentString("Signal Head Config Mode: " + getMode(player).getFriendlyName()));
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

  public ItemSignalHeadConfigToolMode getMode(EntityPlayer player) {
    return modeMap.getOrDefault(player.getUniqueID(),
        ItemSignalHeadConfigToolMode.CYCLE_BODY_COLOR);
  }

  public void switchToNextMode(EntityPlayer player) {
    ItemSignalHeadConfigToolMode current = getMode(player);
    int next = (current.ordinal() + 1) % ItemSignalHeadConfigToolMode.values().length;
    modeMap.put(player.getUniqueID(), ItemSignalHeadConfigToolMode.values()[next]);
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Configuration tool for custom rendering signal heads.");
    list.add("Right-click a signal head to apply the current mode.");
    list.add("Sneak + right-click to switch modes.");
  }

  @Override
  public String getItemRegistryName() {
    return "signalheadconfigtool";
  }
}
