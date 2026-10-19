package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * What a hospital wayfinding panel ({@link BlockHospitalWayfindingPanel}) says and how its cells
 * make one panel: the pictograms, arrows, colour schemes and presets a sign can take, the rule its
 * text is cleaned by, and the walks that find a panel's controller, its size and each cell's place
 * in it.
 *
 * <p><b>A drop-in for Transit's large hanging sign.</b> The NBT keys, their meanings, the
 * metadata, the 16 x 6 limit and the rule that the bottom-left cell as read is the controller are
 * all {@code transit.wayfinding.WayfindingSign}'s, so a generator writing
 * {@code csm:airport_wayfinding_panel} writes this block by changing the name. The values differ
 * where a hospital needs other ones: the schemes are department colours (Transit's four are kept
 * too, with their colours), the pictograms are a hospital's (Transit's {@code restroom} and
 * {@code exit} keep their ids; its airport and transport ones read as none).</p>
 *
 * <p><b>Saved by id.</b> Every enum here is saved by its {@code id}, never its ordinal. An id
 * that is not known reads as the enum's first value.</p>
 *
 * @since 2026.10
 */
public final class HospitalWayfindingSign {

  /** The widest panel, in blocks (Transit's). */
  public static final int MAX_WIDTH = 16;
  /** The tallest panel, in blocks (Transit's). */
  public static final int MAX_HEIGHT = 6;
  /** Characters a line may hold. */
  public static final int MAX_LINE_LENGTH = 32;
  /** How far above a panel a rod reaches for something to hang it from, in blocks. */
  public static final int MAX_ROD_DROP = 24;
  /** The most cells one placement looks at when it works out the panels around it. */
  private static final int MAX_FLOOD = 512;

  private HospitalWayfindingSign() {
  }

  /** The pictogram at the end of the sign away from its arrow. */
  public enum Pictogram {
    NONE("none"),
    EMERGENCY("emergency"),
    XRAY("xray"),
    LAB("lab"),
    PHARMACY("pharmacy"),
    MATERNITY("maternity"),
    ELEVATOR("elevator"),
    STAIRS("stairs"),
    RESTROOM("restroom"),
    CAFE("cafe"),
    CHAPEL("chapel"),
    PARKING("parking"),
    INFO("info"),
    ACCESSIBLE("accessible"),
    EXIT("exit");

    private final String id;

    Pictogram(String id) {
      this.id = id;
    }

