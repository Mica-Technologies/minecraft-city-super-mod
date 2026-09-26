package com.micatechnologies.minecraft.csm.powergrid.water;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The block that draws a unit of {@link BlockTankPart} cells: a tank tile, or a pedestal column
 * section. A part finds the root that covers it among the cells next to it, and takes its item,
 * its clicks and its breaking from there.
 *
 * @since 2026.9
 */
public interface ITankRoot {

  /** Whether the unit whose root is at {@code root} fills {@code cell}. */
  boolean coversCell(IBlockAccess world, BlockPos root, IBlockState rootState, BlockPos cell);

  /**
   * Removes the whole unit (a tank, a tank layer, a column section), dropping its one item for a
   * player not in creative, and nothing when {@code player} is null (an explosion, a command).
   */
  void demolishUnit(World world, BlockPos root, @Nullable EntityPlayer player);

  /** The block whose item places the unit. */
  net.minecraft.block.Block getUnitBlock();
}
