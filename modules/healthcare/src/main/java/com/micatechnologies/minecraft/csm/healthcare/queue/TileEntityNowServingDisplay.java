package com.micatechnologies.minecraft.csm.healthcare.queue;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The now-serving display's tile entity: the number it shows ({@code number}, 0-999, 0 showing
 * dashes) and the desk it names under the number ({@code desk}, 0-99, 0 for none). Both are NBT a
 * generator can set with {@code /blockdata}; a linked display's number is then set by its queue
 * controller ({@link QueueNetwork}), and it catches up from the network when its chunk loads. It
 * does not tick.
 *
 * @since 2026.10
 */
public class TileEntityNowServingDisplay extends AbstractTileEntity {

  private static final String KEY_NUMBER = "number";
  private static final String KEY_DESK = "desk";

  /** The highest desk number shown. */
  public static final int MAX_DESK = 99;

  private int number;
  private int desk;
  private EnumFacing facing;

  /** The number shown, 0 for none yet. */
  public int getNumber() {
    return number;
  }

  /** The desk named under the number, 0 for none. */
  public int getDesk() {
    return desk;
  }

  /**
   * Shows a number and sends it to the clients watching.
   *
   * @param number the number, 0-999
   */
  public void setNumber(int number) {
    int n = Math.max(0, Math.min(QueueNetwork.MAX_NUMBER, number));
    if (n == this.number) {
      return;
    }
    this.number = n;
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  /**
   * The way the screen faces, looked up once. Not cached while the block is not there yet (a
   * client can make the tile entity before the block arrives), and forgotten when the block's
   * state changes in place.
   */
  public EnumFacing getFacing() {
    if (facing == null && world != null) {
      IBlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof BlockNowServingDisplay) {
        facing = s.getValue(BlockHealthcare.FACING);
      }
    }
    return facing == null ? EnumFacing.NORTH : facing;
  }

  @Override
  public void updateContainingBlockInfo() {
    super.updateContainingBlockInfo();
    facing = null;
  }

  /** A linked display left behind while its chunk was unloaded takes up its queue's number. */
  @Override
  public void onLoad() {
    super.onLoad();
    if (world != null && !world.isRemote) {
      QueueNetwork net = QueueNetwork.get(world);
      BlockPos c = net.controllerOf(pos);
      if (c != null) {
        number = net.numberOf(c);
      }
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    number = Math.max(0, Math.min(QueueNetwork.MAX_NUMBER, compound.getInteger(KEY_NUMBER)));
    desk = Math.max(0, Math.min(MAX_DESK, compound.getInteger(KEY_DESK)));
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_NUMBER, number);
    compound.setInteger(KEY_DESK, desk);
    return compound;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockPos p = getPos();
    return new AxisAlignedBB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1,
        p.getZ() + 1);
  }

  /** Big numbers are read further away than the ED board's rows, but not past this. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(48.0 * 48.0);
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
