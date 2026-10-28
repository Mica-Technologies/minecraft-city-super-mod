package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code config/csm/pawn_values.json}: the values a server sets by hand.
 *
 * <ul>
 *   <li>{@code raw} -- what the things no recipe makes are worth (logs, ores, mob drops). The
 *   recipe walk starts here.</li>
 *   <li>{@code overrides} -- any item's value, over whatever its recipes would make it.</li>
 *   <li>{@code blocked} -- items a pawn shop never takes.</li>
 * </ul>
 *
 * <p>Keys are {@code modid:item} for every subtype or {@code modid:item@meta} for one. Both
 * value tables are fixed points for the walk: a recipe never changes them. The file is written
 * with the defaults below the first time a server starts, and from then on is the server's own:
 * a later CSM version never rewrites it, so a new default appears only in a new file.
 * {@code /csmpawn set} and {@code /csmpawn block} rewrite it, without any comments added by
 * hand.</p>
 *
 * @since 2026.10
 */
public final class PawnValues {

  static final String FILE_NAME = "pawn_values.json";

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private final Map<String, Double> raw;
  private final Map<String, Double> overrides;
  private final Set<String> blocked;

  PawnValues(Map<String, Double> raw, Map<String, Double> overrides, Set<String> blocked) {
    this.raw = raw;
    this.overrides = overrides;
    this.blocked = blocked;
  }

  public Map<String, Double> getRaw() {
    return Collections.unmodifiableMap(raw);
  }

  public Map<String, Double> getOverrides() {
    return Collections.unmodifiableMap(overrides);
  }

  /** Whether {@code key} (a subtype key) or its item is blocked. */
  public boolean isBlocked(String key) {
    return blocked.contains(key) || blocked.contains(PawnValueSolver.item(key));
  }

  /** Every fixed value, overrides over raw. */
  Map<String, Double> fixed() {
    Map<String, Double> all = new LinkedHashMap<>(raw);
    all.putAll(overrides);
    return all;
  }

  void setOverride(String key, Double value) {
    if (value == null) {
      overrides.remove(key);
    } else {
      overrides.put(key, value);
    }
  }

  /** Blocks or unblocks; returns whether it is now blocked. */
  boolean toggleBlocked(String key) {
    if (blocked.remove(key)) {
      return false;
    }
    blocked.add(key);
    return true;
  }

