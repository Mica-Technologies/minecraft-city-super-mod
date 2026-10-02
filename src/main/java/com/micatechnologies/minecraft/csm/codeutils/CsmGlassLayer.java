package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.BlockRenderLayer;

/**
 * Which pass draws each face of a glass-fronted block ({@link ICsmGlassFronted}): its glass in
 * the translucent pass, everything else in the cutout pass.
 *
 * <p><b>The rule:</b> contents behind glass go in the cutout layer, only the glass in the
 * translucent one. The translucent pass writes no depth and orders faces by the distance of
 * their centres, which suits panes of glass and nothing else; a solid part drawn there is
 * painted over by whatever face the sort puts after it, so the food in a display case vanished
 * from some angles behind the case's own liner. See "Glass and what is behind it" in
 * {@code PERFORMANCE_AND_SECURITY.md}.</p>
 *
 * <p>A face is glass by its texture's name, the last part of its path: {@code glass},
 * {@code glass_*}, {@code *_glass} or {@code *_glass_*} ({@code glass_clear}, {@code case_glass},
 * {@code water_glass}; not {@code fiberglass_*}), or one of the few see-through textures named
 * otherwise ({@link #OTHER_GLASS}). A texture with partly transparent pixels that is not glass
 * is drawn by alpha test in the cutout pass, which shows any pixel above a tenth opaque as fully
 * opaque, so such a block's other textures must be opaque or fully clear;
 * {@code CsmGlassLayerTest} holds every glass-fronted block's textures to that.</p>
 *
 * @since 2026.10
 */
public final class CsmGlassLayer {

  /**
   * See-through textures not named glass, by path within the {@code csm} textures folder: a
   * texture is glass if its path is one of these or one of these followed by {@code _} and more.
   */
  static final String[] OTHER_GLASS = {
      "blocks/constructionsite/trailer_window",   // the job trailer's frames and glass, one sheet
      "blocks/interior/curtain_sheer",
      "blocks/transit/platforms/bag",             // the platform litter bin's clear bag
  };

  private CsmGlassLayer() {
  }

  /**
   * Whether a glass-fronted block draws in {@code layer}: the cutout pass for its solid parts and
   * the translucent pass for its glass.
   *
   * @param layer the pass
   * @return whether the block has faces in it
   */
  public static boolean drawsIn(BlockRenderLayer layer) {
    return layer == BlockRenderLayer.CUTOUT_MIPPED || layer == BlockRenderLayer.TRANSLUCENT;
  }

  /**
   * Whether a texture is glass, drawn in the translucent pass.
   *
   * @param spriteName the sprite's name, as {@code csm:blocks/furniture/market/glass_clear}
   * @return whether it is glass
   */
  public static boolean isGlass(String spriteName) {
    String path = spriteName.substring(spriteName.indexOf(':') + 1);
    for (String other : OTHER_GLASS) {
      if (path.equals(other) || path.startsWith(other + "_")) {
        return true;
      }
    }
    String name = path.substring(path.lastIndexOf('/') + 1);
    return name.equals("glass") || name.startsWith("glass_") || name.endsWith("_glass")
        || name.contains("_glass_");
  }

  /**
   * Whether a face with this texture is drawn in {@code layer} by a glass-fronted block.
   *
   * @param spriteName the face's sprite name
   * @param layer      the pass being drawn
   * @return whether the face belongs to the pass
   */
  public static boolean belongsIn(String spriteName, BlockRenderLayer layer) {
    return isGlass(spriteName) == (layer == BlockRenderLayer.TRANSLUCENT);
  }
}
