package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
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
 * Tactile paving laid over any floor, one pixel thick, as vanilla carpet is: truncated warning
 * domes, or guidance bars, in yellow or in grey with stainless studs. It lies as well on RCMC's
 * station platform decking as on any other solid floor. One class, constructed by registry name
 * ({@code tactile_<warning|guidance>_<colour>}); Building's floor finishes are the pattern.
 *
 * <p>It needs a floor with a solid top under it, and comes up (dropping itself) when that goes.
 * The only stored state is {@link #AXIS}, the way the player faced when laying it: guidance bars
 * run that way. The blockstate picks one of two drawings and a turn per block position, so a
 * floor of it shows no repeat.</p>
 *
 * @since 2026.9
 */
public class BlockTactilePaving extends AbstractBlock {

  public static final PropertyEnum<EnumFacing.Axis> AXIS =
      PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);

  private static final AxisAlignedBB PAVING = new AxisAlignedBB(0, 0, 0, 1, 1 / 16.0, 1);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs tactile paving.
   *
   * @param registryName {@code tactile_<warning|guidance>_<colour>}
   */
  public BlockTactilePaving(String registryName) {
    super(stash(registryName), SoundType.STONE, "pickaxe", 0, 0.8F, 2F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X));
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
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

  // --- support --------------------------------------------------------------------------------

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

  // --- shape ----------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return PAVING;
  }

  /** Solid underneath, so it sits flush on the floor it covers; nothing else is a face. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
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
