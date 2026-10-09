package com.micatechnologies.minecraft.csm;

import com.micatechnologies.minecraft.csm.codeutils.CsmChunkBuilderBuffers;
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

  private static final String USAGE = "/csmclient chunkbuffers [trim]";

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

  private static void say(ICommandSender sender, String line) {
    TextComponentString text = new TextComponentString("[CSM] " + line);
    text.getStyle().setColor(TextFormatting.GREEN);
    sender.sendMessage(text);
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "chunkbuffers");
    }
    if (args.length == 2 && "chunkbuffers".equalsIgnoreCase(args[0])) {
      return getListOfStringsMatchingLastWord(args, "trim");
    }
    return Collections.emptyList();
  }
}
