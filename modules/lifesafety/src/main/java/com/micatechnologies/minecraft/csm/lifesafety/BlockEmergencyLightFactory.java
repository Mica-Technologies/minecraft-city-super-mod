package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractPoweredBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Emergency lights that differ only in registry name, box and where their lamps are: the
 * twin-head "bug-eye" units, the LED bar, remote heads, the outdoor wall pack and the recessed
 * downlight, written by {@code gen_emergency_lighting.py}. Each behaves as the original emergency
 * lights do: redstone is mains power, and without it the lamps light (light level 15, and the
 * glow {@link TileEntityEmergencyLightRenderer} draws in front of each lamp).
 *
 * <p>The glow's display list is keyed on the block id, so blocks of this one class with different
 * lamps never share a glow.</p>
 *
 * @since 2026.9
 */
public class BlockEmergencyLightFactory extends AbstractPoweredBlockRotatableNSEWUD
    implements ICsmTileEntityProvider, IEmergencyLightBlock {

  /** The registry name, for the constructor calls that run before the fields are set. */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final float[][] bulbs;

  /**
   * Constructs an emergency light.
   *
   * @param registryName its registry name
   * @param boundingBox  its box facing north
   * @param bulbs        each lamp as {fromX, fromY, fromZ, toX, toY, toZ} in sixteenths, facing
   *                     north; the glow is drawn in front of each lamp's low-z face
   */
  public BlockEmergencyLightFactory(String registryName, AxisAlignedBB boundingBox,
      float[][] bulbs) {
    this(initRegistryName(registryName), registryName, boundingBox, bulbs);
  }

  private BlockEmergencyLightFactory(Void ignored, String registryName,
      AxisAlignedBB boundingBox, float[][] bulbs) {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, false);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.bulbs = bulbs;
    PENDING_REGISTRY_NAME.remove();
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(POWERED, false));
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public float[][] getBulbs(IBlockAccess world, BlockPos pos, IBlockState state) {
    return bulbs;
  }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return !state.getValue(POWERED) ? 15 : 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
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
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityEmergencyLight.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityemergencylight";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityEmergencyLight();
  }
}
