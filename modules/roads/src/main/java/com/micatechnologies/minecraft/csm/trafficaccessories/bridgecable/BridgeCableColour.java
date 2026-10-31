package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * The named colours a bridge cable, its stripes, its hangers and its LED strip can take, and
 * {@code #RRGGBB} for anything else.
 *
 * <p>A colour is kept as the name it was given (so {@code /blockdata} shows what was typed and a
 * generator's output round-trips) and turned into RGB only when drawn.
 */
public final class BridgeCableColour {

  /** Name to 0xRRGGBB, in the order the docs list them. */
  private static final Map<String, Integer> NAMED;

  static {
    final Map<String, Integer> named = new LinkedHashMap<>();
    named.put("white", 0xEDEDE8);
    named.put("light_grey", 0xB3B5B8);
    named.put("galvanized", 0x8E9399);
    named.put("black", 0x1A1A1C);
    named.put("red", 0xB31F1A);
    // Strauss's International Orange as the Golden Gate is painted, not safety orange.
    named.put("international_orange", 0xC0362C);
    // A clear civic blue, the Alto bridges' colour.
    named.put("blue", 0x1F55A0);
    named.put("yellow", 0xE6BF26);
    named.put("green", 0x2E7D4F);
    NAMED = Collections.unmodifiableMap(named);
  }

  private BridgeCableColour() {
  }

  /** Every named colour, in the order the docs list them. */
  public static Map<String, Integer> named() {
    return NAMED;
  }

  /**
   * Normalises a colour as given in NBT: a known name (or an alias of one) in its canonical
   * spelling, or {@code #rrggbb}. Returns null when the colour cannot be read.
   */
  @Nullable
  public static String normalise(@Nullable String colour) {
    if (colour == null) {
      return null;
    }
    String name = colour.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    switch (name) {
      case "light_gray":
      case "lightgrey":
      case "lightgray":
      case "silver":
        name = "light_grey";
        break;
      case "grey":
      case "gray":
      case "steel":
      case "galvanised":
        name = "galvanized";
        break;
      case "orange":
      case "internationalorange":
        name = "international_orange";
        break;
      case "alto_blue":
        name = "blue";
        break;
      default:
        break;
    }
    if (NAMED.containsKey(name)) {
      return name;
    }
    if (name.startsWith("#")) {
      name = name.substring(1);
    } else if (name.startsWith("0x")) {
      name = name.substring(2);
    }
    if (name.length() == 6) {
      try {
        Integer.parseInt(name, 16);
        return "#" + name;
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  /** The colour as 0xRRGGBB; an unreadable colour draws galvanized rather than not at all. */
  public static int rgb(@Nullable String colour) {
    final String normal = normalise(colour);
    if (normal == null) {
      return NAMED.get("galvanized");
    }
    final Integer named = NAMED.get(normal);
    if (named != null) {
      return named;
    }
    return Integer.parseInt(normal.substring(1), 16);
  }
}
