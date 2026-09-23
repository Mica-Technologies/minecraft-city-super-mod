package com.micatechnologies.minecraft.csm.streetscape;

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
 * A cover set flush in the road or sidewalk: a manhole, a utility vault lid, a valve box, a
 * drainage grate, a storm drain marker.
 *
 * <p>One class serves every one of them; they differ only in registry name and footprint. The
 * cover itself is a single upward face a fraction of a pixel above the surface, with a cutout
 * texture, so a round cover is drawn round rather than as a polygon. {@code
 * gen_streetscape_covers.py} generates the textures, models and blockstates.</p>
 *
 * <p>It settles onto the surface below it like the work zone devices, and it turns four ways so
 * that a cast legend reads the right way up for the player who placed it. Its box is a sliver,
 * so it is walked straight over.</p>
 *
 * @version 1.0
 */
public class BlockStreetCover extends AbstractBlockRoadSurfaceRotatableNSEW
    implements ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored {

  /**
   * Carries the registry name to the superclass constructor, which asks for it before this
   * class's fields are assigned (the same device {@code BlockWorkZoneDevice} uses).
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  /**
   * The registry name of this cover.
   */
  private final String registryName;

  /**
   * The footprint of this cover facing north, before it settles onto the surface below it.
   */
  private final AxisAlignedBB boundingBox;

  /**
   * Constructs a {@link BlockStreetCover} instance.
   *
   * @param registryName the registry name of the cover
   * @param boundingBox  the footprint of the cover facing north, in block space
   */
  public BlockStreetCover(String registryName, AxisAlignedBB boundingBox) {
    super(initRegistryName(registryName), SoundType.METAL, "pickaxe", 0, 1.5F, 10F, 0F, 0);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
  }

  /**
   * Stashes the registry name for {@link #getBlockRegistryName()} and returns the material to
   * hand to the superclass constructor.
   *
   * @param name the registry name of the cover
   *
   * @return the material of the cover
   */
  private static Material initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.IRON;
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
