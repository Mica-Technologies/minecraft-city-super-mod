package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPipe;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A sprinkler drop (#304): a short vertical pipe that comes down from a branch so a sprinkler head
 * hangs one or more cells below it. It reaches up into the pipe above it (any {@link ICsmPipe}),
 * stacks, and is itself a pipe a {@link BlockSprinklerHead} under it joins. Nothing stored;
 * {@code reach} is how far it reaches into the pipe's cell above, 0 to 8 sixteenths, or 9 for
 * none.
 *
 * @since 2026.10
 */
public class BlockSprinklerDrop extends AbstractBlock implements ICsmPipe {

  public static final PropertyInteger REACH = BlockSprinklerHead.REACH;

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();
  private static final AxisAlignedBB BOX = new AxisAlignedBB(6.5 / 16, 0, 6.5 / 16, 9.5 / 16, 1,
      9.5 / 16);

  private final String registryName;

  public BlockSprinklerDrop(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 0, 2.0F, 6.0F, 0F, 0);
    this.registryName = registryName;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(REACH, BlockSprinklerHead.NONE));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, REACH);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    BlockPos up = pos.up();
    IBlockState above = world.getBlockState(up);
    int reach = BlockSprinklerHead.NONE;
    if (above.getBlock() instanceof ICsmPipe) {
      int bottom = ((ICsmPipe) above.getBlock()).pipeBottom(world, up, above);
      reach = bottom >= 0 && bottom <= 8 ? bottom : BlockSprinklerHead.NONE;
    }
    return state.withProperty(REACH, reach);
  }

  @Override
  public int pipeBottom(IBlockAccess world, BlockPos pos, IBlockState state) {
    return 0;
  }

  @Override
  public int pipeTop(IBlockAccess world, BlockPos pos, IBlockState state) {
    return 16;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
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

  @Nonnull
  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
