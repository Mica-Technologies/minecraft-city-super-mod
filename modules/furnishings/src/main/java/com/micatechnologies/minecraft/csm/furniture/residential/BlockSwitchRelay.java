package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The hidden relay a linked light switch puts beside the block it is linked to while it is on
 * ({@link SwitchLinks}). It powers that block -- and only that block -- as a lever powers the
 * block it is on: weak power into it, and strong power, so a solid block linked to passes it on.
 * That is how a switch reaches any block, a vanilla redstone lamp or door as much as this
 * module's lamps: they all read power from their neighbours.
 *
 * <p>It is invisible and has no box: a player walks and clicks through it and cannot break it,
 * but can place a block into its cell, which ends the power (the switch finds another cell when
 * it is next switched on). It goes by itself when the block it powers is broken, or when a
 * neighbour changes and the switch that placed it is gone, off, or linked elsewhere. It drops
 * nothing and never ticks. {@link #FACING}, stored, is the way from the powered block to it.</p>
 *
 * @since 2026.9
 */
public class BlockSwitchRelay extends AbstractBlock implements ICsmTileEntityProvider {

  /** The way from the block it powers to the relay. */
  public static final PropertyDirection FACING = PropertyDirection.create("facing");

  /** No box at all: nothing collides with it or clicks on it. */
  private static final AxisAlignedBB NO_BOX = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

  /**
   * Constructs the relay.
   */
  public BlockSwitchRelay() {
    super(Material.CIRCUITS, SoundType.STONE, "pickaxe", 0, -1.0F, 18.0F, 0.0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
  }

  @Override
  public String getBlockRegistryName() {
    return "switch_relay";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getIndex();
  }

  // --- power, toward the block it serves only ----------------------------------------------

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(@Nonnull IBlockState state) {
    return true;
  }

  /**
   * A block asks its neighbour on its side {@code side} for power: the relay answers only the
   * block it was put beside, which asks with {@code side} equal to the relay's {@link #FACING}.
   */
  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    return side == state.getValue(FACING) ? 15 : 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getStrongPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    return side == state.getValue(FACING) ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  /**
   * Goes if the block it powers has gone, or if the switch that placed it is loaded and no
   * longer on and linked through it.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (world.isRemote) {
      return;
    }
    if (world.isAirBlock(pos.offset(state.getValue(FACING).getOpposite())) || orphaned(world,
        pos)) {
      world.setBlockToAir(pos);
    }
  }

  private static boolean orphaned(World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    BlockPos switchPos = te instanceof TileEntitySwitchRelay
        ? ((TileEntitySwitchRelay) te).getSwitch() : null;
    if (switchPos == null) {
      return true;
    }
    if (!world.isBlockLoaded(switchPos)) {
      return false;
    }
    IBlockState switchState = world.getBlockState(switchPos);
    if (!(switchState.getBlock() instanceof BlockLightSwitch)
        || !switchState.getValue(BlockLightSwitch.POWERED)) {
      return true;
    }
    TileEntity switchTe = world.getTileEntity(switchPos);
    return !(switchTe instanceof TileEntityLightSwitch)
        || !pos.equals(((TileEntityLightSwitch) switchTe).getRelay());
  }

  /** The block it powers is told when it goes, and so are that block's neighbours. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    super.breakBlock(world, pos, state);
    world.notifyNeighborsOfStateChange(pos.offset(state.getValue(FACING).getOpposite()), this,
        false);
  }

  // --- unseen, untouchable -----------------------------------------------------------------

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
    return EnumBlockRenderType.INVISIBLE;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return NO_BOX;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean canCollideCheck(@Nonnull IBlockState state, boolean hitIfLiquid) {
    return false;
  }

  @Override
  public boolean isReplaceable(@Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
    return true;
  }

  @Override
  @Nonnull
  public Item getItemDropped(@Nonnull IBlockState state, @Nonnull Random rand, int fortune) {
    return Items.AIR;
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    return ItemStack.EMPTY;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.DESTROY;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  // --- the switch it serves ------------------------------------------------------------------

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySwitchRelay.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityswitchrelay";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntitySwitchRelay();
  }
}
