package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * {@code /csmfirealarm}: wires a fire alarm panel from a command, for buildings too large to link
 * one aimed click at a time.
 *
 * <ul>
 *   <li>{@code link <panel x y z> <x1 y1 z1> <x2 y2 z2> [zone]} -- links every appliance,
 *   initiating device and follower in the box to the panel (see {@link FireAlarmAreaLink}), in a
 *   zone, 0 (general) if none is given (#335). With a residential smoke or CO alarm in place of
 *   the panel, it interconnects every such alarm in the box with that alarm's home system.</li>
 *   <li>{@code alarm <panel x y z> [zone]} -- puts the panel in alarm, every zone or just one.</li>
 *   <li>{@code unlink <panel x y z> <x1 y1 z1> <x2 y2 z2>} -- unlinks every device in the box.</li>
 *   <li>{@code status <panel x y z>} -- how many devices the panel has, and which are missing.</li>
 *   <li>{@code prune <panel x y z>} -- unlinks the missing ones.</li>
 *   <li>{@code alarm|drill|ack|silence|resound|reset <panel x y z>} -- operate the panel as its
 *   screen and the Signal Configuration Tool do, so a system can be tested by script with no
 *   player's hands on it (#330).</li>
 *   <li>{@code trigger <device x y z>} -- an initiating device reports to its panel, as a pull
 *   or a detection does.</li>
 * </ul>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class CommandFireAlarm extends CommandBase {

  /** How many missing positions {@code status} lists before it says "and N more". */
  private static final int LIST_LIMIT = 10;

  @Override
  public String getName() {
    return "csmfirealarm";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return "/csmfirealarm <link|unlink <panel x y z> <x1 y1 z1> <x2 y2 z2> [zone]"
        + "|alarm <panel x y z> [zone]"
        + "|status|prune|drill|ack|silence|resound|reset <panel x y z>"
        + "|trigger <device x y z>>";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 2;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args)
      throws CommandException {
    if (args.length < 4) {
      throw new WrongUsageException(getUsage(sender));
    }
    World world = sender.getEntityWorld();
    if ("trigger".equals(args[0])) {
      trigger(sender, world, parseBlockPos(sender, args, 1, false));
      return;
    }
    BlockPos panelPos = parseBlockPos(sender, args, 1, false);
    TileEntity te = world.getTileEntity(panelPos);
    if (("link".equals(args[0]) || "unlink".equals(args[0]))
        && world.getBlockState(panelPos).getBlock() instanceof BlockResidentialAlarm
        && te instanceof TileEntityFireAlarmSensor) {
      interconnect(sender, world, args, (TileEntityFireAlarmSensor) te);
      return;
    }
    if (!(te instanceof TileEntityFireAlarmControlPanel)) {
      throw new CommandException("No fire alarm control panel at " + describe(panelPos) + ".");
    }
    TileEntityFireAlarmControlPanel panel = (TileEntityFireAlarmControlPanel) te;
    switch (args[0]) {
      case "link":
      case "unlink": {
        if (args.length < 10) {
          throw new WrongUsageException("/csmfirealarm " + args[0]
              + " <panel x y z> <x1 y1 z1> <x2 y2 z2>");
        }
        BlockPos a = parseBlockPos(sender, args, 4, false);
        BlockPos b = parseBlockPos(sender, args, 7, false);
        int zone = args.length > 10 ? parseInt(args[10], 0, FireAlarmZones.MAX_ZONE)
            : FireAlarmZones.GENERAL;
        try {
          FireAlarmAreaLink.Result result = "link".equals(args[0])
              ? FireAlarmAreaLink.link(world, panel, a, b, zone)
              : FireAlarmAreaLink.unlink(world, panel, a, b);
          say(sender, ("link".equals(args[0]) ? result.describeLink() + " in "
              + FireAlarmZones.describeZone(zone) : result.describeUnlink())
              + " for the panel at " + describe(panelPos) + ".");
        } catch (IllegalArgumentException tooBig) {
          throw new CommandException(tooBig.getMessage());
        }
        break;
      }
      case "status":
        status(sender, world, panel);
        break;
      case "prune": {
        int removed = panel.removeMissingDevices();
        say(sender, removed == 0 ? "No missing devices on the panel at " + describe(panelPos) + "."
            : "Unlinked " + removed + " missing device" + (removed == 1 ? "" : "s")
                + " from the panel at " + describe(panelPos) + ".");
        break;
      }
      case "alarm":
        // No origin: the panel says only that there is an alarm, as for redstone or the API.
        // Every zone, unless a zone is named; a named zone joins an alarm already sounding.
        if (args.length > 4) {
          int zone = parseInt(args[4], 0, FireAlarmZones.MAX_ZONE);
          panel.alarmZone(zone);
          say(sender, "Put " + FireAlarmZones.describeZone(zone) + " of the panel at "
              + describe(panelPos) + " in alarm.");
        } else if (panel.getAlarmState()) {
          say(sender, "The panel at " + describe(panelPos) + " is already in alarm.");
        } else {
          panel.setAlarmState(true);
          say(sender, "Put the panel at " + describe(panelPos) + " in alarm.");
        }
        break;
      case "drill":
        if (panel.getAlarmState()) {
          throw new CommandException("The panel at " + describe(panelPos)
              + " is already in alarm; a drill starts from normal.");
        }
        panel.startDrill();
        say(sender, "Started a drill on the panel at " + describe(panelPos) + ".");
        break;
      case "ack":
        if (!panel.getAlarmState() && !panel.getTrouble()) {
          say(sender, "Nothing to acknowledge on the panel at " + describe(panelPos) + ".");
        } else {
          panel.acknowledge();
          say(sender, "Acknowledged the panel at " + describe(panelPos) + ".");
        }
        break;
      case "silence":
        requireAlarm(panel, panelPos, "silence");
        panel.setAudibleSilence(true);
        say(sender, "Silenced the panel at " + describe(panelPos) + " (strobes stay on).");
        break;
      case "resound":
        requireAlarm(panel, panelPos, "resound");
        panel.setAudibleSilence(false);
        say(sender, "Re-sounded the panel at " + describe(panelPos) + ".");
        break;
      case "reset":
        if (!panel.getAlarmState()) {
          say(sender, "The panel at " + describe(panelPos) + " is not in alarm; nothing to reset.");
        } else {
          panel.setAlarmState(false);
          say(sender, "Reset the panel at " + describe(panelPos) + ".");
        }
        break;
      default:
        throw new WrongUsageException(getUsage(sender));
    }
  }

  private static void requireAlarm(TileEntityFireAlarmControlPanel panel, BlockPos panelPos,
      String action) throws CommandException {
    if (!panel.getAlarmState()) {
      throw new CommandException("The panel at " + describe(panelPos) + " is not in alarm; "
          + "nothing to " + action + ".");
    }
  }

  /**
   * An initiating device reports to its panel, exactly as a pull or a detection makes it: the
   * panel records it as the alarm's origin if it is the first.
   */
  private static void trigger(ICommandSender sender, World world, BlockPos devicePos)
      throws CommandException {
    Block block = world.getBlockState(devicePos).getBlock();
    if (!(block instanceof AbstractBlockFireAlarmActivator)) {
      throw new CommandException("No pull station or detector at " + describe(devicePos) + ".");
    }
    TileEntity te = world.getTileEntity(devicePos);
    if (block instanceof BlockResidentialAlarm && te instanceof TileEntityFireAlarmSensor
        && ((TileEntityFireAlarmSensor) te).getInterconnectGroup() != null) {
      // A home system of alarms with no panel: the alarm trips its group, as a detection does
      ((BlockResidentialAlarm) block).trip(world, devicePos, (TileEntityFireAlarmSensor) te);
      say(sender, "Triggered the alarm at " + describe(devicePos) + "; the "
          + ResidentialInterconnect.memberCount(world,
          ((TileEntityFireAlarmSensor) te).getInterconnectGroup())
          + " alarms interconnected with it are sounding.");
      return;
    }
    BlockPos panelPos = te instanceof TileEntityFireAlarmSensor
        ? ((TileEntityFireAlarmSensor) te).getLinkedPanelPos(world) : null;
    if (!((AbstractBlockFireAlarmActivator) block).activateLinkedPanel(world, devicePos, null)) {
      throw new CommandException("The device at " + describe(devicePos) + " reached no panel: "
          + (panelPos == null ? "it is not linked to one."
          : "its panel at " + describe(panelPos) + " is gone or not loaded."));
    }
    say(sender, "Triggered the device at " + describe(devicePos) + "; the panel at "
        + describe(panelPos) + " is in alarm.");
  }

  private static void status(ICommandSender sender, World world,
      TileEntityFireAlarmControlPanel panel) {
    List<BlockPos> appliances = panel.getConnectedAppliances();
    List<BlockPos> initiating = panel.getLinkedInitiatingDevices();
    List<BlockPos> missingAppliances = panel.getMissingAppliances();
    List<BlockPos> missingInitiating = panel.getMissingInitiatingDevices();
    int voiceEvac = 0;
    int unloaded = 0;
    for (BlockPos p : appliances) {
      if (!world.isBlockLoaded(p)) {
        unloaded++;
      } else if (world.getBlockState(p).getBlock()
          instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
        voiceEvac++;
      }
    }
    for (BlockPos p : initiating) {
      if (!world.isBlockLoaded(p)) {
        unloaded++;
      }
    }
    say(sender, "Fire alarm control panel at " + describe(panel.getPos()) + ":");
    // One line a script can match each step against: the state the panel's screen shows
    say(sender, "  state: " + panel.getStatusString()
        + "; alarm=" + panel.getAlarmState()
        + ", drill=" + panel.getDrill()
        + ", acknowledged=" + panel.getAcknowledged()
        + ", silenced=" + panel.getAudibleSilence()
        + ", trouble=" + panel.getTrouble()
        + (panel.getAlarmOriginPos() != null
        ? ", origin=" + describe(panel.getAlarmOriginPos()) : "")
        + ", zones=" + zoneList(panel.getAlarmZones())
        + (panel.getAlarmOriginPos() != null ? ", originZone=" + panel.getAlarmOriginZone() : "")
        + (panel.getCarbonMonoxideOnly() ? ", co=true" : ""));
    say(sender, "  " + count(appliances.size(), "appliance") + " ("
        + count(voiceEvac, "voice evac speaker") + "), "
        + count(initiating.size(), "initiating device"));
    say(sender, "  " + count(missingAppliances.size(), "missing appliance") + ", "
        + count(missingInitiating.size(), "missing initiating device")
        + (unloaded > 0 ? "; " + unloaded + " not loaded, so not checked" : ""));
    List<BlockPos> missing = new ArrayList<>(missingAppliances);
    missing.addAll(missingInitiating);
    for (int i = 0; i < Math.min(LIST_LIMIT, missing.size()); i++) {
      say(sender, "  missing: " + describe(missing.get(i)));
    }
    if (missing.size() > LIST_LIMIT) {
      say(sender, "  and " + (missing.size() - LIST_LIMIT) + " more; /csmfirealarm prune "
          + "unlinks them all");
    }
  }

  /** The zones in alarm for a script to match: {@code none}, {@code all} or {@code 1,3}. */
  private static String zoneList(int mask) {
    if (mask == 0) {
      return "none";
    }
    if ((mask & FireAlarmZones.ALL) == FireAlarmZones.ALL) {
      return "all";
    }
    StringBuilder sb = new StringBuilder();
    for (int z = 1; z <= FireAlarmZones.MAX_ZONE; z++) {
      if ((mask & (1 << z)) != 0) {
        sb.append(sb.length() == 0 ? "" : ",").append(z);
      }
    }
    return sb.toString();
  }

  /**
   * {@code link|unlink <alarm x y z> <x1 y1 z1> <x2 y2 z2>} with a residential alarm in place of
   * a panel: interconnects (or disconnects) every smoke and CO alarm in the box with that alarm's
   * home system (#335).
   */
  private static void interconnect(ICommandSender sender, World world, String[] args,
      TileEntityFireAlarmSensor head) throws CommandException {
    if (args.length < 10) {
      throw new WrongUsageException("/csmfirealarm " + args[0]
          + " <alarm x y z> <x1 y1 z1> <x2 y2 z2>");
    }
    BlockPos a = parseBlockPos(sender, args, 4, false);
    BlockPos b = parseBlockPos(sender, args, 7, false);
    try {
      String text = "link".equals(args[0])
          ? FireAlarmAreaLink.describeInterconnect(FireAlarmAreaLink.interconnect(world, head, a, b))
          : FireAlarmAreaLink.disconnect(world, head, a, b).describeUnlink();
      say(sender, text + " with the alarm at " + describe(head.getPos()) + ".");
    } catch (IllegalArgumentException tooBig) {
      throw new CommandException(tooBig.getMessage());
    }
  }

  private static String count(int n, String noun) {
    return n + " " + noun + (n == 1 ? "" : "s");
  }

  private static void say(ICommandSender sender, String text) {
    sender.sendMessage(new TextComponentString(text));
  }

  private static String describe(BlockPos pos) {
    return pos.getX() + " " + pos.getY() + " " + pos.getZ();
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "link", "unlink", "status", "prune", "alarm",
          "drill", "ack", "silence", "resound", "reset", "trigger");
    }
    int limit = "link".equals(args[0]) || "unlink".equals(args[0]) ? 9 : 3;
    if (args.length >= 2 && args.length <= limit + 1) {
      return getTabCompletionCoordinate(args, 1 + ((args.length - 2) / 3) * 3, targetPos);
    }
    return Collections.emptyList();
  }
}
