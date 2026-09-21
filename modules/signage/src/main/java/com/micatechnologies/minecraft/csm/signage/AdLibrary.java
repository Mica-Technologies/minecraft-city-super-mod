package com.micatechnologies.minecraft.csm.signage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Every ad the boards can show, read from the indexes the ad generators write into the module
 * jar ({@code assets/csm/ads/<source>.json}).
 *
 * <p>The indexes are read from the classpath rather than through the resource manager, so the
 * server knows the same ads the client does: a board's ad is chosen and validated on the server.
 * An index that is missing (a source with no ads yet) is skipped; one that is malformed is logged
 * and skipped, never a crash.</p>
 */
public final class AdLibrary {

  /** The sources, in the order the picker lists them. */
  static final String[] SOURCES = {"parody", "vintage"};

  /** The ad a board shows for an id it does not know. */
  public static final String HOUSE_AD = "your_ad_here";

  /** The category of the house ad, which "all" and "random" playlists leave out. */
  public static final String HOUSE_CATEGORY = "house";

  private static final Logger LOGGER = LogManager.getLogger("csm_signage");

  private static volatile AdLibrary instance;

  private final Map<String, AdEntry> ads;

  private AdLibrary(Map<String, AdEntry> ads) {
    this.ads = Collections.unmodifiableMap(ads);
  }

  /** The library, read on first use. */
  public static AdLibrary get() {
    AdLibrary library = instance;
    if (library == null) {
      synchronized (AdLibrary.class) {
        library = instance;
        if (library == null) {
          library = load();
          instance = library;
        }
      }
    }
    return library;
  }

  private static AdLibrary load() {
    Map<String, AdEntry> ads = new LinkedHashMap<>();
    for (String source : SOURCES) {
      String path = "/assets/csm/ads/" + source + ".json";
      try (InputStream in = AdLibrary.class.getResourceAsStream(path)) {
        if (in == null) {
          continue;
        }
        JsonObject index = new JsonParser().parse(
            new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        for (AdEntry entry : parse(source, index)) {
          if (ads.putIfAbsent(entry.getId(), entry) != null) {
            LOGGER.warn("Ad id {} appears in more than one index; keeping the first", entry.getId());
          }
        }
      } catch (IOException | RuntimeException e) {
        LOGGER.error("Could not read the ad index {}", path, e);
      }
    }
    return new AdLibrary(ads);
  }

  static List<AdEntry> parse(String source, JsonObject index) {
    List<AdEntry> out = new ArrayList<>();
    JsonArray array = index.getAsJsonArray("ads");
    if (array == null) {
      return out;
    }
    for (JsonElement element : array) {
      JsonObject ad = element.getAsJsonObject();
      Set<AdShape> shapes = EnumSet.noneOf(AdShape.class);
      for (JsonElement name : ad.getAsJsonArray("shapes")) {
        AdShape shape = AdShape.fromName(name.getAsString());
        if (shape != null) {
          shapes.add(shape);
        }
      }
      if (shapes.isEmpty()) {
        LOGGER.warn("Ad {} lists no shape this version knows; skipped", ad.get("id"));
        continue;
      }
      out.add(new AdEntry(ad.get("id").getAsString(), source, ad.get("brand").getAsString(),
          ad.get("category").getAsString(), shapes,
          parseColour(ad.has("background") ? ad.get("background").getAsString() : null)));
    }
    return out;
  }

  static int parseColour(@Nullable String hex) {
    if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) {
      return 0x000000;
    }
    return Integer.parseInt(hex.substring(1), 16);
  }

  /** Every ad, in index order, the house ad included. */
  public List<AdEntry> all() {
    return new ArrayList<>(ads.values());
  }

  /** The ad with the given id, or {@code null}. */
  @Nullable
  public AdEntry find(String id) {
    return ads.get(id);
  }

  /** The ad with the given id, or the house ad if there is none. */
  public AdEntry resolve(String id) {
    AdEntry entry = ads.get(id);
    return entry != null ? entry : ads.get(HOUSE_AD);
  }

  /** Every ad a playlist of "all" or "random" draws from: all but the house ad. */
  public List<AdEntry> rotation() {
    List<AdEntry> out = new ArrayList<>();
    for (AdEntry entry : ads.values()) {
      if (!HOUSE_CATEGORY.equals(entry.getCategory())) {
        out.add(entry);
      }
    }
    return out;
  }

  /** The ads in one category, in index order. */
  public List<AdEntry> inCategory(String category) {
    List<AdEntry> out = new ArrayList<>();
    for (AdEntry entry : ads.values()) {
      if (entry.getCategory().equals(category)) {
        out.add(entry);
      }
    }
    return out;
  }
}
