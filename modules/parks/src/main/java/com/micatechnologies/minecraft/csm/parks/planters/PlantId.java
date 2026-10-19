package com.micatechnologies.minecraft.csm.parks.planters;

import java.util.Locale;
import java.util.Objects;

/**
 * A plant as text: a registry name and the plant block's metadata, as the planter's tile entity
 * stores it ({@code plant} and {@code variant}) and as {@code /csmparks planter fill} takes it.
 *
 * <p>The text form is {@code [namespace:]path[:meta]}: {@code csm:shrub_lavender},
 * {@code shrub_lavender} (a bare name is CSM's), {@code minecraft:red_flower:3} (an azure
 * bluet). Pure, so the parsing is unit tested.</p>
 *
 * @since 2026.10
 */
public final class PlantId {

  /** The namespace a bare name is read in. */
  public static final String DEFAULT_NAMESPACE = "csm";

  private final String namespace;
  private final String path;
  private final int meta;

  public PlantId(String namespace, String path, int meta) {
    this.namespace = namespace;
    this.path = path;
    this.meta = clampMeta(meta);
  }

  /** Block metadata runs 0 to 15. */
  public static int clampMeta(int meta) {
    return Math.max(0, Math.min(15, meta));
  }

  /**
   * Reads a plant's text form.
   *
   * @param text {@code [namespace:]path[:meta]}
   * @return the plant
   * @throws IllegalArgumentException if it is not that
   */
  public static PlantId parse(String text) {
    if (text == null || text.trim().isEmpty()) {
      throw new IllegalArgumentException("No plant named");
    }
    String[] parts = text.trim().toLowerCase(Locale.ROOT).split(":", -1);
    String namespace = DEFAULT_NAMESPACE;
    String path;
    int meta = 0;
    switch (parts.length) {
      case 1:
        path = parts[0];
        break;
      case 2:
        if (isNumber(parts[1])) {
          path = parts[0];
          meta = Integer.parseInt(parts[1]);
        } else {
          namespace = parts[0];
          path = parts[1];
        }
        break;
      case 3:
        if (!isNumber(parts[2])) {
          throw new IllegalArgumentException("'" + text + "': the variant must be a number");
        }
        namespace = parts[0];
        path = parts[1];
        meta = Integer.parseInt(parts[2]);
        break;
      default:
        throw new IllegalArgumentException("'" + text + "' is not namespace:name[:variant]");
    }
    if (namespace.isEmpty() || path.isEmpty() || !valid(namespace) || !valid(path)) {
      throw new IllegalArgumentException("'" + text + "' is not a registry name");
    }
    if (meta > 15) {
      throw new IllegalArgumentException("'" + text + "': the variant runs 0 to 15");
    }
    return new PlantId(namespace, path, meta);
  }

  private static boolean isNumber(String s) {
    if (s.isEmpty() || s.length() > 3) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean valid(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-'
          || c == '/')) {
        return false;
      }
    }
    return true;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getPath() {
    return path;
  }

  public int getMeta() {
    return meta;
  }

  /** {@code namespace:path}, the registry name. */
  public String getName() {
    return namespace + ":" + path;
  }

  /** {@code namespace:path}, with {@code :meta} when the meta is not 0. */
  @Override
  public String toString() {
    return meta == 0 ? getName() : getName() + ":" + meta;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PlantId)) {
      return false;
    }
    PlantId p = (PlantId) o;
    return meta == p.meta && namespace.equals(p.namespace) && path.equals(p.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, path, meta);
  }
}
