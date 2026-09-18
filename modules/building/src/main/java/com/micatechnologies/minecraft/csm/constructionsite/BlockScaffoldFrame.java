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
 * <h3>Climbing and the deck</h3>
 *
 * <p>The whole block is a ladder. Its only collision is the deck, and the deck is solid only to
 * something standing on it and not sneaking: climbing up through a stack never meets a plank
 * overhead, and sneaking on the top deck drops the player back inside to climb down. This is the
 * behaviour vanilla scaffolding has in later versions, which 1.12 lacks.</p>
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

  /** The planks: the top of the cell, matching the deck model. */
  private static final AxisAlignedBB DECK_BOX = new AxisAlignedBB(0.0, 15.0 / 16.0, 0.0,
      1.0, 1.0, 1.0);

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
    return new BlockStateContainer(this, FACING, NORTH, EAST, SOUTH, WEST, UP, DOWN);
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
    return state
        .withProperty(NORTH, isScaffold(worldIn, pos.north()))
        .withProperty(EAST, isScaffold(worldIn, pos.east()))
        .withProperty(SOUTH, isScaffold(worldIn, pos.south()))
        .withProperty(WEST, isScaffold(worldIn, pos.west()))
        .withProperty(UP, isScaffold(worldIn, pos.up()))
        .withProperty(DOWN, isScaffold(worldIn, pos.down()));
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
    if (entityIn != null) {
      boolean above = entityIn.getEntityBoundingBox().minY
          >= pos.getY() + DECK_BOX.maxY - STANDING_TOLERANCE;
      if (!above || entityIn.isSneaking()) {
        return;
      }
    }
    addCollisionBoxToList(pos, entityBox, collidingBoxes, DECK_BOX);
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
