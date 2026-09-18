package com.micatechnologies.minecraft.csm.constructionsite;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Finds the crane head a clicked mast or head block belongs to.
 *
 * <p>A crane is configured from any block of it, including the foot of its mast: its head can be
 * two hundred blocks up, and a player should not have to climb there to change the jib. Walks up
 * the mast from the clicked block to its top and looks for the head above -- above the same column
 * on a 1x1, above any of the section's four quarters on a 2x2, since the head goes on whichever
 * quarter it was placed on.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class CraneLocator {

  private CraneLocator() {
  }

  /**
   * The head the block at {@code pos} belongs to, or {@code null}.
   *
   * @param world the world
   * @param pos   a mast or head block
   *
   * @return the head, or {@code null} if {@code pos} is not part of a crane with a head
   *
   * @since 1.0
   */
  @Nullable
  public static TileEntityCraneHead findHead(World world, BlockPos pos) {
    TileEntity here = world.getTileEntity(pos);
    if (here instanceof TileEntityCraneHead) {
      return (TileEntityCraneHead) here;
    }
    Block mast = world.getBlockState(pos).getBlock();
    if (!(mast instanceof BlockCraneMast)) {
      return null;
    }
    BlockPos top = pos;
    while (top.getY() < world.getHeight() - 1
        && world.getBlockState(top.up()).getBlock() == mast) {
      top = top.up();
    }
    TileEntityCraneHead head = headAt(world, top.up());
    if (head != null || !(mast instanceof BlockCraneMastLarge)) {
      return head;
    }
    // A 2x2: the head may be on any quarter of the top section.
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        if ((dx != 0 || dz != 0) && world.getBlockState(top.add(dx, 0, dz)).getBlock() == mast) {
          head = headAt(world, top.add(dx, 1, dz));
          if (head != null) {
            return head;
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private static TileEntityCraneHead headAt(World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityCraneHead ? (TileEntityCraneHead) te : null;
  }
}
