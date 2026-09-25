package com.micatechnologies.minecraft.csm.transit.platform;

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
 * A piece of station fit-out that faces one of four ways and does nothing but stand there: a
 * CCTV camera, a hanging wayfinding sign, the gap warning, the network map, the litter bin. It
 * faces the player who places it (a wall-mounted piece has the wall behind it), and its box is
 * given facing north, in sixteenths, and turned with it. Its model comes from
 * {@code dev-env-utils/scripts/gen_transit_platforms.py}; the classes that do something extend
 * this one.
 *
 * @since 2026.9
 */
public class BlockPlatformFixture extends AbstractBlockRotatableNSEW {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;
  private final boolean translucent;

  /**
   * Constructs a fixture drawn in the cutout layer, giving no light.
   *
   * @param registryName its registry name
   * @param box          its box facing north: x0, y0, z0, x1, y1, z1 in sixteenths
   */
  public BlockPlatformFixture(String registryName, double[] box) {
    this(registryName, box, false, 0);
  }

  /**
   * Constructs a fixture.
   *
   * @param registryName its registry name
   * @param box          its box facing north: x0, y0, z0, x1, y1, z1 in sixteenths
   * @param translucent  whether it has see-through parts (the litter bin's clear bag)
   */
  public BlockPlatformFixture(String registryName, double[] box, boolean translucent) {
    this(registryName, box, translucent, 0);
  }

  /**
   * Constructs a fixture.
   *
   * @param registryName its registry name
   * @param box          its box facing north: x0, y0, z0, x1, y1, z1 in sixteenths
   * @param translucent  whether it has see-through parts
   * @param light        the light it gives, 0 to 15
   */
  public BlockPlatformFixture(String registryName, double[] box, boolean translucent,
      int light) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.0F, 6.0F, light / 15.0F, 0);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
    this.translucent = translucent;
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return translucent ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
  }
}
