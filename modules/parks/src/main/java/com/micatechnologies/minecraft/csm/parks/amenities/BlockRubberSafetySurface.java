package com.micatechnologies.minecraft.csm.parks.amenities;

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
 * The playground rubber safety surface as a whole block (#365), one block per colour
 * ({@code ground_rubber_safety_<colour>_full}). Laid in the ground layer in place of the soil, so
 * a swing, seesaw, spring rider or slide placed on it stands at the normal height, where on the
 * thin {@code ground_rubber_safety} mat it stands a block up. Each colour has the texture of the
 * thin mat of that colour; {@link BlockRubberSafetySurfaceSlab} and
 * {@link BlockRubberSafetySurfaceStairs} make its edges and ramps.
 *
 * <p>Rubber to the game is cloth, as for the studded rubber floor block: soft underfoot, taken up
 * by hand like wool. No state at all.</p>
 *
 * @since 2026.11
 */
public class BlockRubberSafetySurface extends AbstractBlock {

  /** Rubber is cloth to the game, as the studded rubber floor block is. */
  static final Material MATERIAL = Material.CLOTH;
  /** Soft underfoot. */
  static final SoundType SOUND = SoundType.CLOTH;
  /** Wool's hardness: taken up by hand. */
  static final float HARDNESS = 0.8F;
  /** Wool's blast resistance. */
  static final float RESISTANCE = 4F;

  /**
   * The registry name being constructed. The base constructor asks for it before this class's
   * field is set, so the constructor parks it here first.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs one colour of the full-block surface.
   *
   * @param registryName {@code ground_rubber_safety_<colour>_full}
   */
  public BlockRubberSafetySurface(String registryName) {
    super(pending(registryName), SOUND, null, 0, HARDNESS, RESISTANCE, 0F, 255);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pending(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return MATERIAL;
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
