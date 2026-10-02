package com.micatechnologies.minecraft.csm.furniture.office;

/**
 * What a cubicle panel with a name on it carries ({@link BlockCubiclePanelNamed}), and where its
 * lines are printed. The frame and the blank insert are the panel's baked model
 * ({@code gen_furniture_office.py}, {@code NAMED_STYLES}); only the words are drawn, by
 * {@link TileEntityCubicleNamePlateRenderer}, so the numbers here are the generator's.
 *
 * <p>Every position is in sixteenths of the model, which is drawn with its plate on the north
 * face of a panel running east-west: the insert's face is at {@link #FACE_Z}, the plate centred
 * across the block at {@code x = 8}.</p>
 *
 * <p>The sign has two layouts. Within its own block it is 13 by 9 sixteenths; with a full plain
 * panel either side ({@link BlockCubiclePanelNamed#WIDE}) it reaches a quarter block over each
 * of them, a block and a half across, and the words are printed larger. A name is a dozen or
 * more characters, so the width of the insert, not its height, is what limits how large it can
 * be printed, and how far away it reads.</p>
 *
 * @since 2026.10
 */
public enum CubicleSignStyle {

  /**
   * A slide-in name plate in a satin aluminium holder: a name over a title, dark on white, as
   * on the end of a cubicle by its opening.
   */
  NAME_PLATE(new Layout(10.0, new Line(13.05, 1.3, Line.INK), new Line(11.7, 0.85, Line.INK)),
      null),
  /**
   * A larger cubicle sign in a black frame: the name, the title under it, and the department
   * reversed out of a dark band along the bottom.
   */
  SIGN(new Layout(12.0, new Line(12.6, 2.0, Line.INK), new Line(10.1, 1.3, Line.INK),
      new Line(7.5, 1.1, Line.REVERSED)),
      new Layout(23.0, new Line(12.6, 2.8, Line.INK), new Line(10.0, 1.7, Line.INK),
          new Line(7.5, 1.4, Line.REVERSED)));

  /** The insert's face, in sixteenths: the plate is on the north face, which is at -z. */
  public static final double FACE_Z = 6.5;
  /** How far in front of the insert the words are drawn, in sixteenths. */
  public static final double PRINT_LIFT = 0.04;
  /** Clear insert left at each end of a line, in sixteenths. */
  public static final double SIDE_MARGIN = 0.5;

  private final Layout layout;
  private final Layout wideLayout;

  CubicleSignStyle(Layout layout, Layout wideLayout) {
    this.layout = layout;
    this.wideLayout = wideLayout;
  }

  /**
   * How many lines it carries.
   *
   * @return the number of lines
   */
  public int getLineCount() {
    return layout.lines.length;
  }

  /**
   * Whether it is drawn wider where it has a full plain panel either side.
   *
   * @return true for the sign
   */
  public boolean canWiden() {
    return wideLayout != null;
  }

  /**
   * Its layout.
   *
   * @param wide whether it is drawn wide (ignored where it cannot be)
   *
   * @return the layout
   */
  public Layout getLayout(boolean wide) {
    return wide && wideLayout != null ? wideLayout : layout;
  }

  /** An insert's width and its lines. */
  public static final class Layout {

    private final double insertWidth;
    private final Line[] lines;

    Layout(double insertWidth, Line... lines) {
      this.insertWidth = insertWidth;
      this.lines = lines;
    }

    /**
     * One of its lines.
     *
     * @param index the line, from the top
     *
     * @return the line
     */
    public Line getLine(int index) {
      return lines[index];
    }

    /**
     * How many lines it has.
     *
     * @return the number of lines
     */
    public int getLineCount() {
      return lines.length;
    }

    /**
     * The widest a line of print may be, in sixteenths; longer text is condensed to fit.
     *
     * @return the width
     */
    public double getPrintWidth() {
      return insertWidth - 2 * SIDE_MARGIN;
    }
  }

  /** One printed line: where its middle is, how tall its capitals are, and its colour. */
  public static final class Line {

    /** Charcoal print on the white insert. */
    public static final int INK = 0x24272B;
    /** White print on the sign's dark band. */
    public static final int REVERSED = 0xF2F2EE;

    private final double centreY;
    private final double capHeight;
    private final int colour;

    Line(double centreY, double capHeight, int colour) {
      this.centreY = centreY;
      this.capHeight = capHeight;
      this.colour = colour;
    }

    /**
     * The height of the line's middle, in sixteenths.
     *
     * @return its middle
     */
    public double getCentreY() {
      return centreY;
    }

    /**
     * The height of its capitals, in sixteenths.
     *
     * @return the cap height
     */
    public double getCapHeight() {
      return capHeight;
    }

    /**
     * Its colour, as {@code 0xRRGGBB}.
     *
     * @return the colour
     */
    public int getColour() {
      return colour;
    }
  }
}
