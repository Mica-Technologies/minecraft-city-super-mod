package com.micatechnologies.minecraft.csm.parks.amenities;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockSlab;
import net.minecraft.block.SoundType;

/**
 * A slab of the full-block rubber safety surface ({@link BlockRubberSafetySurface}, #365), for the
 * edge of a play area or a step down to the path, one per colour
 * ({@code ground_rubber_safety_<colour>_slab}). Top and bottom halves place as any slab does, and
 * two make {@code <name>_double}, which {@link AbstractBlockSlab} creates.
 *
 * @since 2026.11
 */
public class BlockRubberSafetySurfaceSlab extends AbstractBlockSlab {

  /**
   * The registry name being constructed. The slab's base constructor asks for it before this
   * class's field is set, so the constructor parks it here first.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs one colour of the slab.
   *
   * @param registryName {@code ground_rubber_safety_<colour>_slab}
   */
  public BlockRubberSafetySurfaceSlab(String registryName) {
    super(BlockRubberSafetySurface.MATERIAL, pending(registryName), null, 0,
        BlockRubberSafetySurface.HARDNESS, BlockRubberSafetySurface.RESISTANCE, 0F, 255);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
  }

  private static SoundType pending(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return BlockRubberSafetySurface.SOUND;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }
}
