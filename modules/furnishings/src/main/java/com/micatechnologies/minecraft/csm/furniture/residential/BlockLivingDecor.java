package com.micatechnologies.minecraft.csm.furniture.residential;

import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;

/**
 * A decorative piece that does nothing when clicked, of any material and in any render layer,
 * facing whoever places it: wall art and photo frames, a hanging plant. The Residential
 * furniture classes it extends are wood or cloth, or take their material only from a subclass.
 *
 * @since 2026.9
 */
public class BlockLivingDecor extends BlockResidentialFurniture {

  private final BlockRenderLayer layer;

  /**
   * Constructs a decorative piece.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param material     its material
   * @param sound        its block sound
   * @param hardness     how long it takes to break
   * @param layer        its render layer (cutout for leaves or a round frame)
   */
  public BlockLivingDecor(String registryName, int[] box, Material material, SoundType sound,
      float hardness, BlockRenderLayer layer) {
    super(registryName, box, material, sound, hardness);
    this.layer = layer;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return layer;
  }
}
