package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.CsmConfig;
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
    return "/csmhvac <info [x y z]|why [x y z]|spaces|settemp <F>|ff <seconds>|rescan"
        + "|perf [reset]>";
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
    if (w.isLightweight() && !"perf".equals(args[0])) {
      if (!"info".equals(args[0]) && !"why".equals(args[0]) && !"spaces".equals(args[0])) {
        throw new CommandException("HVAC is running lightweight here: there are no rooms to "
            + "change. See lightweightMode in the hvac section of csm.cfg.");
      }
      say(sender, "HVAC is running lightweight here: no rooms are simulated, thermostats read"
          + " their setpoint.");
    }
    switch (args[0]) {
      case "info":
        if (args.length >= 4) {
          at = parseBlockPos(sender, args, 1, false);
        }
        info(sender, w, at);
        break;
      case "why":
        if (args.length >= 4) {
          at = parseBlockPos(sender, args, 1, false);
        }
        why(sender, w, at);
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
    ThermalAnchor a = w.anchor(at);
    if (s == null && a != null && a.space != null) {
      // A unit that fills its own cell reads the room through the cell beside it.
      s = a.space;
    }
    if (s == null) {
      say(sender, String.format("%d %d %d: not in a known room; reads %.1fF. %s",
          at.getX(), at.getY(), at.getZ(), w.temperatureAt(at), whyNoRoom(w, a, at)));
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

  /** Why the cell at {@code at} has no room, from the anchor there (#280). */
  private static String whyNoRoom(HvacThermalWorld w, @Nullable ThermalAnchor a, BlockPos at) {
    if (a == null) {
      return "No HVAC device here: a room is found only around a thermostat, vent, unit or a"
          + " player standing in it (" + kindName(cellKind(w, at)) + " cell).";
    }
    long now = w.world.getTotalWorldTime();
    switch (a.status) {
      case UNLOADED: {
        StringBuilder b = new StringBuilder("Waiting for chunks: the room reaches");
        if (a.waitChunks != null) {
          for (long key : a.waitChunks) {
            b.append(String.format(" (%d,%d)%s", (int) key, (int) (key >>> 32),
                w.chunkLoaded(key) ? " loaded" : " unloaded"));
          }
        }
        return b.append(", so it keeps the temperature it saved.").toString();
      }
      case NOT_ENCLOSED:
        if (a.openAt != ThermalAnchor.NO_CELL) {
          return String.format("Not enclosed: no room air at it or beside it; open sky at %d %d"
                  + " %d.", ThermalScanner.unpackX(a.openAt), ThermalScanner.unpackY(a.openAt),
              ThermalScanner.unpackZ(a.openAt));
        }
        return "Not enclosed: it and the six cells beside it are all solid (buried in a wall?).";
      case TOO_LARGE:
        return String.format("Too large: the space passes %,d cells (a cave or vast hall); looked"
            + " at again in %d ticks.", ThermalScanner.maxCells(), Math.max(0, a.retryTick - now));
      default:
        if (now < a.retryTick) {
          return String.format("Not scanned yet: next look in %d ticks.", a.retryTick - now);
        }
        return w.attachBacklog()
            ? "Not scanned yet: waiting its turn behind the per-tick room-finding budget."
            : "Not scanned yet: due on the next tick.";
    }
  }

  /**
   * {@code /csmhvac why}: {@code info}'s explanation, plus what the room finder sees at the
   * anchor -- the kind of its cell and the six beside it, where the column's roof is, and the
   * chunks it waits on.
   */
  private static void why(ICommandSender sender, HvacThermalWorld w, BlockPos at) {
    info(sender, w, at);
    StringBuilder b = new StringBuilder("  probes: here ").append(kindName(cellKind(w, at)));
    for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.values()) {
      b.append(", ").append(f.getName()).append(' ')
          .append(kindName(cellKind(w, at.offset(f))));
    }
    say(sender, b.toString());
    ThermalCellSource src = HvacAirflow.worldSource(w.world);
    if (src.classify(at.getX(), at.getY(), at.getZ()) == ThermalCellSource.SKY) {
      say(sender, "  column: open to the sky here (no roof above this cell)");
    } else {
      int top = w.world.getHeight();
      int y = at.getY() + 1;
      while (y < top && src.classify(at.getX(), y, at.getZ()) != ThermalCellSource.SKY) {
        y++;
      }
      say(sender, y < top ? String.format("  column: roof at y %d", y - 1)
          : "  column: roofed to the top of the world");
    }
  }

  private static byte cellKind(HvacThermalWorld w, BlockPos p) {
    return HvacAirflow.worldSource(w.world).classify(p.getX(), p.getY(), p.getZ());
  }

  private static String kindName(byte kind) {
    switch (kind) {
      case ThermalCellSource.AIR:
        return "AIR";
      case ThermalCellSource.SKY:
        return "SKY";
      case ThermalCellSource.WALL:
        return "WALL";
      default:
        return "UNLOADED";
    }
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
    say(sender, String.format("  mode: %s (lightweightMode %s, auto at %d devices)",
        w.isLightweight() ? "lightweight" : "full simulation",
        CsmConfig.getHvacLightweightMode(), CsmConfig.getHvacLightweightAutoDevices()));
    say(sender, String.format("  %.2f ms a second on average, worst tick %.1f ms; %.3f ms a tick",
        total / steps, p.maxTickNanos / 1e6, total / (seconds * 20)));
    say(sender, String.format("  per step: rescan %.2f  attach %.2f  couplings %.2f"
            + "  control+physics %.2f  players %.2f ms", p.rescanNanos / 1e6 / steps,
        p.attachNanos / 1e6 / steps, p.couplingNanos / 1e6 / steps, p.stepNanos / 1e6 / steps,
        p.playerNanos / 1e6 / steps));
    say(sender, String.format("  worst tick: rescan %.1f  attach %.1f  couplings %.1f"
            + "  control+physics %.1f  players %.1f ms", p.maxRescanNanos / 1e6,
        p.maxAttachNanos / 1e6, p.maxCouplingNanos / 1e6, p.maxStepNanos / 1e6,
        p.maxPlayerNanos / 1e6));
    say(sender, String.format("  rescans: %d on a change, %d periodic; couplings resolved for"
            + " %d spaces; %d floods of %d cells", p.dirtyRescans, p.periodicRescans, p.couplings,
        p.scans, p.scannedCells));
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
      return getListOfStringsMatchingLastWord(args, "info", "why", "spaces", "settemp", "ff",
          "rescan", "perf");
    }
    return Collections.emptyList();
  }
}
