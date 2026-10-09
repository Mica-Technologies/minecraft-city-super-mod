package com.micatechnologies.minecraft.csm;

import com.micatechnologies.minecraft.csm.codeutils.CsmChunkBuilderBuffers;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * {@code /csmclient}: commands that act on the player's own game rather than the server, so they
 * work on any server and need no operator rights. {@code /csm chunkbuffers} runs on the server,
 * which on a multiplayer server has no chunk builders to report.
 *
 * @author Mica Technologies
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class CommandCsmClient extends CommandBase {

  private static final String USAGE =
      "/csmclient <performance [high|medium|low|custom]|chunkbuffers [trim]>";

  @Override
  public String getName() {
    return "csmclient";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return USAGE;
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 0;
  }

  @Override
  public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
    return true;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args)
      throws WrongUsageException {
    if (args.length > 0 && "performance".equalsIgnoreCase(args[0])) {
      performance(sender, args);
      return;
    }
    if (args.length == 0 || !"chunkbuffers".equalsIgnoreCase(args[0])) {
      throw new WrongUsageException(USAGE);
    }
    if (args.length > 1 && "trim".equalsIgnoreCase(args[1])) {
      long freed = CsmChunkBuilderBuffers.trim(0);
      say(sender, String.format(Locale.ROOT, "chunkbuffers: trimmed %.1f MB",
          freed / (1024.0 * 1024.0)));
    }
    for (String line : CsmChunkBuilderBuffers.describe()) {
      say(sender, line);
    }
  }

  /**
   * {@code /csmclient performance [mode]}: with a mode, sets it, saves it to the configuration and
   * applies it at once; either way, reports the mode and what it does now.
   */
  private static void performance(ICommandSender sender, String[] args)
      throws WrongUsageException {
    if (args.length > 1) {
      if (!CsmConfig.setPerformanceMode(args[1])) {
        throw new WrongUsageException("/csmclient performance <high|medium|low|custom>");
      }
      say(sender, "Performance mode set to " + CsmPerformance.mode() + " and saved.");
    }
    CsmPerformance.Mode mode = CsmPerformance.mode();
    say(sender, "Performance mode: " + mode
        + (mode == CsmPerformance.Mode.CUSTOM ? " (values from config/csm.cfg)" : ""));
    double far = CsmPerformance.capRenderDistanceSq(Double.MAX_VALUE);
    say(sender, "  animated blocks drawn to: " + (far == Double.MAX_VALUE
        ? "their own distance (mostly 128)" : blocks(Math.sqrt(far))));
    say(sender, "  sign legends within: "
        + blocks(Math.sqrt(CsmPerformance.signDetailDistanceSq())));
    double halo = CsmPerformance.arrowBoardHaloDistanceSq();
    say(sender, "  arrow board glow: "
        + (halo <= 0 ? "off" : "within " + blocks(Math.sqrt(halo))));
    say(sender, "  strobes: " + (CsmPerformance.strobeEffect()
        ? CsmPerformance.strobeDetail().name().toLowerCase(Locale.ROOT) : "off")
        + ", emergency light glow: " + onOff(CsmPerformance.emergencyLightGlow()));
    double thermostat = CsmPerformance.thermostatDisplayDistanceSq();
    say(sender, "  thermostat screens: " + (thermostat <= 0 ? "off"
        : thermostat >= Double.MAX_VALUE ? "on" : "within " + blocks(Math.sqrt(thermostat))));
    say(sender, "  door swing: " + onOff(CsmPerformance.doorAnimation())
        + ", incandescent fade: " + onOff(CsmPerformance.incandescentFade())
        + ", ad transitions: " + onOff(CsmPerformance.adBoardTransitions()));
    int perThread = mode == CsmPerformance.Mode.MEDIUM ? 4
        : mode == CsmPerformance.Mode.LOW ? 2 : 0;
    int cap = CsmConfig.getChunkBuilderLimit();
    say(sender, "  chunk builders: " + (perThread > 0 ? perThread + " per build thread"
        : "Minecraft's number") + (cap > 0 ? ", at most " + cap : "") + ", trimmed over "
        + CsmPerformance.chunkBuilderBudgetPercent() + "% of direct memory"
        + (CsmPerformance.trimChunkBuilders() ? "" : " (trimming off)"));
  }

  private static String blocks(double d) {
    return Math.round(d) + " blocks";
  }

  private static String onOff(boolean on) {
    return on ? "on" : "off";
  }

  private static void say(ICommandSender sender, String line) {
    TextComponentString text = new TextComponentString("[CSM] " + line);
    text.getStyle().setColor(TextFormatting.GREEN);
    sender.sendMessage(text);
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "performance", "chunkbuffers");
    }
    if (args.length == 2 && "performance".equalsIgnoreCase(args[0])) {
      return getListOfStringsMatchingLastWord(args, "high", "medium", "low", "custom");
    }
    if (args.length == 2 && "chunkbuffers".equalsIgnoreCase(args[0])) {
      return getListOfStringsMatchingLastWord(args, "trim");
    }
    return Collections.emptyList();
  }
}
