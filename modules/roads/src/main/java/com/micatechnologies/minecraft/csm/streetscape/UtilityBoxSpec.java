package com.micatechnologies.minecraft.csm.streetscape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The size and shape of one utility box: how many cells it fills, the box around the whole unit,
 * and where its ID number decal sits, if it has one.
 *
 * <p>Everything is given for the unit facing north, in the frame of its root cell (the block the
 * player placed). A unit wider than a block grows toward the placing player's right, which
 * facing north is west ({@code -x}); a deeper one grows away from the player ({@code +z}); a
 * taller one grows up. {@code gen_streetscape_utility.py} writes these values into the tab lines,
 * measured from the same geometry it writes into the models, so the box and the model cannot
 * disagree.</p>
 *
 * @version 1.0
 */
public final class UtilityBoxSpec {

  /** No unit is more than this many cells along any axis; parts search this far for a root. */
  public static final int MAX_CELLS = 2;

  private final int width;
  private final int depth;
  private final int height;
  private final AxisAlignedBB unitBox;
  @Nullable
  private final Label label;
  @Nullable
  private final Phone phone;

  /**
   * @param width   cells across, toward the placing player's right
   * @param depth   cells deep, away from the player
   * @param height  cells tall
   * @param unitBox the box around the whole unit facing north, in blocks, relative to the root
   *                cell's origin (so a two-wide unit runs from x = -1)
   * @param label   the ID number decal, or {@code null} for none
   */
  public UtilityBoxSpec(int width, int depth, int height, AxisAlignedBB unitBox,
      @Nullable Label label) {
    this(width, depth, height, unitBox, label, null);
  }

  /**
   * A unit whose trouble sticker carries a phone number the player can set, as a pad-mount
   * transformer's does.
   *
   * @param phone where the phone number is printed, or {@code null} for no phone sticker
   */
  public UtilityBoxSpec(int width, int depth, int height, AxisAlignedBB unitBox,
      @Nullable Label label, @Nullable Phone phone) {
    if (width < 1 || depth < 1 || height < 1 || width > MAX_CELLS || depth > MAX_CELLS
        || height > MAX_CELLS) {
      throw new IllegalArgumentException("A utility box is 1 to " + MAX_CELLS + " cells a side");
    }
    this.width = width;
    this.depth = depth;
    this.height = height;
    this.unitBox = unitBox;
    this.label = label;
    this.phone = phone;
  }

  public boolean isMultiBlock() {
    return width * depth * height > 1;
  }

  @Nullable
  public Label getLabel() {
    return label;
  }

  /** Where the trouble sticker's phone number is printed, or {@code null} if it has none. */
  @Nullable
  public Phone getPhone() {
    return phone;
  }

  /**
   * Every cell the unit fills other than the root, for a root at {@code root} facing
   * {@code facing}.
   */
  public List<BlockPos> partCells(BlockPos root, EnumFacing facing) {
    if (!isMultiBlock()) {
      return Collections.emptyList();
    }
    List<BlockPos> cells = new ArrayList<>();
    for (int dy = 0; dy < height; dy++) {
      for (int dx = 0; dx > -width; dx--) {
        for (int dz = 0; dz < depth; dz++) {
          if (dx == 0 && dy == 0 && dz == 0) {
            continue;
          }
          cells.add(root.add(rotateX(dx, dz, facing), dy, rotateZ(dx, dz, facing)));
        }
      }
    }
    return cells;
  }

  /** Whether {@code cell} is one of the cells a unit rooted at {@code root} fills. */
  public boolean covers(BlockPos root, EnumFacing facing, BlockPos cell) {
    if (cell.equals(root)) {
      return true;
    }
    for (BlockPos part : partCells(root, facing)) {
      if (part.equals(cell)) {
        return true;
      }
    }
    return false;
  }

  /** The unit's box for a root cell facing north: the whole unit, not just the root cell. */
  public AxisAlignedBB getUnitBoxNorth() {
    return unitBox;
  }

  /** The unit's box turned to {@code facing}, still relative to the root cell's origin. */
  public AxisAlignedBB getUnitBox(EnumFacing facing) {
    return rotate(unitBox, facing);
  }

  /** The part of the unit's box inside the root cell, facing north. */
  public AxisAlignedBB getRootCellBoxNorth() {
    return new AxisAlignedBB(Math.max(0, unitBox.minX), Math.max(0, unitBox.minY),
        Math.max(0, unitBox.minZ), Math.min(1, unitBox.maxX), Math.min(1, unitBox.maxY),
        Math.min(1, unitBox.maxZ));
  }

