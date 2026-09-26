package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A toilet partition: one stall's front in a commercial restroom's run of stalls, two blocks
 * tall and placed and broken as one ({@link BlockResidentialTall}). A run is a row of fronts
 * placed side by side, facing out of the stalls, in the row in front of the toilets: each front
 * stands across the middle of its block, so a stall is 1.5 m deep with its toilet in the block
 * behind, and a block wide.
 *
 * <p>The panel between two stalls stands on the line between two blocks and runs from the
 * front back to the wall behind the toilet, a block and a half; the front draws it, reaching
 * into the block behind (as the jet bridge and a tall piece's model do), and it collides there
 * too. {@link #LEFT} and {@link #RIGHT} (actual state, the viewer's standing behind the front,
 * facing {@link #FACING}) say which of the front's two sides carries one:</p>
 * <ul>
 *   <li>a side against a solid wall never does: the wall is the stall's side;</li>
 *   <li>a side where the run goes on (another partition, facing the same way) does on the left
 *   only, and only for a door or a panel: each stall is closed by its own door's left side, so
 *   one panel stands between two doors, and a pilaster continues the stall to its left, which
 *   is how a wide stall is made (a door with a pilaster on its right);</li>
 *   <li>an open side, where the run stops with nothing beside it, always does: the end panel.</li>
 * </ul>
 *
 * <p>Three kinds share this: the door (its own class, {@link BlockToiletPartitionDoor}), the
 * pilaster (a fixed front the width of the block, floor to headrail) and the panel (the
 * panels alone, each ending at a slim pilaster of its own: an open bay, or an end panel on its
 * own). {@link #UPPER} is stored in the bit above the facing, as for every tall piece.</p>
 *
 * @since 2026.9
 */
public class BlockToiletPartition extends BlockResidentialTall {

  /** What the front of a partition piece is. */
  public enum Kind {
    /** A stall door between two narrow pilasters ({@link BlockToiletPartitionDoor}). */
    DOOR,
    /** A fixed front panel from the floor to the headrail. */
    PILASTER,
    /** No front: the panels between stalls on their own, each ending in a slim pilaster. */
    PANEL
  }

  /** There is a panel on the left side (actual state). */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** There is a panel on the right side (actual state). */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  /** The whole piece's height, in sixteenths from the floor of the lower half. */
  protected static final double TOP = 30.5;
  /** The front, facing north, in sixteenths: across the block, floor to headrail. */
  protected static final double[] FRONT = {0, 0, 7.25, 16, TOP, 8.75};
  /** The panel on the left side: on the block line, from the front to the wall behind. */
  private static final double[] PANEL_LEFT = {-0.4, 5, 8.5, 0.4, 29, 31.5};
  /** A panel piece's slim pilaster at the front of its left panel. */
  private static final double[] POST_LEFT = {-1, 0, 7.25, 1, 29.75, 8.75};

  private final Kind kind;

  /**
   * Constructs a pilaster or a panel.
   *
   * @param registryName its registry name, ending in its finish
   * @param kind         which it is ({@link Kind#DOOR} is {@link BlockToiletPartitionDoor})
   */
  public BlockToiletPartition(String registryName, Kind kind) {
    super(registryName, new int[]{0, 0, 7, 16, 31, 9}, FixtureMaterial.METAL.getMaterial(),
        FixtureMaterial.METAL.getSound(), FixtureMaterial.METAL.getHardness());
    this.kind = kind;
    setDefaultState(getDefaultState().withProperty(LEFT, false).withProperty(RIGHT, false));
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
    return new CsmBlockStateContainer(this, FACING, UPPER, LEFT, RIGHT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    Beside left = beside(world, pos, facing, facing.rotateYCCW());
    Beside right = beside(world, pos, facing, facing.rotateY());
    boolean leftPanel = left == Beside.OPEN || (left == Beside.RUN && kind != Kind.PILASTER);
    return s.withProperty(LEFT, leftPanel).withProperty(RIGHT, right == Beside.OPEN);
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

  /**
   * The boxes of the whole piece, facing north, in sixteenths from the floor of the lower half
   * (so up to 32, and reaching into the block behind): the front and the panels its actual
   * state draws.
   *
   * @param actual the actual state
   *
   * @return the boxes, each {x0, y0, z0, x1, y1, z1}
   */
  protected List<double[]> parts(IBlockState actual) {
    List<double[]> out = new ArrayList<>();
    if (kind != Kind.PANEL) {
      out.add(FRONT);
    }
    if (actual.getValue(LEFT)) {
      out.add(PANEL_LEFT);
      if (kind == Kind.PANEL) {
        out.add(POST_LEFT);
      }
    }
    if (actual.getValue(RIGHT)) {
      out.add(mirror(PANEL_LEFT));
      if (kind == Kind.PANEL) {
        out.add(mirror(POST_LEFT));
      }
    }
    return out;
  }

  /** A box reflected from the left side to the right. */
  protected static double[] mirror(double[] b) {
    return new double[]{16 - b[3], b[1], b[2], 16 - b[0], b[4], b[5]};
  }

  /** This half's share of a box of the whole piece, in blocks, or null if it has none. */
  @Nullable
  private static AxisAlignedBB half(double[] b, boolean upper) {
    double lo = upper ? 16 : 0;
    double y0 = Math.max(b[1], lo);
    double y1 = Math.min(b[4], lo + 16);
    if (y1 <= y0) {
      return null;
    }
    return new AxisAlignedBB(b[0] / 16, (y0 - lo) / 16, b[2] / 16, b[3] / 16, (y1 - lo) / 16,
        b[5] / 16);
  }

  /**
   * The outline: this half's share of the parts, cut to its own block, so that it is the part
   * a click can reach.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    boolean upper = state.getValue(UPPER);
    AxisAlignedBB box = null;
    for (double[] part : parts(state)) {
      AxisAlignedBB b = half(part, upper);
      if (b == null) {
        continue;
      }
      b = new AxisAlignedBB(Math.max(b.minX, 0), b.minY, Math.max(b.minZ, 0),
          Math.min(b.maxX, 1), b.maxY, Math.min(b.maxZ, 1));
      box = box == null ? b : box.union(b);
    }
    return box != null ? box : new AxisAlignedBB(0, 0, 0.45, 1, 1, 0.55);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState actual = isActualState ? state : state.getActualState(world, pos);
    EnumFacing facing = actual.getValue(FACING);
    boolean upper = actual.getValue(UPPER);
    for (double[] part : parts(actual)) {
      AxisAlignedBB b = half(part, upper);
      if (b != null) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes,
            RotationUtils.rotateBoundingBoxByFacing(b, facing));
      }
    }
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
