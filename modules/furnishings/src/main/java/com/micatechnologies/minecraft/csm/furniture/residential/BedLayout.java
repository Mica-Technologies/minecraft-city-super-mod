package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The shape of a {@link BlockResidentialBed}: which blocks it fills, and where each of its
 * sleepers lies. Offsets are in the bed's own frame, drawn facing north as every Residential
 * model is: {@code +x} is to the right of someone standing at the foot looking at the bed
 * ({@code facing.rotateY()}), {@code +y} up, and {@code +z} towards the head
 * ({@code facing.getOpposite()}). Cell 0 is where the bed was placed; it drops the item.
 *
 * <p>{@code gen_furniture_bedroom.py} draws each bed whole in this frame and cuts it into these
 * cells, so the two must agree; the mattress heights here are the ones it draws.</p>
 *
 * @since 2026.9
 */
public enum BedLayout {
  /** A single bed, one block wide and two long, the head away from whoever placed it. */
  SINGLE(new int[][]{{0, 0, 0}, {0, 0, 1}},
      new int[]{1}, new boolean[]{false}, new double[]{9}, new int[]{0, 0}),
  /**
   * A double or king bed, two blocks wide and two long, the second column to the right of the
   * placer (the placer faces the bed, so to their right). One sleeper a column.
   */
  WIDE(new int[][]{{0, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {-1, 0, 1}},
      new int[]{1, 3}, new boolean[]{false, false}, new double[]{9, 9}, new int[]{0, 0, 1, 1}),
  /** A bunk bed: a single bed with another above it, each slept in. */
  BUNK(new int[][]{{0, 0, 0}, {0, 0, 1}, {0, 1, 0}, {0, 1, 1}},
      new int[]{1, 3}, new boolean[]{false, false}, new double[]{6, 5}, new int[]{0, 0, 1, 1}),
  /**
   * A day bed, two blocks wide and one deep, its long side to the room like a sofa: the sleeper
   * lies along it, the head at the right-hand end (cell 0).
   */
  DAY(new int[][]{{0, 0, 0}, {-1, 0, 0}},
      new int[]{0}, new boolean[]{true}, new double[]{7}, new int[]{0, 0});

  /** How far towards the head of the bed a sleeper's head lies from its block's middle. */
  private static final double PILLOW_IN = 0.4;
  /** How far above the mattress a sleeper lies, in blocks: a vanilla bed's. */
  private static final double ABOVE_MATTRESS = 0.125;

  private final int[][] cells;
  private final int[] spotHead;
  private final boolean[] spotAlongX;
  private final double[] spotMattress;
  private final int[] cellSpot;

  BedLayout(int[][] cells, int[] spotHead, boolean[] spotAlongX, double[] spotMattress,
      int[] cellSpot) {
    this.cells = cells;
    this.spotHead = spotHead;
    this.spotAlongX = spotAlongX;
    this.spotMattress = spotMattress;
    this.cellSpot = cellSpot;
  }

  /**
   * How many blocks the bed fills.
   *
   * @return the cell count, 2 or 4
   */
  public int size() {
    return cells.length;
  }

  /**
   * Where cell {@code part} of a bed placed at {@code anchor} facing {@code facing} is.
   *
   * @param anchor the bed's cell 0
   * @param facing the way the bed faces (its foot towards the placer)
   * @param part   the cell
   *
   * @return the cell's position
   */
  public BlockPos cellPos(BlockPos anchor, EnumFacing facing, int part) {
    int[] c = cells[part];
    return anchor.offset(facing.rotateY(), c[0]).up(c[1]).offset(facing.getOpposite(), c[2]);
  }

  /**
   * Where cell 0 is, seen from cell {@code part} at {@code pos}.
   *
   * @param pos    the cell
   * @param facing the way the bed faces
   * @param part   which cell it is
   *
   * @return cell 0's position
   */
  public BlockPos anchor(BlockPos pos, EnumFacing facing, int part) {
    int[] c = cells[part];
    return pos.offset(facing.rotateY(), -c[0]).down(c[1]).offset(facing.getOpposite(), -c[2]);
  }

  /**
   * Whether cell {@code part} is on the bed's floor (the lower bunk, or any cell of the others).
   *
   * @param part the cell
   *
   * @return true on the floor
   */
  public boolean onFloor(int part) {
    return cells[part][1] == 0;
  }

  /**
   * Which sleeper cell {@code part} belongs to.
   *
   * @param part the cell
   *
   * @return the sleeping spot
   */
  public int spotOf(int part) {
    return cellSpot[part];
  }

  /**
   * The cell the sleeper of {@code spot} lays their head in: the bed's position for sleeping.
   *
   * @param spot the sleeping spot
   *
   * @return the head cell
   */
  public int headOf(int spot) {
    return spotHead[spot];
  }

  /**
   * Whether cell {@code part} is where a sleeper's head lies.
   *
   * @param part the cell
   *
   * @return true for a head cell
   */
  public boolean isHead(int part) {
    return spotHead[cellSpot[part]] == part;
  }

  /**
   * The way from a sleeper's feet to their head, in the world: vanilla's bed direction.
   *
   * @param facing the way the bed faces
   * @param spot   the sleeping spot
   *
   * @return the direction
   */
  public EnumFacing direction(EnumFacing facing, int spot) {
    return spotAlongX[spot] ? facing.rotateY() : facing.getOpposite();
  }

  /**
   * Where the sleeper of {@code spot} lies: their head, a little above the mattress and a little
   * short of the head of the bed, as in a vanilla bed.
   *
   * @param head   the head cell's position
   * @param facing the way the bed faces
   * @param spot   the sleeping spot
   *
   * @return the point
   */
  public Vec3d pillow(BlockPos head, EnumFacing facing, int spot) {
    EnumFacing dir = direction(facing, spot);
    return new Vec3d(head.getX() + 0.5 + dir.getXOffset() * PILLOW_IN,
        head.getY() + spotMattress[spot] / 16.0 + ABOVE_MATTRESS,
        head.getZ() + 0.5 + dir.getZOffset() * PILLOW_IN);
  }
}
