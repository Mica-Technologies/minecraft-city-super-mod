package com.micatechnologies.minecraft.csm.signage;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * What the two blocks of an advertising board share: the controller the player places and the
 * parts it builds around itself are one object, and every block of it knows which board it is in
 * from its neighbours alone.
 *
 * <p>Stored, in the four metadata bits: {@link #FACING}, the way the ad looks, and {@link #TAG},
 * a number from 0 to 3 that every block of one board shares and that no board touching it of the
 * same kind and facing does. That is how two boards built side by side stay two boards with two
 * frames, and how a block finds the rest of its board -- the controller included -- without a
 * tile entity of its own. {@link AdBoards} picks the tag when a board is built.</p>
 *
 * <p>Actual state: {@link #LEFT}, {@link #RIGHT}, {@link #UP} and {@link #DOWN}, whether the block
 * on that side, seen from the front, is the same board. The frame is drawn only where it is not,
 * so a board of any size has one frame round its outside. The models come from
 * {@code dev-env-utils/scripts/gen_ad_boards.py}.</p>
 *
 * <p>The ad itself is not here: the controller's renderer draws it across the whole board.</p>
 */
public abstract class AbstractBlockAdBoard extends AbstractBlock {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyInteger TAG = PropertyInteger.create("tag", 0, 3);
  public static final PropertyBool LEFT = PropertyBool.create("left");
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");
  /** A floodlight on this block's stretch of catwalk: one every third block along the board. */
  public static final PropertyBool LAMP = PropertyBool.create("lamp");

  /** The catwalk in front of a board's bottom row: its deck, and its railing to fall against. */
  private static final AxisAlignedBB CATWALK_DECK =
      new AxisAlignedBB(0, 0, 1, 1, 1.5 / 16.0, 30 / 16.0);
  private static final AxisAlignedBB CATWALK_RAIL =
      new AxisAlignedBB(0, 0, 29 / 16.0, 1, 1, 30 / 16.0);

  /** Sheet metal that comes down by hand, as signs do. */
  private static final Material BOARD = new Material(MapColor.IRON);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AdBoardKind kind;

  protected AbstractBlockAdBoard(String registryName) {
    super(pendingMaterial(registryName), SoundType.METAL, "pickaxe", 0, 1.5F, 10F, 0F, 0);
    this.registryName = registryName;
    this.kind = AdBoardKind.of(registryName);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH)
        .withProperty(TAG, 0));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return BOARD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  /**
   * The kind of board this block belongs to. Read from the pending name while the constructor is
   * still running: Block's own constructor asks whether the block is an opaque cube, which a
   * cabinet board is.
   */
  public AdBoardKind kind() {
    return kind != null ? kind : AdBoardKind.of(PENDING_REGISTRY_NAME.get());
  }

  /** The block the player places for this kind of board. */
  public static Block controller(AdBoardKind kind) {
    return CsmRegistry.getBlock(kind.getRegistryName());
  }

  /** The block a board of this kind is built out of. */
  public static Block part(AdBoardKind kind) {
    return CsmRegistry.getBlock(kind.getPartRegistryName());
  }

  // --- state --------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, TAG, LEFT, RIGHT, UP, DOWN, LAMP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(TAG, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(TAG) << 2);
  }

  /** The way to the board's right, seen from the front. */
  public static EnumFacing right(EnumFacing facing) {
    return facing.rotateYCCW();
  }

  /**
   * Whether {@code state} is a block of the board whose blocks are of kind {@code kind}, face
   * {@code facing} and carry {@code tag}: the controller or a part.
   */
  public static boolean sameBoard(IBlockState state, AdBoardKind kind, EnumFacing facing,
      int tag) {
    Block block = state.getBlock();
    return block instanceof AbstractBlockAdBoard && ((AbstractBlockAdBoard) block).kind == kind
        && state.getValue(FACING) == facing && state.getValue(TAG) == tag;
  }

  /** Whether the block at {@code other} is the same board as {@code state}. */
  public boolean sameBoard(IBlockAccess world, BlockPos other, IBlockState state) {
    return sameBoard(world.getBlockState(other), kind, state.getValue(FACING),
        state.getValue(TAG));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    EnumFacing right = right(state.getValue(FACING));
    boolean down = sameBoard(worldIn, pos.down(), state);
    // Where along the board a block is, counted in world blocks so it needs no controller: one
    // floodlight every third block of catwalk.
    int along = pos.getX() * right.getXOffset() + pos.getZ() * right.getZOffset();
    boolean lamp = kind().hasCatwalk() && !down && Math.floorMod(along, 3) == 1;
    return state.withProperty(LEFT, sameBoard(worldIn, pos.offset(right.getOpposite()), state))
        .withProperty(RIGHT, sameBoard(worldIn, pos.offset(right), state))
        .withProperty(UP, sameBoard(worldIn, pos.up(), state))
        .withProperty(DOWN, down)
        .withProperty(LAMP, lamp);
  }

  // --- breaking: one board, one object --------------------------------------------------------

  /** The player breaking a board, while they do, so the refund knows whom it is for. */
  private static final ThreadLocal<EntityPlayer> BREAKER = new ThreadLocal<>();

  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest) {
    BREAKER.set(player);
    try {
      return super.removedByPlayer(state, world, pos, player, willHarvest);
    } finally {
      BREAKER.remove();
    }
  }

  /**
   * Breaking any block of a board takes the whole board down: a board is one object, and half a
   * board would be a controller drawing its ad over nothing. Called on the server only.
   */
  @Override
  public void breakBlock(@Nonnull World worldIn, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    AdBoards.onBlockGone(worldIn, pos, state, BREAKER.get());
    super.breakBlock(worldIn, pos, state);
  }

  /** Nothing drops from a block: a survival player's refund is paid by {@link AdBoards}. */
  @Override
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Items.AIR;
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, @Nonnull RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
    return new ItemStack(controller(kind()));
  }

  /** A piston moving one block of a board would tear it; a board does not move. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  /** Any block of a board opens the board's screen. */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (worldIn.isRemote) {
      SignageGuiProvider.open(playerIn, worldIn, pos);
    }
    return true;
  }

  // --- shape --------------------------------------------------------------------------------

  /** A box drawn with the face to the south, turned to the facing. */
  static AxisAlignedBB turn(AxisAlignedBB box, EnumFacing facing) {
    switch (facing) {
      case NORTH:
        return new AxisAlignedBB(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY,
            1 - box.minZ);
      case WEST:
        return new AxisAlignedBB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY,
            box.maxX);
      case EAST:
        return new AxisAlignedBB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY,
            1 - box.minX);
      default:
        return box;
    }
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return turn(new AxisAlignedBB(0, 0, 0, 1, 1, kind().getDepthPx() / 16.0),
        state.getValue(FACING));
  }

  /**
   * The board, and on the bottom row of a board with a catwalk, the catwalk in front of it: its
   * deck to stand on and its railing. Both are outside the block's own cell, which is fine for
   * collision -- entities gather boxes from the blocks around them.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    EnumFacing facing = state.getValue(FACING);
    addCollisionBoxToList(pos, entityBox, collidingBoxes,
        getBlockBoundingBox(state, worldIn, pos));
    if (kind().hasCatwalk() && !sameBoard(worldIn, pos.down(), state)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, turn(CATWALK_DECK, facing));
      addCollisionBoxToList(pos, entityBox, collidingBoxes, turn(CATWALK_RAIL, facing));
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  /**
   * A cabinet board is a solid box, so the faces between its blocks are culled; a wall board is
   * a plate.
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return kind().isCabinet();
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return kind().isCabinet();
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
