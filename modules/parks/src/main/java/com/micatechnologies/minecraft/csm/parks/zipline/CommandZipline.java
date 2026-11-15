package com.micatechnologies.minecraft.csm.parks.zipline;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * {@code /csmzipline}: stringing ziplines from a script or a build, without the linker.
 *
 * <pre>
 * /csmzipline link &lt;launch x y z&gt; &lt;landing x y z&gt;
 * /csmzipline unlink &lt;x y z&gt;
 * /csmzipline info &lt;x y z&gt;
 * </pre>
 *
 * <p>{@code link} makes the same checks the linker does and strings the line only if they pass;
 * {@code unlink} takes down the line either anchor is on; {@code info} says what an anchor is and
 * where its line goes. Both anchors must be loaded.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public class CommandZipline extends CommandBase {

  @Override
  public String getName() {
    return "csmzipline";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return "/csmzipline link <x1> <y1> <z1> <x2> <y2> <z2> | /csmzipline unlink <x> <y> <z>"
        + " | /csmzipline info <x> <y> <z>";
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
    switch (args[0]) {
      case "link": {
        if (args.length != 7) {
          throw new WrongUsageException(getUsage(sender));
        }
        BlockPos launch = parseBlockPos(sender, args, 1, false);
        BlockPos landing = parseBlockPos(sender, args, 4, false);
        TextComponentTranslation why = Ziplines.check(world, launch, landing);
        if (why != null) {
          throw new CommandException(why.getKey(), why.getFormatArgs());
        }
        Ziplines.link(world, launch, landing);
        ZiplineLine line = ZiplineLine.of(launch, landing);
        notifyCommandListener(sender, this, "csm.zipline.linked",
            String.format("%.1f", line.length()), String.format("%.1f", line.drop()));
        break;
      }
      case "unlink": {
        BlockPos pos = parseBlockPos(sender, args, 1, false);
        if (!Ziplines.unlink(world, pos)) {
          throw new CommandException("csm.zipline.info_none", Ziplines.fmt(pos),
              role(world, pos));
        }
        notifyCommandListener(sender, this, "csm.zipline.unlinked");
        break;
      }
      case "info": {
        BlockPos pos = parseBlockPos(sender, args, 1, false);
        sender.sendMessage(describe(world, pos));
        break;
      }
      default:
        throw new WrongUsageException(getUsage(sender));
    }
  }

  /**
   * What an anchor is and where its line goes, for {@code info} and a click on a landing anchor.
   *
   * @since 1.0
   */
  static TextComponentTranslation describe(World world, BlockPos pos) {
    TileEntity te = Ziplines.anchorAt(world, pos);
    if (!(te instanceof TileEntityZiplineAnchor)) {
      return new TextComponentTranslation("csm.zipline.not_anchor", Ziplines.fmt(pos));
    }
    TileEntityZiplineAnchor anchor = (TileEntityZiplineAnchor) te;
    BlockPos other = anchor.getLink();
    if (other == null) {
      return new TextComponentTranslation("csm.zipline.info_none", Ziplines.fmt(pos),
          role(world, pos));
    }
    boolean launch = anchor.isLaunch();
    ZiplineLine line = launch ? ZiplineLine.of(pos, other) : ZiplineLine.of(other, pos);
    return new TextComponentTranslation("csm.zipline.info", Ziplines.fmt(pos), role(world, pos),
        Ziplines.fmt(other), String.format("%.1f", line.length()),
        String.format("%.1f", line.drop()));
  }

  private static TextComponentTranslation role(World world, BlockPos pos) {
    IBlockState s = world.getBlockState(pos);
    boolean launch = s.getBlock() instanceof BlockZiplineAnchor
        && ((BlockZiplineAnchor) s.getBlock()).isLaunch();
    return new TextComponentTranslation(launch ? "csm.zipline.role.launch"
        : "csm.zipline.role.landing");
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "link", "unlink", "info");
    }
    if (args.length >= 2 && args.length <= 7) {
      return getTabCompletionCoordinate(args, args.length <= 4 ? 1 : 4, targetPos);
    }
    return Collections.emptyList();
  }
}
