package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * A flight information board's tile entity. It saves nothing: what a board lists follows from
 * the world's clock, and which page of the list it shows from where it stands in its bank, which
 * is looked up once a second ({@link #refreshView}) rather than every frame.
 *
 * <p><b>Pages.</b> Boards of the same block facing the same way, side by side and stacked, are a
 * bank, read like a page of text: left to right along a row, then the row below. A board's page
 * is the number of boards to its reader's left in its row, plus the whole rows above it times the
 * row's width; each page is {@link TileEntityFlightBoardRenderer#ROWS} flights. A bank is looked
 * at no further than {@link #REACH} boards each way.</p>
 *
 * @since 2026.9
 */
public class TileEntityFlightBoard extends AbstractTileEntity {

  /** How far along a row or up a column a bank is followed. */
  private static final int REACH = 8;
  private static final long VIEW_TICKS = 20;

  private long checkedAt = Long.MIN_VALUE;
  private int page;
  private EnumFacing facing = EnumFacing.NORTH;
  private boolean arrivals;

  /**
   * Looks the facing and the page up again if the last answer is older than a second. The
   * renderer calls this every frame, before reading them.
   */
  public void refreshView() {
    if (world == null) {
      return;
    }
    long now = world.getTotalWorldTime();
    if (checkedAt != Long.MIN_VALUE && now - checkedAt < VIEW_TICKS && now >= checkedAt) {
      return;
    }
    checkedAt = now;
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    if (!(block instanceof BlockFlightBoard)) {
      return;
    }
    facing = state.getValue(BlockFlightBoard.FACING);
    arrivals = ((BlockFlightBoard) block).isArrivals();
    // the reader faces the screen, so their left is the facing turned clockwise
    EnumFacing left = facing.rotateY();
    int before = count(pos, left, block);
    int width = before + 1 + count(pos, left.getOpposite(), block);
    int above = count(pos, EnumFacing.UP, block);
    page = above * width + before;
  }

  /** How many boards of this block, facing this way, follow on from {@code from}. */
  private int count(BlockPos from, EnumFacing step, Block block) {
    int n = 0;
    BlockPos at = from;
    while (n < REACH) {
      at = at.offset(step);
      IBlockState other = world.getBlockState(at);
      if (other.getBlock() != block || other.getValue(BlockFlightBoard.FACING) != facing) {
        break;
      }
      n++;
    }
    return n;
  }

  /** The page of the list this board shows, as last looked up. */
  public int getPage() {
    return page;
  }

  /** The board's facing, as last looked up. */
  public EnumFacing getFacing() {
    return facing;
  }

  /** Whether the board lists arrivals, as last looked up. */
  public boolean isArrivals() {
    return arrivals;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockPos p = getPos();
    return new AxisAlignedBB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1,
        p.getZ() + 1);
  }

  /** Text a sixteenth of a block high is not read from further away than this. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return 48.0 * 48.0;
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
