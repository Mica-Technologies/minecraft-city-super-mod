package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A Sports & Venues piece: one block turned to face the event floor, drawn from baked models
 * that {@code gen_venues_seating.py} writes. Its facing is stored in metadata bits 0-1 as
 * {@link AbstractBlockRotatableNSEW} stores it (S 0, W 1, N 2, E 3); a subclass keeps up to two
 * more bits for itself.
 *
 * <p>The registry name is passed in, so the creative tab's line names it, which is where the
 * reference catalogue reads it from.</p>
 *
 * @since 2026.10
 */
public abstract class BlockVenue extends AbstractBlockRotatableNSEW {

  /** The registry name, for the base constructor, which asks for it before fields are set. */
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a piece.
   *
   * @param registryName its registry name
   * @param material     its material
   * @param sound        its sound
   * @param hardness     its hardness
   */
  protected BlockVenue(String registryName, Material material, SoundType sound, float hardness) {
    super(stash(registryName, material), sound, "pickaxe", 0, hardness, hardness * 2.0F, 0.0F, 0,
        false);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Material stash(String registryName, Material material) {
    PENDING.set(registryName);
    return material;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
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
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
