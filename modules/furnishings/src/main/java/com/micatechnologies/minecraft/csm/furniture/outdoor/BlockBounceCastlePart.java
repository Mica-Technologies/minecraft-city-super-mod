package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
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
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * One of the sixteen invisible blocks round a {@link BlockBounceCastle}'s root that carry its
 * floor and walls: each knows only its place round the root ({@link #INDEX}, stored) and reads
 * the castle's facing from the root, so its boxes ({@link BounceCastleLayout#boxes}) turn with
 * the castle. The floor parts bounce; the ones above do not. Breaking any part breaks the castle,
 * which drops itself; a part whose root has gone removes itself. In the hidden tab.
 *
 * @since 2026.9
 */
public class BlockBounceCastlePart extends AbstractBlock implements IBouncy {

  /** Its place round the root: 0 to 7 on the floor, 8 to 15 above ({@link BounceCastleLayout}). */
  public static final PropertyInteger INDEX = PropertyInteger.create("index", 0,
      BounceCastleLayout.PARTS - 1);

  @Nullable
  private static BlockBounceCastlePart instance;

  /** Constructs the part block; there is one, shared by every castle. */
  public BlockBounceCastlePart() {
    super(Material.CLOTH, SoundType.CLOTH, "pickaxe", 0, 0.8F, 1.0F, 0.0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(INDEX, 0));
    instance = this;
  }

  /**
   * The part block, once it has been made.
   *
   * @return the block, or null before the hidden tab has run
   */
  @Nullable
  public static BlockBounceCastlePart get() {
    return instance;
  }

  @Override
  public String getBlockRegistryName() {
    return "bounce_castle_part";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, INDEX);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(INDEX, meta & 15);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(INDEX);
  }

  /** The castle's facing, from its root; null if the root is not there. */
  @Nullable
  private static EnumFacing castleFacing(IBlockAccess world, BlockPos pos, IBlockState state) {
    IBlockState root = world.getBlockState(BounceCastleLayout.rootOf(pos, state.getValue(INDEX)));
    return root.getBlock() instanceof BlockBounceCastle ? root.getValue(BlockBounceCastle.FACING)
        : null;
  }

  // --- shape -------------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    EnumFacing facing = castleFacing(source, pos, state);
    return facing == null ? FULL_BLOCK_AABB
        : BounceCastleLayout.outline(state.getValue(INDEX), facing);
  }

  @Nullable
  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    EnumFacing facing = castleFacing(source, pos, state);
    if (facing == null || BounceCastleLayout.boxes(state.getValue(INDEX), facing).isEmpty()) {
      return NULL_AABB;
    }
    return BounceCastleLayout.outline(state.getValue(INDEX), facing);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> boxes, @Nullable Entity entity, boolean isActualState) {
    EnumFacing facing = castleFacing(world, pos, state);
    if (facing == null) {
      return;
    }
    for (AxisAlignedBB box : BounceCastleLayout.boxes(state.getValue(INDEX), facing)) {
      addCollisionBoxToList(pos, entityBox, boxes, box);
    }
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

  /** Drawn by the root, not here. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
    return EnumBlockRenderType.INVISIBLE;
  }

  // --- bouncing ----------------------------------------------------------------------------

  @Override
  public boolean isBouncy(IBlockState state) {
    return state.getValue(INDEX) < 8;
  }

  @Override
  public void onFallenUpon(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Entity entity,
      float fallDistance) {
    if (isBouncy(world.getBlockState(pos))) {
      Bounce.fallenUpon(world, pos, entity, fallDistance);
    } else {
      super.onFallenUpon(world, pos, entity, fallDistance);
    }
  }

  @Override
  public void onLanded(@Nonnull World world, @Nonnull Entity entity) {
    BlockPos pos = new BlockPos(entity.posX, entity.posY - 0.2, entity.posZ);
    IBlockState state = world.getBlockState(pos);
    if (state.getBlock() == this && isBouncy(state)) {
      Bounce.landed(this, world, entity);
    } else {
      super.onLanded(world, entity);
    }
  }

  // --- one piece with the root -------------------------------------------------------------

  /** A creative player breaking a part takes the castle with it, dropping nothing. */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (player.capabilities.isCreativeMode) {
      BlockPos root = BounceCastleLayout.rootOf(pos, state.getValue(INDEX));
      if (world.getBlockState(root).getBlock() instanceof BlockBounceCastle) {
        world.setBlockToAir(root);
      }
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  /** Breaking a part breaks the castle, which drops itself and clears the other parts. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    BlockPos root = BounceCastleLayout.rootOf(pos, state.getValue(INDEX));
    if (world.getBlockState(root).getBlock() instanceof BlockBounceCastle) {
      world.destroyBlock(root, true);
    }
    super.breakBlock(world, pos, state);
  }

  /** A part left without its root (a command, an explosion) goes too. */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (!world.isRemote && castleFacing(world, pos, state) == null) {
      world.setBlockToAir(pos);
    }
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Items.AIR;
  }

  /** Picking a part picks the castle. */
  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    IBlockState root = world.getBlockState(BounceCastleLayout.rootOf(pos, state.getValue(INDEX)));
    return root.getBlock() instanceof BlockBounceCastle ? new ItemStack(root.getBlock())
        : ItemStack.EMPTY;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }
}
