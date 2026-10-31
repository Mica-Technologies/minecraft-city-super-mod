package com.micatechnologies.minecraft.csm.codeutils.light;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * The tile entity of a light that lights a footprint of ground rather than one column: a grid of
 * columns, each given one light-up air on the first surface below {@link #footprintTopY()}, placed
 * and removed a few at a time by {@link CsmLightFootprints}. The subclass says where the columns
 * are and whether the light is lit; this class keeps the light-up air it placed, which is what
 * makes removal safe: a light only ever removes light-up air it placed itself.
 *
 * <p>Saved as {@code lpa}: the placed cells as offsets from the tile entity, three ints each.</p>
 */
public abstract class AbstractLightFootprintTileEntity extends AbstractTileEntity {

  /** How far below {@link #footprintTopY()} a column is scanned for a surface, by default. */
  public static final int DEFAULT_DEPTH = 64;

  private static final String KEY_PLACED = "lpa";

  /** The light-up air this light placed and has not yet removed, as {@link BlockPos#toLong}. */
  private final LongOpenHashSet placed = new LongOpenHashSet();

  /**
   * Whether the light is lit, from its block state.
   *
   * @return {@code true} if its footprint should be lit
   */
  public abstract boolean isFootprintLit();

  /**
   * The columns of the footprint, each packed by {@link CsmLightFootprints#column(int, int)}.
   *
   * @return the columns; the caller does not modify the array
   */
  public abstract long[] footprintColumns();

  /**
   * The first cell scanned in every column, going down.
   *
   * @return the scan's top y
   */
  public abstract int footprintTopY();

  /**
   * How many cells each column is scanned for a surface.
   *
   * @return the depth
   */
  public int footprintDepth() {
    return DEFAULT_DEPTH;
  }

  /**
   * Whether the footprint includes a column. The default walks {@link #footprintColumns()}; a
   * light with a regular grid may answer faster.
   *
   * @param x the column's x
   * @param z the column's z
   *
   * @return {@code true} if it is one of the footprint's columns
   */
  public boolean hasFootprintColumn(int x, int z) {
    long key = CsmLightFootprints.column(x, z);
    for (long c : footprintColumns()) {
      if (c == key) {
        return true;
      }
    }
    return false;
  }

  /**
   * Whether this light, lit, would take the light-up air at {@code air} as its own column's: it is
   * in one of its columns and a scan from its top meets it before anything else.
   *
   * @param air a light-up air's position
   *
   * @return {@code true} if it would claim it
   */
  public boolean wouldClaim(BlockPos air) {
    if (world == null || !isFootprintLit() || !hasFootprintColumn(air.getX(), air.getZ())) {
      return false;
    }
    return air.equals(CsmLightupAir.firstBelow(world, air.getX(), footprintTopY(), air.getZ(),
        footprintDepth()));
  }

  /** The light-up air this light placed, as {@link BlockPos#toLong}; live, server thread only. */
  LongSet placed() {
    return placed;
  }

  /**
   * Whether this light still has light-up air of its own in the world.
   *
   * @return {@code true} if it has placed some it has not removed
   */
  public boolean hasPlacedLight() {
    return !placed.isEmpty();
  }

  /**
   * How much light-up air this light has placed and not removed.
   *
   * @return the count
   */
  public int placedLightCount() {
    return placed.size();
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (world != null && !world.isRemote) {
      CsmLightFootprints.track(this);
      CsmLightFootprints.request(this);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    CsmLightFootprints.untrack(this);
  }

  @Override
  public void invalidate() {
    super.invalidate();
    CsmLightFootprints.untrack(this);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    placed.clear();
    int[] cells = compound.getIntArray(KEY_PLACED);
    for (int i = 0; i + 2 < cells.length; i += 3) {
      placed.add(pos.add(cells[i], cells[i + 1], cells[i + 2]).toLong());
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    if (!placed.isEmpty()) {
      int[] cells = new int[placed.size() * 3];
      int i = 0;
      for (long l : placed) {
        BlockPos p = BlockPos.fromLong(l);
        cells[i++] = p.getX() - pos.getX();
        cells[i++] = p.getY() - pos.getY();
        cells[i++] = p.getZ() - pos.getZ();
      }
      compound.setIntArray(KEY_PLACED, cells);
    }
    return compound;
  }
}
