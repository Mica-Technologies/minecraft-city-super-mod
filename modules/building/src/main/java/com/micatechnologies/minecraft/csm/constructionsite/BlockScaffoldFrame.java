package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

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
 * <p>Stored: the {@link #AXIS} the frames run across, and three add-ons fitted to a placed bay
 * with an item and taken off by sneak-right-clicking with an empty hand -- {@link #LADDER},
 * {@link #NETTED} and {@link #CASTERS}; see {@link ScaffoldAddon}. That is exactly the four bits
 * metadata has. The sides, up and down are actual state only, so nothing else about how a
 * scaffold looks is saved with it, and the look can be changed in the generator without
 * invalidating a placed block -- which was a requirement of the first build.</p>
 *
 * <h3>Guardrails</h3>
 *
 * <p>The top deck grows a guardrail -- posts, top rail, mid rail and toeboard -- on every edge
 * where you would fall, drawn up into the cell above with collision. An edge gets none where the
 * scaffold carries on, where the neighbour's top is solid so you would step out level, or where a
 * solid face stands at walking height, which is the building the scaffold is against. There is no
 * per-edge toggle, by choice.</p>
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
   * The axis the frames run across. Stored.
   *
   * @since 1.0
   */
  public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis",
      EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);

  /** Add-on: ladder frames in place of walk-through frames. Stored. */
  public static final PropertyBool LADDER = PropertyBool.create("ladder");

  /** Add-on: debris netting on every open face. Stored. */
  public static final PropertyBool NETTED = PropertyBool.create("netted");

  /** Add-on: casters in place of screw jacks. Stored. */
  public static final PropertyBool CASTERS = PropertyBool.create("casters");

  /** What is on each side. Actual state only. */
  public static final PropertyEnum<ScaffoldSide> NORTH = PropertyEnum.create("north",
      ScaffoldSide.class);
  public static final PropertyEnum<ScaffoldSide> EAST = PropertyEnum.create("east",
      ScaffoldSide.class);
  public static final PropertyEnum<ScaffoldSide> SOUTH = PropertyEnum.create("south",
      ScaffoldSide.class);
  public static final PropertyEnum<ScaffoldSide> WEST = PropertyEnum.create("west",
      ScaffoldSide.class);

  /** Whether a scaffold is above or below. Actual state only. */
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /** Metadata: bit 0 the axis (set for x), then one bit per add-on. */
  private static final int AXIS_X_BIT = 1;
  private static final int LADDER_BIT = 1 << 1;
  private static final int NETTED_BIT = 1 << 2;
  private static final int CASTERS_BIT = 1 << 3;

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

  /**
   * Steel that comes down by hand. {@link Material#IRON} needs a pickaxe to drop anything, so a
   * scaffold broken by hand dropped nothing -- neither the bay nor the netting and casters fitted
   * to it -- and dug at a third of the speed. Overriding {@code canHarvestBlock} fixes the drops
   * and not the speed, since Forge's dig-speed check reads the material directly. Scaffolding
   * comes down by hand on a real site; the pickaxe, its harvest tool, is still faster.
   */
  private static final Material SCAFFOLD_STEEL = new Material(MapColor.IRON);

  /** How far below the deck's top an entity's feet may be and still count as standing on it. */
  private static final double STANDING_TOLERANCE = 1.0E-3;

  /**
   * Constructs a {@link BlockScaffoldFrame}.
   *
   * @since 1.0
   */
  public BlockScaffoldFrame() {
    // Light opacity 0: a scaffold is open steel and planks, and shades nothing.
    super(SCAFFOLD_STEEL, SoundType.METAL, "pickaxe", 1, 1F, 6F, 0F, 0);
    setDefaultState(blockState.getBaseState()
        .withProperty(AXIS, EnumFacing.Axis.Z)
        .withProperty(LADDER, false)
        .withProperty(NETTED, false)
        .withProperty(CASTERS, false));
  }

  @Override
  public String getBlockRegistryName() {
    return "scaffold_frame";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, AXIS, LADDER, NETTED, CASTERS, NORTH, EAST, SOUTH, WEST,
        UP, DOWN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState()
        .withProperty(AXIS, (meta & AXIS_X_BIT) != 0 ? EnumFacing.Axis.X : EnumFacing.Axis.Z)
        .withProperty(LADDER, (meta & LADDER_BIT) != 0)
        .withProperty(NETTED, (meta & NETTED_BIT) != 0)
        .withProperty(CASTERS, (meta & CASTERS_BIT) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return (state.getValue(AXIS) == EnumFacing.Axis.X ? AXIS_X_BIT : 0)
        | (state.getValue(LADDER) ? LADDER_BIT : 0)
        | (state.getValue(NETTED) ? NETTED_BIT : 0)
        | (state.getValue(CASTERS) ? CASTERS_BIT : 0);
  }

  /**
   * Runs the frames across the player's line of sight, so a run laid along a wall while facing
   * it has its frames at right angles to the wall and its braces along it. A new bay has no
   * add-ons, whatever metadata the item carries.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    boolean deck = hasDeck(worldIn, pos);
    return state
        .withProperty(NORTH, side(worldIn, pos, EnumFacing.NORTH, deck))
        .withProperty(EAST, side(worldIn, pos, EnumFacing.EAST, deck))
        .withProperty(SOUTH, side(worldIn, pos, EnumFacing.SOUTH, deck))
        .withProperty(WEST, side(worldIn, pos, EnumFacing.WEST, deck))
        .withProperty(UP, !deck)
        .withProperty(DOWN, isScaffold(worldIn, pos.down()));
  }

  private static ScaffoldSide side(IBlockAccess world, BlockPos pos, EnumFacing side,
      boolean deck) {
    if (isScaffold(world, pos.offset(side))) {
      return ScaffoldSide.SCAFFOLD;
    }
    return deck && needsRail(world, pos, side) ? ScaffoldSide.RAIL : ScaffoldSide.OPEN;
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
   * Takes an add-on off: sneak and right-click with an empty hand. The first fitted, in
   * {@link ScaffoldAddon}'s order, comes off and its item goes back to the player.
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !playerIn.isSneaking()
        || !playerIn.getHeldItem(hand).isEmpty()) {
      return false;
    }
    for (ScaffoldAddon addon : ScaffoldAddon.values()) {
      if (state.getValue(addon.getProperty())) {
        if (!worldIn.isRemote) {
          worldIn.setBlockState(pos, state.withProperty(addon.getProperty(), false), 3);
          if (!playerIn.capabilities.isCreativeMode) {
            ItemHandlerHelper.giveItemToPlayer(playerIn, addon.toStack());
          }
          worldIn.playSound(null, pos, SoundEvents.BLOCK_METAL_BREAK, SoundCategory.BLOCKS,
              0.8F, 1.2F);
        }
        return true;
      }
    }
    return false;
  }

  /**
   * The bay, and every add-on fitted to it: breaking a netted bay gives the netting back.
   *
   * @since 1.0
   */
  @Override
  public void getDrops(@Nonnull NonNullList<ItemStack> drops, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull IBlockState state, int fortune) {
    super.getDrops(drops, world, pos, state, fortune);
    for (ScaffoldAddon addon : ScaffoldAddon.values()) {
      if (state.getValue(addon.getProperty())) {
        ItemStack stack = addon.toStack();
        if (!stack.isEmpty()) {
          drops.add(stack);
        }
      }
    }
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
   * The deck, and only to something standing on it; and the rails. See the class comment.
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
