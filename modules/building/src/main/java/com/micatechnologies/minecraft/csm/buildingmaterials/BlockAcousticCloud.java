package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A hanging acoustic cloud: a flat felt (or wood) panel hung a little below the ceiling on
 * aircraft cables. Clouds side by side make one panel: their touching edges are hidden between
 * the panels' faces, so nothing joins them but placement.
 *
 * <p>Nothing stored. Actual state: {@code cable}, a cable to the ceiling at the panel's
 * north-west corner and every third block each way from it (by neighbours and world position,
 * so a cloud of any size hangs from at least one). The finishes are the linear lights' palette;
 * models from {@code dev-env-utils/scripts/gen_acoustic_ceilings.py}.</p>
 *
 * @since 2026.10
 */
public class BlockAcousticCloud extends AbstractBlock {

  public static final PropertyBool CABLE = PropertyBool.create("cable");

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 11 / 16.0, 0, 1, 12 / 16.0, 1);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  public BlockAcousticCloud(String registryName) {
    super(pending(registryName), SoundType.CLOTH, null, 0, 0.8F, 4F, 0F, 0);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
    setDefaultState(blockState.getBaseState().withProperty(CABLE, false));
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
    return new CsmBlockStateContainer(this, CABLE);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  private static boolean cloud(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockAcousticCloud;
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    boolean alongX = !cloud(world, pos.west()) || Math.floorMod(pos.getX(), 3) == 0;
    boolean alongZ = !cloud(world, pos.north()) || Math.floorMod(pos.getZ(), 3) == 0;
    return state.withProperty(CABLE, alongX && alongZ);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
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
