package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
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
 * A brick trim block: a soldier course, a header course or a course with weep holes, in one of the
 * brick colours.
 *
 * <p>Separate blocks rather than a state on the brick set. A state is free to render and not free
 * to obtain: the Fabricator enumerates blocks, not metadata variants, so a trim carried as a state
 * would be creative-only, and a set's stairs, slab and fence could not carry it anyway. Every trim
 * is a plain full cube and differs from the next only in its registry name and its texture, so
 * there is one class, constructed with the name, rather than a class per block.</p>
 *
 * <p>Each trim keeps the plain wall's courses where it has them, in the same positions, so it lays
 * into a brick wall in bond. Stacked, the header block is common bond.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockBrickTrim extends AbstractBlock {

  /**
   * The name being constructed. {@link AbstractBlock}'s constructor asks for the registry name
   * before this class's fields are assigned, so the name is handed across on the thread instead.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockBrickTrim}.
   *
   * @param registryName the registry name, such as {@code brick_red_soldier}
   *
   * @since 1.0
   */
  public BlockBrickTrim(String registryName) {
    super(pendingMaterial(registryName), SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
