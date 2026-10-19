package com.micatechnologies.minecraft.csm.parks.planters;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleBystander;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A linear trough planter, for a median or a plaza: troughs placed end to end along their axis
 * are one trough, capped only at the two ends of the run, planted one plant a cell like every
 * planter.
 *
 * <p>The axis is stored (two states, placed along the way the player faces); whether each end
 * continues into another trough of the same block and axis is actual state ({@link #NEG},
 * {@link #POS}), which the multipart blockstate draws a cap or an open end from
 * ({@code gen_park_plantings.py}). Eight states in all.</p>
 *
 * @since 2026.10
 */
public class BlockPlanterTrough extends AbstractBlock implements ICsmPlanter,
    ICsmTileEntityProvider, ICsmTrafficPoleBystander {

  /** The axis the trough runs along. */
  public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis",
      EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
  /** The run continues past the negative end (north for axis z, west for axis x). */
  public static final PropertyBool NEG = PropertyBool.create("neg");
  /** The run continues past the positive end (south for axis z, east for axis x). */
  public static final PropertyBool POS = PropertyBool.create("pos");

  private static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final PlanterSoil soil;
  private final double height;

  /**
   * @param registryName its registry name
   * @param metal        corten steel (a metal sound) rather than concrete
   * @param height       its wall height in sixteenths
   * @param soil         the soil surface, in sixteenths
   * @param scale        the scale a plant is drawn at
   */
  public BlockPlanterTrough(String registryName, boolean metal, int height, float soil,
      float scale) {
    super(stash(registryName), metal ? SoundType.METAL : SoundType.STONE, "pickaxe", 0, 1.5F,
        3.0F, 0.0F, 0);
    this.registryName = registryName;
    this.soil = new PlanterSoil(soil, scale, null);
    this.height = height / 16.0;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Z)
        .withProperty(NEG, false).withProperty(POS, false));
  }

  private static Material stash(String registryName) {
    PENDING.set(new Object[]{registryName});
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : (String) PENDING.get()[0];
  }

  // --- state ---

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[]{AXIS, NEG, POS}, Planters.UNLISTED);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == EnumFacing.Axis.X ? 0 : 1;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS,
        (meta & 1) == 0 ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState withRotation(@Nonnull IBlockState state, Rotation rot) {
    if (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) {
      return state.withProperty(AXIS, state.getValue(AXIS) == EnumFacing.Axis.X
          ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
    }
    return state;
  }

  /** Whether the trough continues into the block on the given side. */
  private boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side, EnumFacing.Axis axis) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() == this && other.getValue(AXIS) == axis;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    EnumFacing.Axis axis = state.getValue(AXIS);
    EnumFacing neg = axis == EnumFacing.Axis.X ? EnumFacing.WEST : EnumFacing.NORTH;
    return state.withProperty(NEG, joins(world, pos, neg, axis))
        .withProperty(POS, joins(world, pos, neg.getOpposite(), axis));
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return Planters.extendedState(this, state, world, pos);
  }

  @Override
  public PlanterSoil getSoil(IBlockState state) {
    return soil;
  }

  // --- shape ---

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // The walls stand 1 px in from the long sides; the trough runs the full length of its cell.
    return state.getValue(AXIS) == EnumFacing.Axis.X
        ? new AxisAlignedBB(0, 0, 1 / 16.0, 1, height, 15 / 16.0)
        : new AxisAlignedBB(1 / 16.0, 0, 0, 15 / 16.0, height, 1);
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
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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
  public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
    return Planters.drawsIn(layer);
  }

  // --- planting ---

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
