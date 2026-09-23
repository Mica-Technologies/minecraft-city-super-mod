package com.micatechnologies.minecraft.csm.parks.trees;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
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
import net.minecraft.world.World;

/**
 * A hanging moss, hung under a limb or a crown: Spanish moss on a live oak. Stacks downward into a
 * curtain; the lowest block of a curtain draws the ragged tip ({@link #TIP}, read from the block
 * below, never stored).
 *
 * <p>It needs something above it. When that goes, the moss drops, and so does the rest of the
 * curtain below it, as vines do.</p>
 *
 * @since 2026.9
 */
public class BlockHangingMoss extends AbstractBlock {

  /** Whether this is the bottom of its curtain. Actual state only. */
  public static final PropertyBool TIP = PropertyBool.create("tip");

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0.15, 0.0, 0.15, 0.85, 1.0, 0.85);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  public BlockHangingMoss(String registryName) {
    super(stash(registryName), SoundType.PLANT, null, 0, 0.1F, 0.1F, 0.0F, 0);
    this.registryName = registryName;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(TIP, true));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.PLANTS;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, TIP);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    return state.withProperty(TIP, world.getBlockState(pos.down()).getBlock() != this);
  }

  // --- hanging ---

  private boolean canHang(World world, BlockPos pos) {
    return !world.isAirBlock(pos.up());
  }

  @Override
  public boolean canPlaceBlockAt(World world, @Nonnull BlockPos pos) {
    return super.canPlaceBlockAt(world, pos) && canHang(world, pos);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (!world.isRemote && !canHang(world, pos)) {
      dropBlockAsItem(world, pos, state, 0);
      world.setBlockToAir(pos);
    }
  }

  // --- behaviour ---

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return true;
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

  @Override
  public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return 100;
  }

  @Override
  public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return 60;
  }
}
