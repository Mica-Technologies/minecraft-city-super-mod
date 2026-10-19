package com.micatechnologies.minecraft.csm.parks.planters;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleBystander;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The two flower pots moved from Furniture &amp; Novelties ({@code largeflowerpot},
 * {@code roundflowerpot}), which draw the concrete and corten planters' models but kept their
 * six-way facing, since saved worlds store it. Plantable like the planters they draw while they
 * stand upright (a horizontal facing); one placed on its side ({@code up} or {@code down}, which
 * the blockstate turns over) takes nothing.
 *
 * @since 2026.10
 */
public abstract class AbstractBlockPlanterFacing extends AbstractBlockRotatableNSEWUD
    implements ICsmPlanter, ICsmTileEntityProvider, ICsmTrafficPoleBystander {

  /** The concrete and corten planters' soil: 14 px up, plants full size. */
  private static final PlanterSoil SOIL = new PlanterSoil(14.0F, 1.0F, null);

  protected AbstractBlockPlanterFacing() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, CsmPoleFit.properties(this, FACING),
        Planters.UNLISTED);
  }

  @Override
  @Nullable
  public PlanterSoil getSoil(IBlockState state) {
    return state.getValue(FACING).getAxis().isHorizontal() ? SOIL : null;
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return Planters.extendedState(this, state, world, pos);
  }

  /**
   * The box of the planter model it draws, the same one {@code planter_concrete} and
   * {@code planter_corten} have.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.062500, 0.000000, 0.062500, 0.937500, 0.875000, 0.937500);
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
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
    return Planters.drawsIn(layer);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    return Planters.activate(world, pos, state, player, hand);
  }

  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    Planters.harvested(world, pos, player);
    super.onBlockHarvested(world, pos, state, player);
  }

  @Override
  public void breakBlock(World world, BlockPos pos, IBlockState state) {
    Planters.dropPlant(world, pos);
    super.breakBlock(world, pos, state);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPlanter.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityparkplanter";
  }

  @Override
  public TileEntity createNewTileEntity(World world, int meta) {
    return new TileEntityPlanter();
  }
}
