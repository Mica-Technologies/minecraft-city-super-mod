package com.micatechnologies.minecraft.csm.healthcare;

import com.micatechnologies.minecraft.csm.healthcare.clinical.CeilingHung;
import com.micatechnologies.minecraft.csm.healthcare.nursecall.NurseCallNetwork;
import com.micatechnologies.minecraft.csm.healthcare.wards.BlockCurtainPanel;
import com.micatechnologies.minecraft.csm.healthcare.wards.BlockCurtainTrack;
import com.micatechnologies.minecraft.csm.healthcare.wards.CubicleCurtains;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * {@code /csmhealthcare}: wires the Healthcare module's systems from a command, for a hospital
 * built by a generator rather than by hand.
 *
 * <ul>
 *   <li>{@code nursecall link <parent x y z> <device x y z>} -- links a dome light or a call
 *   device to a master panel, or a call device to a dome light (its room).</li>
 *   <li>{@code nursecall unlink <parent x y z> <device x y z>} -- undoes one link.</li>
 *   <li>{@code nursecall status <x y z>} -- what a panel, dome light or device is linked to, and
 *   which of them are calling.</li>
 *   <li>{@code nursecall prune} -- drops every link to a block that is no longer there (loaded
 *   chunks only).</li>
 *   <li>{@code curtain fill <track x y z> [drop|auto] [pattern]} -- hangs the curtain under the
 *   whole track run: {@code drop} panels a column (1-15), or {@code auto}, down to one block above
 *   the floor; pattern {@code mint}, {@code blue_check}, {@code beige} or {@code white}.</li>
 *   <li>{@code curtain open|close <x y z>} -- opens or closes the curtain at a track or panel
 *   (opened, it bunches at the run's end nearest that position).</li>
 *   <li>{@code curtain pattern <x y z> <pattern>} -- changes a curtain's cloth.</li>
 *   <li>{@code curtain clear <track x y z>} -- takes down the curtain under a track run.</li>
 *   <li>{@code boom fill <head x y z>} -- fills the drop column above a ceiling-hung head (the
 *   ICU pendant, the surgical equipment boom, the trauma exam light) up to the ceiling.</li>
 *   <li>{@code boom clear <head x y z>} -- takes the column above a head down, leaving the
 *   head.</li>
 * </ul>
 *
 * @since 2026.10
 */
public class CommandHealthcare extends CommandBase {

  private static final String USAGE = "/csmhealthcare <nursecall <link|unlink <parent x y z> "
      + "<device x y z>|status <x y z>|prune>|curtain <fill <x y z> [drop|auto] [pattern]"
      + "|open|close|clear <x y z>|pattern <x y z> <pattern>>|boom <fill|clear> <x y z>>";

  @Override
  public String getName() {
    return "csmhealthcare";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return USAGE;
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 2;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args)
      throws CommandException {
    if (args.length >= 2 && "boom".equals(args[0])) {
      boom(sender, args);
      return;
    }
    if (args.length >= 2 && "curtain".equals(args[0])) {
      curtain(sender, args);
      return;
    }
    if (args.length < 2 || !"nursecall".equals(args[0])) {
      throw new WrongUsageException(USAGE);
    }
    World world = sender.getEntityWorld();
    NurseCallNetwork net = NurseCallNetwork.get(world);
    switch (args[1]) {
      case "link":
      case "unlink": {
        if (args.length < 8) {
          throw new WrongUsageException("/csmhealthcare nursecall " + args[1]
              + " <parent x y z> <device x y z>");
        }
        BlockPos parent = parseBlockPos(sender, args, 2, false);
        BlockPos child = parseBlockPos(sender, args, 5, false);
        if ("link".equals(args[1])) {
          try {
            net.link(world, parent, child);
          } catch (IllegalArgumentException e) {
            throw new CommandException("Cannot link: " + e.getMessage() + ".");
          }
          say(sender, "Linked " + NurseCallNetwork.describe(child) + " to "
              + NurseCallNetwork.describe(parent) + ".");
        } else {
          say(sender, net.unlink(world, parent, child)
              ? "Unlinked " + NurseCallNetwork.describe(child) + " from "
                  + NurseCallNetwork.describe(parent) + "."
              : NurseCallNetwork.describe(child) + " is not linked to "
                  + NurseCallNetwork.describe(parent) + ".");
        }
        break;
      }
      case "status": {
        if (args.length < 5) {
          throw new WrongUsageException("/csmhealthcare nursecall status <x y z>");
        }
        status(sender, world, net, parseBlockPos(sender, args, 2, false));
        break;
      }
      case "prune": {
        int n = net.prune(world);
        say(sender, n == 0 ? "No missing nurse call blocks."
            : "Dropped the links of " + n + " missing nurse call block" + (n == 1 ? "" : "s")
                + ".");
        break;
      }
      default:
        throw new WrongUsageException(USAGE);
    }
  }

  private static void curtain(ICommandSender sender, String[] args) throws CommandException {
    if (args.length < 5) {
      throw new WrongUsageException(USAGE);
    }
    World world = sender.getEntityWorld();
    BlockPos pos = parseBlockPos(sender, args, 2, false);
    boolean track = world.getBlockState(pos).getBlock() instanceof BlockCurtainTrack;
    boolean panel = world.getBlockState(pos).getBlock() instanceof BlockCurtainPanel;
    switch (args[1]) {
      case "fill": {
        if (!track) {
          throw new CommandException("No cubicle curtain track at "
              + NurseCallNetwork.describe(pos) + ".");
        }
        Integer drop = null;
        if (args.length > 5 && !"auto".equals(args[5])) {
          drop = parseInt(args[5], 1, CubicleCurtains.MAX_DROP);
        }
        BlockCurtainPanel.Pattern pattern = BlockCurtainPanel.Pattern.MINT;
        if (args.length > 6) {
          pattern = BlockCurtainPanel.Pattern.byName(args[6]);
          if (pattern == null) {
            throw new CommandException("Unknown pattern " + args[6]
                + "; use mint, blue_check, beige or white.");
          }
        }
        int hung = CubicleCurtains.fillRun(world, pos, drop, pattern);
        say(sender, "Hung " + hung + " curtain panels under "
            + CubicleCurtains.trackRun(world, pos).size() + " track cells.");
        break;
      }
      case "clear":
        if (!track) {
          throw new CommandException("No cubicle curtain track at "
              + NurseCallNetwork.describe(pos) + ".");
        }
        CubicleCurtains.clearRun(world, pos);
        say(sender, "Took down the curtain under the track run.");
        break;
      case "open":
      case "close":
      case "pattern": {
        BlockPos at = track ? pos.down() : pos;
        if (!track && !panel || !BlockCurtainPanel.isPanel(world, at)) {
          throw new CommandException("No cubicle curtain at " + NurseCallNetwork.describe(pos)
              + ".");
        }
        if ("pattern".equals(args[1])) {
          BlockCurtainPanel.Pattern pattern = args.length > 5
              ? BlockCurtainPanel.Pattern.byName(args[5]) : null;
          if (pattern == null) {
            throw new CommandException("Give a pattern: mint, blue_check, beige or white.");
          }
          CubicleCurtains.setPattern(world, at, pattern);
        } else {
          CubicleCurtains.setOpen(world, at, "open".equals(args[1]));
        }
        say(sender, "Done.");
        break;
      }
      default:
        throw new WrongUsageException(USAGE);
    }
  }

  private static void boom(ICommandSender sender, String[] args) throws CommandException {
    if (args.length < 5) {
      throw new WrongUsageException("/csmhealthcare boom <fill|clear> <head x y z>");
    }
    World world = sender.getEntityWorld();
    BlockPos head = parseBlockPos(sender, args, 2, false);
    if (!CeilingHung.isHead(world.getBlockState(head).getBlock())) {
      throw new CommandException("No ceiling-hung head at " + NurseCallNetwork.describe(head)
          + ".");
    }
    if ("fill".equals(args[1])) {
      int n = CeilingHung.fillColumn(world, head);
      if (n < 0) {
        throw new CommandException("No ceiling within " + CeilingHung.MAX_COLUMN
            + " blocks above " + NurseCallNetwork.describe(head) + ".");
      }
      say(sender, "Placed " + n + " column cell" + (n == 1 ? "" : "s") + ".");
    } else if ("clear".equals(args[1])) {
      int n = CeilingHung.clearColumn(world, head);
      say(sender, "Removed " + n + " column cell" + (n == 1 ? "" : "s") + ".");
    } else {
      throw new WrongUsageException("/csmhealthcare boom <fill|clear> <head x y z>");
    }
  }

  private static void status(ICommandSender sender, World world, NurseCallNetwork net,
      BlockPos pos) throws CommandException {
    NurseCallNetwork.Kind kind = NurseCallNetwork.kindOf(world, pos);
    if (kind == null) {
      throw new CommandException("No nurse call block at " + NurseCallNetwork.describe(pos)
          + ".");
    }
    say(sender, "Nurse call " + kind.name().toLowerCase() + " at "
        + NurseCallNetwork.describe(pos) + ":");
    BlockPos parent = net.parentOf(pos);
    if (parent != null) {
      say(sender, "  linked to " + NurseCallNetwork.describe(parent));
    } else if (kind != NurseCallNetwork.Kind.PANEL) {
      say(sender, "  not linked to anything");
    }
    if (kind == NurseCallNetwork.Kind.DEVICE) {
      say(sender, "  " + (net.isCalling(pos) ? "calling" : "not calling"));
      return;
    }
    List<Long> kids = net.childrenOf(pos.toLong());
    say(sender, "  " + kids.size() + (kind == NurseCallNetwork.Kind.PANEL ? " room" : " device")
        + (kids.size() == 1 ? "" : "s") + (kind == NurseCallNetwork.Kind.DOME
        ? (net.roomCalling(pos.toLong()) ? ", calling" : ", quiet") : ""));
    for (int i = 0; i < kids.size(); i++) {
      long k = kids.get(i);
      BlockPos kp = BlockPos.fromLong(k);
      boolean calling = kind == NurseCallNetwork.Kind.PANEL ? net.roomCalling(k)
          : net.isCalling(kp);
      say(sender, "  " + (kind == NurseCallNetwork.Kind.PANEL ? "room " + (i + 1) + ": " : "")
          + NurseCallNetwork.describe(kp) + (calling ? "  CALLING" : "")
          + (world.isBlockLoaded(kp) && NurseCallNetwork.kindOf(world, kp) == null
          ? "  (missing)" : ""));
    }
  }

  private static void say(ICommandSender sender, String text) {
    sender.sendMessage(new TextComponentString(text));
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "nursecall", "curtain", "boom");
    }
    if ("boom".equals(args[0])) {
      if (args.length == 2) {
        return getListOfStringsMatchingLastWord(args, "fill", "clear");
      }
      if (args.length >= 3 && args.length <= 5) {
        return getTabCompletionCoordinate(args, 2, targetPos);
      }
      return Collections.emptyList();
    }
    if ("curtain".equals(args[0])) {
      if (args.length == 2) {
        return getListOfStringsMatchingLastWord(args, "fill", "open", "close", "pattern",
            "clear");
      }
      if (args.length >= 3 && args.length <= 5) {
        return getTabCompletionCoordinate(args, 2, targetPos);
      }
      if (args.length == 6 && "fill".equals(args[1])) {
        return getListOfStringsMatchingLastWord(args, "auto", "3", "5", "7");
      }
      if (args.length == 7 && "fill".equals(args[1]) || args.length == 6
          && "pattern".equals(args[1])) {
        return getListOfStringsMatchingLastWord(args, "mint", "blue_check", "beige", "white");
      }
      return Collections.emptyList();
    }
    if (args.length == 2) {
      return getListOfStringsMatchingLastWord(args, "link", "unlink", "status", "prune");
    }
    int last = "status".equals(args[1]) ? 4 : "prune".equals(args[1]) ? 1 : 7;
    if (args.length >= 3 && args.length <= last + 1) {
      return getTabCompletionCoordinate(args, 2 + ((args.length - 3) / 3) * 3, targetPos);
    }
    return Collections.emptyList();
  }
}
