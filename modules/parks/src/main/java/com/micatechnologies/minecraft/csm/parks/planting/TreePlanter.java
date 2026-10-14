package com.micatechnologies.minecraft.csm.parks.planting;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkProp;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLogConnections;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What the Tree Planting Tool and {@code /csmtree} share: what a tree may grow through, how the
 * world around a planting spot is read as a {@link TreeSpace}, and how a plan is placed.
 *
 * @since 2026.10
 */
public final class TreePlanter {

  private TreePlanter() {
  }

  /**
   * Air, plants, snow, and a thin covering on the ground (a mulch or gravel cover, a carpet): a
   * tree is planted through a ground cover, so its trunk stands on the ground beneath rather than
   * a block up on top of the cover.
   */
  public static boolean replaceable(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    Material m = state.getMaterial();
    Block block = state.getBlock();
    boolean cover = block instanceof BlockParkProp
        && ((BlockParkProp) block).getKind() == BlockParkProp.Kind.COVER;
    return block.isReplaceable(world, pos) || m == Material.PLANTS || m == Material.VINE
        || m == Material.CARPET || cover;
  }

  /**
   * The world around a planting spot, as a tree grows into it.
   *
   * @param world   the world
   * @param base    the base of the trunk
   * @param loaded  whether a cell may be read; one that may not is in the way
   * @param mayGrow whether the tree may grow into a cell it could grow through
   *
   * @return the space, uncached ({@link TreeGenerators#grow} caches it)
   */
  public static TreeSpace space(World world, BlockPos base, Predicate<BlockPos> loaded,
      Predicate<BlockPos> mayGrow) {
    return rel -> {
      BlockPos at = base.add(rel);
      if (at.getY() < 0 || at.getY() > 255 || !loaded.test(at)) {
        return TreeSpace.Cell.BLOCKED;
      }
      if (replaceable(world, at)) {
        return mayGrow.test(at) ? TreeSpace.Cell.FREE : TreeSpace.Cell.BLOCKED;
      }
      return TreeLogConnections.isLeaves(world.getBlockState(at).getBlock())
          ? TreeSpace.Cell.FOLIAGE : TreeSpace.Cell.BLOCKED;
    };
  }

  /** Places a plan: logs first, then leaves, then what hangs from them, so each has its support. */
  public static void place(World world, BlockPos base, TreePlan plan) {
    for (TreePlan.Kind kind : TreePlan.Kind.values()) {
      for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
        if (e.getValue().kind == kind) {
          IBlockState state = resolve(e.getValue());
          if (state != null) {
            world.setBlockState(base.add(e.getKey()), state, 3);
          }
        }
      }
    }
  }

  @Nullable
  private static IBlockState resolve(TreePlan.Part part) {
    Block block = CsmRegistry.getBlock(part.block);
    if (block == null) {
      return null;
    }
    IBlockState state = block.getDefaultState();
    if (block instanceof BlockTreeLog && part.axis != null) {
      state = state.withProperty(BlockTreeLog.AXIS, part.axis);
    }
    return state;
  }
}
