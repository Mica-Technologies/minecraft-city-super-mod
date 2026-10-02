package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A brush pile: a low heap of cut branches and leaves, the mess a chainsaw leaves around a felled
 * tree ({@link BrushPiles}). Walked through, broken instantly, and it drops sticks. Like tall
 * grass, placing a block over it replaces it.
 *
 * @since 2026.10
 */
public class BlockBrushPile extends AbstractBlock {

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.3125, 1.0);

  public BlockBrushPile() {
    super(Material.PLANTS, SoundType.PLANT, null, 0, 0.0F, 0.0F, 0.0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "brush_pile";
  }

  private static boolean onGround(World world, BlockPos pos) {
    return world.getBlockState(pos.down()).isSideSolid(world, pos.down(), EnumFacing.UP);
  }

  @Override
  public boolean canPlaceBlockAt(World world, @Nonnull BlockPos pos) {
    return super.canPlaceBlockAt(world, pos) && onGround(world, pos);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (!world.isRemote && !onGround(world, pos)) {
      dropBlockAsItem(world, pos, state, 0);
      world.setBlockToAir(pos);
    }
  }

  // --- drops ---

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Items.STICK;
  }

  @Override
  public int quantityDropped(Random random) {
    return 2 + random.nextInt(3);
  }

  @Override
  public boolean isReplaceable(IBlockAccess world, @Nonnull BlockPos pos) {
    return true;
  }

  // --- shape ---

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
    return BlockRenderLayer.CUTOUT_MIPPED;
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
