package com.micatechnologies.minecraft.csm.parks.planters;

import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkProp;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
 * A planter that can be planted: one of the {@code planter_*} props (concrete, cedar, corten,
 * terracotta, the glazed urn, the concrete bowl, the half barrel). Still a {@code PLANTER} prop,
 * sized and drawn as before, with the same one state and the same metadata, so planters already
 * placed load unchanged and an empty one looks exactly as it did; what is planted travels in an
 * unlisted property ({@link Planters#PLANT}) and is drawn by {@link PlanterBakedModel}.
 *
 * @since 2026.10
 */
public class BlockPlanter extends BlockParkProp implements ICsmPlanter, ICsmTileEntityProvider {

  private final PlanterSoil soil;

  /**
   * @param registryName its registry name
   * @param height       its height in sixteenths
   * @param inset        how far its box stands in from each side, in sixteenths
   * @param soil         the soil surface, in sixteenths
   * @param scale        the scale a plant is drawn at
   */
  public BlockPlanter(String registryName, int height, int inset, float soil, float scale) {
    this(registryName, height, inset, soil, scale, null);
  }

  /**
   * @param hiddenTexture a texture of the model left out while something is planted (the
   *                      bowl's bedding flowers)
   */
  public BlockPlanter(String registryName, int height, int inset, float soil, float scale,
      @Nullable String hiddenTexture) {
    super(registryName, Kind.PLANTER, height, inset);
    this.soil = new PlanterSoil(soil, scale, hiddenTexture);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[0], Planters.UNLISTED);
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
