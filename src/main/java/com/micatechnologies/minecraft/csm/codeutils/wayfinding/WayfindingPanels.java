package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * How cells of a wayfinding panel block make one panel, and the world around it: the walks that
 * find a panel's controller and size, the writes that put a sign on every cell, the server's
 * work-out of each cell's place, and the client's look for the ceiling above, the floor below
 * and the wall behind.
 *
 * <p><b>The panel.</b> Cells of one block facing the same way, side by side and stacked, are one
 * panel, at most {@link #MAX_WIDTH} x {@link #MAX_HEIGHT}. Its controller is the bottom-left cell
 * as the reader sees it. The panel's width is how far the controller's row runs to the reader's
 * right, its height how far the controller's column runs up; a cell outside that rectangle (a
 * ragged build) is drawn as a plain slab. Every cell holds the sign, so whichever cell becomes
 * the controller already has it.</p>
 *
 * @since 2026.10
 */
public final class WayfindingPanels {

  /** The widest panel, in blocks. */
  public static final int MAX_WIDTH = 16;
  /** The tallest panel, in blocks. */
  public static final int MAX_HEIGHT = 6;
  /** How far above a hung panel a rod looks for a ceiling, and below a post a floor, blocks. */
  public static final int MAX_REACH = 24;
  /** The most cells one placement looks at when it works out the panels around it. */
  private static final int MAX_FLOOD = 512;

  /** Cells read with no place (saved before the place was kept), to work out next tick. */
  private static final Map<World, Set<BlockPos>> PENDING = new HashMap<>();

  private WayfindingPanels() {
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
   */
  public static BlockPos controllerOf(IBlockAccess world, BlockPos pos, Block block,
      EnumFacing facing) {
    EnumFacing left = leftOf(facing);
    BlockPos at = pos;
    for (int i = 0; i < MAX_HEIGHT * 4 && joins(world, at.down(), block, facing); i++) {
      at = at.down();
    }
    for (int i = 0; i < MAX_WIDTH * 4 && joins(world, at.offset(left), block, facing); i++) {
      at = at.offset(left);
    }
    return at;
  }

  /** How many cells the controller's row has, from the controller to the reader's right. */
  public static int widthFrom(IBlockAccess world, BlockPos controller, Block block,
      EnumFacing facing) {
    EnumFacing right = leftOf(facing).getOpposite();
    int width = 1;
    while (width < MAX_WIDTH && joins(world, controller.offset(right, width), block, facing)) {
      width++;
    }
    return width;
  }

  /** How many cells the controller's column has, from the controller up. */
  public static int heightFrom(IBlockAccess world, BlockPos controller, Block block,
      EnumFacing facing) {
    int height = 1;
    while (height < MAX_HEIGHT && joins(world, controller.up(height), block, facing)) {
      height++;
    }
    return height;
  }

  /**
   * Writes a sign to every cell of the panel a cell is in (the controller's rectangle), each
   * queued for the clients with the tick's block changes. Server side: the editor's packet, a
   * click and a {@code /blockdata} edit of any cell all come here.
   *
   * @param world the world
   * @param cell  any cell of the panel
   * @param sign  the sign
   */
  public static void applyToPanel(World world, BlockPos cell, WayfindingSign sign) {
    IBlockState state = world.getBlockState(cell);
    if (!(state.getBlock() instanceof AbstractBlockWayfindingPanel)) {
      return;
    }
    Block block = state.getBlock();
    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEW.FACING);
    BlockPos controller = controllerOf(world, cell, block, facing);
    EnumFacing right = leftOf(facing).getOpposite();
    int width = widthFrom(world, controller, block, facing);
    int height = heightFrom(world, controller, block, facing);
    for (int col = 0; col < width; col++) {
      for (int row = 0; row < height; row++) {
        BlockPos at = controller.offset(right, col).up(row);
        TileEntity te = world.getTileEntity(at);
        if (te instanceof AbstractTileEntityWayfindingPanel) {
          AbstractTileEntityWayfindingPanel panel = (AbstractTileEntityWayfindingPanel) te;
          if (panel.setSign(sign)) {
            // queued, not sent: a /setblock with NBT spreads from here before its own block
            // change has gone out, and a client drops an update that arrives ahead of its cell
            panel.markDirtyQueueSync(world, at);
          }
        }
      }
    }
  }

