package com.micatechnologies.minecraft.csm.materials;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * English display names for the mod's blocks, read straight from the shipped lang file.
 *
 * <p>Fabricator costs need to know what a block physically <i>is</i>, and for much of the mod the
 * registry name cannot say: the lighting subsystem uses abbreviations such as {@code novtm},
 * {@code rcpb} and {@code ocpm}, whose names contain no hint that they are a tapered mast and two
 * concrete poles. The display names do — "NOV Tapered Mast", "NOV Round Concrete Pole" — so the
 * cost rules key off those instead.</p>
 *
 * <p>The lang file is read as a classpath resource rather than through {@code I18n}, because the
 * cost must be computed identically on the client and on a dedicated server, and the client-side
 * translation machinery is not available to common code. Reading the mod's own bundled resource is
 * deterministic on both sides and independent of the player's chosen language, which matters: a
 * recipe must not change because someone switched the game to French.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public final class CsmBlockDisplayNames {

  /** The shipped English lang file, relative to the classpath root. */
  private static final String LANG_RESOURCE = "assets/csm/lang/en_us.lang";

  /** Registry name to normalized display name (lower case, parentheticals removed). */
  private static final Map<String, String> DISPLAY_NAMES = load();

  private CsmBlockDisplayNames() {
    throw new UnsupportedOperationException("CsmBlockDisplayNames is a utility class.");
  }

  private static Map<String, String> load() {
    // Every module jar carries its own assets/csm/lang/en_us.lang for the blocks it ships, and
    // Core's copy names only the parts and the Fabricator. A single getResourceAsStream would
    // return whichever jar the class loader happens to reach first, so the names are gathered
    // from every copy on the class path — the same way the game's own locale loader merges them.
    Map<String, String> map = new HashMap<>();
    Enumeration<URL> copies;
    try {
      copies = CsmBlockDisplayNames.class.getClassLoader().getResources(LANG_RESOURCE);
    } catch (IOException e) {
      // Costs fall back to registry-name matching; not worth failing mod load over.
      return Collections.emptyMap();
    }
    while (copies.hasMoreElements()) {
      URL copy = copies.nextElement();
      try (InputStream stream = copy.openStream();
          BufferedReader reader =
              new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.startsWith("tile.")) {
            continue;
          }
          int equals = line.indexOf('=');
          if (equals < 0) {
            continue;
          }
          String key = line.substring("tile.".length(), equals);
          if (!key.endsWith(".name")) {
            continue;
          }
          String registryName = key.substring(0, key.length() - ".name".length());
          map.put(registryName, normalize(line.substring(equals + 1)));
        }
      } catch (IOException e) {
        // One unreadable copy should not cost the others; the affected blocks fall back to
        // registry-name matching.
      }
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * Lower-cases a display name and strips parenthetical qualifiers, so "NOV Round Concrete Pole
   * (Base 1)" reduces to "nov round concrete pole". Without this the trailing qualifier would be
   * mistaken for the noun that names the thing.
   */
  private static String normalize(String display) {
    StringBuilder builder = new StringBuilder(display.length());
    int depth = 0;
    for (int i = 0; i < display.length(); i++) {
      char c = display.charAt(i);
      if (c == '(' || c == '[') {
        depth++;
      } else if (c == ')' || c == ']') {
        depth = Math.max(0, depth - 1);
      } else if (depth == 0) {
        builder.append(c);
      }
    }
    return builder.toString().trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Returns the normalized display name for a registry name, or {@code null} if the block has no
   * lang entry.
   *
   * @since 2026.7
   */
  @Nullable
  public static String get(String registryName) {
    return registryName == null ? null : DISPLAY_NAMES.get(registryName);
  }

  /**
   * Returns the last alphabetic word of a block's display name, which in this mod's naming is
   * reliably the noun that says what the block is — "NOV Tapered Mast" gives {@code mast},
   * "Post Light" gives {@code light}, "Christmas Tree" gives {@code tree}.
   *
   * @return the last word, or an empty string if unknown
   *
   * @since 2026.7
   */
  public static String lastWord(String registryName) {
    String display = get(registryName);
    if (display == null || display.isEmpty()) {
      return "";
    }
    int end = display.length();
    while (end > 0 && !isLetter(display.charAt(end - 1))) {
      end--;
    }
    int start = end;
    while (start > 0 && isLetter(display.charAt(start - 1))) {
      start--;
    }
    return display.substring(start, end);
  }

  /**
   * Whether the block's display name contains the given whole word.
   *
   * <p>Whole-word matching rather than substring is deliberate: it is what keeps "alarm" from
   * matching "arm", and "Christmas" from matching "mast".</p>
   *
   * @since 2026.7
   */
  public static boolean hasWord(String registryName, String word) {
    String display = get(registryName);
    if (display == null || word == null || word.isEmpty()) {
      return false;
    }
    int from = 0;
    while (true) {
      int at = display.indexOf(word, from);
      if (at < 0) {
        return false;
      }
      boolean leftFree = at == 0 || !isLetter(display.charAt(at - 1));
      int after = at + word.length();
      boolean rightFree = after >= display.length() || !isLetter(display.charAt(after));
      if (leftFree && rightFree) {
        return true;
      }
      from = at + 1;
    }
  }

  private static boolean isLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }
}
