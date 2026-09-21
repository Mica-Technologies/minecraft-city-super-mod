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
  /** A server ad's hash, as hex; null for an ad that ships in the jar. */
  private final String serverHex;

  AdEntry(String id, String source, String brand, String category, Set<AdShape> shapes,
      int background) {
    this(id, source, brand, category, shapes, background, null);
  }

  private AdEntry(String id, String source, String brand, String category, Set<AdShape> shapes,
      int background, String serverHex) {
    this.id = id;
    this.source = source;
    this.brand = brand;
    this.category = category;
    this.shapes = Collections.unmodifiableSet(EnumSet.copyOf(shapes));
    this.background = background;
    this.serverHex = serverHex;
  }

  /**
   * An ad a server supplies: one image, in whichever shape is nearest its own proportions, fitted
   * to a board like any other. Its texture exists only once the client has downloaded it; see
   * {@code ServerAdImages}.
   */
  static AdEntry server(ServerAds.Ad ad) {
    AdShape shape = AdShape.nearest(ad.width, ad.height, EnumSet.allOf(AdShape.class));
    return new AdEntry(ad.id, "server", ad.name, "server", EnumSet.of(shape), 0x101010,
        ad.hex());
  }

  /** Whether the ad comes from the server rather than the jar. */
  public boolean isServer() {
    return serverHex != null;
  }

  /** A server ad's hash, as hex, or null. */
  public String getServerHex() {
    return serverHex;
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

  /**
   * The texture of this ad in the given shape. A server ad's is the one texture its download was
   * registered under, whatever the shape; never bind it before it exists, or the texture manager
   * tries to load it from the resource packs and remembers the failure.
   */
  public ResourceLocation texture(AdShape shape) {
    if (serverHex != null) {
      return new ResourceLocation("csm", "server_ads/" + serverHex);
    }
    return new ResourceLocation("csm",
        "textures/ads/" + source + "/" + id + "_" + shape.getName() + ".png");
  }
}
