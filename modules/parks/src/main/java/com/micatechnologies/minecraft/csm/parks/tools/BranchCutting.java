package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.parks.trees.BlockHangingMoss;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLeaves;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import com.micatechnologies.minecraft.csm.parks.trees.ICsmTreeLeaves;
import com.micatechnologies.minecraft.csm.parks.trees.TreeFelling;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLogWidth;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

/**
 * What the pole trimmer and the tree shears may cut, and the cut itself.
 *
 * <p>They take small growth only: leaves of any mod, this module's hanging moss, and its twig and
 * thin logs where what hangs past the cut is small too ({@link #isBranch}). A thicker log, a thin
 * leader carrying the crown, a palm crown or another mod's log is too big. Cutting a twig or thin
 * log goes through {@link TreeFelling} like any broken log, so what it alone held (the rest of
 * the branch past the cut, and the leaves left stranded) comes off with it; a branch never holds
 * up the tree, so the tree stays.</p>
 *
 * @since 2026.10
 */
public final class BranchCutting {

  /** What a branch tool makes of a block. */
  public enum Verdict {
    /** A branch, leaves or moss: cut it. */
    CUT,
    /** Part of a tree too big for a hand tool. */
    TOO_BIG,
    /** Not a tree at all. */
    NOT_A_BRANCH
  }

  /** The most logs a branch tool takes off with one cut. */
  public static final int MAX_BRANCH_LOGS = 48;

  private BranchCutting() {
  }

  /** Whether a block is a twig or thin log of this module's. */
  static boolean isSmall(IBlockState state) {
    if (!(state.getBlock() instanceof BlockTreeLog)) {
      return false;
    }
    TreeLogWidth width = ((BlockTreeLog) state.getBlock()).getWidth();
    return width == TreeLogWidth.TWIG || width == TreeLogWidth.THIN;
  }

  /**
   * Whether what falls when a small log is cut is a branch: no more than
   * {@link #MAX_BRANCH_LOGS} logs, every one of them small. Otherwise the cut log is the leader
   * or a limb something bigger hangs from, and that is chainsaw work.
   *
   * @param falling the logs {@link TreeFelling#unsupportedLogs} says would fall
   * @param small   whether a log is twig or thin
   */
  public static boolean isBranch(Set<BlockPos> falling, Predicate<BlockPos> small) {
    if (falling.size() > MAX_BRANCH_LOGS) {
      return false;
    }
    for (BlockPos p : falling) {
      if (!small.test(p)) {
        return false;
      }
    }
    return true;
  }

  /** What a branch tool makes of the block at {@code pos}. */
  public static Verdict judge(World world, BlockPos pos, IBlockState state) {
    Block block = state.getBlock();
    if (block instanceof BlockTreeLog) {
      if (!isSmall(state)) {
        return Verdict.TOO_BIG;
      }
      // A thin log can still be the leader a whole crown stands on: cut only what is a branch.
      TreeFelling.Cells cells = p -> p.equals(pos) ? TreeFelling.Kind.OTHER
          : world.isBlockLoaded(p) ? TreeFelling.kind(world, p) : TreeFelling.Kind.GROUND;
      Set<BlockPos> falling = TreeFelling.unsupportedLogs(cells, pos);
      return isBranch(falling, p -> isSmall(world.getBlockState(p))) ? Verdict.CUT
          : Verdict.TOO_BIG;
    }
    if (block instanceof BlockTreeLeaves) {
      return ((BlockTreeLeaves) block).getLeafType().isPalm() ? Verdict.TOO_BIG : Verdict.CUT;
    }
    if (block instanceof BlockHangingMoss || AnyTrees.isLeaves(world, pos, state)) {
      return Verdict.CUT;
    }
    if (AnyTrees.isLog(world, pos, state)) {
      return Verdict.TOO_BIG;
    }
    return Verdict.NOT_A_BRANCH;
  }

  /**
   * Cuts the branch, leaves or moss at {@code pos} for {@code player}, with what it alone held.
   * Server side; the caller has judged it {@link Verdict#CUT}.
   *
   * @return whether anything was cut (not when the player may not break the block)
   */
  public static boolean cut(World world, EntityPlayer player, BlockPos pos, ItemStack tool) {
    if (world.isRemote || !world.isBlockModifiable(player, pos)
        || !player.canPlayerEdit(pos, EnumFacing.UP, tool)) {
      return false;
    }
    if (player instanceof EntityPlayerMP && ForgeHooks.onBlockBreakEvent(world,
        ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player,
        pos) == -1) {
      return false;
    }
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    boolean drops = !player.capabilities.isCreativeMode;
    world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 1.0F,
        0.9F + world.rand.nextFloat() * 0.2F);
    world.destroyBlock(pos, drops);
    if (block instanceof BlockTreeLog) {
      TreeFelling.fell(world, pos, player);
    } else if (block instanceof ICsmTreeLeaves) {
      TreeFelling.Cells cells = p -> world.isBlockLoaded(p) ? TreeFelling.kind(world, p)
          : TreeFelling.Kind.LOG;
      Set<BlockPos> gone = Collections.singleton(pos);
      for (BlockPos p : TreeFelling.orphanedLeaves(cells, gone)) {
        world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
      }
    }
    if (drops) {
      tool.damageItem(1, player);
    }
    return true;
  }
}
