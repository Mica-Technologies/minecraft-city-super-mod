package com.micatechnologies.minecraft.csm.venues.lines;

/**
 * A family of layouts with its own anchor and painted cell: the courts (acrylic), the track
 * (polyurethane) and the pool floor (tile). A painted cell only reads anchors of its own family.
 *
 * @since 2026.10
 */
public final class LineArtFamily {

  /** Where the sprites the materials name live (but the turf and the family's surface). */
  static final String SPRITES = "csm:blocks/venues/courts/";
  /** The grass sprite: the football field's light turf, so painted grass matches it. */
  static final String TURF = "csm:blocks/venues/football/turf";

  /** Tennis, pickleball, basketball, volleyball and sand volleyball. */
  public static final LineArtFamily COURTS = new LineArtFamily(0, "courts", CourtLayouts.LAYOUTS,
      SPRITES + "acrylic");
  /** The 400 m track, the jumps and the pole vault. */
  public static final LineArtFamily TRACK = new LineArtFamily(1, "track", TrackLayouts.LAYOUTS,
      "csm:blocks/venues/track/polyurethane");

  /** The 25 m and 50 m pools' floors: lane lines and their T ends on tile. */
  public static final LineArtFamily POOL = new LineArtFamily(2, "pool", PoolLayouts.LAYOUTS,
      "csm:blocks/venues/pool/pool_tile");

  /** Every family, by index. */
  static final LineArtFamily[] ALL = {COURTS, TRACK, POOL};

  /** How far (each way, in cells) a painted cell looks for its anchor. */
  public static final int REACH = 12;

  /** Its index, in a {@link LineArtKey}. */
  final int id;
  /** Its name. */
  public final String name;
  private final LineArtLayout[] layouts;
  /** Its surface sprite, the {@code SURF} materials'. */
  final String surfaceSprite;

  private LineArtFamily(int id, String name, LineArtLayout[] layouts, String surfaceSprite) {
    this.id = id;
    this.name = name;
    this.layouts = layouts;
    this.surfaceSprite = surfaceSprite;
  }

  /** The family with an index. */
  static LineArtFamily byId(int id) {
    return id >= 0 && id < ALL.length ? ALL[id] : COURTS;
  }

  /** How many layouts. */
  public int layoutCount() {
    return layouts.length;
  }

  /** A layout by index, clamped. */
  public LineArtLayout layout(int index) {
    return layouts[Math.max(0, Math.min(layouts.length - 1, index))];
  }

  /** The texture a material's sprite key names in this family. */
  public String sprite(String key) {
    if ("SURF".equals(key)) {
      return surfaceSprite;
    }
    if ("turf".equals(key)) {
      return TURF;
    }
    return SPRITES + key;
  }
}
