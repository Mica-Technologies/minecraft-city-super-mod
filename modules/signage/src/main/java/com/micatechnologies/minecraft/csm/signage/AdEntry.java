package com.micatechnologies.minecraft.csm.signage;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.util.ResourceLocation;

/**
 * One ad in the library: who it is for, what it is about, and which shapes it was drawn in.
 */
public final class AdEntry {

  private final String id;
  private final String source;
  private final String brand;
  private final String category;
  private final Set<AdShape> shapes;
  private final int background;

  AdEntry(String id, String source, String brand, String category, Set<AdShape> shapes,
      int background) {
    this.id = id;
    this.source = source;
    this.brand = brand;
    this.category = category;
    this.shapes = Collections.unmodifiableSet(EnumSet.copyOf(shapes));
    this.background = background;
  }

  /** The ad's id, unique across every source; what a board stores. */
  public String getId() {
    return id;
  }

  /** Where the ad came from: {@code parody} or {@code vintage}. */
  public String getSource() {
    return source;
  }

  /** The brand's name, for the ad picker. */
  public String getBrand() {
    return brand;
  }

  /** The category, for "random from a category"; {@code house} is the fallback ad only. */
  public String getCategory() {
    return category;
  }

  /** The shapes this ad was drawn in; never empty. */
  public Set<AdShape> getShapes() {
    return shapes;
  }

  /** The ad's background colour as {@code 0xRRGGBB}, for letterboxing. */
  public int getBackground() {
    return background;
  }

  /** The shape of this ad best suited to a face {@code width} by {@code height}. */
  public AdShape shapeFor(double width, double height) {
    return AdShape.nearest(width, height, shapes);
  }

  /** The texture of this ad in the given shape. */
  public ResourceLocation texture(AdShape shape) {
    return new ResourceLocation("csm",
        "textures/ads/" + source + "/" + id + "_" + shape.getName() + ".png");
  }
}
