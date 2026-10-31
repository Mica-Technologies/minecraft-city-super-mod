package com.micatechnologies.minecraft.csm.venues.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.light.AbstractLightFootprintTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.light.CsmLightFootprints;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

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
 * <p><b>Aiming.</b> By default the grid lies square to the head's facing, centred straight ahead.
 * {@code aim} (NBT, two whole numbers: {@code /blockdata x y z {aim:[I;20,-30]}} or
 * {@code {aim:[20,-30]}}) moves the grid's centre to that many blocks east and south of the head
 * and turns the grid to face along that line, so a corner tower can light the middle of the field:
 * the columns are the same {@code n x n} grid every {@link BlockLightHead#GRID_SPACING} blocks,
 * turned to any angle and each rounded to the nearest block. {@code aim:[I;0,0]} goes back to
 * straight ahead. Re-aiming gives up the cells outside the new grid and fills the new ones
 * (Core's {@code CsmLightFootprints.reshaped}). The head's model still turns only four ways.</p>
 *
 * <p>Saved: Core's {@code lpa} list, {@code aim} if set, and {@code pw}, whether the master was
 * powered last time a neighbour changed, so redstone switches the head on its edges only and a
 * click still works.</p>
 *
 * @since 2026.10
 */
public class TileEntityLightHead extends AbstractLightFootprintTileEntity {

  private static final String KEY_POWERED = "pw";
  private static final String KEY_AIM = "aim";

  private boolean powered;
  /** The grid's centre, blocks east and south of the master; both 0 for straight ahead. */
  private int aimX;
  private int aimZ;
  /** Set once the tile entity is in the world, so an aim read later is a re-aim. */
  private boolean live;
  private EnumFacing cachedFacing;
  private long[] cachedColumns;
  private final LongOpenHashSet cachedSet = new LongOpenHashSet();

  /** Whether the head is aimed somewhere other than straight ahead. */
  public boolean isAimed() {
    return aimX != 0 || aimZ != 0;
  }

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
      // the grid's forward and right, unit vectors; its centre
      double fx = facing.getXOffset();
      double fz = facing.getZOffset();
      double cx = pos.getX() + fx * head.gridDistance();
      double cz = pos.getZ() + fz * head.gridDistance();
      if (isAimed()) {
        double len = Math.sqrt((double) aimX * aimX + (double) aimZ * aimZ);
        fx = aimX / len;
        fz = aimZ / len;
        cx = pos.getX() + aimX;
        cz = pos.getZ() + aimZ;
      }
      double rx = -fz;
      double rz = fx;
      long[] columns = new long[n * n];
      cachedSet.clear();
      int k = 0;
      // Near row first, so the light comes on from the tower outwards
      for (int j = -half; j <= half; j++) {
        for (int i = -half; i <= half; i++) {
          int x = (int) Math.round(cx + (rx * i + fx * j) * s);
          int z = (int) Math.round(cz + (rz * i + fz * j) * s);
          long c = CsmLightFootprints.column(x, z);
          if (cachedSet.add(c)) {
            columns[k++] = c;
          }
        }
      }
      cachedColumns = k == columns.length ? columns : java.util.Arrays.copyOf(columns, k);
      cachedFacing = facing;
    }
    return cachedColumns;
  }

  @Override
  public boolean hasFootprintColumn(int x, int z) {
    footprintColumns();
    return cachedSet.contains(CsmLightFootprints.column(x, z));
  }

  @Override
  public void onLoad() {
    live = true;
    super.onLoad();
  }

  @Override
  public int footprintTopY() {
    return pos.getY();
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    powered = compound.getBoolean(KEY_POWERED);
    int[] aim = readAim(compound);
    if (aim[0] != aimX || aim[1] != aimZ) {
      aimX = aim[0];
      aimZ = aim[1];
      cachedColumns = null;
      if (live && world != null && !world.isRemote) {
        CsmLightFootprints.reshaped(this);
      }
    }
  }

  /** The aim in a compound: an int array or a list of two ints; {0, 0} if absent or odd. */
  private static int[] readAim(NBTTagCompound compound) {
    int[] out = {0, 0};
    if (compound.hasKey(KEY_AIM, Constants.NBT.TAG_INT_ARRAY)) {
      int[] a = compound.getIntArray(KEY_AIM);
      if (a.length == 2) {
        out = a;
      }
    } else if (compound.hasKey(KEY_AIM, Constants.NBT.TAG_LIST)) {
      NBTTagList list = compound.getTagList(KEY_AIM, Constants.NBT.TAG_INT);
      if (list.tagCount() == 2) {
        out = new int[]{list.getIntAt(0), list.getIntAt(1)};
      }
    }
    // a footprint further than 96 blocks off could not be handed over between heads
    out[0] = Math.max(-96, Math.min(96, out[0]));
    out[1] = Math.max(-96, Math.min(96, out[1]));
    return out;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    if (powered) {
      compound.setBoolean(KEY_POWERED, true);
    }
    if (isAimed()) {
      compound.setTag(KEY_AIM, new NBTTagIntArray(new int[]{aimX, aimZ}));
    }
    return compound;
  }
}
