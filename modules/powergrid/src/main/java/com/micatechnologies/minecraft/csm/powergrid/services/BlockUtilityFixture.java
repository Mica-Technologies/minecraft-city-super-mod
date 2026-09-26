package com.micatechnologies.minecraft.csm.powergrid.services;

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
 * A building service fitting hung on a wall: an electric meter and its socket, the service
 * disconnect, a gas meter on its riser, the water meter setter, a utility room label. It faces
 * the player who places it, so the wall the player was looking at is behind it, and its box is
 * given facing north, in sixteenths, and turned with it. It has no tile entity and nothing ticks:
 * what moves on a meter's face is an animated texture. Its model comes from
 * {@code dev-env-utils/scripts/gen_utilities_meters.py}; the classes that do something extend
 * this one.
 *
 * @since 2026.9
 */
public class BlockUtilityFixture extends AbstractBlockRotatableNSEW {

  /**
   * Carries the registry name to the superclass constructor, which asks for it before this
   * class's fields are assigned.
   */
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * Constructs a fitting.
   *
   * @param registryName its registry name
   * @param box          its box facing north: x0, y0, z0, x1, y1, z1 in sixteenths
   */
  public BlockUtilityFixture(String registryName, double[] box) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.0F, 6.0F, 0F, 0, true);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
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

  /** Cutout: a meter's face is drawn as a square with its octagon cut out of the corners. */
  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
