package com.micatechnologies.minecraft.csm.hvac;

import java.util.Arrays;
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
 * {@code /csmhvac}: looks inside the thermal simulation, and drives it for testing.
 *
 * <ul>
 *   <li>{@code info [x y z]} -- the room at your feet (or a position): size, regions, envelope,
 *   outdoor and room temperatures, heat delivered and lost.</li>
 *   <li>{@code spaces} -- every room the simulation holds in this dimension.</li>
 *   <li>{@code settemp <F>} -- sets every region of the room at your feet to a temperature, to
 *   start a test from cold or hot.</li>
 *   <li>{@code ff <seconds>} -- runs that much simulated time at once.</li>
 *   <li>{@code rescan} -- rescans the room at your feet now, and says how long it took.</li>
 *   <li>{@code perf [reset]} -- what the simulation has cost the server tick since the counters
 *   were last reset: time per step by phase, rescans and why, cells flooded, block changes seen.</li>
 * </ul>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class CommandHvac extends CommandBase {

  private static final int MAX_FAST_FORWARD = 36_000;

  @Override
  public String getName() {
    return "csmhvac";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return "/csmhvac <info [x y z]|spaces|settemp <F>|ff <seconds>|rescan|perf [reset]>";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 2;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args)
      throws CommandException {
    if (args.length == 0) {
      throw new WrongUsageException(getUsage(sender));
    }
    World world = sender.getEntityWorld();
    HvacThermalWorld w = HvacThermal.get(world);
    if (w == null) {
      throw new CommandException("No thermal simulation in this world.");
    }
    BlockPos at = sender.getPosition();
    switch (args[0]) {
      case "info":
        if (args.length >= 4) {
          at = parseBlockPos(sender, args, 1, false);
        }
        info(sender, w, at);
        break;
      case "spaces":
        say(sender, w.spaces().size() + " spaces:");
        for (ThermalSpace s : w.spaces()) {
          BlockPos p = BlockPos.fromLong(s.sampleCell);
          say(sender, String.format("  #%d at %d %d %d: %d cells, %d regions, mean %.1fF,"
                  + " outdoor %.1fF, %d anchors", s.id, p.getX(), p.getY(), p.getZ(), s.volume,
              s.regionCount, s.meanTemperature(), s.outdoor, s.anchors.size()));
        }
        break;
      case "settemp": {
        if (args.length < 2) {
          throw new WrongUsageException("/csmhvac settemp <F>");
        }
        float t = (float) parseDouble(args[1], -200, 400);
        ThermalSpace s = w.spaceAt(at);
        if (s == null) {
          throw new CommandException("You are not standing in a room the simulation knows.");
        }
        Arrays.fill(s.temperature, t);
        say(sender, String.format("Room #%d set to %.1fF.", s.id, t));
        break;
      }
      case "rescan": {
        ThermalSpace s = w.spaceAt(at);
        if (s == null) {
          throw new CommandException("You are not standing in a room the simulation knows.");
        }
        s.dirty = true;
        s.lastScanTick = Long.MIN_VALUE / 2;
        long t0 = System.nanoTime();
        w.maintain(w.world.getTotalWorldTime(), Long.MAX_VALUE);
        say(sender, String.format("Rescanned room #%d (%d cells) in %.1f ms.", s.id, s.volume,
            (System.nanoTime() - t0) / 1e6));
        break;
      }
      case "ff": {
        if (args.length < 2) {
          throw new WrongUsageException("/csmhvac ff <seconds>");
        }
        int seconds = parseInt(args[1], 1, MAX_FAST_FORWARD);
        long t0 = System.nanoTime();
        w.fastForward(seconds);
        say(sender, String.format("Ran %d simulated seconds in %.0f ms.", seconds,
            (System.nanoTime() - t0) / 1e6));
        info(sender, w, at);
        break;
      }
      case "perf":
        perf(sender, w, args.length >= 2 && "reset".equals(args[1]));
        break;
      default:
        throw new WrongUsageException(getUsage(sender));
    }
  }

  private static void info(ICommandSender sender, HvacThermalWorld w, BlockPos at) {
    ThermalSpace s = w.spaceAt(at);
    if (s == null) {
      say(sender, String.format("%d %d %d: not in a known room; reads %.1fF (outdoors).",
          at.getX(), at.getY(), at.getZ(), w.temperatureAt(at)));
      return;
    }
    int r = s.regionOfCell(ThermalScanner.pack(at.getX(), at.getY(), at.getZ()));
    float heat = 0;
    for (float q : s.lastHeat) {
      heat += q;
    }
    float mean = s.meanTemperature();
    say(sender, String.format("Room #%d: %d cells in %d regions, %d openings, %d anchors",
        s.id, s.volume, s.regionCount, s.openingFaces, s.anchors.size()));
    say(sender, String.format("  outdoor %.1fF  mean %.1fF  here %.1fF (region %d)",
        s.outdoor, mean, r >= 0 ? s.temperature[r] : mean, r));
    say(sender, String.format("  capacity %.0f  envelope UA %.2f  loss %.0f/s  delivered %.0f/s",
        s.totalCapacity(), s.envelopeUA(), s.envelopeLossAt(mean), heat));
    float min = Float.MAX_VALUE;
    float max = -Float.MAX_VALUE;
    for (float t : s.temperature) {
      min = Math.min(min, t);
      max = Math.max(max, t);
    }
    say(sender, String.format("  regions %.1fF .. %.1fF", min, max));
  }

  private static void perf(ICommandSender sender, HvacThermalWorld w, boolean reset) {
    HvacThermalWorld.Perf p = w.perf;
    long now = w.world.getTotalWorldTime();
    if (reset || p.sinceTick == Long.MIN_VALUE) {
      p.reset(now);
      say(sender, "HVAC cost counters reset.");
      return;
    }
    double seconds = Math.max(1, now - p.sinceTick) / 20.0;
    long steps = Math.max(1, p.steps);
    double total = (p.rescanNanos + p.attachNanos + p.couplingNanos + p.stepNanos
        + p.playerNanos) / 1e6;
    say(sender, String.format("HVAC over %.0f s (%d steps): %d spaces, %d anchors", seconds,
        p.steps, w.spaces().size(), w.anchors().size()));
    say(sender, String.format("  %.2f ms a step on average, %.1f ms at most; %.3f ms a tick",
        total / steps, p.maxTickNanos / 1e6, total / (seconds * 20)));
    say(sender, String.format("  per step: rescan %.2f  attach %.2f  couplings %.2f"
            + "  control+physics %.2f  players %.2f ms", p.rescanNanos / 1e6 / steps,
        p.attachNanos / 1e6 / steps, p.couplingNanos / 1e6 / steps, p.stepNanos / 1e6 / steps,
        p.playerNanos / 1e6 / steps));
    say(sender, String.format("  rescans: %d on a change, %d periodic; %d couplings;"
            + " %d floods of %d cells", p.dirtyRescans, p.periodicRescans, p.couplings, p.scans,
        p.scannedCells));
    say(sender, String.format("  block changes: %d seen, %d changed a room", p.blockUpdates,
        p.relevantBlockUpdates));
    int waiting = 0;
    for (ThermalAnchor a : w.anchors()) {
      if (a.space == null && a.waitChunks != null) {
        waiting++;
      }
    }
    say(sender, String.format("  %d anchors waiting for a chunk to load; %d retries skipped",
        waiting, p.waitSkips));
  }

  private static void say(ICommandSender sender, String text) {
    sender.sendMessage(new TextComponentString(text));
    CsmHvac.getLogger().info("[csmhvac] " + text);
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "info", "spaces", "settemp", "ff",
          "rescan", "perf");
    }
    return Collections.emptyList();
  }
}
