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
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A ceiling finish of the Interior Finishes tab: a full-size block, one class constructed by
 * registry name. Three kinds, told apart by the name:
 *
 * <ul>
 *   <li>a plain finish (lay-in tiles, perforated metal, pressed tin): a solid cube;</li>
 *   <li>a directional one (wood slats, corrugated metal, {@code _slat_} or {@code _corrugated_}
 *   in the name): stores the {@code axis} its slats or ribs run along, the placer's look, so a
 *   field of them lines up across blocks;</li>
 *   <li>a grid (open cell, exposed T-bar, {@code _open_cell_} or {@code _grid_}): the grid drawn
 *   at the bottom of the block, cutout, open to the plenum above, but still a full block to
 *   stand on and to select.</li>
 * </ul>
 *
 * <p>The models come from {@code dev-env-utils/scripts/gen_ceilings.py}.</p>
 *
 * @since 2026.10
 */
public class BlockCeilingFinish extends AbstractBlock {

  public static final PropertyEnum<Axis> AXIS = PropertyEnum.create("axis", Axis.class, Axis.X,
      Axis.Z);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final boolean directional;
  private final boolean grid;

  public BlockCeilingFinish(String registryName) {
    super(pendingMaterial(registryName), metal(registryName) ? SoundType.METAL
            : wood(registryName) ? SoundType.WOOD : SoundType.STONE,
        wood(registryName) ? "axe" : "pickaxe", 0, 1.5F, 10F, 0F, grid(registryName) ? 0 : 255);
    this.registryName = registryName;
    this.directional = directional(registryName);
    this.grid = grid(registryName);
    PENDING_REGISTRY_NAME.remove();
    if (directional) {
      setDefaultState(blockState.getBaseState().withProperty(AXIS, Axis.Z));
    }
  }

  private static boolean directional(String name) {
    return name.contains("_slat_") || name.contains("_corrugated_");
  }

  private static boolean grid(String name) {
    return name.contains("_open_cell_") || name.contains("_grid_");
  }

  private static boolean wood(String name) {
    return name.contains("_slat_");
  }

  private static boolean metal(String name) {
    return name.contains("_corrugated_") || name.contains("_perforated_")
        || name.contains("_tin_") || grid(name);
  }

  private static Material pendingMaterial(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return wood(name) ? Material.WOOD : metal(name) ? Material.IRON : Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  private boolean isDirectional() {
    return registryName != null ? directional : directional(PENDING_REGISTRY_NAME.get());
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    // createBlockState runs inside the super constructor, before the fields are set
    return isDirectional() ? new CsmBlockStateContainer(this, AXIS)
        : new CsmBlockStateContainer(this);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return directional ? getDefaultState().withProperty(AXIS, (meta & 1) == 0 ? Axis.Z : Axis.X)
        : getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return directional && state.getValue(AXIS) == Axis.X ? 1 : 0;
  }

  @Nonnull
  @Override
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return directional ? getDefaultState().withProperty(AXIS,
        placer.getHorizontalFacing().getAxis()) : getDefaultState();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return !grid;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return !grid;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return grid ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID;
  }
}
