package com.micatechnologies.minecraft.csm.parks.planting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLogConnections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

/**
 * {@code /csmtree}: plants trees from the server, many to a call, keeping chosen clearances over
 * roads and walks ({@link TreeClearance}), so a district's street trees need neither a player at
 * each spot nor barriers filling the clearances first.
 *
 * <ul>
 *   <li>{@code plant <preset> <x,y,z[,heading][,seed]>;... [options]} -- the compact form.</li>
 *   <li>{@code plantjson {...}} -- the same as one JSON object ({@link TreeBatch}).</li>
 *   <li>{@code presets} -- every preset id.</li>
 * </ul>
 *
 * <p>Trees are planted in order, so a later tree meets an earlier one's leaves and the canopies
 * join, just as when planting by hand; a dry run plans them the same way without placing a block.
 * A spot may be the ground block or the cell above it. Chunks a tree reaches into that are not
 * loaded are loaded for the call and queued to unload after it; a chunk never generated is not
 * generated, and counts as in the way. The last line of feedback is {@code CSMTREE} and a JSON
 * summary for scripts: what was planted, trimmed or had no room, with each tree's seed and yaw so
 * a re-run can plant the same tree.</p>
 *
 * @since 2026.10
 */
public class CommandTree extends CommandBase {

  @Override
  public String getName() {
    return "csmtree";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return "/csmtree <plant <preset> <x,y,z[,heading][,seed]>;... [clear=pattern:height,...]"
        + " [skip=pattern,...] [seed=n] [heading=h] [dry] [cells]|plantjson {...}|presets>";
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
    TreeBatch batch;
    try {
      switch (args[0]) {
        case "presets":
          say(sender, Arrays.stream(TreePreset.values()).map(p -> p.id)
              .collect(Collectors.joining(", ")));
          return;
        case "plant": {
          BlockPos me = sender.getPosition();
          batch = TreeBatch.parseCompact(Arrays.copyOfRange(args, 1, args.length),
              (axis, text) -> coordinate(axis == 0 ? me.getX() : axis == 1 ? me.getY()
                  : me.getZ(), text));
          break;
        }
        case "plantjson":
          batch = TreeBatch.parseJson(String.join(" ", Arrays.copyOfRange(args, 1,
              args.length)));
          break;
        default:
          throw new WrongUsageException(getUsage(sender));
      }
    } catch (IllegalArgumentException e) {
      throw new CommandException(e.getMessage());
    }
    World world = sender.getEntityWorld();
    if (!(world instanceof WorldServer)) {
      throw new CommandException("Trees are planted on the server");
    }
    plant((WorldServer) world, batch, sender);
  }

  private static int coordinate(int base, String text) {
    try {
      return (int) Math.floor(parseDouble(base, text, false));
    } catch (NumberInvalidException e) {
      throw new IllegalArgumentException("Coordinate '" + text + "' is not a number");
    }
  }

  /** Chunks this call loaded, and the ones it found never generated. */
  private static final class Chunks {

    final ChunkProviderServer provider;
    final Set<Long> loaded = new HashSet<>();
    final Set<Long> missing = new HashSet<>();

    Chunks(WorldServer world) {
      provider = world.getChunkProvider();
    }

    boolean ensure(BlockPos pos) {
      int cx = pos.getX() >> 4;
      int cz = pos.getZ() >> 4;
      if (provider.getLoadedChunk(cx, cz) != null) {
        return true;
      }
      long key = ChunkPos.asLong(cx, cz);
      if (missing.contains(key)) {
        return false;
      }
      if (!provider.isChunkGeneratedAt(cx, cz) || provider.loadChunk(cx, cz) == null) {
        missing.add(key);
        return false;
      }
      loaded.add(key);
      return true;
    }

    void release() {
      for (long key : loaded) {
        Chunk c = provider.getLoadedChunk((int) key, (int) (key >> 32));
        if (c != null) {
          provider.queueUnload(c);
        }
      }
    }
  }

