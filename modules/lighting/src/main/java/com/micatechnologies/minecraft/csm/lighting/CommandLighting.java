package com.micatechnologies.minecraft.csm.lighting;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * {@code /csmlighting}: maintenance for the lighting module.
 *
 * <ul>
 *   <li>{@code relight <x1 y1 z1> <x2 y2 z2>} -- gives every lit bright light in the box the
 *   lightupair it is missing. Lights that arrived already lit (setblock, fill, clone, a WorldEdit
 *   or FAWE paste) heal themselves on their random tick anyway; this is for doing a whole
 *   building at once. Only loaded chunks are visited.</li>
 * </ul>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class CommandLighting extends CommandBase {

  /**
   * The most blocks one relight visits: a 512 x 64 x 512 box, a few hundred milliseconds of
   * section scanning at worst. Empty sections are skipped without reading a block.
   */
  private static final long MAX_VOLUME = 512L * 64L * 512L;

  @Override
  public String getName() {
    return "csmlighting";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return "/csmlighting relight <x1 y1 z1> <x2 y2 z2>";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 2;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args)
      throws CommandException {
    if (args.length == 7 && "relight".equals(args[0])) {
      relight(sender, parseBlockPos(sender, args, 1, false), parseBlockPos(sender, args, 4, false));
    } else {
      throw new WrongUsageException(getUsage(sender));
    }
  }

  private static void relight(ICommandSender sender, BlockPos a, BlockPos b)
      throws CommandException {
    World world = sender.getEntityWorld();
    int x0 = Math.min(a.getX(), b.getX());
    int y0 = Math.max(0, Math.min(a.getY(), b.getY()));
    int z0 = Math.min(a.getZ(), b.getZ());
    int x1 = Math.max(a.getX(), b.getX());
    int y1 = Math.min(255, Math.max(a.getY(), b.getY()));
    int z1 = Math.max(a.getZ(), b.getZ());
    long volume = (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
    if (volume > MAX_VOLUME) {
      throw new CommandException(
          "That box holds " + volume + " blocks; relight at most " + MAX_VOLUME + " at once.");
    }

    long t0 = System.nanoTime();
    int lit = 0;
    int added = 0;
    int skippedChunks = 0;
    for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
      for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
        if (!world.isBlockLoaded(new BlockPos(cx << 4, 0, cz << 4))) {
          skippedChunks++;
          continue;
        }
        Chunk chunk = world.getChunk(cx, cz);
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        for (int sy = y0 >> 4; sy <= y1 >> 4; sy++) {
          ExtendedBlockStorage section = sections[sy];
          if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
            continue;
          }
          int xa = Math.max(x0, cx << 4);
          int xb = Math.min(x1, (cx << 4) + 15);
          int za = Math.max(z0, cz << 4);
          int zb = Math.min(z1, (cz << 4) + 15);
          int ya = Math.max(y0, sy << 4);
          int yb = Math.min(y1, (sy << 4) + 15);
          for (int y = ya; y <= yb; y++) {
            for (int x = xa; x <= xb; x++) {
              for (int z = za; z <= zb; z++) {
                IBlockState state = section.get(x & 15, y & 15, z & 15);
                Block block = state.getBlock();
                if (block instanceof AbstractBrightLight && AbstractBrightLight.isLit(state)) {
                  lit++;
                  if (((AbstractBrightLight) block).ensureAirLightBlock(world,
                      new BlockPos(x, y, z))) {
                    added++;
                  }
                }
              }
            }
          }
        }
      }
    }
    say(sender, String.format("Checked %d lit lights, added %d lightupair, in %.0f ms.", lit,
        added, (System.nanoTime() - t0) / 1e6));
    if (skippedChunks > 0) {
      say(sender, skippedChunks + " chunks in the box were not loaded and were skipped.");
    }
  }

  private static void say(ICommandSender sender, String message) {
    sender.sendMessage(new TextComponentString(message));
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "relight");
    }
    if (args.length >= 2 && args.length <= 7 && "relight".equals(args[0])) {
      return getTabCompletionCoordinate(args, args.length <= 4 ? 1 : 4, targetPos);
    }
    return Collections.emptyList();
  }
}