    /** Its saved id, and the name of its cell on the sheet. */
    public String getId() {
      return id;
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
   * Which way the arrow points, as the reader of the front sees it (Transit's ids and angles).
   * The arrow is drawn at the end it points to: the left end for the three leftward ones, the
   * right end otherwise.
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

    /** The arrow the back shows: left and right swapped, pointing the same way in the world. */
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

  /**
   * The sign's colours: its background, its legend and arrow, and its pictogram's square. The
   * hospital's department colours first; Transit's four last, with Transit's colours, so a sign
   * written for the airport panel looks the same here.
   */
  public enum Scheme {
    NEUTRAL("neutral", 0x1C2A44, 0xF2F4F8, 0xF2F4F8),
    EMERGENCY("emergency", 0xB3201C, 0xFFFFFF, 0xFFFFFF),
    IMAGING("imaging", 0x1F5DAA, 0xFFFFFF, 0xFFFFFF),
    SURGERY("surgery", 0x1E7A44, 0xFFFFFF, 0xFFFFFF),
    MATERNITY("maternity", 0xC2477E, 0xFFFFFF, 0xFFFFFF),
    OUTPATIENT("outpatient", 0x13807C, 0xFFFFFF, 0xFFFFFF),
    INPATIENT("inpatient", 0x5B3C8C, 0xFFFFFF, 0xFFFFFF),
    LABORATORY("laboratory", 0xD9701E, 0xFFFFFF, 0xFFFFFF),
    ADMIN("admin", 0x5E6670, 0xFFFFFF, 0xFFFFFF),
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

    public static Scheme byId(String id) {
      for (Scheme s : values()) {
        if (s.id.equals(id)) {
          return s;
        }
      }
      return NEUTRAL;
    }
  }

  /**
   * A whole sign at a click (a click on any cell steps through these): its lines, pictogram and
   * colours. The arrow is left as it was, since which way a sign points depends on where it
   * hangs; a sneak-click steps the arrow.
   */
  public enum Preset {
    INFORMATION("INFORMATION", "", Pictogram.INFO, Scheme.NEUTRAL),
    EMERGENCY("EMERGENCY", "", Pictogram.EMERGENCY, Scheme.EMERGENCY),
    IMAGING("IMAGING", "X-RAY  CT  MRI", Pictogram.XRAY, Scheme.IMAGING),
    SURGERY("SURGERY", "", Pictogram.NONE, Scheme.SURGERY),
    MATERNITY("MATERNITY", "", Pictogram.MATERNITY, Scheme.MATERNITY),
    OUTPATIENT("OUTPATIENT", "CLINICS", Pictogram.NONE, Scheme.OUTPATIENT),
    WARDS("WARDS", "", Pictogram.NONE, Scheme.INPATIENT),
    LABORATORY("LABORATORY", "", Pictogram.LAB, Scheme.LABORATORY),
    PHARMACY("PHARMACY", "", Pictogram.PHARMACY, Scheme.NEUTRAL),
    ADMINISTRATION("ADMINISTRATION", "", Pictogram.NONE, Scheme.ADMIN),
    ELEVATORS("ELEVATORS", "", Pictogram.ELEVATOR, Scheme.NEUTRAL),
    STAIRS("STAIRS", "", Pictogram.STAIRS, Scheme.NEUTRAL),
    RESTROOMS("RESTROOMS", "", Pictogram.RESTROOM, Scheme.NEUTRAL),
    CAFE("CAFE", "", Pictogram.CAFE, Scheme.NEUTRAL),
    CHAPEL("CHAPEL", "", Pictogram.CHAPEL, Scheme.NEUTRAL),
    PARKING("PARKING", "", Pictogram.PARKING, Scheme.NEUTRAL),
    EXIT("EXIT", "", Pictogram.EXIT, Scheme.EXIT);

    private final String line1;
    private final String line2;
    private final Pictogram pictogram;
    private final Scheme scheme;

    Preset(String line1, String line2, Pictogram pictogram, Scheme scheme) {
      this.line1 = line1;
      this.line2 = line2;
      this.pictogram = pictogram;
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

    public Scheme getScheme() {
      return scheme;
    }

    /** The preset after the one a sign shows (the first when it shows none of them). */
    public static Preset after(String line1, String line2) {
      Preset[] all = values();
      for (int i = 0; i < all.length; i++) {
        if (all[i].line1.equals(line1) && all[i].line2.equals(line2)) {
          return all[(i + 1) % all.length];
        }
      }
      return all[0];
    }

    /** The legend as one line, for a status message. */
    public String getLabel() {
      return line2.isEmpty() ? line1 : line1 + " / " + line2;
    }
  }

  /**
   * What a line may hold: printable ASCII, at most {@link #MAX_LINE_LENGTH} characters, trimmed
   * (Transit's rule).
   *
   * @param text the text, may be null
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

  // --- the panel ---------------------------------------------------------------------------

  /** Whether the cell at {@code pos} is this block facing this way: part of the same panel. */
  public static boolean joins(IBlockAccess world, BlockPos pos, Block block, EnumFacing facing) {
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() == block
        && state.getValue(AbstractBlockRotatableNSEW.FACING) == facing;
  }

  /** The reader's left for a panel facing this way: they face the panel, so it is clockwise. */
  public static EnumFacing leftOf(EnumFacing facing) {
    return facing.rotateY();
  }

  /**
   * The controller of the panel a cell is in: down the cell's column as far as the panel goes,
   * then along that row to the reader's left.
   */
  public static BlockPos controllerOf(IBlockAccess world, BlockPos pos, Block block,
      EnumFacing facing) {
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

  /**
   * Writes a sign to every cell of the panel a cell is in (the controller's rectangle), each sent
   * to the clients. Server side: a {@code /blockdata} edit of any cell and a click both come here.
   */
  public static void applyToPanel(World world, BlockPos cell, String line1, String line2,
      Pictogram pictogram, Arrow arrow, Scheme scheme, boolean doubleSided) {
    IBlockState state = world.getBlockState(cell);
    if (!(state.getBlock() instanceof BlockHospitalWayfindingPanel)) {
      return;
    }
    Block block = state.getBlock();
    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEW.FACING);
    BlockPos controller = controllerOf(world, cell, block, facing);
    EnumFacing right = leftOf(facing).getOpposite();
    int width = widthFrom(world, controller, block, facing);
    int height = heightFrom(world, controller, block, facing);
    String l1 = clamp(line1);
    String l2 = clamp(line2);
    for (int col = 0; col < width; col++) {
      for (int row = 0; row < height; row++) {
        BlockPos at = controller.offset(right, col).up(row);
        TileEntity te = world.getTileEntity(at);
        if (te instanceof TileEntityHospitalWayfindingPanel) {
          TileEntityHospitalWayfindingPanel panel = (TileEntityHospitalWayfindingPanel) te;
          panel.setSign(l1, l2, pictogram, arrow, scheme, doubleSided);
          panel.markDirtyQueueSync(world, at);
        }
      }
    }
  }

  /**
   * Works out again every cell's place in the panels around {@code origin}, after a cell was
   * added or taken away there: each panel's size and each of its cells' column and row, and the
   * controller's sign on every cell of its rectangle (a cell outside it, on a ragged build, is
   * drawn plain). A cell whose place or sign changed is sent to the clients, which is what makes
   * the baked legend move with the panel's shape, even into a chunk section the change did not
   * touch. Server side, from the block's {@code onBlockAdded} and {@code breakBlock}.
   *
   * @param world  the world
   * @param origin where a cell was added or removed
   * @param block  the panel block
   * @param facing the facing of the cell added or removed
   */
  public static void refresh(World world, BlockPos origin, Block block, EnumFacing facing) {
    EnumFacing left = leftOf(facing);
    EnumFacing[] plane = {left, left.getOpposite(), EnumFacing.UP, EnumFacing.DOWN};
    List<BlockPos> seeds = new ArrayList<>();
    seeds.add(origin);
    for (EnumFacing f : plane) {
      seeds.add(origin.offset(f));
    }
    Set<BlockPos> seen = new HashSet<>();
    List<BlockPos> cells = new ArrayList<>();
    ArrayDeque<BlockPos> queue = new ArrayDeque<>();
    for (BlockPos s : seeds) {
      if (joins(world, s, block, facing) && seen.add(s)) {
        queue.add(s);
      }
    }
    while (!queue.isEmpty() && cells.size() < MAX_FLOOD) {
      BlockPos at = queue.poll();
      cells.add(at);
      for (EnumFacing f : plane) {
        BlockPos n = at.offset(f);
        if (!seen.contains(n) && world.isBlockLoaded(n) && joins(world, n, block, facing)) {
          seen.add(n);
          queue.add(n);
        }
      }
    }
    EnumFacing right = left.getOpposite();
    Map<BlockPos, int[]> sizes = new HashMap<>();
    for (BlockPos at : cells) {
      TileEntity te = world.getTileEntity(at);
      if (!(te instanceof TileEntityHospitalWayfindingPanel)) {
        continue;
      }
      TileEntityHospitalWayfindingPanel cell = (TileEntityHospitalWayfindingPanel) te;
      BlockPos controller = controllerOf(world, at, block, facing);
      int[] size = sizes.computeIfAbsent(controller, c -> new int[]{
          widthFrom(world, c, block, facing), heightFrom(world, c, block, facing)});
      int col = (at.getX() - controller.getX()) * right.getXOffset()
          + (at.getZ() - controller.getZ()) * right.getZOffset();
      int row = at.getY() - controller.getY();
      boolean inside = col >= 0 && row >= 0 && col < size[0] && row < size[1];
      boolean changed;
      if (inside) {
        changed = cell.setPlace(col, row, size[0], size[1]);
        TileEntity src = world.getTileEntity(controller);
        if (src instanceof TileEntityHospitalWayfindingPanel && src != cell) {
          changed |= cell.copySign((TileEntityHospitalWayfindingPanel) src);
        }
      } else {
        changed = cell.setPlace(0, 0, 0, 0);
      }
      if (changed) {
        // queued, not sent: a panel built in one tick refreshes its earlier cells with each one
        // added, and sent at once those reached a client ahead of the cells' block changes
        cell.markDirtyQueueSync(world, at);
      }
    }
  }

  /**
   * A cell placed fresh (no sign of its own yet) takes the sign of a cell it joins, so a panel
   * keeps its legend whichever cell ends up its controller (Transit does this for a cell placed
   * by hand; here any placement, a {@code /setblock} included, does it).
   */
  static void adoptNeighbourSign(World world, BlockPos pos, Block block, EnumFacing facing,
      TileEntityHospitalWayfindingPanel mine) {
    EnumFacing left = leftOf(facing);
    for (EnumFacing side : new EnumFacing[]{left.getOpposite(), EnumFacing.UP, left,
        EnumFacing.DOWN}) {
      BlockPos at = pos.offset(side);
      if (!joins(world, at, block, facing)) {
        continue;
      }
      TileEntity other = BlockUtils.getTileEntitySafe(world, at);
      if (other instanceof TileEntityHospitalWayfindingPanel
          && ((TileEntityHospitalWayfindingPanel) other).hasOwnSign()) {
        mine.copySign((TileEntityHospitalWayfindingPanel) other);
        return;
      }
    }
  }

  /**
   * The rods a top-row cell hangs: Transit's spacing (two, or one for about every four blocks of
   * a wider panel, evenly along the top), each from the top of the panel up through air to the
   * first block above; a rod with a block right on top, or nothing within
   * {@link #MAX_ROD_DROP}, is not drawn.
   *
   * @param world  the world (a chunk cache, on the client)
   * @param pos    the cell
   * @param col    its column
   * @param width  the panel's width
   * @return for each rod in this cell's column, its x from the panel's left edge (blocks) and its
   *     drop (blocks), in pairs; empty for none
   */
  public static float[] rodsOf(IBlockAccess world, BlockPos pos, int col, int width) {
    int n = Math.max(2, Math.round(width / 4f));
    float[] out = new float[0];
    for (int i = 0; i < n; i++) {
      float x = width * (i + 0.5f) / n;
      if (Math.min(width - 1, (int) x) != col) {
        continue;
      }
      int drop = 0;
      BlockPos at = pos.up();
      while (drop < MAX_ROD_DROP && world.isAirBlock(at)) {
        drop++;
        at = at.up();
      }
      if (drop > 0 && drop < MAX_ROD_DROP) {
        float[] grown = new float[out.length + 2];
        System.arraycopy(out, 0, grown, 0, out.length);
        grown[out.length] = x;
        grown[out.length + 1] = drop;
        out = grown;
      }
    }
    return out;
  }
}
