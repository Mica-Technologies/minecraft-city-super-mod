package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A hanging acoustic baffle: a vertical felt (or wood) fin hung from the ceiling on aircraft
 * cables, joined with the baffles along its run into one long fin.
 *
 * <p>Stored: the {@code axis} it runs along, the placer's look. Actual state: {@code front} and
 * {@code back}, whether a baffle on the same axis continues the run that way (its felt edge is
 * drawn only where the run stops: north and east are front), and {@code cable}, its cable to the
 * ceiling at a run's ends and every fourth block along it. Nothing ticks, no tile entity. The
 * finishes are the linear lights' palette, so a fin and the light under it match; models from
 * {@code dev-env-utils/scripts/gen_acoustic_ceilings.py}.</p>
 *
 * @since 2026.10
 */
public class BlockAcousticBaffle extends AbstractBlock {

  public static final PropertyEnum<Axis> AXIS = PropertyEnum.create("axis", Axis.class, Axis.X,
      Axis.Z);
  public static final PropertyBool FRONT = PropertyBool.create("front");
  public static final PropertyBool BACK = PropertyBool.create("back");
  public static final PropertyBool CABLE = PropertyBool.create("cable");

  private static final AxisAlignedBB BOX_Z = new AxisAlignedBB(7 / 16.0, 4 / 16.0, 0, 9 / 16.0,
      1, 1);
  private static final AxisAlignedBB BOX_X = new AxisAlignedBB(0, 4 / 16.0, 7 / 16.0, 1, 1,
      9 / 16.0);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  public BlockAcousticBaffle(String registryName) {
    super(pending(registryName), SoundType.CLOTH, null, 0, 0.8F, 4F, 0F, 0);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
    setDefaultState(blockState.getBaseState().withProperty(AXIS, Axis.Z)
        .withProperty(FRONT, false).withProperty(BACK, false).withProperty(CABLE, false));
  }

  private static Material pending(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.CLOTH;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, FRONT, BACK, CABLE);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS, (meta & 1) == 0 ? Axis.Z : Axis.X);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == Axis.X ? 1 : 0;
  }

  @Nonnull
  @Override
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
  }

  private static boolean continues(IBlockAccess world, BlockPos pos, Axis axis,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() instanceof BlockAcousticBaffle && other.getValue(AXIS) == axis;
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    Axis axis = state.getValue(AXIS);
    EnumFacing front = axis == Axis.Z ? EnumFacing.NORTH : EnumFacing.EAST;
    boolean f = continues(world, pos, axis, front);
    boolean b = continues(world, pos, axis, front.getOpposite());
    int along = axis == Axis.X ? pos.getX() : pos.getZ();
    return state.withProperty(FRONT, f).withProperty(BACK, b)
        .withProperty(CABLE, !f || !b || Math.floorMod(along, 4) == 0);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(AXIS) == Axis.X ? BOX_X : BOX_Z;
  }

  @Nonnull
  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
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
    return BlockRenderLayer.CUTOUT;
  }
}
