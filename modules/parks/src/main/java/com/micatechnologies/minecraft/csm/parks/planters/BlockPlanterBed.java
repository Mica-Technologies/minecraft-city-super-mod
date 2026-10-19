package com.micatechnologies.minecraft.csm.parks.planters;

import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkJoining;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A raised planting bed that can be planted, one plant a cell: still a joining {@code BED}
 * whose walls stand only round the outside of a run, with the same four actual-state sides and
 * no stored state, and the plant in an unlisted property like every planter's.
 *
 * @since 2026.10
 */
public class BlockPlanterBed extends BlockParkJoining implements ICsmPlanter,
    ICsmTileEntityProvider {

  private final PlanterSoil soil;

  /**
   * @param registryName its registry name
   * @param height       its wall height in sixteenths
   * @param width        how wide a run of it is, in sixteenths (16: it fills its cell)
   * @param soil         the soil surface, in sixteenths
   * @param scale        the scale a plant is drawn at
   */
  public BlockPlanterBed(String registryName, int height, int width, float soil, float scale) {
    super(registryName, Kind.BED, height, width);
    this.soil = new PlanterSoil(soil, scale, null);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[]{NORTH, EAST, SOUTH, WEST},
        Planters.UNLISTED);
  }

  @Override
  public PlanterSoil getSoil(IBlockState state) {
    return soil;
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return Planters.extendedState(this, state, world, pos);
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
