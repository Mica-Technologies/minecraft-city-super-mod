package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

public class ItemEWSignalLinker extends AbstractItem {

  private final Map<UUID, BlockPos> sensorPosMap = new HashMap<>();
  private final Map<UUID, BlockPos> corner1PosMap = new HashMap<>();
  private final Map<UUID, Integer> modeMap = new HashMap<>();

  @Override
  public EnumActionResult onItemUse(EntityPlayer player,
      World worldIn,
      BlockPos pos,
      EnumHand hand,
      EnumFacing facing,
      float hitX,
      float hitY,
      float hitZ) {
    if (!worldIn.isRemote) {
      IBlockState state = worldIn.getBlockState(pos);
      if (state.getBlock() instanceof AbstractBlockTrafficSignalSensor) {
        modeMap.put(player.getUniqueID(), 1);
        sensorPosMap.put(player.getUniqueID(), pos);
        corner1PosMap.remove(player.getUniqueID());
        player.sendMessage(new TextComponentString("Selected sensor at position: [" +
            pos.getX() +
            ", " +
            pos.getY() +
            ", " +
            pos.getZ() +
            "]. " +
            "Please select corner #1 of sensor search " +
            "box!"));

        return EnumActionResult.SUCCESS;
      } else if (player.isSneaking()) {
        // Increment mode
        int currentMode = modeMap.getOrDefault(player.getUniqueID(), 1);
        int newMode = currentMode + 1;
        if (newMode > 3) {
          newMode = 1;
        }

        // Update mode
        modeMap.put(player.getUniqueID(), newMode);

        player.sendMessage(
            new TextComponentString("Configuring sensor detection zone: " + getModeName(player)));

        return EnumActionResult.SUCCESS;
      } else if (sensorPosMap.getOrDefault(player.getUniqueID(), null) != null &&
          corner1PosMap.getOrDefault(player.getUniqueID(), null) == null) {
        corner1PosMap.put(player.getUniqueID(), pos);
        player.sendMessage(new TextComponentString(
            "Search box corner 1 set to: [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                + "]."));

        return EnumActionResult.SUCCESS;
      } else if (sensorPosMap.getOrDefault(player.getUniqueID(), null) != null &&
          corner1PosMap.getOrDefault(player.getUniqueID(), null) != null) {
        BlockPos corner1Pos = corner1PosMap.getOrDefault(player.getUniqueID(), null);
        BlockPos corner2Pos = pos;
        if (corner1Pos.getY() > corner2Pos.getY()) {
          corner1Pos = corner1Pos.add(0, 4, 0);
        } else {
          corner2Pos = corner2Pos.add(0, 4, 0);
        }
        player.sendMessage(new TextComponentString(
            "Search box corner 2 set to: [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                + "]."));

        try {
          TileEntity tileEntity = worldIn.getTileEntity(
              sensorPosMap.getOrDefault(player.getUniqueID(), null));
          if (tileEntity instanceof TileEntityTrafficSignalSensor) {
            TileEntityTrafficSignalSensor tileEntityTrafficSignalSensor
                = (TileEntityTrafficSignalSensor) tileEntity;
            boolean overwrote;
            int currentMode = modeMap.getOrDefault(player.getUniqueID(), 1);
            if (currentMode == 2) {
              overwrote = tileEntityTrafficSignalSensor.setLeftScanCorners(corner1Pos, corner2Pos);
            } else if (currentMode == 3) {
              overwrote =
                  tileEntityTrafficSignalSensor.setProtectedScanCorners(corner1Pos, corner2Pos);
            } else {
              overwrote = tileEntityTrafficSignalSensor.setScanCorners(corner1Pos, corner2Pos);
            }

            if (overwrote) {
              player.sendMessage(
                  new TextComponentString("The selected search box corners have been " +
                      "applied to the following sensor detection zone " +
                      "successfully: " +
                      getModeName(player) +
                      " (Replaced previous " +
                      "search box)"));
            } else {
              player.sendMessage(
                  new TextComponentString("The selected search box corners have been " +
                      "applied to the following sensor detection zone " +
                      "successfully: " +
                      getModeName(player)));
            }

          } else {
            player.sendMessage(
                new TextComponentString("The selected traffic signal sensor did not " +
                    "respond to programming. Please " +
                    "replace the sensor and try again!"));

          }
        } catch (Exception e) {
          player.sendMessage(new TextComponentString("An error occurred while programming the " +
              "selected traffic signal sensor. Please " +
              "replace the sensor and try again!"));

        }

        corner1PosMap.put(player.getUniqueID(), null);
      } else {
        player.sendMessage(
            new TextComponentString("Please select a sensor to begin configuration!"));
      }
    }

    return EnumActionResult.SUCCESS;
  }

  public String getModeName(EntityPlayer player) {
    int currentMode = modeMap.getOrDefault(player.getUniqueID(), 1);

    // Get mode name string
    String modeName = "";
    if (currentMode == 1) {
      modeName = "Standard Lane(s) (Through/Right Turn)";
    } else if (currentMode == 2) {
      modeName = "Left Turn Lane(s)";
    } else if (currentMode == 3) {
      modeName = "Protected Lane(s) (Bus/Train/Bike)";
    }

    return modeName;
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Link traffic signals to the Secondary circuit of a signal controller.");
  }

  /**
   * Retrieves the registry name of the item.
   *
   * @return The registry name of the item.
   *
   * @since 1.0
   */
  @Override
  public String getItemRegistryName() {
    return "ewsignallinker";
  }
}
