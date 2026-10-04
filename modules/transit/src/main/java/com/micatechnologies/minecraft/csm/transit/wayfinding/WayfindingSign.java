package com.micatechnologies.minecraft.csm.transit.wayfinding;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * What a large hanging sign ({@link BlockWayfindingPanel}) says and how its cells make one panel:
 * the pictograms, arrows, colour schemes and presets a sign can take, the rule its text is
 * cleaned by, and the walks that find a panel's controller and its size.
 *
 * <p><b>Saved by id.</b> Every enum here is saved in a tile entity by its {@code id}, never its
 * ordinal, so the lists may be reordered or grown without changing a placed sign. An id that is
 * not known (a sign from a later version) reads as the enum's first value.</p>
 *
 * <p><b>The panel.</b> Cells of this block facing the same way, side by side and stacked, are one
 * panel. Its controller is the bottom-left cell as the reader sees it: it holds the sign and
 * draws it. The panel's width is how far the controller's row runs to the reader's right, its
 * height how far the controller's column runs up, at most {@link #MAX_WIDTH} by
 * {@link #MAX_HEIGHT}.</p>
 *
 * @since 2026.10
 */
public final class WayfindingSign {

  /** The widest panel, in blocks. */
  public static final int MAX_WIDTH = 16;
  /** The tallest panel, in blocks. */
  public static final int MAX_HEIGHT = 6;
  /** Characters a line may hold. */
  public static final int MAX_LINE_LENGTH = 32;
  /** How far above a panel the renderer looks for something to hang it from, in blocks. */
  public static final int MAX_ROD_DROP = 24;

  private WayfindingSign() {
  }

  /** The pictogram at the end of the sign away from its arrow. */
  public enum Pictogram {
    NONE("none"),
    DEPART("depart"),
    ARRIVE("arrive"),
    CHECKIN("checkin"),
    BAGGAGE("baggage"),
    GROUND("ground"),
    TRAIN("train"),
    BUS("bus"),
    TAXI("taxi"),
    RESTROOM("restroom"),
    EXIT("exit");

    private final String id;

    Pictogram(String id) {
      this.id = id;
    }

    /** Its saved id, its lang key's last part and, but for {@link #NONE}, its sprite's name. */
    public String getId() {
      return id;
    }

    /** The sprite the renderer draws, {@code null} for {@link #NONE}. */
    public String getSprite() {
      return this == NONE ? null : "csm:blocks/transit/airport/wayfinding_picto_" + id;
    }

    public Pictogram next() {
      return values()[(ordinal() + 1) % values().length];
    }

    public static Pictogram byId(String id) {
      for (Pictogram p : values()) {
        if (p.id.equals(id)) {
          return p;
        }
      }
      return NONE;
    }
  }

  /**
   * Which way the arrow points, as the reader of the front sees it. The arrow is drawn at the
   * end it points to: the left end for the three leftward ones, the right end otherwise.
   */
  public enum Arrow {
    NONE("none", 0),
    LEFT("left", 180),
    RIGHT("right", 0),
    UP("up", 90),
    DOWN("down", 270),
    UP_LEFT("up_left", 135),
    UP_RIGHT("up_right", 45),
    DOWN_LEFT("down_left", 225),
    DOWN_RIGHT("down_right", 315);

    private final String id;
    private final int angle;

    Arrow(String id, int angle) {
      this.id = id;
      this.angle = angle;
    }

    public String getId() {
      return id;
    }

    /** Degrees anticlockwise from pointing right, as the reader sees it. */
    public int getAngle() {
      return angle;
    }

    /** Whether the arrow sits at the reader's left end. */
    public boolean isLeftEnd() {
      return this == LEFT || this == UP_LEFT || this == DOWN_LEFT;
    }

    /**
     * The arrow the back of the sign shows: left and right swapped, so that it points the same
     * way in the world from either side.
     */
    public Arrow mirrored() {
      switch (this) {
        case LEFT:
          return RIGHT;
        case RIGHT:
          return LEFT;
        case UP_LEFT:
          return UP_RIGHT;
        case UP_RIGHT:
          return UP_LEFT;
        case DOWN_LEFT:
          return DOWN_RIGHT;
        case DOWN_RIGHT:
          return DOWN_LEFT;
        default:
          return this;
      }
    }

    public Arrow next() {
      return values()[(ordinal() + 1) % values().length];
    }

    public static Arrow byId(String id) {
      for (Arrow a : values()) {
        if (a.id.equals(id)) {
          return a;
        }
      }
      return NONE;
    }
  }

  /** The sign's colours: its background, its legend and arrow, and its pictogram's square. */
  public enum Scheme {
    AIRPORT("airport", 0x2A2C30, 0xFAC81E, 0xFAC81E),
    METRO("metro", 0x12204A, 0xF2F4F8, 0xF2F4F8),
    EXIT("exit", 0x0B6B3A, 0xF2F4F8, 0xF2F4F8),
    INFO("info", 0x1F5DAA, 0xF2F4F8, 0xF2F4F8);

    private final String id;
    private final int background;
    private final int legend;
    private final int pictogram;

    Scheme(String id, int background, int legend, int pictogram) {
      this.id = id;
      this.background = background;
      this.legend = legend;
      this.pictogram = pictogram;
    }

    public String getId() {
      return id;
    }

    public int getBackground() {
      return background;
    }

    public int getLegend() {
      return legend;
    }

    /** The colour the pictogram's white square is tinted. */
    public int getPictogram() {
      return pictogram;
    }

    public Scheme next() {
      return values()[(ordinal() + 1) % values().length];
    }

    public static Scheme byId(String id) {
      for (Scheme s : values()) {
        if (s.id.equals(id)) {
          return s;
        }
      }
      return AIRPORT;
    }
  }

  /**
   * A whole sign at a click: its lines, pictogram and colours, and its arrow where it has one
   * (otherwise the sign keeps the arrow it had, since which way a sign points depends on where it
   * hangs, not on what it says).
   */
  public enum Preset {
    GATES("GATES", "", Pictogram.DEPART, Arrow.RIGHT, Scheme.AIRPORT),
    ARRIVALS("ARRIVALS", "", Pictogram.ARRIVE, null, Scheme.AIRPORT),
    CHECK_IN("CHECK-IN", "", Pictogram.CHECKIN, null, Scheme.AIRPORT),
    BAGGAGE_CLAIM("BAGGAGE CLAIM", "", Pictogram.BAGGAGE, null, Scheme.AIRPORT),
    GROUND_TRANSPORTATION("GROUND", "TRANSPORTATION", Pictogram.GROUND, null, Scheme.AIRPORT),
    TO_TRAINS("TO TRAINS", "", Pictogram.TRAIN, null, Scheme.METRO),
    EXIT("EXIT", "", Pictogram.EXIT, null, Scheme.EXIT),
    RESTROOMS("RESTROOMS", "", Pictogram.RESTROOM, null, Scheme.INFO);

    private final String line1;
    private final String line2;
    private final Pictogram pictogram;
    private final Arrow arrow;
    private final Scheme scheme;

    Preset(String line1, String line2, Pictogram pictogram, Arrow arrow, Scheme scheme) {
      this.line1 = line1;
      this.line2 = line2;
      this.pictogram = pictogram;
      this.arrow = arrow;
      this.scheme = scheme;
    }

    public String getLine1() {
      return line1;
    }

    public String getLine2() {
      return line2;
    }

    public Pictogram getPictogram() {
      return pictogram;
    }

    /** The arrow the preset sets, or {@code null} to keep the sign's. */
    public Arrow getArrow() {
      return arrow;
    }

    public Scheme getScheme() {
      return scheme;
    }

    /** The legend as one line of text, for the editor's button. */
    public String getLabel() {
      return line2.isEmpty() ? line1 : line1 + " " + line2;
    }
  }

  /**
   * What a line may hold: printable ASCII (the font has nothing else), at most
   * {@link #MAX_LINE_LENGTH} characters, trimmed.
   *
   * @param text the text, may be null
   *
   * @return the cleaned text, never null
   */
  public static String clamp(String text) {
    if (text == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(Math.min(text.length(), MAX_LINE_LENGTH));
    for (int i = 0; i < text.length() && out.length() < MAX_LINE_LENGTH; i++) {
      char c = text.charAt(i);
      if (c >= 32 && c < 127) {
        out.append(c);
      }
    }
    return out.toString().trim();
  }

  /** Whether the cell at {@code pos} is this block facing this way: part of the same panel. */
  static boolean joins(IBlockAccess world, BlockPos pos, Block block, EnumFacing facing) {
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() == block
        && state.getValue(BlockWayfindingPanel.FACING) == facing;
  }

  /** The reader's left for a panel facing this way: they face the panel, so it is clockwise. */
  static EnumFacing leftOf(EnumFacing facing) {
    return facing.rotateY();
  }

  /**
   * The controller of the panel a cell is in: down the cell's column as far as the panel goes,
   * then along that row to the reader's left.
   *
   * @param world the world
   * @param pos   a cell of the panel
   * @param state its state
   *
   * @return the controller's position
   */
  public static BlockPos controllerOf(IBlockAccess world, BlockPos pos, IBlockState state) {
    Block block = state.getBlock();
    EnumFacing facing = state.getValue(BlockWayfindingPanel.FACING);
    EnumFacing left = leftOf(facing);
    BlockPos at = pos;
    for (int i = 0; i < MAX_HEIGHT * 4 && joins(world, at.down(), block, facing); i++) {
      at = at.down();
    }
    for (int i = 0; i < MAX_WIDTH * 4 && joins(world, at.offset(left), block, facing); i++) {
      at = at.offset(left);
    }
    return at;
  }

  /** How many cells the controller's row has, from the controller to the reader's right. */
  public static int widthFrom(IBlockAccess world, BlockPos controller, Block block,
      EnumFacing facing) {
    EnumFacing right = leftOf(facing).getOpposite();
    int width = 1;
    while (width < MAX_WIDTH && joins(world, controller.offset(right, width), block, facing)) {
      width++;
    }
    return width;
  }

  /** How many cells the controller's column has, from the controller up. */
  public static int heightFrom(IBlockAccess world, BlockPos controller, Block block,
      EnumFacing facing) {
    int height = 1;
    while (height < MAX_HEIGHT && joins(world, controller.up(height), block, facing)) {
      height++;
    }
    return height;
  }
}
