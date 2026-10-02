package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Logs and leaves of any mod, as the tree tools see them, and the chainsaw's felling of a tree
 * that is not one of this module's ({@link AnyTreeFelling}).
 *
 * @since 2026.10
 */
public final class AnyTrees {

  /** Of the logs felled at once, how many play their break effect (the rest go quietly). */
  private static final int EFFECTS = 24;

  /** Whether a block state is a log of another mod, by class or ore dictionary; by state. */
  private static final Map<IBlockState, Boolean> ORE_LOGS = new ConcurrentHashMap<>();

  private AnyTrees() {
  }

  /**
   * Whether a block is a log: a vanilla log or anything extending it, a block that calls itself
   * wood, or one whose item is in the ore dictionary as a log ({@code logWood}, {@code logRubber}
   * and the like). This module's own logs count.
   */
  public static boolean isLog(World world, BlockPos pos, IBlockState state) {
    Block block = state.getBlock();
    if (block instanceof BlockLog || block instanceof BlockTreeLog) {
      return true;
    }
    if (block.isWood(world, pos)) {
      return true;
    }
    return ORE_LOGS.computeIfAbsent(state, AnyTrees::oreLog);
  }

  private static boolean oreLog(IBlockState state) {
    Block block = state.getBlock();
    Item item = Item.getItemFromBlock(block);
    if (item == net.minecraft.init.Items.AIR) {
      return false;
    }
    ItemStack stack;
    try {
      stack = new ItemStack(item, 1, block.damageDropped(state));
    } catch (RuntimeException e) {
      return false;
    }
    if (stack.isEmpty()) {
      return false;
    }
    for (int id : OreDictionary.getOreIDs(stack)) {
      if (OreDictionary.getOreName(id).startsWith("log")) {
        return true;
      }
    }
    return false;
  }

  /** Whether a block is leaves of any mod. */
  public static boolean isLeaves(World world, BlockPos pos, IBlockState state) {
    return state.getBlock().isLeaves(state, world, pos);
  }

  /**
   * Whether a leaves block grew: a vanilla-style leaves block a player placed is marked not to
   * decay, and that is the one sign left of who put it there.
   */
  public static boolean isNaturalLeaves(World world, BlockPos pos, IBlockState state) {
    if (!isLeaves(world, pos, state)) {
      return false;
    }
    return !state.getPropertyKeys().contains(BlockLeaves.DECAYABLE)
        || state.getValue(BlockLeaves.DECAYABLE);
  }

  /** How the world reads to {@link AnyTreeFelling}. This module's own logs are never felled. */
  static AnyTreeFelling.Kind kind(World world, BlockPos pos) {
    if (!world.isBlockLoaded(pos)) {
      return AnyTreeFelling.Kind.OTHER;
    }
    IBlockState state = world.getBlockState(pos);
    if (state.getBlock() instanceof BlockTreeLog) {
      return AnyTreeFelling.Kind.OTHER;
    }
    if (isLog(world, pos, state)) {
      return AnyTreeFelling.Kind.LOG;
    }
    if (isNaturalLeaves(world, pos, state)) {
      return AnyTreeFelling.Kind.LEAVES;
    }
    return AnyTreeFelling.Kind.OTHER;
  }

  /**
   * Fells what stood on the log just cut at {@code cut}: logs drop as items and leaves drop what
   * decaying leaves drop (saplings, apples), unless the player is in creative. A cell the player
   * may not change is left alone. Server side only.
   *
   * @return how many logs fell, not counting the cut one
   */
  public static int fell(World world, BlockPos cut, EntityPlayer player) {
    if (world.isRemote) {
      return 0;
    }
    AnyTreeFelling.Result result = AnyTreeFelling.fell(p -> kind(world, p), cut);
    boolean drops = !player.capabilities.isCreativeMode;
    int effects = 0;
    int felled = 0;
    for (BlockPos p : result.logs) {
      if (!world.isBlockModifiable(player, p)) {
        continue;
      }
      felled++;
      if (effects++ < EFFECTS) {
        world.destroyBlock(p, drops);
      } else {
        IBlockState state = world.getBlockState(p);
        if (drops) {
          state.getBlock().dropBlockAsItem(world, p, state, 0);
        }
        world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
      }
    }
    for (BlockPos p : result.leaves) {
      if (!world.isBlockModifiable(player, p)) {
        continue;
      }
      IBlockState state = world.getBlockState(p);
      if (drops) {
        state.getBlock().dropBlockAsItem(world, p, state, 0);
      }
      world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
    }
    return felled;
  }
}
