package com.micatechnologies.minecraft.csm.parks.planting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;

/**
 * A {@code /csmtree} request: the trees to plant and the rules they share, parsed from the
 * command's compact form or from JSON. Nothing here touches a world, so it is tested offline.
 *
 * <p>Compact form, after {@code plant}: a preset, then the spots, {@code x,y,z[,heading][,seed]}
 * separated by {@code ;}, then options: {@code clear=pattern:height,...}, {@code skip=pattern,...},
 * {@code seed=n}, {@code heading=h}, {@code dry}, {@code cells}. JSON form, after {@code plantjson}:
 * the same options as keys ({@code clear} and {@code skip} as a string or an array), with
 * {@code trees} an array of {@code {"x","y","z"[,"preset"][,"heading"][,"seed"]}}.</p>
 *
 * <p>A heading is a compass point ({@code n}, {@code ne}, ... {@code nw}), a Minecraft yaw in
 * degrees (0 south, 90 west), {@code @x:z} to lean toward a point, or {@code -} for a heading drawn
 * from the tree's seed.</p>
 *
 * @since 2026.10
 */
public final class TreeBatch {

  /** One tree. Its position is the trunk's base cell, or the ground block it stands on. */
  public static final class Spot {

    public final BlockPos pos;
    public final TreePreset preset;
    @Nullable
    public final String heading;
    @Nullable
    public final Long seed;

    Spot(BlockPos pos, TreePreset preset, @Nullable String heading, @Nullable Long seed) {
      this.pos = pos;
      this.preset = preset;
      this.heading = heading;
      this.seed = seed;
    }
  }

  /** Reads a coordinate, which may be relative ({@code ~}, {@code ~3}) to the sender. */
  public interface Coordinates {

    int coordinate(int axis, String text);
  }

  public final List<Spot> spots = new ArrayList<>();
  public TreeClearance clearance = TreeClearance.NONE;
  @Nullable
  public Long seed;
  @Nullable
  public String heading;
  public boolean dry;
  public boolean cells;

  private TreeBatch() {
  }

  /** The most trees one command may plant, so one call cannot stall the server for long. */
  public static final int MAX_TREES = 256;

  /**
   * Parses the compact form.
   *
   * @param args   the arguments after {@code plant}
   * @param coords how coordinates are read
   *
   * @return the request
   *
   * @throws IllegalArgumentException with a message for the sender, on anything malformed
   */
  public static TreeBatch parseCompact(String[] args, Coordinates coords) {
    if (args.length < 2) {
      throw new IllegalArgumentException("Usage: plant <preset> <x,y,z[,heading][,seed]>;... "
          + "[clear=pattern:height,...] [skip=pattern,...] [seed=n] [heading=h] [dry] [cells]");
    }
    TreeBatch batch = new TreeBatch();
    TreePreset preset = preset(args[0]);
    String clear = null;
    String skip = null;
    StringBuilder spots = new StringBuilder();
    for (int i = 1; i < args.length; i++) {
      String a = args[i];
      String lower = a.toLowerCase(Locale.ROOT);
      if (lower.startsWith("clear=")) {
        clear = a.substring(6);
      } else if (lower.startsWith("skip=")) {
        skip = a.substring(5);
      } else if (lower.startsWith("seed=")) {
        batch.seed = parseSeed(a.substring(5));
      } else if (lower.startsWith("heading=")) {
        batch.heading = checkHeading(a.substring(8));
      } else if (lower.equals("dry")) {
        batch.dry = true;
      } else if (lower.equals("cells")) {
        batch.cells = true;
      } else {
        spots.append(a); // spots may arrive split at spaces after a ';'
      }
    }
    for (String spot : spots.toString().split(";")) {
      if (spot.trim().isEmpty()) {
        continue;
      }
      String[] f = spot.trim().split(",");
      if (f.length < 3 || f.length > 5) {
        throw new IllegalArgumentException("Spot '" + spot + "' must be x,y,z[,heading][,seed]");
      }
      BlockPos pos = new BlockPos(coords.coordinate(0, f[0]), coords.coordinate(1, f[1]),
          coords.coordinate(2, f[2]));
      String heading = f.length > 3 && !f[3].isEmpty() ? checkHeading(f[3]) : null;
      Long seed = f.length > 4 && !f[4].isEmpty() ? parseSeed(f[4]) : null;
      batch.add(new Spot(pos, preset, heading, seed));
    }
    batch.clearance = TreeClearance.parse(clear, skip);
    return batch.checked();
  }

