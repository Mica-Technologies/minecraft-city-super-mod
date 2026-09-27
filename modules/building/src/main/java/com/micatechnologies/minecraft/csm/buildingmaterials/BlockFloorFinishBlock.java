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
 * A floor finish as a full block: carpet tile, vinyl composition tile, ceramic tile or studded
 * rubber built up to a whole block, so furniture and anything else stands on it at the normal
 * height. One class, constructed by registry name ({@code floor_<material>_<colour>_block}), the
 * name of the {@link BlockFloorFinish} overlay it is a block of plus {@code _block}.
 *
 * <p>No state at all: the blockstate picks the overlay's turns and second drawing by block
 * position, so a floor of it shows no repeat. Hardwood and polished concrete have full-block sets
 * instead. The models come from {@code dev-env-utils/scripts/gen_flooring.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockFloorFinishBlock extends AbstractBlock {

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockFloorFinishBlock}.
   *
   * @param registryName {@code floor_<material>_<colour>_block}
   *
   * @since 1.0
   */
  public BlockFloorFinishBlock(String registryName) {
    super(pendingMaterial(registryName), soft(registryName) ? SoundType.CLOTH : SoundType.STONE,
        soft(registryName) ? null : "pickaxe", 0, soft(registryName) ? 0.8F : 1.5F,
        soft(registryName) ? 4F : 10F, 0F, 255);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
  }

  /** Carpet and rubber: soft underfoot, cloth to the game, taken up by hand like wool. */
  private static boolean soft(String registryName) {
    return registryName.contains("carpet") || registryName.contains("rubber");
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return soft(registryName) ? Material.CLOTH : Material.ROCK;
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
