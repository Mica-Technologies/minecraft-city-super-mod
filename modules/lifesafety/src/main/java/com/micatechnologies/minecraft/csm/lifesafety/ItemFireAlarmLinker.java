package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.api.CsmFireAlarmLink;
import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.lifesafety.stations.ILinkedDeviceController;
import java.util.List;
import net.minecraft.block.state.IBlockState;
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
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

/**
 * Hand-held item used to link fire alarm devices (pull stations, detectors, sounders) to a
 * fire alarm control panel. Click the panel first, then click each device to establish the link.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class ItemFireAlarmLinker extends AbstractItem {

  /**
   * Where the selected panel is remembered on the stack. A long rather than three integers, since
   * {@link BlockPos} already packs and unpacks itself that way.
   */
  private static final String SELECTED_PANEL_KEY = "SelectedPanelPos";

  /** The zone every link this linker makes is stamped with (#335); absent is zone 0. */
  private static final String ZONE_KEY = "Zone";

  /**
   * The most recent selection made by anybody, kept only to answer the deprecated no-argument
   * {@link #getSelectedPanel()}. Not read by anything else in this class.
   */
  private BlockPos lastSelectedPanelPos = null;

  public ItemFireAlarmLinker() {
    super(0, 1);
  }

  /**
   * Gets the fire alarm control panel the given linker has selected, or null if it has none.
   * <p>
   * Exposed so that other mods can take part in linking. The linker itself only knows how to attach
   * CSM's own sounders and sensors to a panel, and teaching it about every block another mod might
   * want to link would mean CSM knowing about those mods. Letting the other mod read the selection
   * inverts that: it can handle its own blocks with this same tool, and CSM needs to know nothing
   * about it.
   * <p>
   * The selection lives on the stack, so two players wiring two buildings do not overwrite each
   * other, and a linker keeps its selection when it is put down, stored in a chest, or logged out
   * with. The item itself is a singleton and is the wrong place to have kept it.
   *
   * @param stack the linker being used; anything else, or an empty stack, selects nothing
   *
   * @return the selected fire alarm control panel position, or null
   *
   * @since 2026.08.10
   */
  public static BlockPos getSelectedPanel(ItemStack stack) {
    if (stack == null || !(stack.getItem() instanceof ItemFireAlarmLinker)) {
      return null;
    }
    NBTTagCompound tag = stack.getTagCompound();
    return tag == null || !tag.hasKey(SELECTED_PANEL_KEY, Constants.NBT.TAG_LONG)
        ? null
        : BlockPos.fromLong(tag.getLong(SELECTED_PANEL_KEY));
  }

  /**
   * Remembers a panel on the stack, or forgets it when given null.
   */
  static void setSelectedPanel(ItemStack stack, BlockPos panelPos) {
    if (stack == null || stack.isEmpty()) {
      return;
    }
    NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
    if (panelPos == null) {
      tag.removeTag(SELECTED_PANEL_KEY);
    } else {
      tag.setLong(SELECTED_PANEL_KEY, panelPos.toLong());
    }
    stack.setTagCompound(tag);
  }

  /**
   * The zone a linker stamps on every link it makes (#335): 0, the general zone, until it is
   * cycled with a right-click in the air.
   *
   * @param stack the linker
   *
   * @return its zone, 0-8
   *
   * @since 2026.11
   */
  public static int getZone(ItemStack stack) {
    NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
    return tag == null ? FireAlarmZones.GENERAL : FireAlarmZones.clamp(tag.getInteger(ZONE_KEY));
  }

  static void setZone(ItemStack stack, int zone) {
    if (stack == null || stack.isEmpty()) {
      return;
    }
    NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
    int z = FireAlarmZones.clamp(zone);
    if (z == FireAlarmZones.GENERAL) {
      tag.removeTag(ZONE_KEY);
    } else {
      tag.setInteger(ZONE_KEY, z);
    }
    stack.setTagCompound(tag);
  }

  /**
   * @return the panel most recently selected by any player holding a linker, or null
   *
   * @deprecated The selection belongs to the stack, not to the item: this answers with whoever
   *     clicked a panel last, which on a server is not necessarily the player asking. Kept working
   *     so that consumers built against 2026.08.07 do not break. Use
   *     {@link #getSelectedPanel(ItemStack)}, which every caller can reach -- the stack is in hand
   *     wherever this is asked.
   * @since 2026.08.07
   */
  @Deprecated
  public BlockPos getSelectedPanel() {
    return lastSelectedPanelPos;
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
    IBlockState state = worldIn.getBlockState(pos);
    ItemStack heldStack = player.getHeldItem(hand);
    BlockPos alarmPanelPos = getSelectedPanel(heldStack);

    // Sneak-click unlinks: on a fire alarm panel it drops every device of that panel's that has
    // gone missing (they cannot be clicked, being gone); on a device it leaves the selected panel
    if (player.isSneaking() && unlink(worldIn, player, pos, state, alarmPanelPos)) {
      return EnumActionResult.SUCCESS;
    }

    // Emergency Services controllers (station alerting, warning sirens) are selected the way a
    // panel is, and offered each device clicked after.
    if (state.getBlock() instanceof ILinkedDeviceController.ControllerBlock
        && worldIn.getTileEntity(pos) instanceof ILinkedDeviceController) {
      setSelectedPanel(heldStack, pos);
      if (!worldIn.isRemote) {
        player.sendMessage(new TextComponentString("Linking to the "
            + ((ILinkedDeviceController) worldIn.getTileEntity(pos)).describe() + " at ("
            + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")"));
      }
      return EnumActionResult.SUCCESS;
    }
    if (alarmPanelPos != null
        && worldIn.getTileEntity(alarmPanelPos) instanceof ILinkedDeviceController) {
      ILinkedDeviceController controller =
          (ILinkedDeviceController) worldIn.getTileEntity(alarmPanelPos);
      ILinkedDeviceController.LinkResult result = controller.link(state.getBlock(), pos);
      if (!worldIn.isRemote) {
        String where = controller.describe() + " at (" + alarmPanelPos.getX() + ","
            + alarmPanelPos.getY() + "," + alarmPanelPos.getZ() + ")";
        player.sendMessage(new TextComponentString(
            result == ILinkedDeviceController.LinkResult.LINKED ? "Linked to the " + where
                : result == ILinkedDeviceController.LinkResult.ALREADY_LINKED
                    ? "Already linked to the " + where
                    : "That is not something the " + where + " drives"));
      }
      return EnumActionResult.SUCCESS;
    }

    // A residential alarm with no panel selected starts, or joins, a home system of its own
    if (state.getBlock() instanceof BlockResidentialAlarm
        && !(alarmPanelPos != null
        && worldIn.getTileEntity(alarmPanelPos) instanceof TileEntityFireAlarmControlPanel)) {
      if (!worldIn.isRemote) {
        linkResidential(worldIn, player, heldStack, pos, alarmPanelPos);
      }
      return EnumActionResult.SUCCESS;
    }

    // Save panel location if click on panel
    if (state.getBlock() instanceof BlockFireAlarmControlPanel) {
      setSelectedPanel(heldStack, pos);
      lastSelectedPanelPos = pos;
      if (!worldIn.isRemote) {
        player.sendMessage(new TextComponentString("Linking to fire alarm control panel at " +
            "(" +
            pos.getX() +
            "," +
            pos.getY() +
            "," +
            pos.getZ() +
            ")"));
      }
      return EnumActionResult.SUCCESS;
    }

    // Link alarm to panel if panel selected and alarm clicked
    if (alarmPanelPos != null &&
        worldIn.getTileEntity(alarmPanelPos) instanceof TileEntityFireAlarmControlPanel) {
      TileEntityFireAlarmControlPanel fireAlarmControlPanel
          = (TileEntityFireAlarmControlPanel) worldIn.getTileEntity(alarmPanelPos);

      if (state.getBlock() instanceof CsmFireAlarmLink.Follower) {
        // Another module's block that follows a panel (a fare gate, a station gate): it keeps
        // the panel and looks at it through Core, as a door holder does.
        boolean linked = ((CsmFireAlarmLink.Follower) state.getBlock())
            .followFireAlarmPanel(worldIn, pos, alarmPanelPos);
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString((linked ? "Now following" : "Already following")
              + " fire alarm control panel at (" + alarmPanelPos.getX() + ","
              + alarmPanelPos.getY() + "," + alarmPanelPos.getZ() + ")"));
        }
        return EnumActionResult.SUCCESS;
      } else if (state.getBlock() instanceof IFireAlarmPanelFollower) {
        // A door holder or annunciator: it keeps the panel it follows and looks at it; the panel
        // keeps no list of them.
        TileEntity followerTe = worldIn.getTileEntity(pos);
        if (followerTe instanceof TileEntityFireAlarmSensor) {
          TileEntityFireAlarmSensor.LinkResult result =
              ((TileEntityFireAlarmSensor) followerTe).setLinkedPanelPos(alarmPanelPos);
          if (!worldIn.isRemote) {
            String panelDescription = "fire alarm control panel at (" + alarmPanelPos.getX()
                + "," + alarmPanelPos.getY() + "," + alarmPanelPos.getZ() + ")";
            player.sendMessage(new TextComponentString(
                result == TileEntityFireAlarmSensor.LinkResult.ALREADY_LINKED
                    ? "Already following " + panelDescription
                    : "Now following " + panelDescription));
          }
        }
        return EnumActionResult.SUCCESS;
      } else if (state.getBlock() instanceof AbstractBlockFireAlarmSounder) {
        // Every link is stamped with the linker's zone (#335); clicking a linked appliance with
        // another zone moves it into that zone
        int zone = getZone(heldStack);
        boolean wasLinked = fireAlarmControlPanel.isLinkedAppliance(pos);
        int wasZone = fireAlarmControlPanel.getApplianceZone(pos);
        if (fireAlarmControlPanel.addLinkedAlarm(pos, zone)) {
          fireAlarmControlPanel.afterBulkLink();
        }
        if (!worldIn.isRemote) {
          String circuit = state.getBlock() instanceof AbstractBlockFireAlarmSounderVoiceEvac
              ? "voice evac circuit" : "main circuit";
          String where = circuit + " of fire alarm control panel at (" + alarmPanelPos.getX()
              + "," + alarmPanelPos.getY() + "," + alarmPanelPos.getZ() + "), "
              + FireAlarmZones.describeZone(zone);
          player.sendMessage(new TextComponentString(!wasLinked
              ? "Successfully linked to " + where
              : wasZone != zone ? "Moved from " + FireAlarmZones.describeZone(wasZone) + " to "
                  + where
                  : "Already linked to " + where));
        }
        return EnumActionResult.SUCCESS;
      } else if (state.getBlock() instanceof AbstractBlockFireAlarmActivator) {
        TileEntity tileEntityAtClickedPos = worldIn.getTileEntity(pos);
        if (tileEntityAtClickedPos instanceof TileEntityFireAlarmSensor) {
          linkInitiatingDevice(worldIn, player, (TileEntityFireAlarmSensor) tileEntityAtClickedPos,
              pos, alarmPanelPos, getZone(heldStack));
        }

        return EnumActionResult.SUCCESS;
      }
    } else {
      if (!worldIn.isRemote) {
        player.sendMessage(new TextComponentString("No panel selected!"));
      }
    }
    return EnumActionResult.FAIL;
  }

  /**
   * Points an initiating device at a panel in a zone, indexes it there and says what happened.
   * A residential alarm leaves any home system of alarms it was on: the panel is its system now.
   */
  static void linkInitiatingDevice(World world, EntityPlayer player,
      TileEntityFireAlarmSensor sensor, BlockPos pos, BlockPos panelPos, int zone) {
    TileEntityFireAlarmSensor.LinkResult result = sensor.setLinkedPanelPos(panelPos);
    boolean zoneChanged = sensor.setZone(zone);
    sensor.setInterconnectGroup(null);

    // Index the device on the panel too. The device stores which panel it reports to; the
    // panel needs the reverse to reach its devices when it is reset.
    TileEntity tileEntityAtPanelPos = world.getTileEntity(panelPos);
    if (tileEntityAtPanelPos instanceof TileEntityFireAlarmControlPanel) {
      TileEntityFireAlarmControlPanel panel = (TileEntityFireAlarmControlPanel) tileEntityAtPanelPos;
      panel.addLinkedInitiatingDevice(pos);
      if (zoneChanged) {
        // the panel reads the zone off the device; a strobe or a residential alarm moved to
        // another zone joins or leaves the alarm at the next tick
        panel.afterBulkLink();
      }
    }

    if (!world.isRemote) {
      String panelDescription = "alarm control panel at (" + panelPos.getX() + ","
          + panelPos.getY() + "," + panelPos.getZ() + "), " + FireAlarmZones.describeZone(zone);
      // Every outcome is reported. A device that was already linked used to be refused in
      // silence, which was indistinguishable from the click not registering.
      if (result == TileEntityFireAlarmSensor.LinkResult.RELINKED) {
        player.sendMessage(new TextComponentString("Moved activator to " + panelDescription));
      } else if (result == TileEntityFireAlarmSensor.LinkResult.ALREADY_LINKED) {
        player.sendMessage(new TextComponentString(zoneChanged
            ? "Moved activator to " + panelDescription
            : "Activator is already linked to " + panelDescription));
      } else {
        player.sendMessage(new TextComponentString(
            "Successfully linked activator to " + panelDescription));
      }
    }
  }

  /**
   * A linker click on a residential alarm with no panel selected (#335). With nothing (or not
   * another alarm) selected, the alarm becomes the selection: the home system starts from it.
   * With another alarm selected, this one joins that alarm's system: its panel if it has one,
   * otherwise its interconnect group, which is created, keyed by the selected alarm's position,
   * if it had none.
   */
  private static void linkResidential(World world, EntityPlayer player, ItemStack stack,
      BlockPos pos, BlockPos selectedPos) {
    TileEntity clickedTe = world.getTileEntity(pos);
    if (!(clickedTe instanceof TileEntityFireAlarmSensor)) {
      return;
    }
    TileEntityFireAlarmSensor clicked = (TileEntityFireAlarmSensor) clickedTe;
    TileEntity selectedTe = selectedPos == null ? null : world.getTileEntity(selectedPos);
    boolean selectedIsAlarm = selectedPos != null && !selectedPos.equals(pos)
        && world.getBlockState(selectedPos).getBlock() instanceof BlockResidentialAlarm
        && selectedTe instanceof TileEntityFireAlarmSensor;
    if (!selectedIsAlarm) {
      setSelectedPanel(stack, pos);
      player.sendMessage(new TextComponentString("Interconnecting smoke and CO alarms with the "
          + "alarm at " + describe(pos) + ". Click each other alarm in the home; sneak-click the "
          + "air when done."));
      return;
    }
    TileEntityFireAlarmSensor head = (TileEntityFireAlarmSensor) selectedTe;
    BlockPos headPanel = head.getLinkedPanelPos(world);
    if (headPanel != null && world.getTileEntity(headPanel) instanceof TileEntityFireAlarmControlPanel) {
      // The selected alarm reports to a panel, so this one does too
      linkInitiatingDevice(world, player, clicked, pos, headPanel, head.getZone());
      return;
    }
    BlockPos group = head.getInterconnectGroup();
    if (group == null) {
      group = selectedPos;
      head.setInterconnectGroup(group);
    }
    // An alarm leaving a panel for a group of alarms is unlinked from the panel
    BlockPos oldPanel = clicked.getLinkedPanelPos(world);
    if (oldPanel != null) {
      clicked.clearLinkedPanel();
      TileEntity oldPanelTe = world.getTileEntity(oldPanel);
      if (oldPanelTe instanceof TileEntityFireAlarmControlPanel) {
        ((TileEntityFireAlarmControlPanel) oldPanelTe).removeLinkedInitiatingDevice(pos);
      }
    }
    boolean changed = clicked.setInterconnectGroup(group);
    player.sendMessage(new TextComponentString((changed ? "Interconnected with" : "Already "
        + "interconnected with") + " the alarm at " + describe(selectedPos) + " ("
        + ResidentialInterconnect.memberCount(world, group) + " alarms in this home)"));
  }

  private static String describe(BlockPos pos) {
    return "(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")";
  }

  /**
   * Sneaking and right-clicking the air forgets the selected panel.
   * <p>
   * Needed because the selection now persists. It used to live on the item instance, which is to say
   * in memory, so restarting the game cleared it for everybody whether they wanted that or not --
   * there was nothing to forget and no way to ask. On the stack it survives being put down and
   * logged out with, so a linker carried from one building to the next would otherwise keep pointing
   * at a panel across the map, and the next click would wire a sounder to the wrong building.
   */
  /**
   * Handles a sneak-click. Returns {@code false} to let the click fall through to linking, which
   * is what happens when the selected controller is not a fire alarm panel.
   */
  private boolean unlink(World world, EntityPlayer player, BlockPos pos, IBlockState state,
      BlockPos panelPos) {
    TileEntity clicked = world.getTileEntity(pos);
    // A residential alarm on a home system of alarms leaves it
    if (state.getBlock() instanceof BlockResidentialAlarm
        && clicked instanceof TileEntityFireAlarmSensor
        && ((TileEntityFireAlarmSensor) clicked).getInterconnectGroup() != null) {
      ((TileEntityFireAlarmSensor) clicked).setInterconnectGroup(null);
      if (!world.isRemote) {
        player.sendMessage(new TextComponentString(
            "Disconnected this alarm from the other alarms in the home"));
      }
      return true;
    }
    if (state.getBlock() instanceof BlockFireAlarmControlPanel
        && clicked instanceof TileEntityFireAlarmControlPanel) {
      int removed = ((TileEntityFireAlarmControlPanel) clicked).removeMissingDevices();
      if (!world.isRemote) {
        String where = describePanel(pos);
        player.sendMessage(new TextComponentString(removed == 0
            ? "No missing devices on the " + where
            : "Unlinked " + removed + " missing device" + (removed == 1 ? "" : "s")
                + " from the " + where));
      }
      return true;
    }
    if (panelPos == null
        || !(world.getTileEntity(panelPos) instanceof TileEntityFireAlarmControlPanel)) {
      return false;
    }
    TileEntityFireAlarmControlPanel panel =
        (TileEntityFireAlarmControlPanel) world.getTileEntity(panelPos);
    boolean removed = panel.removeLinkedAlarm(pos);
    if (clicked instanceof TileEntityFireAlarmSensor) {
      // A pull station, detector, door holder or annunciator keeps its own link to the panel
      TileEntityFireAlarmSensor device = (TileEntityFireAlarmSensor) clicked;
      if (panelPos.equals(device.getLinkedPanelPos(world))) {
        device.clearLinkedPanel();
        removed = true;
      }
      removed |= panel.removeLinkedInitiatingDevice(pos);
    }
    if (!world.isRemote) {
      player.sendMessage(new TextComponentString(
          (removed ? "Unlinked from the " : "Not linked to the ") + describePanel(panelPos)));
    }
    return true;
  }

  private static String describePanel(BlockPos pos) {
    return "fire alarm control panel at (" + pos.getX() + "," + pos.getY() + "," + pos.getZ()
        + ")";
  }

  @Override
  public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player,
      EnumHand hand) {
    ItemStack heldStack = player.getHeldItem(hand);
    if (player.isSneaking() && getSelectedPanel(heldStack) != null) {
      setSelectedPanel(heldStack, null);
      if (!worldIn.isRemote) {
        player.sendMessage(new TextComponentString("Cleared the selected fire alarm control panel"));
      }
      return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
    }
    // A plain right-click in the air cycles the zone the next links get (#335). Only in the air:
    // the game also offers this to the item after a block click that did not link anything, and
    // that must not turn the zone.
    if (!player.isSneaking()) {
      RayTraceResult hit = rayTrace(worldIn, player, false);
      if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
        int zone = FireAlarmZones.next(getZone(heldStack));
        setZone(heldStack, zone);
        if (!worldIn.isRemote) {
          player.sendStatusMessage(new TextComponentString("Linker zone: "
              + FireAlarmZones.describeZone(zone)
              + (zone == FireAlarmZones.GENERAL ? ", sounds for every zone" : "")), true);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
      }
    }
    return super.onItemRightClick(worldIn, player, hand);
  }

  /**
   * Says which panel this particular linker is holding. Worth stating now that the selection is per
   * stack: with one shared selection there was nothing a player could usefully be told, since the
   * answer belonged to whoever clicked last rather than to the tool in their hand.
   */
  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Link fire alarm appliances to a fire alarm control panel");
    list.add("Sneak-click a device to unlink it from the selected panel");
    list.add("Sneak-click a panel to unlink its missing devices");
    list.add("Right-click the air to change the zone");
    list.add("Click a smoke or CO alarm, then the others, to interconnect a home");
    list.add("Zone: " + FireAlarmZones.describeZone(getZone(itemstack)));
    BlockPos selected = getSelectedPanel(itemstack);
    list.add(selected == null
        ? "No panel selected"
        : "Panel selected at (" + selected.getX() + "," + selected.getY() + "," + selected.getZ()
            + ")");
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
    return "firealarmlinker";
  }
}