  /** Reads the file, writing the defaults first if there is none. */
  static PawnValues load(File file) {
    if (!file.isFile()) {
      PawnValues defaults = defaults();
      defaults.save(file);
      return defaults;
    }
    try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
      return parse(reader);
    } catch (IOException | RuntimeException e) {
      CsmFurnishings.getLogger().error(
          "Could not read {}; pawn shops use the built-in values until it is fixed: {}",
          file, e.toString());
      return defaults();
    }
  }

  /** Parses the file's text. Lenient, so {@code //} and {@code #} comments are allowed. */
  static PawnValues parse(Reader reader) {
    JsonReader json = new JsonReader(reader);
    json.setLenient(true);
    JsonObject root = new JsonParser().parse(json).getAsJsonObject();
    Map<String, Double> raw = readValues(root, "raw");
    Map<String, Double> overrides = readValues(root, "overrides");
    Set<String> blocked = new LinkedHashSet<>();
    if (root.has("blocked")) {
      for (JsonElement e : root.getAsJsonArray("blocked")) {
        blocked.add(e.getAsString().trim());
      }
    }
    return new PawnValues(raw, overrides, blocked);
  }

  private static Map<String, Double> readValues(JsonObject root, String name) {
    Map<String, Double> values = new LinkedHashMap<>();
    if (root.has(name)) {
      for (Map.Entry<String, JsonElement> e : root.getAsJsonObject(name).entrySet()) {
        double v = e.getValue().getAsDouble();
        if (v >= 0 && !Double.isInfinite(v)) {
          values.put(e.getKey().trim(), v);
        }
      }
    }
    return values;
  }

  void save(File file) {
    JsonObject root = new JsonObject();
    root.addProperty("_comment", "CSM pawn shop values. Keys are modid:item (every subtype) or "
        + "modid:item@meta (one). 'raw' prices what no recipe makes; every other item is worth "
        + "its cheapest recipe. 'overrides' sets any item's value. 'blocked' items are never "
        + "taken. Run /csmpawn reload after editing.");
    root.add("raw", GSON.toJsonTree(raw));
    root.add("overrides", GSON.toJsonTree(overrides));
    JsonArray list = new JsonArray();
    for (String key : blocked) {
      list.add(key);
    }
    root.add("blocked", list);
    try {
      File dir = file.getParentFile();
      if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
        throw new IOException("could not create " + dir);
      }
      try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
        GSON.toJson(root, writer);
      }
    } catch (IOException e) {
      CsmFurnishings.getLogger().error("Could not write {}: {}", file, e.toString());
    }
  }

  /** The built-in table, for a new file. */
  static PawnValues defaults() {
    return parse(new StringReader(DEFAULTS));
  }

  /**
   * The built-in values, in money. An iron ingot (an ore and a smelt) is about 2, a diamond 25,
   * a cobblestone 0.02. Only what a survival player gathers rather than crafts is listed: the
   * walk prices the rest, CSM's parts and blocks included.
   */
  private static final String DEFAULTS = "{\n"
      + "  \"raw\": {\n"
      // Ground and stone
      + "    \"minecraft:dirt\": 0.01, \"minecraft:grass\": 0.02, \"minecraft:gravel\": 0.02,\n"
      + "    \"minecraft:sand\": 0.02, \"minecraft:cobblestone\": 0.02,\n"
      + "    \"minecraft:stone@1\": 0.03, \"minecraft:stone@3\": 0.03, \"minecraft:stone@5\": 0.03,\n"
      + "    \"minecraft:netherrack\": 0.01, \"minecraft:soul_sand\": 0.05,\n"
      + "    \"minecraft:end_stone\": 0.05, \"minecraft:obsidian\": 1.5,\n"
      + "    \"minecraft:clay_ball\": 0.05, \"minecraft:hardened_clay\": 0.1,\n"
      + "    \"minecraft:ice\": 0.05, \"minecraft:packed_ice\": 0.2, \"minecraft:snowball\": 0.01,\n"
      + "    \"minecraft:mycelium\": 0.05, \"minecraft:flint\": 0.05,\n"
      + "    \"minecraft:prismarine_shard\": 0.5, \"minecraft:prismarine_crystals\": 0.6,\n"
      + "    \"minecraft:sponge\": 5.0, \"minecraft:mossy_cobblestone\": 0.1,\n"
      + "    \"minecraft:web\": 0.2, \"minecraft:monster_egg\": 0.05,\n"
      + "    \"minecraft:grass_path\": 0.02, \"minecraft:farmland\": 0.01,\n"
      // Plants
      + "    \"minecraft:log\": 0.4, \"minecraft:log2\": 0.4, \"minecraft:sapling\": 0.05,\n"
      + "    \"minecraft:leaves\": 0.01, \"minecraft:leaves2\": 0.01, \"minecraft:apple\": 0.2,\n"
      + "    \"minecraft:vine\": 0.05, \"minecraft:waterlily\": 0.1, \"minecraft:cactus\": 0.1,\n"
      + "    \"minecraft:reeds\": 0.1, \"minecraft:pumpkin\": 0.2, \"minecraft:melon\": 0.05,\n"
      + "    \"minecraft:wheat\": 0.1, \"minecraft:wheat_seeds\": 0.02, \"minecraft:carrot\": 0.1,\n"
      + "    \"minecraft:potato\": 0.1, \"minecraft:poisonous_potato\": 0.02,\n"
      + "    \"minecraft:beetroot\": 0.1, \"minecraft:beetroot_seeds\": 0.02,\n"
      + "    \"minecraft:dye@3\": 0.1, \"minecraft:nether_wart\": 0.3,\n"
      + "    \"minecraft:chorus_fruit\": 0.2, \"minecraft:chorus_flower\": 0.5,\n"
      + "    \"minecraft:chorus_plant\": 0.1,\n"
      + "    \"minecraft:red_flower\": 0.05, \"minecraft:yellow_flower\": 0.05,\n"
      + "    \"minecraft:double_plant\": 0.05, \"minecraft:tallgrass\": 0.01,\n"
      + "    \"minecraft:deadbush\": 0.02, \"minecraft:brown_mushroom\": 0.05,\n"
      + "    \"minecraft:red_mushroom\": 0.05, \"minecraft:brown_mushroom_block\": 0.05,\n"
      + "    \"minecraft:red_mushroom_block\": 0.05,\n"
      // Ores and what they drop
      + "    \"minecraft:coal@0\": 0.5, \"minecraft:coal_ore\": 0.6,\n"
      + "    \"minecraft:iron_ore\": 2.0, \"minecraft:gold_ore\": 4.0,\n"
      + "    \"minecraft:diamond\": 25.0, \"minecraft:diamond_ore\": 25.0,\n"
      + "    \"minecraft:emerald\": 10.0, \"minecraft:emerald_ore\": 10.0,\n"
      + "    \"minecraft:redstone\": 0.3, \"minecraft:redstone_ore\": 1.2,\n"
      + "    \"minecraft:dye@4\": 0.4, \"minecraft:lapis_ore\": 2.0,\n"
      + "    \"minecraft:quartz\": 0.4, \"minecraft:quartz_ore\": 0.8,\n"
      + "    \"minecraft:glowstone_dust\": 0.3,\n"
      // Mobs
      + "    \"minecraft:rotten_flesh\": 0.02, \"minecraft:bone\": 0.1, \"minecraft:string\": 0.05,\n"
      + "    \"minecraft:spider_eye\": 0.1, \"minecraft:gunpowder\": 0.2,\n"
      + "    \"minecraft:ender_pearl\": 1.5, \"minecraft:blaze_rod\": 1.5,\n"
      + "    \"minecraft:ghast_tear\": 3.0, \"minecraft:slime_ball\": 0.3,\n"
      + "    \"minecraft:leather\": 0.3, \"minecraft:rabbit_hide\": 0.1,\n"
      + "    \"minecraft:rabbit_foot\": 1.0, \"minecraft:feather\": 0.05, \"minecraft:egg\": 0.05,\n"
      + "    \"minecraft:beef\": 0.15, \"minecraft:porkchop\": 0.15, \"minecraft:chicken\": 0.1,\n"
      + "    \"minecraft:mutton\": 0.12, \"minecraft:rabbit\": 0.12, \"minecraft:fish\": 0.1,\n"
      + "    \"minecraft:dye@0\": 0.1, \"minecraft:wool@0\": 0.2, \"minecraft:shulker_shell\": 5.0,\n"
      + "    \"minecraft:dragon_breath\": 2.0, \"minecraft:nether_star\": 100.0,\n"
      + "    \"minecraft:totem_of_undying\": 20.0, \"minecraft:elytra\": 100.0,\n"
      + "    \"minecraft:dragon_egg\": 500.0, \"minecraft:skull\": 2.0,\n"
      + "    \"minecraft:milk_bucket\": 6.5, \"minecraft:water_bucket\": 6.2,\n"
      + "    \"minecraft:lava_bucket\": 6.5,\n"
      // Found, not made
      + "    \"minecraft:saddle\": 3.0, \"minecraft:name_tag\": 2.0,\n"
      + "    \"minecraft:iron_horse_armor\": 5.0, \"minecraft:golden_horse_armor\": 8.0,\n"
      + "    \"minecraft:diamond_horse_armor\": 30.0, \"minecraft:golden_apple@1\": 50.0,\n"
      + "    \"minecraft:enchanted_book\": 1.0, \"minecraft:experience_bottle\": 0.5,\n"
      + "    \"minecraft:potion\": 0.5, \"minecraft:splash_potion\": 0.6,\n"
      + "    \"minecraft:lingering_potion\": 0.7,\n"
      + "    \"minecraft:chainmail_helmet\": 5.0, \"minecraft:chainmail_chestplate\": 8.0,\n"
      + "    \"minecraft:chainmail_leggings\": 7.0, \"minecraft:chainmail_boots\": 4.0,\n"
      + "    \"minecraft:anvil@1\": 40.0, \"minecraft:anvil@2\": 20.0,\n"
      // Special recipes, whose output depends on what goes in
      + "    \"minecraft:fireworks\": 0.3, \"minecraft:firework_charge\": 0.3,\n"
      + "    \"minecraft:tipped_arrow\": 0.3, \"minecraft:filled_map\": 1.0,\n"
      + "    \"minecraft:written_book\": 0.5,\n"
      + "    \"minecraft:record_13\": 5.0, \"minecraft:record_cat\": 5.0,\n"
      + "    \"minecraft:record_blocks\": 5.0, \"minecraft:record_chirp\": 5.0,\n"
      + "    \"minecraft:record_far\": 5.0, \"minecraft:record_mall\": 5.0,\n"
      + "    \"minecraft:record_mellohi\": 5.0, \"minecraft:record_stal\": 5.0,\n"
      + "    \"minecraft:record_strad\": 5.0, \"minecraft:record_ward\": 5.0,\n"
      + "    \"minecraft:record_11\": 5.0, \"minecraft:record_wait\": 5.0\n"
      + "  },\n"
      + "  \"overrides\": {},\n"
      + "  \"blocked\": [\n"
      + "    \"minecraft:bedrock\", \"minecraft:barrier\", \"minecraft:command_block\",\n"
      + "    \"minecraft:chain_command_block\", \"minecraft:repeating_command_block\",\n"
      + "    \"minecraft:command_block_minecart\", \"minecraft:structure_block\",\n"
      + "    \"minecraft:structure_void\", \"minecraft:mob_spawner\",\n"
      + "    \"minecraft:end_portal_frame\", \"minecraft:spawn_egg\",\n"
      + "    \"minecraft:knowledge_book\"\n"
      + "  ]\n"
      + "}\n";
}
