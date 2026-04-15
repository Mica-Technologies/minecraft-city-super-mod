package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
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
 * HVAC linking tool. All linking flows through the thermostat as the central controller:
 * <ul>
 *   <li><b>Click thermostat</b> — Select it as the linking source</li>
 *   <li><b>Then click heater/cooler</b> — Links the unit to the thermostat for auto control</li>
 *   <li><b>Then click vent relay</b> — Links the vent to the thermostat for distribution</li>
 *   <li><b>Click zone thermostat</b> — If primary selected and zone not yet linked, links zone to primary. If already linked (or no primary selected), selects zone as vent-linking source.</li>
 *   <li><b>Sneak+click</b> thermostat, zone, or vent — Clear all links</li>
 * </ul>
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class ItemHvacLinker extends AbstractItem {

  private BlockPos thermostatPos = null;

  public ItemHvacLinker() {
    super(0, 1);
  }

  @Override
  public EnumActionResult onItemUse(EntityPlayer player,
      World worldIn,
      BlockPos pos,
      EnumHand hand,
      EnumFacing facing,
      float hitX,
      float hitY,
      float hitZ) {
    TileEntity te = worldIn.getTileEntity(pos);

    // === Sneak+click: clear links ===
    if (player.isSneaking()) {
      if (te instanceof TileEntityHvacVentRelay) {
        ((TileEntityHvacVentRelay) te).clearLink();
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString("\u00A7aVent relay link cleared"));
        }
        return EnumActionResult.SUCCESS;
      }
      if (te instanceof TileEntityHvacZoneThermostat) {
        TileEntityHvacZoneThermostat zone = (TileEntityHvacZoneThermostat) te;
        int ventCount = zone.getLinkedVentCount();
        for (BlockPos ventPos : new java.util.ArrayList<>(zone.getLinkedVents())) {
          zone.unlinkVent(ventPos);
        }
        // Unlink from primary
        BlockPos primaryPos = zone.getLinkedPrimaryPos();
        if (primaryPos != null) {
          TileEntity primaryTe = worldIn.getTileEntity(primaryPos);
          if (primaryTe instanceof TileEntityHvacThermostat) {
            ((TileEntityHvacThermostat) primaryTe).unlinkZone(pos);
          }
          zone.setLinkedPrimaryPos(null);
        }
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString(
              "\u00A7aCleared " + ventCount + " vent(s) and unlinked from primary"));
        }
        return EnumActionResult.SUCCESS;
      }
      if (te instanceof TileEntityHvacThermostat) {
        TileEntityHvacThermostat thermostat = (TileEntityHvacThermostat) te;
        int unitCount = thermostat.getLinkedUnitCount();
        int ventCount = thermostat.getLinkedVentCount();
        int zoneCount = thermostat.getLinkedZoneCount();
        for (BlockPos unitPos : new java.util.ArrayList<>(thermostat.getLinkedUnits())) {
          thermostat.unlinkUnit(unitPos);
        }
        for (BlockPos ventPos : new java.util.ArrayList<>(thermostat.getLinkedVents())) {
          thermostat.unlinkVent(ventPos);
        }
        for (BlockPos zonePos : new java.util.ArrayList<>(thermostat.getLinkedZones())) {
          thermostat.unlinkZone(zonePos);
        }
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString(
              "\u00A7aCleared " + unitCount + " unit(s), " + ventCount + " vent(s), and "
                  + zoneCount + " zone(s)"));
        }
        return EnumActionResult.SUCCESS;
      }
    }

    // === Click primary thermostat: select as linking source ===
    if (te instanceof TileEntityHvacThermostat) {
      thermostatPos = pos;
      if (!worldIn.isRemote) {
        TileEntityHvacThermostat t = (TileEntityHvacThermostat) te;
        player.sendMessage(new TextComponentString(
            "\u00A7bSelected thermostat (" + t.getLinkedUnitCount() + " units, "
                + t.getLinkedVentCount() + " vents, " + t.getLinkedZoneCount()
                + " zones). Click a heater/cooler, vent relay, or zone thermostat."));
      }
      return EnumActionResult.SUCCESS;
    }

    // === Click zone thermostat ===
    if (te instanceof TileEntityHvacZoneThermostat) {
      TileEntityHvacZoneThermostat zone = (TileEntityHvacZoneThermostat) te;

      // If we have a primary thermostat selected, try to link the zone to it
      if (thermostatPos != null) {
        TileEntity selectedTe = worldIn.getTileEntity(thermostatPos);
        if (selectedTe instanceof TileEntityHvacThermostat) {
          TileEntityHvacThermostat primary = (TileEntityHvacThermostat) selectedTe;
          boolean success = primary.linkZone(pos);
          if (success) {
            zone.setLinkedPrimaryPos(thermostatPos);
            if (!worldIn.isRemote) {
              player.sendMessage(new TextComponentString(
                  "\u00A7aLinked zone to primary thermostat ("
                      + primary.getLinkedZoneCount() + " total zones). "
                      + "Click zone again to link vents to it."));
            }
            return EnumActionResult.SUCCESS;
          }
          // Already linked to this primary. Repair the back-link if the zone was
          // replaced and its new TE doesn't know its primary yet.
          if (!thermostatPos.equals(zone.getLinkedPrimaryPos())) {
            zone.setLinkedPrimaryPos(thermostatPos);
          }
          // Fall through to select the zone as the vent-linking source.
        }
      }

      // Select the zone thermostat as the linking source (for vent linking)
      thermostatPos = pos;
      if (!worldIn.isRemote) {
        player.sendMessage(new TextComponentString(
            "\u00A7bSelected zone thermostat (" + zone.getLinkedVentCount()
                + " vents). Click vent relays to link them to this zone."));
      }
      return EnumActionResult.SUCCESS;
    }

    // === Click heater/cooler: link to primary thermostat ===
    if (te instanceof TileEntityHvacHeater) {
      if (thermostatPos == null) {
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString(
              "\u00A7eClick a thermostat first to start linking."));
        }
        return EnumActionResult.FAIL;
      }
      TileEntity thermostatTe = worldIn.getTileEntity(thermostatPos);
      if (!(thermostatTe instanceof TileEntityHvacThermostat)) {
        if (!worldIn.isRemote) {
          if (thermostatTe instanceof TileEntityHvacZoneThermostat) {
            player.sendMessage(new TextComponentString(
                "\u00A7cZone thermostats cannot link directly to units. Select a primary thermostat."));
          } else {
            player.sendMessage(new TextComponentString(
                "\u00A7cThermostat no longer exists. Click a thermostat first."));
          }
        }
        thermostatPos = null;
        return EnumActionResult.FAIL;
      }
      TileEntityHvacThermostat thermostat = (TileEntityHvacThermostat) thermostatTe;
      boolean success = thermostat.linkUnit(pos);
      if (!worldIn.isRemote) {
        if (success) {
          player.sendMessage(new TextComponentString(
              "\u00A7aLinked unit to thermostat (" + thermostat.getLinkedUnitCount()
                  + " total units)"));
        } else {
          player.sendMessage(new TextComponentString(
              "\u00A7cUnit already linked to this thermostat"));
        }
      }
      return EnumActionResult.SUCCESS;
    }

    // === Click vent relay: link to thermostat (primary or zone) ===
    if (te instanceof TileEntityHvacVentRelay) {
      if (thermostatPos == null) {
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString(
              "\u00A7eClick a thermostat first to start linking."));
        }
        return EnumActionResult.FAIL;
      }
      TileEntity thermostatTe = worldIn.getTileEntity(thermostatPos);

      // Link to primary thermostat
      if (thermostatTe instanceof TileEntityHvacThermostat) {
        TileEntityHvacThermostat thermostat = (TileEntityHvacThermostat) thermostatTe;
        int maxDist = thermostat.getMaxVentLinkDistance();
        boolean success = thermostat.linkVent(pos, maxDist);
        if (!worldIn.isRemote) {
          if (success) {
            player.sendMessage(new TextComponentString(
                "\u00A7aLinked vent to thermostat (" + thermostat.getLinkedVentCount()
                    + " total vents)"));
          } else {
            player.sendMessage(new TextComponentString(
                "\u00A7cVent too far or already linked (max " + maxDist + " blocks)"));
          }
        }
        return EnumActionResult.SUCCESS;
      }

      // Link to zone thermostat
      if (thermostatTe instanceof TileEntityHvacZoneThermostat) {
        TileEntityHvacZoneThermostat zone = (TileEntityHvacZoneThermostat) thermostatTe;
        int maxDist = zone.getMaxVentLinkDistance();
        boolean success = zone.linkVent(pos, maxDist);
        if (!worldIn.isRemote) {
          if (success) {
            player.sendMessage(new TextComponentString(
                "\u00A7aLinked vent to zone thermostat (" + zone.getLinkedVentCount()
                    + " total vents)"));
          } else {
            player.sendMessage(new TextComponentString(
                "\u00A7cVent too far or already linked (max " + maxDist + " blocks)"));
          }
        }
        return EnumActionResult.SUCCESS;
      }

      if (!worldIn.isRemote) {
        player.sendMessage(new TextComponentString(
            "\u00A7cThermostat no longer exists. Click a thermostat first."));
      }
      thermostatPos = null;
      return EnumActionResult.FAIL;
    }

    // === Clicked something else ===
    if (!worldIn.isRemote) {
      if (thermostatPos != null) {
        player.sendMessage(new TextComponentString(
            "\u00A7eClick a heater/cooler, vent relay, or zone thermostat to link."));
      } else {
        player.sendMessage(new TextComponentString(
            "\u00A7eClick a thermostat to start linking."));
      }
    }
    return EnumActionResult.FAIL;
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Link HVAC components to a thermostat");
    list.add("\u00A77Click thermostat \u2192 heater/cooler (auto control)");
    list.add("\u00A77Click thermostat \u2192 vent relay (primary zone vents)");
    list.add("\u00A77Click thermostat \u2192 zone thermostat \u2192 zone thermostat \u2192 vent relay");
    list.add("\u00A77  (links zone, then selects it for vent linking)");
    list.add("\u00A77Sneak+click to clear links");
  }

  @Override
  public String getItemRegistryName() {
    return "hvaclinker";
  }
}