  /**
   * Parses the JSON form.
   *
   * @param json the JSON object
   *
   * @return the request
   *
   * @throws IllegalArgumentException with a message for the sender, on anything malformed
   */
  public static TreeBatch parseJson(String json) {
    JsonObject o;
    try {
      o = new JsonParser().parse(json).getAsJsonObject();
    } catch (JsonParseException | IllegalStateException e) {
      throw new IllegalArgumentException("Not a JSON object: " + e.getMessage());
    }
    TreeBatch batch = new TreeBatch();
    TreePreset preset = o.has("preset") ? preset(string(o.get("preset"))) : null;
    if (o.has("seed")) {
      batch.seed = parseSeed(string(o.get("seed")));
    }
    if (o.has("heading")) {
      batch.heading = checkHeading(string(o.get("heading")));
    }
    batch.dry = o.has("dry") && o.get("dry").getAsBoolean();
    batch.cells = o.has("cells") && o.get("cells").getAsBoolean();
    batch.clearance = TreeClearance.parse(list(o.get("clear")), list(o.get("skip")));
    if (!o.has("trees") || !o.get("trees").isJsonArray()) {
      throw new IllegalArgumentException("JSON needs a \"trees\" array");
    }
    for (JsonElement e : o.getAsJsonArray("trees")) {
      if (!e.isJsonObject()) {
        throw new IllegalArgumentException("Each tree must be an object with x, y and z");
      }
      JsonObject t = e.getAsJsonObject();
      if (!t.has("x") || !t.has("y") || !t.has("z")) {
        throw new IllegalArgumentException("Tree " + t + " needs x, y and z");
      }
      TreePreset p = t.has("preset") ? preset(string(t.get("preset"))) : preset;
      if (p == null) {
        throw new IllegalArgumentException("Tree " + t + " has no preset, and there is no default");
      }
      BlockPos pos = new BlockPos(t.get("x").getAsInt(), t.get("y").getAsInt(),
          t.get("z").getAsInt());
      String heading = t.has("heading") ? checkHeading(string(t.get("heading"))) : null;
      Long seed = t.has("seed") ? parseSeed(string(t.get("seed"))) : null;
      batch.add(new Spot(pos, p, heading, seed));
    }
    return batch.checked();
  }

  private void add(Spot spot) {
    spots.add(spot);
    if (spots.size() > MAX_TREES) {
      throw new IllegalArgumentException("At most " + MAX_TREES + " trees a command");
    }
  }

  private TreeBatch checked() {
    if (spots.isEmpty()) {
      throw new IllegalArgumentException("No trees to plant");
    }
    return this;
  }

  private static String string(JsonElement e) {
    return e.getAsString();
  }

  @Nullable
  private static String list(@Nullable JsonElement e) {
    if (e == null || e.isJsonNull()) {
      return null;
    }
    if (e.isJsonArray()) {
      StringBuilder sb = new StringBuilder();
      for (JsonElement item : (JsonArray) e) {
        sb.append(sb.length() > 0 ? "," : "").append(item.getAsString());
      }
      return sb.toString();
    }
    return e.getAsString();
  }

  private static long parseSeed(String s) {
    try {
      return Long.parseLong(s.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Seed '" + s + "' is not a whole number");
    }
  }

  /**
   * A preset by its id ({@code cherryyoshino}), enum name ({@code CHERRY_YOSHINO}) or ordinal,
   * ignoring case and underscores.
   */
  public static TreePreset preset(String name) {
    String key = name.trim().toLowerCase(Locale.ROOT).replace("_", "");
    for (TreePreset p : TreePreset.values()) {
      if (p.id.equals(key) || p.name().toLowerCase(Locale.ROOT).replace("_", "").equals(key)) {
        return p;
      }
    }
    try {
      int i = Integer.parseInt(key);
      if (i >= 0 && i < TreePreset.values().length) {
        return TreePreset.values()[i];
      }
    } catch (NumberFormatException ignored) {
      // not an ordinal
    }
    throw new IllegalArgumentException("Unknown tree preset '" + name
        + "' (/csmtree presets lists them)");
  }

  private static String checkHeading(String spec) {
    heading(spec, BlockPos.ORIGIN); // throws on a malformed one
    return spec.trim();
  }

  /**
   * A heading in radians, 0 toward +X (east) and {@code PI / 2} toward +Z (south), as
   * {@link TreeGenerators#grow} takes it.
   *
   * @param spec the heading as written
   * @param at   where the tree stands, for a heading toward a point
   *
   * @return the heading, or null where it is to be drawn from the tree's seed
   */
  @Nullable
  public static Double heading(@Nullable String spec, BlockPos at) {
    if (spec == null) {
      return null;
    }
    String s = spec.trim().toLowerCase(Locale.ROOT);
    switch (s) {
      case "":
      case "-":
      case "random":
        return null;
      case "e":
        return 0.0;
      case "se":
        return Math.PI / 4;
      case "s":
        return Math.PI / 2;
      case "sw":
        return 3 * Math.PI / 4;
      case "w":
        return Math.PI;
      case "nw":
        return -3 * Math.PI / 4;
      case "n":
        return -Math.PI / 2;
      case "ne":
        return -Math.PI / 4;
      default:
        break;
    }
    try {
      if (s.startsWith("@")) {
        String[] xz = s.substring(1).split(":");
        if (xz.length != 2) {
          throw new IllegalArgumentException("Heading '" + spec + "' must be @x:z");
        }
        double dx = Double.parseDouble(xz[0]) - at.getX();
        double dz = Double.parseDouble(xz[1]) - at.getZ();
        return dx == 0 && dz == 0 ? null : Math.atan2(dz, dx);
      }
      return TreeGenerators.headingOfYaw((float) Double.parseDouble(s));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Heading '" + spec
          + "' must be a compass point, a yaw in degrees, @x:z or -");
    }
  }

  /** The Minecraft yaw, in whole degrees, of a heading: the inverse of {@code headingOfYaw}. */
  public static int yawOf(double heading) {
    double yaw = Math.toDegrees(Math.atan2(-Math.cos(heading), Math.sin(heading)));
    return (int) Math.round((yaw % 360 + 360) % 360);
  }

  /**
   * The seed of a tree with no seed of its own, from the request's seed and where it stands, so a
   * re-run plants the same tree there whatever order the trees come in.
   */
  public static long seedAt(long seed, BlockPos pos) {
    long h = seed ^ pos.toLong() * 0x9E3779B97F4A7C15L;
    h ^= h >>> 33;
    h *= 0xFF51AFD7ED558CCDL;
    h ^= h >>> 33;
    return h;
  }
}
