package com.micatechnologies.minecraft.csm.parks.planters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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
 * {@code /csmparks}: the Parks module's commands for generators. One so far:
 *
 * <pre>
 * /csmparks planter fill &lt;x1 y1 z1&gt; &lt;x2 y2 z2&gt; &lt;plant|mix&gt; [seed]
 * /csmparks planter clear &lt;x1 y1 z1&gt; &lt;x2 y2 z2&gt;
 * </pre>
 *
 * <p>{@code fill} plants every planter in the box (planters, raised beds, troughs and the two
 * flower pots, a cell at a time), replacing what was there without dropping it; the plant is
 * {@code [namespace:]name[:variant]} ({@link PlantId}), or {@code mix} for a mixed bed picked
 * by position and seed ({@link PlanterMix}), the same bed for the same box and seed every time.
 * {@code clear} empties them, dropping nothing. At most {@value #MAX_CELLS} cells a call; cells
 * in unloaded chunks are skipped and counted.</p>
 *
 * @since 2026.10
 */
public class CommandParks extends CommandBase {

  /** The most cells one call looks at: a 64 x 64 x 64 box. */
  static final int MAX_CELLS = 64 * 64 * 64;

  @Override
  public String getName() {
    return "csmparks";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return "/csmparks planter fill <x1> <y1> <z1> <x2> <y2> <z2> <plant|mix> [seed]"
        + " | /csmparks planter clear <x1> <y1> <z1> <x2> <y2> <z2>";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 2;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args)
      throws CommandException {
    if (args.length < 8 || !"planter".equals(args[0])) {
      throw new WrongUsageException(getUsage(sender));
    }
    boolean fill;
    switch (args[1]) {
      case "fill":
        fill = true;
        if (args.length < 9 || args.length > 10) {
          throw new WrongUsageException(getUsage(sender));
        }
        break;
      case "clear":
        fill = false;
        if (args.length != 8) {
          throw new WrongUsageException(getUsage(sender));
        }
        break;
      default:
        throw new WrongUsageException(getUsage(sender));
    }
    BlockPos a = parseBlockPos(sender, args, 2, false);
    BlockPos b = parseBlockPos(sender, args, 5, false);
    BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()),
        Math.min(a.getZ(), b.getZ()));
    BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()),
        Math.max(a.getZ(), b.getZ()));
    long cells = (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1)
        * (max.getZ() - min.getZ() + 1);
    if (cells > MAX_CELLS) {
      throw new CommandException("The box is " + cells + " cells; at most " + MAX_CELLS
          + " a call");
    }

    boolean mix = false;
    IBlockState fixed = null;
    long seed = 0L;
    if (fill) {
      if ("mix".equals(args[8])) {
        mix = true;
      } else {
        fixed = plant(args[8]);
      }
      if (args.length == 10) {
        seed = parseLong(args[9]);
      }
    }

    World world = sender.getEntityWorld();
    Map<String, IBlockState> mixed = new HashMap<>();
    int done = 0;
    int unloaded = 0;
    int sideways = 0;
    for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(min, max)) {
      if (!world.isBlockLoaded(p)) {
        unloaded++;
        continue;
      }
      IBlockState state = world.getBlockState(p);
      Block block = state.getBlock();
      if (!(block instanceof ICsmPlanter)) {
        continue;
      }
      if (((ICsmPlanter) block).getSoil(state) == null) {
        sideways++;
        continue;
      }
      BlockPos pos = p.toImmutable();
      TileEntity te = world.getTileEntity(pos);
      if (!(te instanceof TileEntityPlanter)) {
        continue;
      }
      TileEntityPlanter planter = (TileEntityPlanter) te;
      if (fill) {
        IBlockState plant = fixed;
        if (mix) {
          String name = PlanterMix.pick(pos.getX(), pos.getY(), pos.getZ(), seed);
          plant = mixed.computeIfAbsent(name,
              n -> PlanterPlants.resolve(new PlantId(PlantId.DEFAULT_NAMESPACE, n, 0)));
          if (plant == null) {
            continue;
          }
        }
        planter.setPlant(plant);
        done++;
      } else if (planter.hasPlant()) {
        planter.setPlant(null);
        done++;
      }
    }
    StringBuilder text = new StringBuilder(fill ? "Planted " : "Cleared ").append(done)
        .append(done == 1 ? " planter" : " planters");
    if (fill) {
      text.append(" with ").append(mix ? "a mixed bed (seed " + seed + ")"
          : PlanterPlants.idOf(fixed).toString());
    }
    if (unloaded > 0) {
      text.append("; ").append(unloaded).append(" cells skipped in unloaded chunks");
    }
    if (sideways > 0) {
      text.append("; ").append(sideways).append(" on their side, left alone");
    }
    sender.sendMessage(new TextComponentString(text.toString()));
  }

  /** The plant a command names. */
  private static IBlockState plant(String text) throws CommandException {
    PlantId id;
    try {
      id = PlantId.parse(text);
    } catch (IllegalArgumentException e) {
      throw new CommandException(e.getMessage());
    }
    IBlockState state = PlanterPlants.resolve(id);
    if (state == null) {
      throw new CommandException("'" + text + "' is not a plant that goes in a planter");
    }
    return state;
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "planter");
    }
    if (args.length == 2) {
      return getListOfStringsMatchingLastWord(args, "fill", "clear");
    }
    if (args.length >= 3 && args.length <= 5) {
      return getTabCompletionCoordinate(args, 2, targetPos);
    }
    if (args.length >= 6 && args.length <= 8) {
      return getTabCompletionCoordinate(args, 5, targetPos);
    }
    if (args.length == 9 && "fill".equals(args[1])) {
      List<String> names = new ArrayList<>();
      names.add("mix");
      for (String plant : PlanterMix.PLANTS) {
        names.add(PlantId.DEFAULT_NAMESPACE + ":" + plant);
      }
      return getListOfStringsMatchingLastWord(args, names);
    }
    return Collections.emptyList();
  }
}
