package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A platform canopy, one block of roof at a time, set on columns: a steel deck with a white
 * soffit and a light strip down the platform. Canopies side by side join on all four sides into
 * one roof of any shape: {@link #NORTH} to {@link #WEST} (actual state, world sides) say where
 * another canopy continues, and the fascia is drawn only round the outside. The light strip runs
 * along {@link #AXIS}, the way the player faced when placing it, which is the only stored state.
 * The roof light is always on (light 11).
 *
 * <p>The deck lies at the foot of its block, so a canopy set on top of a column rests on it.
 * Over a platform an RCMC train stops at, keep it over the platform: a metro car's roof stands
 * about four blocks over the platform surface.</p>
 *
 * @since 2026.9
 */
public class BlockPlatformCanopy extends AbstractBlock {

  public static final PropertyEnum<EnumFacing.Axis> AXIS =
      PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");

  private static final AxisAlignedBB DECK = new AxisAlignedBB(0, 0, 0, 1, 3.2 / 16.0, 1);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a canopy.
   *
   * @param registryName its registry name
   */
  public BlockPlatformCanopy(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.5F, 8.0F, 11 / 15.0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X)
        .withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false)
        .withProperty(WEST, false));
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, AXIS, NORTH, EAST, SOUTH, WEST);
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
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NORTH, isCanopy(world, pos.north()))
        .withProperty(EAST, isCanopy(world, pos.east()))
        .withProperty(SOUTH, isCanopy(world, pos.south()))
        .withProperty(WEST, isCanopy(world, pos.west()));
  }

  private static boolean isCanopy(IBlockAccess world, BlockPos at) {
    return world.getBlockState(at).getBlock() instanceof BlockPlatformCanopy;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return DECK;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
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
    return BlockRenderLayer.CUTOUT;
  }
}
