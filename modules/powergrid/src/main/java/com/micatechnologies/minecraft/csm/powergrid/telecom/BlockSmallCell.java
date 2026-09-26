package com.micatechnologies.minecraft.csm.powergrid.telecom;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPostTopFixture;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A small cell's canister antenna on the top of a street pole: a round radome over a block tall
 * on a short mast, slipped over the pole's top with a collar sized to the pole below it
 * ({@link CsmPoleFit#PROPERTY}, actual state: the 12-across pole family, the thin pole, the
 * pedestal pole; anything else takes the widest collar). It is a post-top fixture, so a concrete
 * pole under it shows its tenon, and the pole family draws no mount stub up into it. Three
 * states, no tile entity.
 *
 * @since 2026.9
 */
public class BlockSmallCell extends AbstractBlock implements ICsmPostTopFixture,
    ICsmTrafficPoleIgnored {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * @param registryName its registry name
   * @param box          its box, in sixteenths
   */
  public BlockSmallCell(String registryName, double[] box) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2F, 6F, 0F, 0);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, CsmPoleFit.PROPERTY);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    Block below = world.getBlockState(pos.down()).getBlock();
    CsmPoleFit fit = below instanceof AbstractBlockTrafficPole
        ? CsmPoleFit.forPoleRadius(((AbstractBlockTrafficPole) below).getPoleRadius())
        : CsmPoleFit.LARGE;
    return state.withProperty(CsmPoleFit.PROPERTY, fit);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
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
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
