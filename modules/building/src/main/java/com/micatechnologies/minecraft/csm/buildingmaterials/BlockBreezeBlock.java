package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
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
 * A breeze block: a decorative concrete screen unit, pierced right through in a pattern (circles,
 * quatrefoils, diamonds or squares), laid up into a wall that light, air and a view pass through.
 * One class, constructed with its registry name; the generator
 * ({@code dev-env-utils/scripts/gen_screen_blocks.py}) draws every pattern and colour.
 *
 * <p>The pattern goes through one way, so the block stores the axis it is seen through: placed,
 * the pattern faces the player. The pierced faces are cutout and the block is neither opaque nor
 * a full cube, so it shades nothing and light passes through it as through the holes; against
 * another breeze block no face is drawn, so a wall of them is one screen without seams.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public class BlockBreezeBlock extends AbstractBlock {

  /** The axis the pattern is seen through: {@code z} shows it to the north and south. */
  public static final PropertyEnum<EnumFacing.Axis> AXIS =
      PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);

  /**
   * The name being constructed. {@link AbstractBlock}'s constructor asks for the registry name
   * before this class's fields are assigned, so the name is handed across on the thread instead.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockBreezeBlock}.
   *
   * @param registryName the registry name, such as {@code breeze_block_circle_white}
   *
   * @since 1.0
   */
  public BlockBreezeBlock(String registryName) {
    super(pendingMaterial(registryName), SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Z));
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState()
        .withProperty(AXIS, (meta & 1) == 1 ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == EnumFacing.Axis.X ? 1 : 0;
  }

  /**
   * The pattern faces the player: a screen is laid looking at the face it shows.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
  }

  /**
   * No face against another breeze block, so a wall of them has no seams and its insides are not
   * drawn twice.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public boolean shouldSideBeRendered(@Nonnull IBlockState blockState,
      @Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    if (blockAccess.getBlockState(pos.offset(side)).getBlock() instanceof BlockBreezeBlock) {
      return false;
    }
    return super.shouldSideBeRendered(blockState, blockAccess, pos, side);
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Not a full cube, though it fills its cell: a full cube darkens its neighbours' smooth lighting
   * and would read as solid, and a breeze block wall is meant to let the light through.
   *
   * @since 1.0
   */
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
    return BlockRenderLayer.CUTOUT;
  }
}
