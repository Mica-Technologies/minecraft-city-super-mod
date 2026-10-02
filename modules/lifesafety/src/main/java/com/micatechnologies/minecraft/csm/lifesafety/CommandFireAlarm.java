package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
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
 *   <li>{@code link <panel x y z> <x1 y1 z1> <x2 y2 z2>} -- links every appliance, initiating
 *   device and follower in the box to the panel (see {@link FireAlarmAreaLink}).</li>
 *   <li>{@code unlink <panel x y z> <x1 y1 z1> <x2 y2 z2>} -- unlinks every device in the box.</li>
 *   <li>{@code status <panel x y z>} -- how many devices the panel has, and which are missing.</li>
 *   <li>{@code prune <panel x y z>} -- unlinks the missing ones.</li>
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
    return "/csmfirealarm <link|unlink <panel x y z> <x1 y1 z1> <x2 y2 z2>"
        + "|status <panel x y z>|prune <panel x y z>>";
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
    BlockPos panelPos = parseBlockPos(sender, args, 1, false);
    TileEntity te = world.getTileEntity(panelPos);
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
        try {
          FireAlarmAreaLink.Result result = "link".equals(args[0])
              ? FireAlarmAreaLink.link(world, panel, a, b)
              : FireAlarmAreaLink.unlink(world, panel, a, b);
          say(sender, ("link".equals(args[0]) ? result.describeLink() : result.describeUnlink())
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
      default:
        throw new WrongUsageException(getUsage(sender));
    }
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
      return getListOfStringsMatchingLastWord(args, "link", "unlink", "status", "prune");
    }
    int limit = "link".equals(args[0]) || "unlink".equals(args[0]) ? 9 : 3;
    if (args.length >= 2 && args.length <= limit + 1) {
      return getTabCompletionCoordinate(args, 1 + ((args.length - 2) / 3) * 3, targetPos);
    }
    return Collections.emptyList();
  }
}
