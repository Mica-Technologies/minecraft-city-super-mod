package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A toilet partition cell: one block of a commercial restroom's run of stalls. A run is a row
 * of fronts placed side by side, facing out of the stalls, in the row in front of the toilets:
 * each front stands at the outer edge of its block, so a stall is the toilet's block and a clear
 * block to stand in, and a block wide.
 *
 * <p>Pilasters and panels are one-block cells that stack to any height: the bottom cell of a
 * column stands on its shoes and the top one carries the headrail ({@link #BOTTOM} and
 * {@link #TOP}, actual state), and a cell between them runs the whole block, so a stack joins
 * without a seam. The door ({@link BlockToiletPartitionDoor}) is two blocks tall; a cell stacked
 * on it makes a floor-to-ceiling stall, the door's headrail becoming the transom's bottom rail.
 * A column built with the old two-block pieces is two cells already: the upper half keeps its
 * old {@link #UPPER} bit, which nothing reads.</p>
 *
 * <p>The panel between two stalls stands on the line between two blocks. A cell draws it through
 * its own block and, when the block behind holds no partition piece and no wall ({@link #BACK}),
 * on through that block too, the toilet's, to the wall behind it. Panel cells placed in the
 * blocks behind a front, facing the same way, make the stall deeper: each carries the stall's
 * sides through its own block ({@link Front#NONE}), and the last carries them on into the
 * toilet's. A stall with no panel cells is the old two-deep stall exactly. Every panel is drawn
 * by a block within one block of it, which is as far as 1.12's collision query looks.</p>
 *
 * <p>The collision boxes are the drawn parts and no thicker: the fronts and pilasters only as
 * deep as their panels, not their shoes and headrail, so a player turns round in the stall
 * freely. {@link #LEFT} and {@link #RIGHT} (actual state, the viewer standing behind the front,
 * facing {@link #FACING}) say which of a cell's two sides carries a panel, at every depth and
 * height:</p>
 * <ul>
 *   <li>a side against a solid wall never does: the wall is the stall's side;</li>
 *   <li>a side where the run goes on (another partition, facing the same way) does on the left
 *   only, and only in a stall fronted by a door or a panel: each stall is closed by its own
 *   door's left side, so one panel stands between two doors, and a pilaster continues the stall
 *   to its left, which is how a wide stall is made (a door with a pilaster on its right). What
 *   fronts a cell's stall is found by walking forward through panel cells to the front, and down
 *   the front's column to its foot;</li>
 *   <li>an open side, where the run stops with nothing beside it, does: the end panel. A right
 *   side does not where the next stall's front, beside this cell's own front, already carries
 *   its left panel through that block (a deeper stall beside a shallower one).</li>
 * </ul>
 *
 * <p>Three kinds share this: the door, the pilaster (a fixed front the width of the block) and
 * the panel (no front: an open bay or an end panel, each panel ending at a slim pilaster of its
 * own; or, stacked over a door or pilaster, the fixed front; or, behind a front, the stall's
 * sides, up through the empty block above too where the front stands taller ({@link
 * Front#REACH})). A pilaster or panel stores its facing and, unused, the old upper half's bit
 * ({@link #UPPER}).</p>
 *
 * @since 2026.9
 */
public class BlockToiletPartition extends BlockResidentialFurniture {

  /** What the front of a partition piece is. */
  public enum Kind {
    /** A stall door between two narrow pilasters ({@link BlockToiletPartitionDoor}). */
    DOOR,
    /** A fixed front panel from the floor to the headrail. */
    PILASTER,
    /** No front of its own: the panels between stalls, each ending in a slim pilaster. */
    PANEL
  }

  /** What a panel cell draws across its front (actual state). */
  public enum Front implements IStringSerializable {
    /** Nothing: it stands behind a front, carrying its stall's sides back. */
    NONE("none"),
    /**
     * Nothing, as {@link #NONE}, and its sides go on up through the empty block above it to
     * the headrail's height, where the front beside it stands taller: so a panel cell placed on
     * the floor behind a door carries the whole height of the stall's sides.
     */
    REACH("reach"),
    /** A slim pilaster at the front of each side panel: an open bay, or an end panel. */
    POST("post"),
    /** The fixed front, as a pilaster: a panel cell stacked over a door or a pilaster. */
    FIXED("fixed");

    private final String name;

    Front(String name) {
      this.name = name;
    }

    @Override
    @Nonnull
    public String getName() {
      return name;
    }
  }

  /**
   * Set in the upper half of a pilaster or panel placed before the cells, two blocks tall
   * then; stored, kept and otherwise ignored (see {@link #getStateFromMeta}). A door's halves
   * still use it.
   */
  public static final PropertyBool UPPER = BlockResidentialTall.UPPER;
  /** There is a panel on the left side (actual state). */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** There is a panel on the right side (actual state). */
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  /** The side panels run on through the block behind (actual state). */
  public static final PropertyBool BACK = PropertyBool.create("back");
  /** The top of its column: it carries the headrail (actual state). */
  public static final PropertyBool TOP = PropertyBool.create("top");
  /** The foot of its column: it stands on shoes (actual state). */
  public static final PropertyBool BOTTOM = PropertyBool.create("bottom");
  /** A panel cell's front (actual state). */
  public static final PropertyEnum<Front> FRONT = PropertyEnum.create("front", Front.class);

  /** How far a walk through a column or a stall's depth looks before giving up. */
  private static final int WALK = 16;

  // Heights in a cell, in sixteenths: the panels' bottom in a bottom cell, the panels' and
  // pilasters' top in a top cell, the headrail's top, a slim pilaster's cap's top.
  private static final double PANEL_BOTTOM = 5;
  private static final double PANEL_TOP = 13;
  private static final double RAIL_TOP = 14.5;
  private static final double CAP_TOP = 13.75;

  private static final ThreadLocal<Kind> PENDING_KIND = new ThreadLocal<>();

  private final Kind kind;

  /**
   * Constructs a pilaster or a panel cell.
   *
   * @param registryName its registry name, ending in its finish
   * @param kind         which it is ({@link Kind#DOOR} is {@link BlockToiletPartitionDoor})
   */
  public BlockToiletPartition(String registryName, Kind kind) {
    super(stashKind(registryName, kind), new int[]{0, 0, 0, 16, 16, 2},
        FixtureMaterial.METAL.getMaterial(), FixtureMaterial.METAL.getSound(),
        FixtureMaterial.METAL.getHardness());
    PENDING_KIND.remove();
    this.kind = kind;
  }

  /** Holds the kind for {@link #createBlockState}, which runs before the constructor's body. */
  private static String stashKind(String registryName, Kind kind) {
    PENDING_KIND.set(kind);
    return registryName;
  }

  /**
   * Which kind of partition piece this is.
   *
   * @return its kind
   */
  public Kind getKind() {
    return kind;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    List<IProperty<?>> props = new ArrayList<>();
    props.add(FACING);
    props.add(UPPER);
    props.add(LEFT);
    props.add(RIGHT);
    props.add(BACK);
    props.add(TOP);
    props.add(BOTTOM);
    if (PENDING_KIND.get() == Kind.PANEL) {
      props.add(FRONT);
    }
    return new CsmBlockStateContainer(this, props.toArray(new IProperty<?>[0]));
  }

  /**
   * The facing, and the bit above it that an old two-block piece set in its upper half
   * ({@link #UPPER}). Nothing reads that bit any more, but it has to stay a state: a saved
   * block's meta is looked up among the metas the states give back, and one none gives back
   * loads as air.
   */
  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(UPPER, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(UPPER) ? 4 : 0);
  }

  /**
   * A cell placed on another partition piece, or against one, takes its facing, so a stack or
   * a run goes on the way it was started whichever way the player is looking.
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta,
        placer).withProperty(UPPER, false);
    for (BlockPos from : new BlockPos[]{pos.down(), pos.offset(facing.getOpposite())}) {
      IBlockState other = world.getBlockState(from);
      if (other.getBlock() instanceof BlockToiletPartition) {
        return state.withProperty(FACING, other.getValue(FACING));
      }
    }
    return state;
  }

  // --- actual state --------------------------------------------------------------------------

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    boolean behindStall = kind == Kind.PANEL && isPartition(world, pos.offset(facing), facing);
    boolean reach = behindStall && reachesUp(world, pos, facing);
    Kind governing = governingKind(world, pos, facing);
    s = withSides(s, world, pos, facing, governing)
        .withProperty(BACK, runsBack(world, pos, facing))
        .withProperty(TOP, !reach && !isPartition(world, pos.up(), facing))
        .withProperty(BOTTOM, !isPartition(world, pos.down(), facing));
    if (kind == Kind.PANEL) {
      Front front = behindStall ? (reach ? Front.REACH : Front.NONE)
          : governing == Kind.PANEL ? Front.POST : Front.FIXED;
      s = s.withProperty(FRONT, front);
    }
    return s;
  }

  /** Whether the block at pos is a partition piece facing the given way. */
  protected static boolean isPartition(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() instanceof BlockToiletPartition && state.getValue(FACING) == facing;
  }

  /** The kind of the partition piece at pos, which must be one. */
  private static Kind kindAt(IBlockAccess world, BlockPos pos) {
    return ((BlockToiletPartition) world.getBlockState(pos).getBlock()).getKind();
  }

  /**
   * What fronts the stall this cell belongs to: forward through the panel cells behind a front
   * to the front itself, then down the front's column to its foot (a cell stacked on a door
   * belongs to the door's stall).
   */
  protected static Kind governingKind(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    BlockPos at = pos;
    for (int i = 0; i < WALK && kindAt(world, at) == Kind.PANEL
        && isPartition(world, at.offset(facing), facing); i++) {
      at = at.offset(facing);
    }
    for (int i = 0; i < WALK && isPartition(world, at.down(), facing); i++) {
      at = at.down();
    }
    return kindAt(world, at);
  }

  /**
   * Whether a panel cell behind a front carries its sides up through the block above: that
   * block holds no partition piece and no full block, and the stall goes on up in the block
   * ahead of it, a partition piece there or the panel cell ahead reaching up into it.
   */
  private static boolean reachesUp(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    BlockPos at = pos;
    for (int i = 0; i < WALK; i++) {
      if (!clearAbove(world, at)) {
        return false;
      }
      BlockPos ahead = at.offset(facing);
      if (isPartition(world, ahead.up(), facing)) {
        return true;
      }
      if (!isPartition(world, ahead, facing) || kindAt(world, ahead) != Kind.PANEL
          || !isPartition(world, ahead.offset(facing), facing)) {
        return false;
      }
      at = ahead;
    }
    return false;
  }

  /** Whether the block above holds no partition piece and no full block. */
  @SuppressWarnings("deprecation")
  private static boolean clearAbove(IBlockAccess world, BlockPos pos) {
    IBlockState above = world.getBlockState(pos.up());
    return !(above.getBlock() instanceof BlockToiletPartition) && !above.isNormalCube();
  }

  /**
   * Whether the side panels run on through the block behind: it holds no partition piece (a
   * panel cell carries them on from there), no panel cell below it reaches up into it ({@link
   * Front#REACH}), and it is no solid wall (nothing there to draw into).
   */
  protected static boolean runsBack(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    BlockPos behind = pos.offset(facing.getOpposite());
    IBlockState state = world.getBlockState(behind);
    if (state.getBlock() instanceof BlockToiletPartition) {
      return false;
    }
    // A panel cell on the floor behind this cell's column reaches up into that block already.
    BlockPos below = behind.down();
    if (isPartition(world, below, facing) && kindAt(world, below) == Kind.PANEL
        && isPartition(world, below.offset(facing), facing)) {
      return false;
    }
    return state.getBlockFaceShape(world, behind, facing) != BlockFaceShape.SOLID;
  }

  /** The state with {@link #LEFT} and {@link #RIGHT} set by the rules in the class comment. */
  protected static IBlockState withSides(IBlockState s, IBlockAccess world, BlockPos pos,
      EnumFacing facing, Kind governing) {
    EnumFacing leftward = facing.rotateYCCW();
    EnumFacing rightward = facing.rotateY();
    Beside left = beside(world, pos, facing, leftward);
    Beside right = beside(world, pos, facing, rightward);
    boolean leftPanel = left == Beside.OPEN || (left == Beside.RUN && governing != Kind.PILASTER);
    boolean rightPanel = right == Beside.OPEN && !drawnFromBeside(world, pos, facing, rightward);
    return s.withProperty(LEFT, leftPanel).withProperty(RIGHT, rightPanel);
  }

  /**
   * Whether the block to the right is the block behind the next stall's own front, beside this
   * cell's front, whose left panel already runs through it on this line.
   */
  private static boolean drawnFromBeside(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing rightward) {
    BlockPos ahead = pos.offset(rightward).offset(facing);
    return isPartition(world, ahead, facing) && runsBack(world, ahead, facing)
        && governingKind(world, ahead, facing) != Kind.PILASTER;
  }

  /** What is beside a partition piece. */
  private enum Beside {
    /** Another partition piece facing the same way: the run goes on. */
    RUN,
    /** A solid wall. */
    WALL,
    /** Anything else: the run stops here. */
    OPEN
  }

  private static Beside beside(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    BlockPos other = pos.offset(side);
    IBlockState state = world.getBlockState(other);
    if (state.getBlock() instanceof BlockToiletPartition) {
      return state.getValue(FACING) == facing ? Beside.RUN : Beside.OPEN;
    }
    return state.getBlockFaceShape(world, other, side.getOpposite()) == BlockFaceShape.SOLID
        ? Beside.WALL : Beside.OPEN;
  }

  // --- parts -----------------------------------------------------------------------------

  /**
   * The boxes of this block, facing north, in sixteenths (reaching into the block behind where
   * the side panels run on): the front and the panels its actual state draws.
   *
   * @param actual the actual state
   * @param solid  true for the collision boxes, no thicker than the parts' panels; false for
   *               the click boxes, which take in their hardware
   *
   * @return the boxes, each {x0, y0, z0, x1, y1, z1}
   */
  protected List<double[]> parts(IBlockState actual, boolean solid) {
    List<double[]> out = new ArrayList<>();
    boolean bottom = actual.getValue(BOTTOM);
    boolean top = actual.getValue(TOP);
    Front front = kind == Kind.PANEL ? actual.getValue(FRONT) : Front.FIXED;
    if (front == Front.FIXED) {
      double y1 = top ? RAIL_TOP : 16;
      out.add(solid ? new double[]{0, 0, 0.5, 16, y1, 1.5} : new double[]{0, 0, 0.25, 16, y1, 1.75});
    }
    double[] post = null;
    if (front == Front.POST) {
      double y1 = top ? CAP_TOP : 16;
      post = solid ? new double[]{-0.75, 0, 0.5, 0.75, y1, 1.5}
          : new double[]{-1, 0, 0.25, 1, y1, 1.75};
    }
    boolean behind = front == Front.NONE || front == Front.REACH;
    double[] panel = sidePanel(bottom, top, behind ? 0 : 1.5, actual.getValue(BACK));
    if (front == Front.REACH) {
      panel[4] = 16 + PANEL_TOP;
    }
    addSides(out, actual, panel, post);
    return out;
  }

  /** The left side panel, from z0 to the block's back or on through the block behind. */
  protected static double[] sidePanel(boolean bottom, boolean top, double z0, boolean back) {
    return new double[]{-0.4, bottom ? PANEL_BOTTOM : 0, z0, 0.4, top ? PANEL_TOP : 16,
        back ? 31.5 : 16};
  }

  /** Adds the side panels, and with them the posts if any, that the actual state asks for. */
  protected static void addSides(List<double[]> out, IBlockState actual, double[] panel,
      @Nullable double[] post) {
    if (actual.getValue(LEFT)) {
      out.add(panel);
      if (post != null) {
        out.add(post);
      }
    }
    if (actual.getValue(RIGHT)) {
      out.add(mirror(panel));
      if (post != null) {
        out.add(mirror(post));
      }
    }
  }

  /** A box reflected from the left side to the right. */
  protected static double[] mirror(double[] b) {
    return new double[]{16 - b[3], b[1], b[2], 16 - b[0], b[4], b[5]};
  }

  /**
   * This block's boxes, in blocks, facing north: the parts cut to the block's height. A door
   * draws its parts as one two-block piece and cuts out its half.
   */
  protected List<AxisAlignedBB> boxes(IBlockState actual, boolean solid) {
    List<AxisAlignedBB> out = new ArrayList<>();
    for (double[] b : parts(actual, solid)) {
      out.add(new AxisAlignedBB(b[0] / 16, b[1] / 16, b[2] / 16, b[3] / 16, b[4] / 16,
          b[5] / 16));
    }
    return out;
  }

  /** The box a click finds on a cell that draws nothing: a panel cell between two walls. */
  private static final AxisAlignedBB EMPTY = new AxisAlignedBB(0, 0, 0, 1, 1 / 16.0, 1);

  /**
   * The outline: the parts cut to the block's own space, so that it is the part a click can
   * reach.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    AxisAlignedBB box = null;
    for (AxisAlignedBB b : boxes(state, false)) {
      b = new AxisAlignedBB(Math.max(b.minX, 0), b.minY, Math.max(b.minZ, 0),
          Math.min(b.maxX, 1), b.maxY, Math.min(b.maxZ, 1));
      box = box == null ? b : box.union(b);
    }
    return box != null ? box : EMPTY;
  }

  /**
   * A click finds the parts themselves, not the outline. The outline is the union of the parts
   * in this block, and with the door open, or on a panel cell, that union takes in the whole
   * back half of the block: a player standing in the stall is inside it, and every ray from
   * there would hit this block, so the toilet in front of them could not be clicked and the
   * click would work the door instead. The panel reaching into the block behind is found here
   * too, when the ray passes through this block first.
   */
  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public RayTraceResult collisionRayTrace(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end) {
    IBlockState actual = state.getActualState(world, pos);
    EnumFacing facing = actual.getValue(FACING);
    List<AxisAlignedBB> boxes = boxes(actual, false);
    if (boxes.isEmpty()) {
      boxes.add(EMPTY);
    }
    RayTraceResult best = null;
    double bestDistance = Double.MAX_VALUE;
    for (AxisAlignedBB b : boxes) {
      RayTraceResult hit = rayTrace(pos, start, end,
          RotationUtils.rotateBoundingBoxByFacing(b, facing));
      if (hit != null) {
        double distance = hit.hitVec.squareDistanceTo(start);
        if (distance < bestDistance) {
          best = hit;
          bestDistance = distance;
        }
      }
    }
    return best;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState actual = isActualState ? state : state.getActualState(world, pos);
    EnumFacing facing = actual.getValue(FACING);
    for (AxisAlignedBB b : boxes(actual, true)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(b, facing));
    }
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
