package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.RoadSurfaceHeight;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;

/**
 * What a bus stop's blocks read from the stack they stand in: a stop is a column of
 * {@link AbstractBlockBusStopStack} blocks (a pole and the fittings clamped to it), and each
 * block's look depends on the others.
 *
 * <ul>
 *   <li>A block wears the pole's cap only with nothing of the stack above it, and the base only
 *       with nothing below it.</li>
 *   <li>A fitting draws the pole in the style of the nearest pole below it (or above, for a
 *       fitting at the bottom).</li>
 *   <li>The whole stack settles onto the surface under its bottom block, by that block's
 *       {@link RoadSurfaceHeight} offset: every block of it moves by the same amount, or the pole
 *       would come apart at the first joint.</li>
 * </ul>
 *
 * <p>Every lookup is bounded by {@link #REACH} blocks, so a stack taller than that simply stops
 * sharing beyond it.</p>
 *
 * @since 2026.9
 */
public final class BusStopStack {

  /** How far up or down a lookup walks the stack. */
  public static final int REACH = 8;

  private BusStopStack() {
  }

  /**
   * Whether the block at {@code pos} is part of a bus stop stack.
   *
   * @param world the world
   * @param pos   the position
   *
   * @return true for a pole or a fitting
   */
  public static boolean isPart(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof AbstractBlockBusStopStack;
  }

  /**
   * The pole style a fitting at {@code pos} draws: the nearest pole's below it, else the nearest
   * above, else galvanized round.
   *
   * @param world the world
   * @param pos   the fitting
   *
   * @return the style
   */
  public static BusStopPoleStyle styleAt(IBlockAccess world, BlockPos pos) {
    for (int dir = -1; dir <= 1; dir += 2) {
      BlockPos p = pos;
      for (int i = 0; i < REACH; i++) {
        p = p.up(dir);
        IBlockState state = world.getBlockState(p);
        if (state.getBlock() instanceof BlockBusStopPole) {
          return ((BlockBusStopPole) state.getBlock()).getStyle();
        }
        if (!(state.getBlock() instanceof AbstractBlockBusStopStack)) {
          break;
        }
      }
    }
    return BusStopPoleStyle.ROUND_GALVANIZED;
  }

  /**
   * The bottom block of the stack {@code pos} is in.
   *
   * @param world the world
   * @param pos   a block of the stack
   *
   * @return its bottom block
   */
  public static BlockPos bottomOf(IBlockAccess world, BlockPos pos) {
    BlockPos p = pos;
    for (int i = 0; i < REACH && isPart(world, p.down()); i++) {
      p = p.down();
    }
    return p;
  }

  /**
   * How far the stack {@code pos} is in settles: the bottom block's offset onto the surface under
   * it.
   *
   * @param world the world
   * @param pos   a block of the stack
   *
   * @return the offset, at most zero
   */
  public static double offsetAt(IBlockAccess world, BlockPos pos) {
    return RoadSurfaceHeight.offsetFor(world, bottomOf(world, pos));
  }

  /**
   * The flag of the stack {@code pos} is in, the nearest above it and then below. Safe off the main
   * thread: a chunk being rendered is read without creating a tile entity in it.
   *
   * @param world the world
   * @param pos   a block of the stack
   *
   * @return the flag's tile entity, or null if the stack has none
   */
  public static TileEntityBusStopFlag flagNear(IBlockAccess world, BlockPos pos) {
    for (int dir = 1; dir >= -1; dir -= 2) {
      BlockPos p = pos;
      for (int i = 0; i < REACH; i++) {
        p = p.up(dir);
        IBlockState state = world.getBlockState(p);
        if (state.getBlock() instanceof BlockBusStopFlag) {
          TileEntity te = tileEntity(world, p);
          if (te instanceof TileEntityBusStopFlag) {
            return (TileEntityBusStopFlag) te;
          }
        }
        if (!(state.getBlock() instanceof AbstractBlockBusStopStack)) {
          break;
        }
      }
    }
    return null;
  }

  /**
   * The tile entity at {@code pos}, without creating one in a chunk being rendered.
   *
   * @param world the world
   * @param pos   the position
   *
   * @return the tile entity, or null
   */
  public static TileEntity tileEntity(IBlockAccess world, BlockPos pos) {
    return world instanceof ChunkCache
        ? ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK)
        : world.getTileEntity(pos);
  }
}
