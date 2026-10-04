package com.micatechnologies.minecraft.csm.transit.panel;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import java.util.function.BiConsumer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The walks that make cells of one block into a panel: the large hanging sign
 * ({@code transit.wayfinding.BlockWayfindingPanel}) and the large flight board
 * ({@code transit.airport.BlockFlightBoardLarge}) share them.
 *
 * <p><b>The panel.</b> Cells of one block facing the same way, side by side and stacked, are one
 * panel. Its controller is the bottom-left cell as the reader sees it. The panel's width is how far
 * the controller's row runs to the reader's right, its height how far the controller's column
 * runs up, each at most the block's own limit; a cell outside that rectangle (a ragged build, or
 * past the limit) belongs to the panel but is drawn plain.</p>
 *
 * @since 2026.10
 */
public final class CellPanel {

  /** How far above a panel a renderer looks for something to hang it from, in blocks. */
  public static final int MAX_ROD_DROP = 24;

  /**
   * Bumped whenever a chunk section holding a cell is rebuilt on the client (which is how a
   * neighbour change shows there: neighbour notifications are the server's). Each cell's layout
   * ({@link PanelLayout}) is looked at again when this has moved.
   */
  private static volatile int layoutGeneration;

  private CellPanel() {
  }

  /**
   * Called from a cell block's {@code getActualState}: under a {@link ChunkCache} that is a chunk
   * section being rebuilt on the client, so layouts are looked at again.
   *
   * @param world the world the actual state is asked of
   */
  public static void noteActualState(IBlockAccess world) {
    if (world instanceof ChunkCache) {
      layoutGeneration++;
    }
  }

  /** See {@link #noteActualState}. */
  public static int layoutGeneration() {
    return layoutGeneration;
  }

  /** Whether the cell at {@code pos} is this block facing this way: part of the same panel. */
  public static boolean joins(IBlockAccess world, BlockPos pos, Block block, EnumFacing facing) {
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() == block
        && state.getValue(AbstractBlockRotatableNSEW.FACING) == facing;
  }

  /** The reader's left for a panel facing this way: they face the panel, so it is clockwise. */
  public static EnumFacing leftOf(EnumFacing facing) {
    return facing.rotateY();
  }

  /**
   * The controller of the panel a cell is in: down the cell's column as far as the panel goes,
   * then along that row to the reader's left.
   *
   * @param world     the world
   * @param pos       a cell of the panel
   * @param state     its state
   * @param maxWidth  the widest panel the block makes
   * @param maxHeight the tallest panel the block makes
   *
   * @return the controller's position
   */
  public static BlockPos controllerOf(IBlockAccess world, BlockPos pos, IBlockState state,
      int maxWidth, int maxHeight) {
    Block block = state.getBlock();
    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEW.FACING);
    EnumFacing left = leftOf(facing);
    BlockPos at = pos;
    for (int i = 0; i < maxHeight * 4 && joins(world, at.down(), block, facing); i++) {
      at = at.down();
    }
    for (int i = 0; i < maxWidth * 4 && joins(world, at.offset(left), block, facing); i++) {
      at = at.offset(left);
    }
    return at;
  }

  /** Whether a cell is its panel's controller: no cell of it to the reader's left or below. */
  public static boolean isController(IBlockAccess world, BlockPos pos, Block block,
      EnumFacing facing) {
    return !joins(world, pos.offset(leftOf(facing)), block, facing)
        && !joins(world, pos.down(), block, facing);
  }

  /** How many cells the controller's row has, from the controller to the reader's right. */
  public static int widthFrom(IBlockAccess world, BlockPos controller, Block block,
      EnumFacing facing, int maxWidth) {
    EnumFacing right = leftOf(facing).getOpposite();
    int width = 1;
    while (width < maxWidth && joins(world, controller.offset(right, width), block, facing)) {
      width++;
    }
    return width;
  }

  /** How many cells the controller's column has, from the controller up. */
  public static int heightFrom(IBlockAccess world, BlockPos controller, Block block,
      EnumFacing facing, int maxHeight) {
    int height = 1;
    while (height < maxHeight && joins(world, controller.up(height), block, facing)) {
      height++;
    }
    return height;
  }

  /**
   * Visits the tile entity of every cell of the panel a cell is in: the controller's rectangle,
   * at most {@code maxWidth} x {@code maxHeight}.
   *
   * @param world     the world
   * @param cell      any cell of the panel
   * @param maxWidth  the widest panel the block makes
   * @param maxHeight the tallest panel the block makes
   * @param visitor   given each cell's position and tile entity (which may be null)
   */
  public static void forEachCell(World world, BlockPos cell, int maxWidth, int maxHeight,
      BiConsumer<BlockPos, TileEntity> visitor) {
    IBlockState state = world.getBlockState(cell);
    if (!(state.getBlock() instanceof AbstractBlockRotatableNSEW)) {
      return;
    }
    Block block = state.getBlock();
    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEW.FACING);
    BlockPos controller = controllerOf(world, cell, state, maxWidth, maxHeight);
    EnumFacing right = leftOf(facing).getOpposite();
    int width = widthFrom(world, controller, block, facing, maxWidth);
    int height = heightFrom(world, controller, block, facing, maxHeight);
    for (int col = 0; col < width; col++) {
      for (int row = 0; row < height; row++) {
        BlockPos at = controller.offset(right, col).up(row);
        if (joins(world, at, block, facing)) {
          visitor.accept(at, world.getTileEntity(at));
        }
      }
    }
  }
}