  /**
   * Works out again every cell's place in the panels around {@code origin}, after a cell was
   * added or taken away there: each panel's size and each of its cells' column and row, and the
   * controller's sign on every cell of its rectangle (a cell outside it, on a ragged build, is
   * drawn plain). A cell whose place or sign changed is sent to the clients, which is what makes
   * the baked legend move with the panel's shape, even into a chunk section the change did not
   * touch. Server side.
   *
   * @param world  the world
   * @param origin where a cell was added or removed
   * @param block  the panel block
   * @param facing the facing of the cell added or removed
   */
  public static void refresh(World world, BlockPos origin, Block block, EnumFacing facing) {
    EnumFacing left = leftOf(facing);
    EnumFacing[] plane = {left, left.getOpposite(), EnumFacing.UP, EnumFacing.DOWN};
    List<BlockPos> seeds = new ArrayList<>();
    seeds.add(origin);
    for (EnumFacing f : plane) {
      seeds.add(origin.offset(f));
    }
    Set<BlockPos> seen = new HashSet<>();
    List<BlockPos> cells = new ArrayList<>();
    ArrayDeque<BlockPos> queue = new ArrayDeque<>();
    for (BlockPos s : seeds) {
      if (world.isBlockLoaded(s) && joins(world, s, block, facing) && seen.add(s)) {
        queue.add(s);
      }
    }
    while (!queue.isEmpty() && cells.size() < MAX_FLOOD) {
      BlockPos at = queue.poll();
      cells.add(at);
      for (EnumFacing f : plane) {
        BlockPos n = at.offset(f);
        if (!seen.contains(n) && world.isBlockLoaded(n) && joins(world, n, block, facing)) {
          seen.add(n);
          queue.add(n);
        }
      }
    }
    EnumFacing right = left.getOpposite();
    Map<BlockPos, int[]> sizes = new HashMap<>();
    for (BlockPos at : cells) {
      TileEntity te = world.getTileEntity(at);
      if (!(te instanceof AbstractTileEntityWayfindingPanel)) {
        continue;
      }
      AbstractTileEntityWayfindingPanel cell = (AbstractTileEntityWayfindingPanel) te;
      BlockPos controller = controllerOf(world, at, block, facing);
      int[] size = sizes.computeIfAbsent(controller, c -> new int[]{
          widthFrom(world, c, block, facing), heightFrom(world, c, block, facing)});
      int col = (at.getX() - controller.getX()) * right.getXOffset()
          + (at.getZ() - controller.getZ()) * right.getZOffset();
      int row = at.getY() - controller.getY();
      boolean inside = col >= 0 && row >= 0 && col < size[0] && row < size[1];
      boolean changed;
      if (inside) {
        changed = cell.setPlace(col, row, size[0], size[1]);
        TileEntity src = world.getTileEntity(controller);
        if (src instanceof AbstractTileEntityWayfindingPanel && src != cell
            && ((AbstractTileEntityWayfindingPanel) src).hasOwnSign()) {
          changed |= cell.setSign(((AbstractTileEntityWayfindingPanel) src).getSign());
        }
      } else {
        changed = cell.setPlace(0, 0, 0, 0);
      }
      if (changed) {
        // queued, not sent: a panel built in one tick refreshes its earlier cells with each one
        // added, and sent at once those reached a client ahead of the cells' block changes
        cell.markDirtyQueueSync(world, at);
      }
    }
  }

  /**
   * A cell placed fresh (no sign of its own yet) takes the sign of a cell it joins, so a panel
   * keeps its legend whichever cell ends up its controller.
   */
  static void adoptNeighbourSign(World world, BlockPos pos, Block block, EnumFacing facing,
      AbstractTileEntityWayfindingPanel mine) {
    EnumFacing left = leftOf(facing);
    for (EnumFacing side : new EnumFacing[]{left.getOpposite(), EnumFacing.UP, left,
        EnumFacing.DOWN}) {
      BlockPos at = pos.offset(side);
      if (!joins(world, at, block, facing)) {
        continue;
      }
      TileEntity other = BlockUtils.getTileEntitySafe(world, at);
      if (other instanceof AbstractTileEntityWayfindingPanel
          && ((AbstractTileEntityWayfindingPanel) other).hasOwnSign()) {
        mine.setSign(((AbstractTileEntityWayfindingPanel) other).getSign());
        return;
      }
    }
  }

  /**
   * The mount a panel is drawn with: its own, or for {@link WayfindingMount#AUTO} a wall when the
   * controller has a solid face behind it, else hung.
   *
   * @param world      the world (a chunk cache, on the client)
   * @param controller the panel's controller
   * @param facing     the panel's facing, towards the reader
   * @param mount      the sign's mount
   * @return the mount, never {@link WayfindingMount#AUTO}
   */
  public static WayfindingMount resolveMount(IBlockAccess world, BlockPos controller,
      EnumFacing facing, WayfindingMount mount) {
    if (mount != WayfindingMount.AUTO) {
      return mount;
    }
    BlockPos behind = controller.offset(facing.getOpposite());
    IBlockState state = world.getBlockState(behind);
    return state.getBlockFaceShape(world, behind, facing) == BlockFaceShape.SOLID
        ? WayfindingMount.WALL : WayfindingMount.HUNG;
  }

  /**
   * How many blocks of air there are from a cell up to the first block above, or down to the
   * first block below.
   *
   * @return the count, or -1 when nothing is found within {@link #MAX_REACH}
   */
  public static int reach(IBlockAccess world, BlockPos pos, EnumFacing direction) {
    int n = 0;
    BlockPos at = pos.offset(direction);
    while (n < MAX_REACH && world.isAirBlock(at)) {
      n++;
      at = at.offset(direction);
    }
    return n < MAX_REACH ? n : -1;
  }

  // --- cells saved before the place was kept ----------------------------------------------

  /**
   * Asks for a cell's place to be worked out at the end of the server tick: a cell read from a
   * chunk is not in the world yet, and walking its panel then would load chunks.
   */
  static void queueRefresh(World world, BlockPos pos) {
    synchronized (PENDING) {
      PENDING.computeIfAbsent(world, w -> new HashSet<>()).add(pos.toImmutable());
    }
  }

  /** Works out the queued cells' places. Registered on the event bus by Core. */
  public static final class Ticker {

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
      if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
        return;
      }
      Set<BlockPos> cells;
      synchronized (PENDING) {
        cells = PENDING.remove(event.world);
      }
      if (cells == null) {
        return;
      }
      World world = event.world;
      for (BlockPos pos : cells) {
        if (!world.isBlockLoaded(pos)) {
          continue;
        }
        TileEntity te = world.getTileEntity(pos);
        // one refresh places the whole panel, so its other queued cells are known by now
        if (te instanceof AbstractTileEntityWayfindingPanel
            && !((AbstractTileEntityWayfindingPanel) te).isPlaceKnown()) {
          IBlockState state = world.getBlockState(pos);
          refresh(world, pos, state.getBlock(), state.getValue(AbstractBlockRotatableNSEW.FACING));
        }
      }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
      synchronized (PENDING) {
        PENDING.remove(event.getWorld());
      }
    }
  }
}