  /**
   * Turns a box given facing north about the root cell's centre, the way the blockstate turns
   * the model: {@code y: 90} takes north to east.
   */
  public static AxisAlignedBB rotate(AxisAlignedBB box, EnumFacing facing) {
    double x0 = box.minX - 0.5;
    double z0 = box.minZ - 0.5;
    double x1 = box.maxX - 0.5;
    double z1 = box.maxZ - 0.5;
    double ax = rotateX(x0, z0, facing);
    double az = rotateZ(x0, z0, facing);
    double bx = rotateX(x1, z1, facing);
    double bz = rotateZ(x1, z1, facing);
    return new AxisAlignedBB(Math.min(ax, bx) + 0.5, box.minY, Math.min(az, bz) + 0.5,
        Math.max(ax, bx) + 0.5, box.maxY, Math.max(az, bz) + 0.5);
  }

  private static int rotateX(int x, int z, EnumFacing facing) {
    return (int) Math.round(rotateX((double) x, z, facing));
  }

  private static int rotateZ(int x, int z, EnumFacing facing) {
    return (int) Math.round(rotateZ((double) x, z, facing));
  }

  private static double rotateX(double x, double z, EnumFacing facing) {
    switch (facing) {
      case EAST:
        return -z;
      case SOUTH:
        return -x;
      case WEST:
        return z;
      default:
        return x;
    }
  }

  private static double rotateZ(double x, double z, EnumFacing facing) {
    switch (facing) {
      case EAST:
        return x;
      case SOUTH:
        return -z;
      case WEST:
        return -x;
      default:
        return z;
    }
  }

  /**
   * Where a unit's ID number decal is, facing north: the centre of its first line on the front
   * face, in pixels from the root cell's origin, and how big the numbers are.
   */
  public static final class Label {

    /** A utility decal's colours: yellow characters on black. */
    public static final int DECAL_YELLOW = 0xF0C020;
    public static final int DECAL_BLACK = 0x0F0F0D;

    private final float centreX;
    private final float centreY;
    private final float faceZ;
    private final int lines;
    private final float textHeight;
    private final boolean vertical;
    private final int textColour;
    private final int backColour;

    /**
     * @param centreX    the decal's centre across the face, in pixels
     * @param centreY    the centre of its first line, in pixels up
     * @param faceZ      the front face it is stuck on, in pixels
     * @param lines      one line ("P5") or two stacked ("2290" over "50")
     * @param textHeight a character's height, in pixels
     * @param vertical   one character per row, top to bottom, as a pedestal's numbers are
     */
    public Label(float centreX, float centreY, float faceZ, int lines, float textHeight,
        boolean vertical) {
      this(centreX, centreY, faceZ, lines, textHeight, vertical, DECAL_YELLOW, DECAL_BLACK);
    }

    /**
     * A label in other colours than a utility decal's yellow on black.
     *
     * @param textColour the characters' colour, 0xRRGGBB
     * @param backColour the backing's colour, 0xRRGGBB
     */
    public Label(float centreX, float centreY, float faceZ, int lines, float textHeight,
        boolean vertical, int textColour, int backColour) {
      this.textColour = textColour;
      this.backColour = backColour;
      this.centreX = centreX;
      this.centreY = centreY;
      this.faceZ = faceZ;
      this.lines = lines;
      this.textHeight = textHeight;
      this.vertical = vertical;
    }

    public float getCentreX() {
      return centreX;
    }

    public float getCentreY() {
      return centreY;
    }

    public float getFaceZ() {
      return faceZ;
    }

    public int getLines() {
      return lines;
    }

    public float getTextHeight() {
      return textHeight;
    }

    public boolean isVertical() {
      return vertical;
    }

    public int getTextColour() {
      return textColour;
    }

    public int getBackColour() {
      return backColour;
    }
  }

  /**
   * Where a pad-mount transformer's trouble sticker leaves room for its phone number ("IN CASE
   * OF TROUBLE / CALL ..."), facing north, in pixels from the root cell's origin. The sticker
   * itself is baked into the model; only the number is drawn, dark on the sticker's yellow.
   * {@code gen_streetscape_utility.py} measures these off the sticker's texture layout.
   */
  public static final class Phone {

    /** The sticker's print colour, a browner black. */
    public static final int INK = 0x34240C;

    private final float centreX;
    private final float centreY;
    private final float faceZ;
    private final float textHeight;
    private final float maxWidth;

    /**
     * @param centreX    the blank's centre across the face, in pixels
     * @param centreY    the blank's centre, in pixels up
     * @param faceZ      the sticker's face, in pixels
     * @param textHeight a character's height, in pixels
     * @param maxWidth   the blank's width: a longer number is drawn narrower to fit it
     */
    public Phone(float centreX, float centreY, float faceZ, float textHeight, float maxWidth) {
      this.centreX = centreX;
      this.centreY = centreY;
      this.faceZ = faceZ;
      this.textHeight = textHeight;
      this.maxWidth = maxWidth;
    }

    public float getCentreX() {
      return centreX;
    }

    public float getCentreY() {
      return centreY;
    }

    public float getFaceZ() {
      return faceZ;
    }

    public float getTextHeight() {
      return textHeight;
    }

    public float getMaxWidth() {
      return maxWidth;
    }
  }
}