  private static void plant(WorldServer world, TreeBatch batch, ICommandSender sender) {
    Chunks chunks = new Chunks(world);
    // A dry run's plans, so later trees in it meet earlier ones as if they had been planted
    Map<BlockPos, TreePlan.Kind> planned = new HashMap<>();
    TreeClearance.Ground ground = new TreeClearance.Ground() {
      @Override
      public String id(BlockPos pos) {
        if (!chunks.ensure(pos)) {
          return "";
        }
        return String.valueOf(world.getBlockState(pos).getBlock().getRegistryName());
      }

      @Override
      public boolean seeThrough(BlockPos pos) {
        return chunks.ensure(pos) && !world.getBlockState(pos).isOpaqueCube();
      }
    };

    JsonArray trees = new JsonArray();
    JsonArray trimmedList = new JsonArray();
    JsonArray noRoom = new JsonArray();
    JsonArray unloaded = new JsonArray();
    JsonArray noGround = new JsonArray();
    int planted = 0;
    try {
      for (TreeBatch.Spot spot : batch.spots) {
        JsonObject t = new JsonObject();
        trees.add(t);
        t.addProperty("preset", spot.preset.id);
        if (!chunks.ensure(spot.pos)) {
          t.add("at", xyz(spot.pos));
          t.addProperty("result", "unloaded");
          unloaded.add(xyz(spot.pos));
          continue;
        }
        BlockPos base = spot.pos.getY() < 0 || spot.pos.getY() > 255
            || TreePlanter.replaceable(world, spot.pos) ? spot.pos : spot.pos.up();
        t.add("at", xyz(base));
        BlockPos under = base.down();
        if (under.getY() < 0 || !chunks.ensure(under) || TreePlanter.replaceable(world, under)
            || TreeLogConnections.isLeaves(world.getBlockState(under).getBlock())) {
          // A wrong y, or a spot on another tree's crown: a trunk would stand on nothing
          t.addProperty("result", "noground");
          noGround.add(xyz(base));
          continue;
        }
        long seed = spot.seed != null ? spot.seed
            : batch.seed != null ? TreeBatch.seedAt(batch.seed, base) : world.rand.nextLong();
        Random rng = new Random(seed);
        String headingSpec = spot.heading != null ? spot.heading : batch.heading;
        Double h = TreeBatch.heading(headingSpec, base);
        double heading = h != null ? h : rng.nextDouble() * 2 * Math.PI - Math.PI;
        t.addProperty("seed", seed);
        t.addProperty("yaw", TreeBatch.yawOf(heading));

        TreeSpace world0 = TreePlanter.space(world, base, chunks::ensure,
            at -> world.getWorldBorder().contains(at)
                && !batch.clearance.keepsClear(at, base, ground));
        TreeSpace space = rel -> {
          TreePlan.Kind k = planned.get(base.add(rel));
          if (k == TreePlan.Kind.LOG) {
            return TreeSpace.Cell.BLOCKED;
          }
          if (k == TreePlan.Kind.LEAVES || k == TreePlan.Kind.HANGING) {
            return TreeSpace.Cell.FOLIAGE;
          }
          return world0.at(rel);
        };
        TreePlan plan = TreeGenerators.grow(spot.preset, heading, rng, space);
        if (plan.hasNoRoom() || plan.parts().isEmpty()) {
          t.addProperty("result", "noroom");
          noRoom.add(xyz(base));
          continue;
        }
        if (batch.dry) {
          for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
            planned.put(base.add(e.getKey()), e.getValue().kind);
          }
        } else {
          TreePlanter.place(world, base, plan);
        }
        planted++;
        t.addProperty("result", batch.dry ? "planned" : "planted");
        describe(t, base, plan, batch.cells);
        if (plan.getTrimmed() > 0) {
          JsonArray tr = xyz(base);
          tr.add(plan.getTrimmed());
          trimmedList.add(tr);
        }
      }
    } finally {
      chunks.release();
    }

    JsonObject out = new JsonObject();
    out.addProperty("dry", batch.dry);
    out.addProperty("planted", planted);
    out.add("trimmed", trimmedList);
    out.add("noroom", noRoom);
    out.add("noground", noGround);
    out.add("skippedUnloaded", unloaded);
    out.addProperty("chunksLoaded", chunks.loaded.size());
    out.add("trees", trees);
    say(sender, String.format("%s %d of %d trees: %d trimmed to fit, %d no room, %d no ground,"
            + " %d in chunks never generated", batch.dry ? "Planned" : "Planted", planted,
        batch.spots.size(), trimmedList.size(), noRoom.size(), noGround.size(),
        unloaded.size()));
    say(sender, "CSMTREE " + out);
  }

  private static void describe(JsonObject t, BlockPos base, TreePlan plan, boolean cells) {
    int x0 = Integer.MAX_VALUE;
    int y0 = Integer.MAX_VALUE;
    int z0 = Integer.MAX_VALUE;
    int x1 = Integer.MIN_VALUE;
    int y1 = Integer.MIN_VALUE;
    int z1 = Integer.MIN_VALUE;
    JsonArray parts = new JsonArray();
    for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
      BlockPos p = base.add(e.getKey());
      x0 = Math.min(x0, p.getX());
      y0 = Math.min(y0, p.getY());
      z0 = Math.min(z0, p.getZ());
      x1 = Math.max(x1, p.getX());
      y1 = Math.max(y1, p.getY());
      z1 = Math.max(z1, p.getZ());
      if (cells) {
        JsonArray c = xyz(p);
        c.add("csm:" + e.getValue().block);
        parts.add(c);
      }
    }
    t.addProperty("blocks", plan.parts().size());
    t.addProperty("trimmed", plan.getTrimmed());
    t.add("min", xyz(new BlockPos(x0, y0, z0)));
    t.add("max", xyz(new BlockPos(x1, y1, z1)));
    if (cells) {
      t.add("cells", parts);
    }
  }

  private static JsonArray xyz(BlockPos p) {
    JsonArray a = new JsonArray();
    a.add(p.getX());
    a.add(p.getY());
    a.add(p.getZ());
    return a;
  }

  private static void say(ICommandSender sender, String text) {
    sender.sendMessage(new TextComponentString(text));
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "plant", "plantjson", "presets");
    }
    if (args.length == 2 && args[0].equals("plant")) {
      List<String> ids = new ArrayList<>();
      for (TreePreset p : TreePreset.values()) {
        ids.add(p.id);
      }
      return getListOfStringsMatchingLastWord(args, ids);
    }
    return Collections.emptyList();
  }
}
