package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A floor finish laid over any floor, one pixel thick, as vanilla carpet is: carpet tile, vinyl
 * composition tile, ceramic tile, hardwood, polished concrete or studded rubber. One class,
 * constructed by registry name ({@code floor_<material>_<colour>}).
 *
 * <p>It needs a floor with a solid top under it, and comes up (dropping itself) when that goes. The
 * only stored state is {@link #AXIS}, the way the player faced when laying it: hardwood's boards
 * run that way. The blockstate picks one of several turns of the model per block position, so a
 * floor of it has no visible repeat; the models come from
 * {@code dev-env-utils/scripts/gen_flooring.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockFloorFinish extends AbstractBlock {

  public static final PropertyEnum<EnumFacing.Axis> AXIS =
      PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);

  private static final AxisAlignedBB FINISH = new AxisAlignedBB(0, 0, 0, 1, 1 / 16.0, 1);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockFloorFinish}.
   *
   * @param registryName {@code floor_<material>_<colour>}
   *
   * @since 1.0
   */
  public BlockFloorFinish(String registryName) {
    super(pendingMaterial(registryName), sound(registryName), tool(registryName), 0, 0.8F, 2F, 0F,
        0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    if (registryName.contains("carpet")) {
      return Material.CARPET;
    }
    return registryName.contains("hardwood") ? Material.WOOD : Material.ROCK;
  }

  private static SoundType sound(String registryName) {
    if (registryName.contains("carpet") || registryName.contains("rubber")) {
      return SoundType.CLOTH;
    }
    return registryName.contains("hardwood") ? SoundType.WOOD : SoundType.STONE;
  }

  private static String tool(String registryName) {
    if (registryName.contains("hardwood")) {
      return "axe";
    }
    return registryName.contains("carpet") || registryName.contains("rubber") ? null
        : "pickaxe";
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, AXIS);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS, meta == 1 ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == EnumFacing.Axis.Z ? 1 : 0;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
  }

  // --- support ------------------------------------------------------------------------------------

  private static boolean supported(World world, BlockPos pos) {
    BlockPos below = pos.down();
    return world.getBlockState(below).isSideSolid(world, below, EnumFacing.UP);
  }

  @Override
  public boolean canPlaceBlockAt(World worldIn, @Nonnull BlockPos pos) {
    return super.canPlaceBlockAt(worldIn, pos) && supported(worldIn, pos);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (!worldIn.isRemote && !supported(worldIn, pos)) {
      dropBlockAsItem(worldIn, pos, state, 0);
      worldIn.setBlockState(pos, Blocks.AIR.getDefaultState());
    }
  }

  // --- shape ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FINISH;
  }

  /**
   * Solid underneath, so it sits flush on the floor it covers; nothing else is a face.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
