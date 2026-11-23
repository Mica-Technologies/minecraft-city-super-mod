package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CommandCsm;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AdvancedPlanJson;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPatternSelect;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficTimeOfDaySchedule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * {@code /csm signal export|import <name> [x y z]}: writes the ADVANCED plan of a signal
 * controller to {@code <world>/csm/timing/<name>.json}, or loads one from there, in SPATT's
 * {@code csm-asc3-plan} format ({@link AdvancedPlanJson}). The controller is the one at the
 * given position, or else the one the player is looking at. An import is one edit: it resets the
 * controller's runtime and syncs, as a panel edit does, and a file that does not parse changes
 * nothing.
 *
 * <p>{@code /csm signal pattern [clock|1-4] [x y z]}: with no value, says which coordination
 * pattern the controller is running and who chose it (the clock, a manual hold or a pattern select
 * switch); with one, holds that pattern by hand, or hands the choice back to the clock
 * ({@link TileEntityTrafficSignalController#setManualPattern}). The controller is not restarted:
 * it changes pattern at its next cycle boundary.</p>
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class CommandCsmSignal implements CommandCsm.Subcommand {

  /** How far a player may be looking to pick a controller, in blocks. */
  private static final double LOOK_RANGE = 8.0;

  /** Plan names: letters, digits, dot, dash and underscore, so a name is never a path. */
  private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9._-]{0,63}");

  @Override
  public String usage() {
    return "<export|import> <name> [x y z] | pattern [clock|1-4] [x y z]";
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args)
      throws CommandException {
    if (args.length >= 1 && "pattern".equalsIgnoreCase(args[0])) {
      pattern(sender, args);
      return;
    }
    if (args.length != 2 && args.length != 5) {
      throw new WrongUsageException("/csm signal " + usage());
    }
    String action = args[0].toLowerCase();
    if (!"export".equals(action) && !"import".equals(action)) {
      throw new WrongUsageException("/csm signal " + usage());
    }
    String name = planName(args[1]);
    World world = sender.getEntityWorld();
    TileEntityTrafficSignalController controller = controllerAt(sender, world,
        args.length == 5 ? CommandBase.parseBlockPos(sender, args, 2, false) : lookedAt(sender));
    File file = new File(timingDir(server), name + ".json");
    if ("export".equals(action)) {
      try {
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(),
            controller.exportAdvancedPlanJson(name).getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new CommandException("Could not write " + file.getName() + ": " + e.getMessage());
      }
      tell(sender, TextFormatting.GREEN, "Exported the controller's plan to csm/timing/"
          + file.getName() + ".");
      return;
    }
    if (!file.isFile()) {
      throw new CommandException("No plan named " + name + " in csm/timing.");
    }
    if (file.length() > AdvancedPlanJson.MAX_LENGTH * 4L) {
      throw new CommandException(file.getName() + " is too large to be a plan.");
    }
    String json;
    try {
      json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CommandException("Could not read " + file.getName() + ": " + e.getMessage());
    }
    try {
      tell(sender, TextFormatting.GREEN, controller.importAdvancedPlanJson(json));
    } catch (AdvancedPlanJson.PlanFormatException e) {
      throw new CommandException("Plan not loaded: " + e.getMessage());
    }
  }

  /** {@code pattern [clock|1-4] [x y z]}: 1 or 4 arguments report, 2 or 5 set. */
  private static void pattern(ICommandSender sender, String[] args) throws CommandException {
    if (args.length != 1 && args.length != 2 && args.length != 4 && args.length != 5) {
      throw new WrongUsageException("/csm signal pattern [clock|1-4] [x y z]");
    }
    boolean set = args.length == 2 || args.length == 5;
    int at = set ? 2 : 1;
    TileEntityTrafficSignalController controller = controllerAt(sender, sender.getEntityWorld(),
        args.length > at ? CommandBase.parseBlockPos(sender, args, at, false) : lookedAt(sender));
    if (set) {
      controller.setManualPattern(patternSlot(args[1]));
    }
    tell(sender, TextFormatting.GREEN, controller.describePattern() + ".");
  }

  /**
   * The slot a pattern argument names: {@code clock} (also {@code auto}, {@code off}) for none,
   * or 1 to 4.
   */
  static int patternSlot(String raw) throws CommandException {
    String value = raw.toLowerCase();
    if ("clock".equals(value) || "auto".equals(value) || "off".equals(value)) {
      return TrafficSignalPatternSelect.NONE;
    }
    try {
      int number = Integer.parseInt(value);
      if (number >= 1 && number <= TrafficTimeOfDaySchedule.SLOT_COUNT) {
        return number - 1;
      }
    } catch (NumberFormatException ignored) {
      // falls through to the usage error
    }
    throw new CommandException("A pattern is clock, or 1 to "
        + TrafficTimeOfDaySchedule.SLOT_COUNT + ".");
  }

  private static TileEntityTrafficSignalController controllerAt(ICommandSender sender, World world,
      BlockPos pos) throws CommandException {
    if (pos == null) {
      throw new CommandException("Look at a signal controller, or give its x y z.");
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityTrafficSignalController)) {
      throw new CommandException("There is no signal controller at " + pos.getX() + " "
          + pos.getY() + " " + pos.getZ() + ".");
    }
    return (TileEntityTrafficSignalController) te;
  }

  @Override
  public List<String> tabComplete(MinecraftServer server, ICommandSender sender, String[] args) {
    if (args.length == 1) {
      return CommandBase.getListOfStringsMatchingLastWord(args, "export", "import", "pattern");
    }
    if (args.length == 2 && "pattern".equalsIgnoreCase(args[0])) {
      return CommandBase.getListOfStringsMatchingLastWord(args, "clock", "1", "2", "3", "4");
    }
    if (args.length == 2 && "import".equalsIgnoreCase(args[0])) {
      List<String> names = new ArrayList<>();
      File[] files = timingDir(server).listFiles((dir, n) -> n.endsWith(".json"));
      if (files != null) {
        for (File f : files) {
          names.add(f.getName().substring(0, f.getName().length() - ".json".length()));
        }
      }
      Collections.sort(names);
      return CommandBase.getListOfStringsMatchingLastWord(args, names);
    }
    return Collections.emptyList();
  }

  /** The plan name without a trailing {@code .json}, checked so that it can never be a path. */
  static String planName(String raw) throws CommandException {
    String name = raw.endsWith(".json") ? raw.substring(0, raw.length() - ".json".length()) : raw;
    if (!NAME.matcher(name).matches() || name.contains("..")) {
      throw new CommandException("A plan name is up to 64 letters, digits, dots, dashes and "
          + "underscores.");
    }
    return name;
  }

  private static File timingDir(MinecraftServer server) {
    File worldDir = server.getEntityWorld().getSaveHandler().getWorldDirectory();
    return new File(new File(worldDir, "csm"), "timing");
  }

  /** The block the sending player is looking at within {@link #LOOK_RANGE}, or null. */
  private static BlockPos lookedAt(ICommandSender sender) {
    if (!(sender.getCommandSenderEntity() instanceof EntityPlayer)) {
      return null;
    }
    EntityPlayer player = (EntityPlayer) sender.getCommandSenderEntity();
    // Not getPositionEyes: that is client-only, and this runs on the server.
    Vec3d eyes = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
    Vec3d look = player.getLook(1.0F);
    RayTraceResult hit = player.world.rayTraceBlocks(eyes,
        eyes.add(look.x * LOOK_RANGE, look.y * LOOK_RANGE, look.z * LOOK_RANGE), false, true,
        false);
    return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK ? hit.getBlockPos() : null;
  }

  private static void tell(ICommandSender sender, TextFormatting color, String text) {
    sender.sendMessage(new TextComponentString(color + "[CSM] " + text));
  }
}
