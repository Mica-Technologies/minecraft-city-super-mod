package com.micatechnologies.minecraft.csm.codeutils;

/**
 * A block with see-through glass in front of drawn parts: a display case and the food on its
 * shelves, a glazed panel and its frame. Such a block is drawn in two passes, only its glass in
 * the translucent pass and everything else in the cutout pass ({@link CsmGlassLayer},
 * {@link AbstractBlock#canRenderInLayer}, split by {@code CsmGlassLayerModel}).
 *
 * <p>Drawn whole in the translucent pass, its solid parts were lost from some angles: that pass
 * writes no depth and orders its faces by the distance of each face's centre, so a large face
 * behind the contents (a case's liner, its back wall) whose centre sorted nearer was painted
 * over the food in front of it. In the cutout pass the contents write depth like any block, and
 * the glass drawn after them is sorted only among itself.</p>
 *
 * <p>Which faces are glass is decided by their texture's name ({@link CsmGlassLayer#isGlass}).
 * Keep {@link #getBlockRenderLayer} returning the translucent layer: it is what other code asks
 * (a custom door made of the block takes its layer from it).</p>
 *
 * @since 2026.10
 */
public interface ICsmGlassFronted {

  /**
   * Whether this block draws its glass apart. A class with both glazed and opaque instances (a
   * shelter style, a sheer or a blackout curtain) answers for each instance.
   *
   * @return true to draw the glass in the translucent pass and the rest in the cutout pass
   */
  default boolean isGlassFronted() {
    return true;
  }
}
