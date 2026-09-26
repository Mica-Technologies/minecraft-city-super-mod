package com.micatechnologies.minecraft.csm.powergrid.water;

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
 * A block-long section of a water tower's leg or riser, stacked to the tower's height. The
 * lowest section ({@link #BASE}) stands on its concrete pier and the highest ({@link #TOP})
 * takes a cap -- unless a tank is on it, whose own stub carries the leg or the riser on up into
 * it. Both are actual state, so nothing is stored and a column re-forms as it is built.
 *
 * @since 2026.9
 */
public class BlockTowerColumn extends AbstractBlock {

  /** Nothing of this column is below. */
  public static final PropertyBool BASE = PropertyBool.create("base");
  /** Nothing of this column, and no tank, is above. */
  public static final PropertyBool TOP = PropertyBool.create("top");

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * @param registryName its registry name
   * @param radius       the shaft's radius, in sixteenths
   */
  public BlockTowerColumn(String registryName, double radius) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3F, 10F, 0F, 0);
    this.registryName = registryName;
    double r = radius / 16.0;
    this.box = new AxisAlignedBB(0.5 - r, 0, 0.5 - r, 0.5 + r, 1, 0.5 + r);
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
    return new CsmBlockStateContainer(this, BASE, TOP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState above = world.getBlockState(pos.up());
    return state.withProperty(BASE, world.getBlockState(pos.down()).getBlock() != this)
        .withProperty(TOP, above.getBlock() != this && !TankUnits.isTankBlock(above));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face.getAxis() == EnumFacing.Axis.Y ? BlockFaceShape.CENTER_BIG
        : BlockFaceShape.UNDEFINED;
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
    return BlockRenderLayer.SOLID;
  }
}
