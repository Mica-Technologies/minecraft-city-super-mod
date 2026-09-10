package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRoadSurface;
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
 * A work zone channelizing device with no rotation: a cone, a channelizer, a delineator post, a
 * sand barrel.
 *
 * <p>One class serves every one of them. They differ only in registry name and bounding box —
 * the shape and the markings live in the model and the texture, which
 * {@code gen_work_zone_devices.py} generates along with the blockstate that pairs them. A class
 * per device would be a class per line of difference.</p>
 *
 * <p>Extends {@link AbstractBlockRoadSurface}, so the device settles onto the surface below it:
 * a cone on a road that climbs in sixteenths sits on the road rather than floating above it.</p>
 *
 * @version 1.0
 * @see AbstractBlockRoadSurface
 * @since 2026.9
 */
public class BlockWorkZoneDevice extends AbstractBlockRoadSurface
    implements ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored {

  /**
   * Carries the registry name to the superclass constructor.
   *
   * <p>{@code AbstractBlock}'s constructor calls {@link #getBlockRegistryName()} before this
   * class's fields are assigned, so the name cannot simply be read from a field there. This is
   * the same device the other factory blocks in this package use.
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
   * Constructs a {@link BlockWorkZoneDevice} instance.
   *
   * @param registryName the registry name of the device
   * @param boundingBox  the bounding box of the device, in block space
   *
   * @since 1.0
   */
  public BlockWorkZoneDevice(String registryName, AxisAlignedBB boundingBox) {
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
