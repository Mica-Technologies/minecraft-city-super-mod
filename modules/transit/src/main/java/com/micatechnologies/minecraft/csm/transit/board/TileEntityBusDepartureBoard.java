package com.micatechnologies.minecraft.csm.transit.board;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.transit.stop.BusAgency;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * A bus departure board's tile entity. It saves only what a player sets: which agency the board
 * lists ({@link #getFilter}) and whether it announces departures ({@link #isAnnouncing}). What it
 * lists follows from the stops it can see and the clock; which page of the list it shows, from
 * where it stands in its bank, looked up once a second ({@link #refreshView}) rather than every
 * frame.
 *
 * <p><b>Pages.</b> Boards of the same block facing the same way, side by side and stacked, are a
 * bank, read like a page of text as the flight information boards are: left to right along a
 * row, then the row below. A board's page is the boards to its reader's left plus the whole rows
 * above it; each page is {@link TileEntityBusDepartureBoardRenderer#ROWS} departures. When a
 * station has more departures than the bank has rows, the whole bank turns to the next set every
 * {@link TileEntityBusDepartureBoardRenderer#CYCLE_TICKS} ticks. A bank is followed no further
 * than {@link #REACH} boards each way.</p>
 *
 * @since 2026.9
 */
public class TileEntityBusDepartureBoard extends AbstractTileEntity {

  /** How far along a row or up a column a bank is followed. */
  private static final int REACH = 8;
  private static final long VIEW_TICKS = 20;

  private static final String KEY_FILTER = "f";
  private static final String KEY_ANNOUNCE = "a";

  private int filter;
  private boolean announcing;

  private long checkedAt = Long.MIN_VALUE;
  private int page;
  private int bankSize = 1;
  private EnumFacing facing = EnumFacing.NORTH;

  /**
   * The agency's name a filter lists, for the action bar and the board's title.
   *
   * @param filter 1 to 4
   *
   * @return the agency's name
   */
  static String agencyTitle(int filter) {
    BusAgency[] all = BusAgency.values();
    return all[Math.max(0, Math.min(all.length - 1, filter - 1))].getTitle();
  }

  /**
   * Which agency the board lists.
   *
   * @return 0 for every agency, else 1 + the {@link BusAgency} ordinal
   */
  public int getFilter() {
    return filter;
  }

  /**
   * Sets which agency the board lists and tells the players in range (server side).
   *
   * @param filter 0 for every agency, else 1 + the {@link BusAgency} ordinal
   */
  public void setFilter(int filter) {
    this.filter = clampFilter(filter);
    if (world != null && !world.isRemote) {
      markDirty();
      syncServerToClient(world);
    }
  }

  /**
   * Whether the board reads departures out (when the Text to Speech module is installed).
   *
   * @return true to announce
   */
  public boolean isAnnouncing() {
    return announcing;
  }

  /**
   * Sets whether the board reads departures out, and tells the players in range (server side).
   *
   * @param announcing true to announce
   */
  public void setAnnouncing(boolean announcing) {
    this.announcing = announcing;
    if (world != null && !world.isRemote) {
      markDirty();
      syncServerToClient(world);
    }
  }

  private static int clampFilter(int f) {
    return Math.max(0, Math.min(BusStation.FILTERS - 1, f));
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    filter = clampFilter(compound.getByte(KEY_FILTER));
    announcing = compound.getBoolean(KEY_ANNOUNCE);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setByte(KEY_FILTER, (byte) filter);
    compound.setBoolean(KEY_ANNOUNCE, announcing);
    return compound;
  }

  /**
   * Looks the facing, the page and the bank's size up again if the last answer is older than a
   * second. The renderer and the announcer call this before reading them.
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
    if (!(block instanceof BlockBusBoard)) {
      return;
    }
    facing = state.getValue(BlockBusBoard.FACING);
    // the reader faces the screen, so their left is the facing turned clockwise
    EnumFacing left = facing.rotateY();
    int before = count(pos, left, block);
    int width = before + 1 + count(pos, left.getOpposite(), block);
    int above = count(pos, EnumFacing.UP, block);
    int height = above + 1 + count(pos, EnumFacing.DOWN, block);
    page = above * width + before;
    bankSize = width * height;
  }

  /** How many boards of this block, facing this way, follow on from {@code from}. */
  private int count(BlockPos from, EnumFacing step, Block block) {
    int n = 0;
    BlockPos at = from;
    while (n < REACH) {
      at = at.offset(step);
      IBlockState other = world.getBlockState(at);
      if (other.getBlock() != block || other.getValue(BlockBusBoard.FACING) != facing) {
        break;
      }
      n++;
    }
    return n;
  }

  /** The board's page within its bank, as last looked up: 0 for the first board. */
  public int getPage() {
    return page;
  }

  /** How many boards the bank has, as last looked up. */
  public int getBankSize() {
    return bankSize;
  }

  /** The board's facing, as last looked up. */
  public EnumFacing getFacing() {
    return facing;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockPos p = getPos();
    return new AxisAlignedBB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1,
        p.getZ() + 1);
  }

  /** Text half a sixteenth of a block high is not read from further away than this. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(48.0 * 48.0);
  }

  /** Nothing a baked model draws comes from here: the filter and the title are the renderer's. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
