package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmStumpGrinder;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Grinding a stump away, by {@link StumpGrinding}'s rules: the hand-guided {@link ItemStumpGrinder}
 * and, through Core's {@code CsmStumpGrinders}, any machine that grinds stumps (CSM: Vehicles'
 * stump grinder) grind the same way.
 *
 * <p>The stump and its roots go, with no drops; where it stood, if the cell is clear and the ground
 * under it solid, a layer of this module's ground mulch is left.</p>
 *
 * @since 2026.10
 */
public final class ParksStumpGrinder implements ICsmStumpGrinder {

  /** The registry name of the mulch left where a stump stood. */
  static final String MULCH = "ground_mulch";
  /** Of the logs ground at once, how many play their break effect. */
  private static final int EFFECTS = 12;

  @Override
  public boolean isLog(World world, BlockPos pos) {
    return world.isBlockLoaded(pos) && AnyTrees.isLog(world, pos, world.getBlockState(pos));
  }

  @Override
  public int grind(World world, BlockPos log, @Nullable EntityPlayer player) {
    if (world.isRemote || !isLog(world, log) || StumpGrinding.isStanding(cells(world), log)) {
      return 0;
    }
    return grindStump(world, log, player);
  }

  /** How the world reads to {@link StumpGrinding}: logs of any mod, this module's included. */
  static StumpGrinding.Cells cells(World world) {
    return p -> world.isBlockLoaded(p) && AnyTrees.isLog(world, p, world.getBlockState(p));
  }

  /**
   * How the world reads to {@link StumpGrinding} as ground: the natural soil a stump stands in.
   * Dirt, grass, sand, gravel, clay and the like, by material; never stone, wood or anything
   * built.
   */
  static StumpGrinding.Cells soil(World world) {
    return p -> {
      if (!world.isBlockLoaded(p)) {
        return false;
      }
      Material m = world.getBlockState(p).getMaterial();
      return m == Material.GROUND || m == Material.GRASS || m == Material.SAND
          || m == Material.CLAY;
    };
  }

  /**
   * Grinds the stump the log at {@code pos} belongs to: the caller has made sure the log is not a
   * standing tree. Server side.
   *
   * @param player who is grinding, for build permissions; null for a machine nobody drives
   *
   * @return how many logs went, 0 if it is not a stump (a log on a floor or a foundation, or too
   *     tall)
   */
  static int grindStump(World world, BlockPos pos, @Nullable EntityPlayer player) {
    StumpGrinding.Cells cells = cells(world);
    StumpGrinding.Cells soil = soil(world);
    Set<BlockPos> logs = StumpGrinding.grind(cells, soil, pos);
    if (logs.isEmpty()) {
      return 0;
    }
    int groundLevel = StumpGrinding.groundLevel(cells, soil, pos);
    int ground = 0;
    for (BlockPos p : logs) {
      if (!p.equals(pos) && !modifiable(world, player, p)) {
        continue;
      }
      IBlockState s = world.getBlockState(p);
      if (ground++ < EFFECTS) {
        world.playEvent(2001, p, Block.getStateId(s));
      }
      world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
    }
    // On the ground where the stump stood, or, when its root at ground level went too, in the
    // hole that left.
    BlockPos onGround = new BlockPos(pos.getX(), groundLevel + 1, pos.getZ());
    if (!placeMulch(world, player, onGround)) {
      placeMulch(world, player, onGround.down());
    }
    return ground;
  }

  private static boolean modifiable(World world, @Nullable EntityPlayer player, BlockPos pos) {
    return player == null || world.isBlockModifiable(player, pos);
  }

  /**
   * Leaves ground mulch at {@code pos}, if the cell is clear and stands on solid ground.
   *
   * @return whether mulch was placed
   */
  private static boolean placeMulch(World world, @Nullable EntityPlayer player, BlockPos pos) {
    Block mulch = CsmRegistry.getBlock(MULCH);
    if (mulch == null || !world.isAirBlock(pos) || !modifiable(world, player, pos)) {
      return false;
    }
    BlockPos below = pos.down();
    IBlockState ground = world.getBlockState(below);
    if (!ground.isSideSolid(world, below, EnumFacing.UP) || !mulch.canPlaceBlockAt(world, pos)) {
      return false;
    }
    world.setBlockState(pos, mulch.getDefaultState(), 3);
    return true;
  }
}
