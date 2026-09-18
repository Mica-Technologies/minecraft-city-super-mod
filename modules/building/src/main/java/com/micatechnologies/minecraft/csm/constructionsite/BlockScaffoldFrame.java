package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A bay of frame scaffolding that draws itself from its neighbours.
 *
 * <p>A real frame scaffold is end frames, cross braces and planks, but a cell holds one block, so
 * each block is one bay and decides its own parts: an end frame on its near side always and on its
 * far side only where the run ends, a cross brace on each open long face, a plank deck on top only
 * when nothing is stacked on it, and screw jacks where it stands on something that is not
 * scaffold. The parts and their geometry live in {@code dev-env-utils/scripts/gen_scaffold.py}.</p>
 *
 * <h3>State</h3>
 *
 * <p>Only {@link #FACING} is stored; it says which way the frames run. The six connections are
 * actual-state only. Nothing about how the scaffold looks is saved with a placed block, so the
 * look can be changed in the generator without invalidating a single one -- which was a
 * requirement of the first build.</p>
 *
 * <h3>Guardrails</h3>
 *
 * <p>The top deck grows a guardrail -- posts, top rail, mid rail and toeboard -- on every edge
 * where you would fall, drawn up into the cell above with collision. An edge gets none where the
 * scaffold carries on, where the neighbour's top is solid so you would step out level, or where a
 * solid face stands at walking height, which is the building the scaffold is against. Like the
 * rest, rails are actual state and nothing is stored: there is no per-edge toggle, by choice, so
 * the scaffold keeps its promise that its look can change without touching a placed block.</p>
 *
 * <h3>Climbing and the deck</h3>
 *
 * <p>The whole block is a ladder. Apart from the rails, its only collision is the deck, and the
 * deck is solid only to something standing on it and not sneaking: climbing up through a stack
 * never meets a plank overhead, and sneaking on the top deck drops the player back inside to
 * climb down. This is the behaviour vanilla scaffolding has in later versions, which 1.12
 * lacks.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockScaffoldFrame extends AbstractBlock {

  /**
   * Which way the frames run. Only the axis matters.
   *
   * @since 1.0
   */
  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  /** Whether a scaffold is on that side. Actual state only. */
  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /** Whether the top deck has a guardrail on that side. Actual state only. */
  public static final PropertyBool RAIL_NORTH = PropertyBool.create("rail_north");
  public static final PropertyBool RAIL_EAST = PropertyBool.create("rail_east");
  public static final PropertyBool RAIL_SOUTH = PropertyBool.create("rail_south");
  public static final PropertyBool RAIL_WEST = PropertyBool.create("rail_west");

  /** The planks: the top of the cell, matching the deck model. */
  private static final AxisAlignedBB DECK_BOX = new AxisAlignedBB(0.0, 15.0 / 16.0, 0.0,
      1.0, 1.0, 1.0);

  /**
   * The guardrails' collision, one per side, standing on the deck. Thin, and on the edge, so a
   * player climbing up the middle of the bay never touches one. A block and a half tall, as a
   * fence is, because a player can jump a block and a quarter; see {@link ScaffoldRailCollision}
   * for why that alone is not enough.
   */
  private static final double RAIL_TOP = 2.5;
  private static final AxisAlignedBB RAIL_NORTH_BOX = new AxisAlignedBB(0.0, 1.0, 0.0,
      1.0, RAIL_TOP, 2.0 / 16.0);
  private static final AxisAlignedBB RAIL_SOUTH_BOX = new AxisAlignedBB(0.0, 1.0, 14.0 / 16.0,
      1.0, RAIL_TOP, 1.0);
  private static final AxisAlignedBB RAIL_WEST_BOX = new AxisAlignedBB(0.0, 1.0, 0.0,
      2.0 / 16.0, RAIL_TOP, 1.0);
  private static final AxisAlignedBB RAIL_EAST_BOX = new AxisAlignedBB(14.0 / 16.0, 1.0, 0.0,
      1.0, RAIL_TOP, 1.0);

  /** How far below the deck's top an entity's feet may be and still count as standing on it. */
  private static final double STANDING_TOLERANCE = 1.0E-3;

  /**
   * Constructs a {@link BlockScaffoldFrame}.
   *
   * @since 1.0
   */
  public BlockScaffoldFrame() {
    // Light opacity 0: a scaffold is open steel and planks, and shades nothing.
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 1F, 6F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
  }

  @Override
  public String getBlockRegistryName() {
    return "scaffold_frame";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, NORTH, EAST, SOUTH, WEST, UP, DOWN,
        RAIL_NORTH, RAIL_EAST, RAIL_SOUTH, RAIL_WEST);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  /**
   * Faces the frames across the player's line of sight, so a run laid along a wall while facing
   * it has its frames at right angles to the wall and its braces along it.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    boolean deck = hasDeck(worldIn, pos);
    return state
        .withProperty(NORTH, isScaffold(worldIn, pos.north()))
        .withProperty(EAST, isScaffold(worldIn, pos.east()))
        .withProperty(SOUTH, isScaffold(worldIn, pos.south()))
        .withProperty(WEST, isScaffold(worldIn, pos.west()))
        .withProperty(UP, !deck)
        .withProperty(DOWN, isScaffold(worldIn, pos.down()))
        .withProperty(RAIL_NORTH, deck && needsRail(worldIn, pos, EnumFacing.NORTH))
        .withProperty(RAIL_EAST, deck && needsRail(worldIn, pos, EnumFacing.EAST))
        .withProperty(RAIL_SOUTH, deck && needsRail(worldIn, pos, EnumFacing.SOUTH))
        .withProperty(RAIL_WEST, deck && needsRail(worldIn, pos, EnumFacing.WEST));
  }

  /**
   * Whether the deck edge on {@code side} is a drop: the scaffold does not carry on, the
   * neighbour's top is not something to step out onto level, and nothing solid stands against
   * the edge at walking height.
   *
   * @since 1.0
   */
  private static boolean needsRail(IBlockAccess world, BlockPos pos, EnumFacing side) {
    BlockPos beside = pos.offset(side);
    if (isScaffold(world, beside)) {
      return false;
    }
    IBlockState besideState = world.getBlockState(beside);
    if (besideState.getBlockFaceShape(world, beside, EnumFacing.UP) == BlockFaceShape.SOLID) {
      return false;
    }
    BlockPos against = beside.up();
    IBlockState againstState = world.getBlockState(against);
    return againstState.getBlockFaceShape(world, against, side.getOpposite())
        != BlockFaceShape.SOLID;
  }

  private static boolean isScaffold(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockScaffoldFrame;
  }

  /**
   * Whether this bay carries a deck: only the top of a stack does.
   *
   * @since 1.0
   */
  private static boolean hasDeck(IBlockAccess world, BlockPos pos) {
    return !isScaffold(world, pos.up());
  }

  /**
   * The whole bay climbs like a ladder, from any side.
   *
   * @since 1.0
   */
  @Override
  public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos,
      EntityLivingBase entity) {
    return true;
  }

  /**
   * The deck, and only to something standing on it. See the class comment.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    if (!hasDeck(worldIn, pos)) {
      return;
    }
    // The rails stop everything, whatever it is doing: they are what keeps a sneaking player
    // from walking off the edge as much as anyone else.
    addRailBoxes(worldIn, pos, entityBox, collidingBoxes);
    if (entityIn != null) {
      boolean above = entityIn.getEntityBoundingBox().minY
          >= pos.getY() + DECK_BOX.maxY - STANDING_TOLERANCE;
      if (!above || entityIn.isSneaking()) {
        return;
      }
    }
    addCollisionBoxToList(pos, entityBox, collidingBoxes, DECK_BOX);
  }

  /**
   * Adds this bay's guardrail boxes that intersect {@code entityBox}, if it has any.
   *
   * @param world          the world
   * @param pos            this scaffold's position
   * @param entityBox      the box being collided
   * @param collidingBoxes where the boxes go
   *
   * @since 1.0
   */
  static void addRailBoxes(IBlockAccess world, BlockPos pos, AxisAlignedBB entityBox,
      List<AxisAlignedBB> collidingBoxes) {
    if (!hasDeck(world, pos)) {
      return;
    }
    for (EnumFacing side : EnumFacing.HORIZONTALS) {
      if (needsRail(world, pos, side)) {
        AxisAlignedBB box = railBox(side).offset(pos);
        if (entityBox.intersects(box)) {
          collidingBoxes.add(box);
        }
      }
    }
  }

  private static AxisAlignedBB railBox(EnumFacing side) {
    switch (side) {
      case NORTH:
        return RAIL_NORTH_BOX;
      case SOUTH:
        return RAIL_SOUTH_BOX;
      case WEST:
        return RAIL_WEST_BOX;
      default:
        return RAIL_EAST_BOX;
    }
  }

  @Nullable
  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return hasDeck(source, pos) ? DECK_BOX : NULL_AABB;
  }

  /**
   * Nothing attaches to a scaffold as if it were a solid face: no torch on a brace, no snow on
   * the planks.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
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
