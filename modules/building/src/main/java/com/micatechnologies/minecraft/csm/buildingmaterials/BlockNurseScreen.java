package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.CsmGlassLayer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A nurse-station glass privacy screen: a glass panel in an aluminium shoe, standing on the far
 * edge of the counter it is placed on. One class, constructed by registry name
 * ({@code nurse_screen_clear}, {@code nurse_screen_frosted}).
 *
 * <p>Stored: the facing only (horizontal index, S0 W1 N2 E3), the way the player looked when
 * placing it from behind the counter; the screen stands along that side of its cell. Screens of
 * one kind side by side facing the same way join into one run: the glass and shoe run on, and a
 * post is drawn only at each end ({@code left} / {@code right}, actual state). 16 states. The
 * models come from {@code gen_wall_finishes.py}.</p>
 *
 * @version 1.0
 * @since 2026.10
 */
public class BlockNurseScreen extends AbstractBlock implements ICsmGlassFronted {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  /** Whether the run goes on past the left (seen from behind the counter, facing out). */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  /** SHARED with gen_wall_finishes.SCREEN_Z and SCREEN_TOP. */
  private static final AxisAlignedBB NORTH = new AxisAlignedBB(0, 0, 0.25 / 16, 1, 11.75 / 16,
      1.75 / 16);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockNurseScreen}.
   *
   * @param registryName {@code nurse_screen_<kind>}
   *
   * @since 1.0
   */
  public BlockNurseScreen(String registryName) {
    super(pending(registryName), SoundType.GLASS, null, 0, 0.5F, 2F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(LEFT, false).withProperty(RIGHT, false));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pending(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.GLASS;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, RIGHT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(LEFT, same(worldIn, pos.offset(f.rotateYCCW()), f))
        .withProperty(RIGHT, same(worldIn, pos.offset(f.rotateY()), f));
  }

  // --- shape ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BlockGarageDoor.turn(NORTH, state.getValue(FACING));
  }

  @Override
  @Nonnull
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }

  @Override
  public boolean canRenderInLayer(@Nonnull IBlockState state, @Nonnull BlockRenderLayer layer) {
    return CsmGlassLayer.drawsIn(layer);
  }
}
