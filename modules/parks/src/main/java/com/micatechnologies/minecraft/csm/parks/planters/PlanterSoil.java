package com.micatechnologies.minecraft.csm.parks.planters;

import javax.annotation.Nullable;

/**
 * Where a planter's soil is and how big a plant planted in it is drawn: the soil surface's
 * height, the scale a plant is drawn at (a full-size shrub in a small pot looks absurd), and a
 * texture of the planter's own model to leave out while something is planted (the concrete
 * bowl's bedding flowers).
 *
 * <p>Pure numbers, measured off each planter's model by {@code gen_park_plantings.py}, which
 * writes them into the tab line that constructs the block.</p>
 *
 * @since 2026.10
 */
public final class PlanterSoil {

  /**
   * The highest a planted plant may reach, in sixteenths above the planter's own block: the top
   * of the block above it. A plant is scaled down further to stay under it, so a sunflower in a
   * tall planter does not stand three blocks high, and no quad reaches past the chunk section
   * the frustum test puts the planter in by more than a block.
   */
  public static final float MAX_TOP = 32.0F;

  private final float soil;
  private final float scale;
  @Nullable
  private final String hiddenTexture;

  /**
   * @param soil          the soil surface, in sixteenths above the block's bottom
   * @param scale         the scale a plant is drawn at, about the middle of the block
   * @param hiddenTexture a texture name (its last path part) of the planter's model left out
   *                      while something is planted, or null
   */
  public PlanterSoil(float soil, float scale, @Nullable String hiddenTexture) {
    this.soil = soil;
    this.scale = scale;
    this.hiddenTexture = hiddenTexture;
  }

  /** The soil surface, in sixteenths. */
  public float getSoil() {
    return soil;
  }

  /** The scale a plant is drawn at, before the height cap. */
  public float getScale() {
    return scale;
  }

  /** The texture left out while planted, or null. */
  @Nullable
  public String getHiddenTexture() {
    return hiddenTexture;
  }

  /**
   * The scale a plant this tall is drawn at here: this planter's scale, or less if the plant
   * would otherwise reach past {@link #MAX_TOP}.
   *
   * @param plantHeight the plant model's height, in sixteenths
   * @return the scale
   */
  public float scaleFor(float plantHeight) {
    if (plantHeight <= 0) {
      return scale;
    }
    return Math.min(scale, (MAX_TOP - soil) / plantHeight);
  }

  /**
   * Whether a sprite is the one left out while planted.
   *
   * @param spriteName the sprite's name, as {@code csm:blocks/parks/landscape/mixed_basket}
   * @return whether it is
   */
  public boolean hides(String spriteName) {
    return hiddenTexture != null && (spriteName.endsWith("/" + hiddenTexture)
        || spriteName.endsWith(":" + hiddenTexture));
  }
}
