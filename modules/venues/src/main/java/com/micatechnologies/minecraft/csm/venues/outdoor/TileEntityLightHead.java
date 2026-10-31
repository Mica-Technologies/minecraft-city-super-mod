package com.micatechnologies.minecraft.csm.venues.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.light.AbstractLightFootprintTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.light.CsmLightFootprints;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * A stadium light head's tile entity, on its master: the field it lights and the light-up air it
 * has put there (Core's {@link AbstractLightFootprintTileEntity}), and the redstone it last saw.
 *
 * <p><b>The footprint</b> is a square grid of columns on the ground in front of the tower, a
 * column every {@link BlockLightHead#GRID_SPACING} blocks: {@code n x n} columns centred
 * {@code d} blocks out along the facing (large head 9 x 9 at 16, the near row 4 blocks out and the
 * far one 28; small head 5 x 5 at 9, from 3 to 15). Each column gets one light-up air on the
 * first surface below the head's bottom row, so a stand roof or the field under it is lit
 * whichever comes first, up to 64 blocks down. Nothing ticks: Core places and removes the cells a
 * few a tick, and the head asks again when it is switched, loaded or random-ticked.</p>
 *
 * <p>Saved: Core's {@code lpa} list and {@code pw}, whether the master was powered last time a
 * neighbour changed, so redstone switches the head on its edges only and a click still works.</p>
 *
 * @since 2026.10
 */
public class TileEntityLightHead extends AbstractLightFootprintTileEntity {

  private static final String KEY_POWERED = "pw";

  private boolean powered;
  private EnumFacing cachedFacing;
  private long[] cachedColumns;

  /** Whether the master was powered when a neighbour last changed. */
  boolean wasPowered() {
    return powered;
  }

  void setPowered(boolean powered) {
    this.powered = powered;
    markDirty();
  }

  private BlockLightHead head() {
    return getBlockType() instanceof BlockLightHead ? (BlockLightHead) getBlockType() : null;
  }

  private EnumFacing facing() {
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() instanceof BlockLightHead ? state.getValue(BlockLightHead.FACING)
        : EnumFacing.NORTH;
  }

  @Override
  public boolean isFootprintLit() {
    if (world == null) {
      return false;
    }
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() instanceof BlockLightHead && state.getValue(BlockLightHead.LIT);
  }

  @Override
  public long[] footprintColumns() {
    BlockLightHead head = head();
    if (head == null) {
      return new long[0];
    }
    EnumFacing facing = facing();
    if (cachedColumns == null || cachedFacing != facing) {
      int n = head.gridCount();
      int half = n / 2;
      int s = BlockLightHead.GRID_SPACING;
      EnumFacing right = facing.rotateY();
      int cx = pos.getX() + facing.getXOffset() * head.gridDistance();
      int cz = pos.getZ() + facing.getZOffset() * head.gridDistance();
      long[] columns = new long[n * n];
      int k = 0;
      // Near row first, so the light comes on from the tower outwards
      for (int j = -half; j <= half; j++) {
        for (int i = -half; i <= half; i++) {
          int x = cx + right.getXOffset() * i * s + facing.getXOffset() * j * s;
          int z = cz + right.getZOffset() * i * s + facing.getZOffset() * j * s;
          columns[k++] = CsmLightFootprints.column(x, z);
        }
      }
      cachedColumns = columns;
      cachedFacing = facing;
    }
    return cachedColumns;
  }

  @Override
  public boolean hasFootprintColumn(int x, int z) {
    BlockLightHead head = head();
    if (head == null) {
      return false;
    }
    EnumFacing facing = facing();
    int s = BlockLightHead.GRID_SPACING;
    int half = head.gridCount() / 2;
    int dx = x - (pos.getX() + facing.getXOffset() * head.gridDistance());
    int dz = z - (pos.getZ() + facing.getZOffset() * head.gridDistance());
    if (Math.floorMod(dx, s) != 0 || Math.floorMod(dz, s) != 0) {
      return false;
    }
    return Math.abs(dx) <= half * s && Math.abs(dz) <= half * s;
  }

  @Override
  public int footprintTopY() {
    return pos.getY();
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    powered = compound.getBoolean(KEY_POWERED);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    if (powered) {
      compound.setBoolean(KEY_POWERED, true);
    }
    return compound;
  }
}
