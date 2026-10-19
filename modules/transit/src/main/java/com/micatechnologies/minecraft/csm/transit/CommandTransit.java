package com.micatechnologies.minecraft.csm.transit;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.transit.airport.BlockHelipadMarking;
import com.micatechnologies.minecraft.csm.transit.airport.HelipadLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * {@code /csmtransit}: lays Transit's build-from-pieces things from a command, for a city built by
 * a generator rather than by hand.
 *
 * <ul>
 *   <li>{@code helipad <x y z> <size> [plain|hospital] [north|east|south|west] [box ...]} --
 *   lays a square helipad {@code size} blocks across (odd, 5 to 15) centred on {@code x z}, at
 *   height {@code y} (the layer above the deck): the H marking in the middle 5 x 5, the
 *   perimeter stripe round the edge from 7 up, plain deck between. The H's legs run along the
 *   facing (the approach), north by default; {@code hospital} fills its square red. From 9 up,
 *   up to two mass and size boxes ({@code 4T}, {@code 6T}, {@code D12}, {@code D16}) go on the
 *   near side, right then left of the centreline, reading from the approach. A cell is laid
 *   only where it is free (or already marking) and the deck under it is solid.</li>
 * </ul>
 *
 * @since 2026.10
 */
public class CommandTransit extends CommandBase {

  private static final String USAGE = "/csmtransit helipad <x y z> <size> [plain|hospital] "
      + "[north|east|south|west] [4T|6T|D12|D16] [4T|6T|D12|D16]";

  @Override
  public String getName() {
    return "csmtransit";
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
    if (args.length < 5 || !"helipad".equals(args[0])) {
      throw new WrongUsageException(USAGE);
    }
    BlockPos centre = parseBlockPos(sender, args, 1, false);
    int size = parseInt(args[4], 5, 15);
    if (size % 2 == 0) {
      throw new CommandException("A helipad's size must be odd (5, 7, 9 ... 15).");
    }
    boolean hospital = false;
    EnumFacing facing = EnumFacing.NORTH;
    List<Integer> boxes = new ArrayList<>();
    for (int i = 5; i < args.length; i++) {
      String a = args[i];
      if ("plain".equalsIgnoreCase(a) || "hospital".equalsIgnoreCase(a)) {
        hospital = "hospital".equalsIgnoreCase(a);
        continue;
      }
      EnumFacing f = EnumFacing.byName(a.toLowerCase());
      if (f != null && f.getAxis().isHorizontal()) {
        facing = f;
        continue;
      }
      int legend = legendOf(a);
      if (legend < 0) {
        throw new WrongUsageException(USAGE);
      }
      boxes.add(legend);
    }
    if (boxes.size() > 2) {
      throw new CommandException("At most two boxes.");
    }
    if (!boxes.isEmpty() && size < 9) {
      throw new CommandException("Boxes need a pad of 9 or more, for room beside the circle.");
    }
    int[] laid = lay(sender.getEntityWorld(), centre, size, hospital, facing, boxes);
    sender.sendMessage(new TextComponentString("Laid a " + size + " x " + size + " helipad: "
        + laid[0] + " cells" + (laid[1] > 0 ? ", " + laid[1] + " skipped (not free, or no deck "
        + "under them)" : "") + "."));
  }

  private static int legendOf(String text) {
    for (int i = 0; i < HelipadLayout.LEGENDS.length; i++) {
      if (HelipadLayout.LEGENDS[i].equalsIgnoreCase(text)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Lays the pad.
   *
   * @return the cells laid, and the cells skipped
   */
  static int[] lay(World world, BlockPos centre, int size, boolean hospital, EnumFacing facing,
      List<Integer> boxes) {
    Block surface = block("airport_helipad_surface");
    Block circle = block("airport_helipad_circle");
    Block h = block(hospital ? "airport_helipad_h_red" : "airport_helipad_h");
    Block box = block("airport_helipad_box");
    boolean alongX = facing.getAxis() == EnumFacing.Axis.X;
    int half = size / 2;
    int mark = HelipadLayout.SIZE / 2;
    int turn = BlockHelipadMarking.turnOf(facing);
    EnumFacing right = facing.rotateY();
    int[] boxAt = new int[4];
    for (int i = 0; i < boxes.size(); i++) {
      // the near side, a block inside the stripe; right of the centreline then left
      int side = i == 0 ? 2 : -2;
      int back = half - 1;
      boxAt[i * 2] = right.getXOffset() * side - facing.getXOffset() * back;
      boxAt[i * 2 + 1] = right.getZOffset() * side - facing.getZOffset() * back;
    }
    int laid = 0;
    int skipped = 0;
    for (int dz = -half; dz <= half; dz++) {
      for (int dx = -half; dx <= half; dx++) {
        Block block;
        int piece;
        int edge = Math.max(Math.abs(dx), Math.abs(dz));
        if (edge <= mark) {
          int col = dx + mark;
          int row = dz + mark;
          block = HelipadLayout.isH(alongX, col, row) ? h : circle;
          piece = HelipadLayout.piece(alongX, col, row);
        } else if (edge == half && size >= 7) {
          block = surface;
          if (Math.abs(dx) == half && Math.abs(dz) == half) {
            // the stripe round a corner, drawn round the north-west one
            int t = dz < 0 ? (dx < 0 ? 0 : 1) : (dx > 0 ? 2 : 3);
            piece = HelipadLayout.CORNER * 4 + t;
          } else {
            int t = dz == -half ? 0 : dx == half ? 1 : dz == half ? 2 : 3;
            piece = HelipadLayout.EDGE * 4 + t;
          }
        } else {
          block = surface;
          piece = HelipadLayout.PLAIN * 4 + ((dx + dz) & 3);
          for (int i = 0; i < boxes.size(); i++) {
            if (dx == boxAt[i * 2] && dz == boxAt[i * 2 + 1]) {
              block = box;
              piece = boxes.get(i) * 4 + turn;
            }
          }
        }
        BlockPos pos = centre.add(dx, 0, dz);
        IBlockState here = world.getBlockState(pos);
        boolean free = here.getBlock() instanceof BlockHelipadMarking
            || here.getBlock().isReplaceable(world, pos);
        BlockPos below = pos.down();
        if (!free || !world.getBlockState(below).isSideSolid(world, below, EnumFacing.UP)) {
          skipped++;
          continue;
        }
        world.setBlockState(pos, block.getStateFromMeta(piece), 2);
        laid++;
      }
    }
    return new int[]{laid, skipped};
  }

  private static Block block(String registryName) {
    Block block = CsmRegistry.getBlock(registryName);
    if (block == null) {
      throw new IllegalStateException("No block csm:" + registryName);
    }
    return block;
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "helipad");
    }
    if (args.length >= 2 && args.length <= 4) {
      return getTabCompletionCoordinate(args, 1, targetPos);
    }
    if (args.length == 5) {
      return getListOfStringsMatchingLastWord(args, "5", "7", "9", "11");
    }
    if (args.length >= 6) {
      return getListOfStringsMatchingLastWord(args, "plain", "hospital", "north", "east",
          "south", "west", "4T", "6T", "D12", "D16");
    }
    return Collections.emptyList();
  }
}
