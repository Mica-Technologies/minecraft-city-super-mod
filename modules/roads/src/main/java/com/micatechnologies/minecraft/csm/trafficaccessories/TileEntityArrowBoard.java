package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Holds what an arrow board is displaying.
 *
 * <p>The pattern lives here rather than in block metadata because it will not fit: four facings
 * by five patterns is twenty states and metadata holds sixteen. A tile entity is needed anyway
 * for the glow renderer, so putting the pattern in it costs nothing extra and leaves room for
 * more patterns later.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityArrowBoard extends AbstractTileEntity {

  /**
   * The NBT key the pattern is stored under.
   *
   * @since 1.0
   */
  private static final String NBT_PATTERN = "pattern";

  /**
   * What the board is displaying.
   *
   * @since 1.0
   */
  private ArrowBoardPattern pattern = ArrowBoardPattern.CHEVRON_RIGHT;

  /**
   * How far into its own sequence this board starts, in milliseconds.
   *
   * <p>Two boards in the same work zone are not wired together and do not step in unison; giving
   * each one a random place in the cycle reproduces that for the cost of one field and nothing
   * per frame. Deliberately not persisted — a board that comes back on a different beat after a
   * reload is exactly what a real one does.</p>
   *
   * @since 1.0
   */
  private final long sequenceOffset = ThreadLocalRandom.current().nextLong(4000L);

  /**
   * Gets how far into its own sequence this board starts, in milliseconds.
   *
   * @return the offset
   *
   * @since 1.0
   */
  public long getSequenceOffset() {
    return sequenceOffset;
  }

  /**
   * Gets what the board is displaying.
   *
   * @return the pattern
   *
   * @since 1.0
   */
  public ArrowBoardPattern getPattern() {
    return pattern;
  }

  /**
   * Sets what the board is displaying.
   *
   * @param pattern the pattern
   *
   * @since 1.0
   */
  public void setPattern(ArrowBoardPattern pattern) {
    this.pattern = pattern == null ? ArrowBoardPattern.CHEVRON_RIGHT : pattern;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    pattern = ArrowBoardPattern.fromOrdinal(compound.getInteger(NBT_PATTERN));
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_PATTERN, pattern.ordinal());
    return compound;
  }

  /**
   * The board is several blocks tall and wider than its own cell, and the lamps it lights are at
   * the very top of it. A render box the size of the block would cull the glow the moment the
   * block's own cell left view, which is most of the time the board is worth looking at.
   *
   * @return the render bounding box
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 3.0, pos.getY() - 1.0, pos.getZ() - 3.0,
        pos.getX() + 4.0, pos.getY() + 6.0, pos.getZ() + 4.0);
  }
}
