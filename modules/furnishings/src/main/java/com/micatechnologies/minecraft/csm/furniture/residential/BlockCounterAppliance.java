package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceHelper;
import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceSpec;
import com.micatechnologies.minecraft.csm.furniture.appliance.IAppliance;
import com.micatechnologies.minecraft.csm.furniture.appliance.TileEntityAppliance;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A countertop appliance that works ({@link IAppliance}): the microwave, toaster, air fryer,
 * blender and coffee machine. It stands on the counter like any {@link BlockCounterPiece};
 * right-click opens its screen (or, with a water bucket or bottle, fills the coffee machine's
 * tank). {@link IAppliance#RUNNING} is stored, in the bit above the facing, and lights its window
 * or its toasting slots through the blockstate.
 *
 * @since 2026.9
 */
public class BlockCounterAppliance extends BlockCounterPiece
    implements IAppliance, ICsmTileEntityProvider {

  private final ApplianceSpec spec;

  /**
   * Constructs a countertop appliance.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor, in sixteenths
   * @param spec         what kind of appliance it is
   */
  public BlockCounterAppliance(String registryName, int[] box, ApplianceSpec spec) {
    super(registryName, box, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID);
    this.spec = spec;
    setDefaultState(getDefaultState().withProperty(RUNNING, false));
  }

  @Override
  public ApplianceSpec getApplianceSpec() {
    return spec;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, REST, RUNNING);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(RUNNING, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(RUNNING) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(RUNNING, false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(RUNNING) ? spec.getLight() : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    return ApplianceHelper.activate(world, pos, player, hand);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    ApplianceHelper.dropContents(world, pos);
    super.breakBlock(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(@Nonnull IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(@Nonnull IBlockState state, World world,
      @Nonnull BlockPos pos) {
    return ApplianceHelper.comparator(world, pos);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityAppliance.class;
  }

  @Override
  public String getTileEntityName() {
    return TILE_ENTITY_NAME;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityAppliance();
  }
}
