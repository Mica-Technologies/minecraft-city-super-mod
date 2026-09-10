package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRoadSurfaceRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A work zone channelizing device that faces a direction: the channelizer-cades, whose striped
 * panel has a front, and the drum whose warning light is mounted off to one side.
 *
 * <p>The horizontal counterpart of {@link BlockWorkZoneDevice}; see there for why one class
 * covers many devices.</p>
 *
 * <p>The two cades are separate blocks rather than one block turned around. Their stripes slope
 * toward the side traffic should pass, so a keep-left cade rotated 180 degrees is still a
 * keep-left cade — it would be pointing drivers into the work rather than around it.</p>
 *
 * @version 1.0
 * @see AbstractBlockRoadSurfaceRotatableNSEW
 * @since 2026.9
 */
public class BlockWorkZoneDeviceRotatable extends AbstractBlockRoadSurfaceRotatableNSEW
    implements ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored {

  /**
   * Carries the registry name to the superclass constructor; see
   * {@link BlockWorkZoneDevice#initRegistryName(String)}.
   *
   * @since 1.0
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  /**
   * The registry name of this device.
   *
   * @since 1.0
   */
  private final String registryName;

  /**
   * The bounding box of this device, before it settles onto the surface below it.
   *
   * @since 1.0
   */
  private final AxisAlignedBB boundingBox;

  /**
   * Constructs a {@link BlockWorkZoneDeviceRotatable} instance.
   *
   * @param registryName the registry name of the device
   * @param boundingBox  the bounding box of the device, in block space
   *
   * @since 1.0
   */
  public BlockWorkZoneDeviceRotatable(String registryName, AxisAlignedBB boundingBox) {
    super(initRegistryName(registryName), SoundType.STONE, "pickaxe", 0, 0.6F, 3F, 0F, 0);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
  }

  /**
   * Stashes the registry name for {@link #getBlockRegistryName()} and returns the material to
   * hand to the superclass constructor.
   *
   * @param name the registry name of the device
   *
   * @return the material of the device
   *
   * @since 1.0
   */
  protected static Material initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
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
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
