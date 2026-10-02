package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The mess a chainsaw leaves: a few brush piles (cut branches and leaves) on the ground around
 * the stump of a felled tree, as many as {@code chainsawBrushPiles} in Core's configuration says.
 *
 * <p>A pile goes only into an air cell standing on a solid top that is open to the sky. The sky
 * test is what keeps them out of buildings: a tree felled beside a house drops nothing on its
 * floor, under its porch roof or in its basement. Nothing is ever replaced, so a pile never
 * costs anyone a block.</p>
 *
 * @since 2026.10
 */
public final class BrushPiles {

  /** The registry name of the brush pile block. */
  public static final String BLOCK = "brush_pile";
  /** How far from the stump, sideways, a pile may land. */
  static final int RADIUS = 4;
  /** How far above or below the stump's cell a pile may land. */
  static final int RISE = 2;

  private BrushPiles() {
  }

  /**
   * How many piles a felling leaves.
   *
   * @param setting {@code NONE}, {@code FEW} or {@code MANY}
   * @param logs    how many logs fell, the cut one included
   *
   * @return how many piles to place
   */
  public static int count(String setting, int logs) {
    if ("NONE".equals(setting) || logs <= 0) {
      return 0;
    }
    if ("MANY".equals(setting)) {
      return 3 + Math.min(5, logs / 6);
    }
    return 1 + Math.min(2, logs / 12);
  }

  /**
   * Where the piles go: in each column around the stump, the highest cell {@code open} accepts,
   * shuffled, the first {@code count}.
   *
   * @param open  whether a pile can go in a cell
   * @param stump the stump's cell (where the cut log was)
   * @param count how many to choose
   * @param rand  the shuffle
   *
   * @return at most {@code count} cells, at most one in a column
   */
  public static List<BlockPos> choose(Predicate<BlockPos> open, BlockPos stump, int count,
      Random rand) {
    List<BlockPos> found = new ArrayList<>();
    if (count <= 0) {
      return found;
    }
    for (int dx = -RADIUS; dx <= RADIUS; dx++) {
      for (int dz = -RADIUS; dz <= RADIUS; dz++) {
        if (dx == 0 && dz == 0 || dx * dx + dz * dz > RADIUS * RADIUS) {
          continue;
        }
        for (int dy = RISE; dy >= -RISE; dy--) {
          BlockPos p = stump.add(dx, dy, dz);
          if (open.test(p)) {
            found.add(p);
            break;
          }
        }
      }
    }
    Collections.shuffle(found, rand);
    return found.size() > count ? new ArrayList<>(found.subList(0, count)) : found;
  }

  /**
   * Leaves the piles a felling makes around {@code stump}. Server side, after the tree is gone.
   *
   * @param world  the world
   * @param player who felled it
   * @param stump  where the cut log was
   * @param logs   how many logs fell, the cut one included
   */
  public static void place(World world, EntityPlayer player, BlockPos stump, int logs) {
    Block pile = CsmRegistry.getBlock(BLOCK);
    int n = count(CsmConfig.getChainsawBrushPiles(), logs);
    if (world.isRemote || pile == null || n == 0) {
      return;
    }
    Predicate<BlockPos> open = p -> p.getY() > 0 && p.getY() < world.getHeight()
        && world.isBlockLoaded(p) && world.isAirBlock(p)
        && world.getBlockState(p.down()).isSideSolid(world, p.down(), EnumFacing.UP)
        && world.canSeeSky(p) && world.isBlockModifiable(player, p);
    for (BlockPos p : choose(open, stump, n, world.rand)) {
      world.setBlockState(p, pile.getDefaultState(), 3);
    }
  }
}
