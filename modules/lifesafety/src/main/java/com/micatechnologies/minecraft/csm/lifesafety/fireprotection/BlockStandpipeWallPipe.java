package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A length of dry standpipe run along a wall or a bridge parapet, or a fitting in such a run.
 *
 * <p>The pipe's axis is set back toward the wall behind it (facing north, at z = 11 pixels, so a
 * main clears the wall by a pixel), and it joins the wall pipes above, below and to either side
 * of it that face the same way: a run climbs a pier, turns and runs along the parapet, and
 * elbows and tees form by themselves. Its front joins a free-standing pipe
 * ({@link BlockStandpipePipe}) standing in front of it, which is how a run leaves the wall.</p>
 *
 * <p>Which sides are joined is actual state, from the neighbours. {@link #LEFT} and {@link #RIGHT}
 * are as seen from the front, so facing north the left arm is the east one (+x). A plain pipe
 * joined to nothing stands upright, as the riser always did; {@link #JOINT} draws the cast
 * fitting wherever the run is not straight. Above or below, a solid face counts as a join: the
 * pipe runs into it and {@link #FLOOR} or {@link #CEILING} draws the collar plate where it goes
 * through, so a riser carries on from one storey to the next.</p>
 *
 * <p>A {@link Fitting} is the same block with its own body drawn at the joint (a valve, the
 * hose outlet, the air release valve, the inlet manifold); each gives up the side its body
 * occupies. The Standpipe Riser is this block, the red branch pipe, under its old registry name,
 * so risers already placed join the system.</p>
 *
 * @version 1.0
 */
public class BlockStandpipeWallPipe extends AbstractBlockRotatableNSEW {

  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");
  public static final PropertyBool LEFT = PropertyBool.create("left");
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  public static final PropertyBool FRONT = PropertyBool.create("front");
  public static final PropertyBool JOINT = PropertyBool.create("joint");
  /** Set where the pipe goes down through a floor, to draw the collar plate there. */
  public static final PropertyBool FLOOR = BlockStandpipePipe.FLOOR;
  /** Set where the pipe goes up through a ceiling. */
  public static final PropertyBool CEILING = BlockStandpipePipe.CEILING;

  /** The axis's distance from the front of the cell, facing north, in pixels. */
  public static final float AXIS_Z = 11.0F;

  /** What is drawn at the joint, and which sides that leaves free to join. */
  public enum Fitting {
    /** Plain pipe: joins on every side. */
    NONE(true, true, true),
    /** An in-line drain or check valve, its handwheel to the front. */
    VALVE(true, true, false),
    /** A tee with a capped hose valve on its outlet, to the front. */
    HOSE_OUTLET(true, true, false),
    /** An air release valve standing on the run: the top is taken. */
    AIR_VALVE(false, true, true),
    /** The fire department inlet manifold at the foot of a run: the bottom is taken. */
    INLET(true, false, false);

    private final boolean up;
    private final boolean down;
    private final boolean front;

    Fitting(boolean up, boolean down, boolean front) {
      this.up = up;
      this.down = down;
      this.front = front;
    }

    public boolean takesFront() {
      return front;
    }

    boolean takes(EnumFacing relative) {
      switch (relative) {
        case UP:
          return up;
        case DOWN:
          return down;
        case NORTH:
          return front;
        default:
          return true;
      }
    }
  }

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final float radius;
  private final Fitting fitting;
  /** The fitting's body, facing north, in blocks; {@code null} for plain pipe. */
  @Nullable
  private final AxisAlignedBB fittingBox;

  /**
   * @param registryName its registry name
   * @param radius       the pipe's radius, in pixels (4 for a main, 3 for a branch)
   * @param fitting      what is drawn at the joint
   * @param fittingBox   the fitting's body facing north, in blocks, or {@code null}
   */
  public BlockStandpipeWallPipe(String registryName, float radius, Fitting fitting,
      @Nullable AxisAlignedBB fittingBox) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 0, 2.0F, 6.0F, 0.0F, 0);
    this.registryName = registryName;
    this.radius = radius;
    this.fitting = fitting;
    this.fittingBox = fittingBox;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(UP, false).withProperty(DOWN, false).withProperty(LEFT, false)
        .withProperty(RIGHT, false).withProperty(FRONT, false).withProperty(JOINT, false)
        .withProperty(FLOOR, false).withProperty(CEILING, false));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  public Fitting getFitting() {
    return fitting;
  }

  /**
   * Whether the wall pipe at {@code pos}, facing {@code front}, joins its neighbour toward the
   * world direction {@code side}.
   */
  private boolean joins(IBlockAccess world, BlockPos pos, EnumFacing front, EnumFacing side) {
    EnumFacing relative = relative(front, side);
    if (!fitting.takes(relative)) {
      return false;
    }
    IBlockState other = world.getBlockState(pos.offset(side));
    if (side == front) {
      return other.getBlock() instanceof BlockStandpipePipe;
    }
    if (!(other.getBlock() instanceof BlockStandpipeWallPipe)
        || other.getValue(FACING) != front) {
      return false;
    }
    return ((BlockStandpipeWallPipe) other.getBlock()).fitting.takes(relative.getOpposite());
  }

  /**
   * A world direction as seen by a pipe facing {@code front}, in the frame of one facing north:
   * the front is north, the back south, and up and down stay as they are.
   */
  private static EnumFacing relative(EnumFacing front, EnumFacing side) {
    if (side.getAxis() == EnumFacing.Axis.Y) {
      return side;
    }
    EnumFacing out = EnumFacing.NORTH;
    for (EnumFacing f = front; f != side; f = f.rotateY()) {
      out = out.rotateY();
    }
    return out;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, UP, DOWN, LEFT, RIGHT, FRONT, JOINT, FLOOR,
        CEILING);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState base = super.getActualState(state, world, pos);
    EnumFacing front = base.getValue(FACING);
    boolean up = joins(world, pos, front, EnumFacing.UP);
    boolean down = joins(world, pos, front, EnumFacing.DOWN);
    boolean left = joins(world, pos, front, front.rotateY());
    boolean right = joins(world, pos, front, front.rotateYCCW());
    boolean out = joins(world, pos, front, front);
    boolean lone = !up && !down && !left && !right && !out;
    // Through a floor or ceiling: the arm runs into the solid face and gets a collar plate.
    boolean floor = fitting.takes(EnumFacing.DOWN)
        && BlockStandpipePipe.passesThrough(world, pos, EnumFacing.DOWN);
    boolean ceiling = fitting.takes(EnumFacing.UP)
        && BlockStandpipePipe.passesThrough(world, pos, EnumFacing.UP);
    up |= ceiling;
    down |= floor;
    if (fitting == Fitting.NONE && lone) {
      // Joined to no other pipe: upright, as the riser always stood, floor or no floor.
      up = true;
      down = true;
    }
    int count = (up ? 1 : 0) + (down ? 1 : 0) + (left ? 1 : 0) + (right ? 1 : 0) + (out ? 1 : 0);
    boolean straight = count == 2 && ((up && down) || (left && right));
    return base.withProperty(UP, up).withProperty(DOWN, down).withProperty(LEFT, left)
        .withProperty(RIGHT, right).withProperty(FRONT, out)
        .withProperty(JOINT, fitting == Fitting.NONE && !straight)
        .withProperty(FLOOR, floor).withProperty(CEILING, ceiling);
  }

  /** The pipe along each joined arm, the joint, and the fitting's body; facing north. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    double r = (radius + (state.getValue(JOINT) ? 0.8 : 0.0)) / 16.0;
    double z = AXIS_Z / 16.0;
    AxisAlignedBB pipe = new AxisAlignedBB(
        state.getValue(RIGHT) ? 0 : 0.5 - r,
        state.getValue(DOWN) ? 0 : 0.5 - r,
        state.getValue(FRONT) ? 0 : z - r,
        state.getValue(LEFT) ? 1 : 0.5 + r,
        state.getValue(UP) ? 1 : 0.5 + r,
        z + r);
    return fittingBox != null ? pipe.union(fittingBox) : pipe;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
