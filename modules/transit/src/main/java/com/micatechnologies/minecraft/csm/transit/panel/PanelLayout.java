package com.micatechnologies.minecraft.csm.transit.panel;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A cell's view of its panel, client only and never saved: whether the cell is the controller,
 * the panel's size and where its hanger rods go and how long they are ({@link CellPanel}). A cell's
 * tile entity holds one and its renderer calls {@link #refresh} every frame, which walks the
 * world only when a nearby chunk section was rebuilt ({@link CellPanel#layoutGeneration}) or two
 * seconds have passed, the latter for a ceiling changing out of reach of that signal.
 *
 * @since 2026.10
 */
public final class PanelLayout {

  /** Ticks after which the layout is looked at again whatever happened. */
  private static final long LAYOUT_TICKS = 40;
  /** Ticks between looks prompted by a chunk rebuild, so a burst of them costs one. */
  private static final long LAYOUT_MIN_TICKS = 2;

  private final Class<? extends Block> type;
  private final int maxWidth;
  private final int maxHeight;
  private final boolean wallStopsRods;

  private long layoutAt = Long.MIN_VALUE;
  private int layoutGeneration;
  private boolean controller;
  private int width = 1;
  private int height = 1;
  private EnumFacing facing = EnumFacing.NORTH;
  private float[] rodX = new float[0];
  private int[] rodDrop = new int[0];
  private AxisAlignedBB renderBox;

  /**
   * Constructs a layout.
   *
   * @param type          the cells' block class
   * @param maxWidth      the widest panel, in blocks
   * @param maxHeight     the tallest panel, in blocks
   * @param wallStopsRods whether a column with a block behind its top cell (a wall) has no rod
   */
  public PanelLayout(Class<? extends Block> type, int maxWidth, int maxHeight,
      boolean wallStopsRods) {
    this.type = type;
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
    this.wallStopsRods = wallStopsRods;
  }

  /** Makes the next {@link #refresh} walk the world whatever. */
  public void invalidate() {
    layoutAt = Long.MIN_VALUE;
  }

  /**
   * Works the layout out again if it may have changed. It costs two block lookups for a cell
   * that is not a controller.
   *
   * @param world the world
   * @param pos   the cell
   */
  @SideOnly(Side.CLIENT)
  public void refresh(World world, BlockPos pos) {
    if (world == null) {
      return;
    }
    long now = world.getTotalWorldTime();
    int generation = CellPanel.layoutGeneration();
    if (layoutAt != Long.MIN_VALUE && now >= layoutAt) {
      long age = now - layoutAt;
      boolean rebuilt = generation != layoutGeneration && age >= LAYOUT_MIN_TICKS;
      if (age < LAYOUT_TICKS && !rebuilt) {
        return;
      }
    }
    layoutAt = now;
    layoutGeneration = generation;
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    if (!type.isInstance(block)) {
      controller = false;
      return;
    }
    facing = state.getValue(AbstractBlockRotatableNSEW.FACING);
    controller = CellPanel.isController(world, pos, block, facing);
    if (!controller) {
      return;
    }
    width = CellPanel.widthFrom(world, pos, block, facing, maxWidth);
    height = CellPanel.heightFrom(world, pos, block, facing, maxHeight);
    EnumFacing right = CellPanel.leftOf(facing).getOpposite();
    hangers(world, pos, right);
    renderBox = box(pos, right);
  }

  /**
   * Where the rods go: two, or one for about every four blocks of a wider panel, evenly along
   * the top; each reaches up through air to the first block above, and a rod with a block right
   * on top of the panel, or nothing within {@link CellPanel#MAX_ROD_DROP}, is not drawn (nor, for
   * a panel that hangs only off a wall's line, one with a block behind its column's top cell).
   */
  private void hangers(World world, BlockPos pos, EnumFacing right) {
    int n = Math.max(2, Math.round(width / 4f));
    if (rodX.length != n) {
      rodX = new float[n];
      rodDrop = new int[n];
    }
    for (int i = 0; i < n; i++) {
      float x = width * (i + 0.5f) / n;
      rodX[i] = x;
      int column = Math.min(width - 1, (int) x);
      BlockPos top = pos.offset(right, column).up(height - 1);
      if (wallStopsRods && !world.isAirBlock(top.offset(facing.getOpposite()))) {
        rodDrop[i] = 0;
        continue;
      }
      int drop = 0;
      BlockPos at = top.up();
      while (drop < CellPanel.MAX_ROD_DROP && world.isAirBlock(at)) {
        drop++;
        at = at.up();
      }
      rodDrop[i] = drop < CellPanel.MAX_ROD_DROP ? drop : 0;
    }
  }

  private AxisAlignedBB box(BlockPos pos, EnumFacing right) {
    int maxDrop = 0;
    for (int d : rodDrop) {
      maxDrop = Math.max(maxDrop, d);
    }
    BlockPos far = pos.offset(right, width - 1).up(height - 1 + maxDrop);
    return new AxisAlignedBB(Math.min(pos.getX(), far.getX()), pos.getY(),
        Math.min(pos.getZ(), far.getZ()), Math.max(pos.getX(), far.getX()) + 1, far.getY() + 1,
        Math.max(pos.getZ(), far.getZ()) + 1);
  }

  /** Whether the cell drew the panel when the layout was last worked out. */
  public boolean isController() {
    return controller;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public EnumFacing getFacing() {
    return facing;
  }

  /** The rods' places along the top, in blocks from the panel's left edge as read. */
  public float[] getRodX() {
    return rodX;
  }

  /** Each rod's length in blocks, 0 for none. */
  public int[] getRodDrop() {
    return rodDrop;
  }

  /**
   * The box the controller draws in, or the cell's own block for any other cell.
   *
   * @param pos the cell
   *
   * @return the render box
   */
  public AxisAlignedBB getRenderBox(BlockPos pos) {
    AxisAlignedBB box = renderBox;
    return box != null && controller ? box : new AxisAlignedBB(pos, pos.add(1, 1, 1));
  }
}
