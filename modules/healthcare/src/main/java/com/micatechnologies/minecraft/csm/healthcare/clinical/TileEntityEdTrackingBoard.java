package com.micatechnologies.minecraft.csm.healthcare.clinical;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The ED tracking board's tile entity: its two settings ({@code beds}, {@code seed}) and, on the
 * client, which page of the bed list it shows, looked up once a second from where it stands in
 * its bank ({@link #refreshView}) rather than every frame. It does not tick.
 *
 * @since 2026.10
 */
public class TileEntityEdTrackingBoard extends AbstractTileEntity {

  private static final String KEY_BEDS = "beds";
  private static final String KEY_SEED = "seed";
  private static final int REACH = 6;
  private static final long VIEW_TICKS = 20;

  private int beds = EdTrackingSchedule.DEFAULT_BEDS;
  private int seed;

  private long checkedAt = Long.MIN_VALUE;
  private int page;
  private EnumFacing facing = EnumFacing.NORTH;

  /** Looks the facing and the page up again if the last answer is older than a second. */
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
    if (!(block instanceof BlockEdTrackingBoard)) {
      return;
    }
    facing = state.getValue(BlockHealthcare.FACING);
    EnumFacing left = facing.rotateY();
    int before = count(pos, left, block);
    int width = before + 1 + count(pos, left.getOpposite(), block);
    int above = count(pos, EnumFacing.UP, block);
    page = above * width + before;
  }

  private int count(BlockPos from, EnumFacing step, Block block) {
    int n = 0;
    BlockPos at = from;
    while (n < REACH) {
      at = at.offset(step);
      IBlockState other = world.getBlockState(at);
      if (other.getBlock() != block || other.getValue(BlockHealthcare.FACING) != facing) {
        break;
      }
      n++;
    }
    return n;
  }

  public int getPage() {
    return page;
  }

  public EnumFacing getFacing() {
    return facing;
  }

  /** The beds the department has, 1 to {@link EdTrackingSchedule#MAX_BEDS}. */
  public int getBeds() {
    return beds;
  }

  /** The department's seed. */
  public int getSeed() {
    return seed;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    beds = compound.hasKey(KEY_BEDS) ? Math.max(1, Math.min(EdTrackingSchedule.MAX_BEDS,
        compound.getInteger(KEY_BEDS))) : EdTrackingSchedule.DEFAULT_BEDS;
    seed = compound.getInteger(KEY_SEED);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_BEDS, beds);
    compound.setInteger(KEY_SEED, seed);
    return compound;
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
    return CsmPerformance.capRenderDistanceSq(32.0 * 32.0);
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
