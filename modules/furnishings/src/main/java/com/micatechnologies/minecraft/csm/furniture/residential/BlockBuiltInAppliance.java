package com.micatechnologies.minecraft.csm.furniture.residential;

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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A full-size kitchen appliance that works ({@link IAppliance}): the range, whose oven cooks
 * food, the wall oven, and the dishwasher. The range and the dishwasher stand in the base
 * cabinets' run ({@link KitchenLine#BASE}), so the cabinets either side carry the countertop
 * line on through them ({@link IKitchenFitting}); the wall oven stands in no run.
 *
 * <p>{@link IAppliance#RUNNING} is stored, in the bit above the facing, and lights the oven
 * window or the status lamp through the blockstate.</p>
 *
 * @since 2026.9
 */
public class BlockBuiltInAppliance extends BlockResidentialFurniture
    implements IAppliance, ICsmTileEntityProvider, IKitchenFitting {

  private final ApplianceSpec spec;
  @Nullable
  private final KitchenLine line;

  /**
   * Constructs a built-in appliance.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param spec         what kind of appliance it is
   * @param line         the run it stands in, or null for none
   */
  public BlockBuiltInAppliance(String registryName, int[] box, ApplianceSpec spec,
      @Nullable KitchenLine line) {
    super(registryName, box, Material.WOOD, SoundType.METAL, 2.0F);
    this.spec = spec;
    this.line = line;
    setDefaultState(getDefaultState().withProperty(RUNNING, false));
  }

  @Override
  public ApplianceSpec getApplianceSpec() {
    return spec;
  }

  /**
   * The run it stands in.
   *
   * @return its line, or null
   */
  @Nullable
  public KitchenLine getLine() {
    return line;
  }

  @Override
  public boolean fitsRun(IBlockState state, EnumFacing side, EnumFacing runFacing,
      KitchenLine runLine) {
    EnumFacing f = state.getValue(FACING);
    return line != null && runLine == line && runFacing == f && side.getAxis() != f.getAxis();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, RUNNING);
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
