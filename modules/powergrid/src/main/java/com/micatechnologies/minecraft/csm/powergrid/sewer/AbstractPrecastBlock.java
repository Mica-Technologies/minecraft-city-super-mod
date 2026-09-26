package com.micatechnologies.minecraft.csm.powergrid.sewer;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A precast concrete piece of the sewer and stormwater system, facing the player who places it:
 * its box is given facing north, in sixteenths, and turned with it. Nothing here ticks or has a
 * tile entity; the subclasses add what their neighbours decide (a run's ends, a stack's ends).
 * Models come from {@code dev-env-utils/scripts/gen_utilities_sewer.py}.
 *
 * @since 2026.9
 */
public abstract class AbstractPrecastBlock extends AbstractBlockRotatableNSEW {

  /**
   * Carries the registry name to the superclass constructor, which asks for it before this
   * class's fields are assigned.
   */
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * @param registryName its registry name
   * @param box          its box facing north: x0, y0, z0, x1, y1, z1 in sixteenths
   */
  protected AbstractPrecastBlock(String registryName, double[] box) {
    super(stash(registryName), SoundType.STONE, "pickaxe", 1, 2.5F, 12.0F, 0F, 0, true);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
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
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
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

  /** Cutout: the curb inlet's manhole lid and the outlet riser's trash rack are cut out. */
  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
