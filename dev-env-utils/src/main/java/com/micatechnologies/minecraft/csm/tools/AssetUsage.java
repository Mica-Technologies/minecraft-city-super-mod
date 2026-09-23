package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Decides, for an asset no blockstate or model reaches, whether it is used anyway, only read by
 * a generator, or genuinely unused.
 *
 * <p>The integrity tool marks an asset used when a registered block's blockstate reaches it,
 * through its models and their textures. That misses every asset the game reaches another way,
 * and it reported 330 files as unused on {@code feat/signage-advertising}, most of which the game
 * loads. Each rule below was written against a file checked by hand, and each stops short of
 * vouching for a file that nothing actually names:
 *
 * <ul>
 *   <li><b>Companion</b> -- {@code x.png.mcmeta} (animation) and {@code x_e.png} (OptiFine's
 *       emissive overlay, suffix from {@code emissive.properties}) are read by the game whenever
 *       {@code x.png} is, and never named by anything. Used exactly when {@code x.png} is.</li>
 *   <li><b>Named in Java</b> -- a string literal in the mod's sources, comments stripped, that is
 *       exactly one of the asset's resource names: {@code csm:blocks/…}, {@code blocks/…},
 *       {@code textures/blocks/….png}, {@code csm:item/…}, and so on. A bare file name does not
 *       count: {@code "yellow"} in an enum is not {@code bodies/yellow.png}.</li>
 *   <li><b>Per-setup item model</b> -- {@code models/item/<block>_<parts>.json} for a registered
 *       block whose class, a superclass, or a class it calls binds item models in code
 *       ({@code setCustomMeshDefinition} / {@code registerItemVariants}), when every
 *       underscore-separated part is a word the mod has (see {@link LangKeyUsage}). That is how
 *       an exit sign names an icon per legend, colour, housing and heads.</li>
 * </ul>
 *
 * <p>An asset none of those reach, but whose name a dev-env-utils tool or script gives exactly,
 * is a <b>generator source</b>: an atlas tile, a legacy input to a texture generator. The game
 * never loads it, but deleting it breaks the next regeneration, so it is reported separately from
 * the unused files, with the tool that reads it.
 *
 * @since 1.2
 */
final class AssetUsage {

  /** Where the per-stack model binding happens: a block that calls either binds item models. */
  private static final Pattern BINDS_ITEM_MODELS =
      Pattern.compile("setCustomMeshDefinition|registerItemVariants");

  private static final Pattern CLASS_TOKEN = Pattern.compile("\\b([A-Z]\\w+)\\b");

  private final LangKeyUsage sources;
  private final Set<String> usedPaths;
  private final String emissiveSuffix;
  private final java.util.List<File> assetRoots;
  private final Set<String> blocksWithBoundItemModels = new HashSet<>();

  /** Quoted string in a dev-env-utils tool or script to the files it is in. */
  private final Map<String, Set<String>> generatorStrings = new HashMap<>();

  /** Each dev-env-utils tool or script's comment-stripped text, by file name. */
  private final Map<String, String> generatorTexts = new HashMap<>();

  /**
   * @param layout    the repository layout
   * @param sources   the source index the lang check already built
   * @param usedPaths canonical paths of every file a blockstate or model reaches
   */
  AssetUsage(CsmLayout layout, LangKeyUsage sources, Set<String> usedPaths) {
    this.sources = sources;
    this.usedPaths = usedPaths;
    this.assetRoots = layout.assetRoots();
    this.emissiveSuffix = readEmissiveSuffix(layout);
    Map<String, CsmLayout.SourceClass> classes = layout.classes();
    for (CsmLayout.Registration registration : layout.registrations()) {
      if (!registration.item && bindsItemModels(registration.className, classes)) {
        blocksWithBoundItemModels.add(registration.registryName);
      }
    }
    File devEnvUtils = new File(layout.repoRoot(), "dev-env-utils");
    collectGeneratorStrings(new File(devEnvUtils, "src"), ".java");
    collectGeneratorStrings(new File(devEnvUtils, "scripts"), ".py");
  }

  /** The verdict on one file no blockstate or model reaches. */
  enum Verdict { USED, GENERATOR_SOURCE, UNUSED }

  /**
   * Classifies a file.
   *
   * @param file an asset under some tree's {@code assets/csm}
   *
   * @return the verdict
   */
  Verdict classify(File file) {
    String canonical = canonical(file);
    if (usedPaths.contains(canonical)) {
      return Verdict.USED;
    }
    // A companion is read whenever its base is, however the base is reached: the tool icons'
    // _e overlays sit beside icons that item models name, not blockstates.
    String base = companionBase(canonical);
    if (base != null) {
      return classify(new File(base)) == Verdict.USED ? Verdict.USED : Verdict.UNUSED;
    }
    String relative = relativeToAssets(canonical);
    if (relative == null) {
      return Verdict.UNUSED;
    }
    if (namedInJava(relative) || isBoundItemModel(relative)) {
      return Verdict.USED;
    }
    // Only a texture can be a generator's input (an atlas tile). A model a generator names is its
    // output, and one no blockstate reaches is a stale output: gen_work_zone_devices.py names
    // "workzone_concrete_barrier" as the stem of the _core/_end pieces it writes now, not as the
    // whole-barrier OBJ it wrote before the barriers joined, and that OBJ is unused.
    boolean texture = relative.startsWith("textures/") && relative.endsWith(".png");
    return texture && generatorReading(relative) != null ? Verdict.GENERATOR_SOURCE
        : Verdict.UNUSED;
  }

  /**
   * The dev-env-utils file that names a generator source, for the report.
   *
   * @param file the file
   *
   * @return that tool or script's name, or null
   */
  String generatorReading(File file) {
    String relative = relativeToAssets(canonical(file));
    return relative == null ? null : generatorReading(relative);
  }

  /**
   * A tool or script that names the file: by a full resource name, or by its bare file name when
   * the same tool also names the folder it sits in. The folder test is what keeps a script that
   * merely says {@code "white"} from vouching for {@code bodies/white.png}.
   */
  private String generatorReading(String relative) {
    for (String form : names(relative)) {
      Set<String> readers = generatorStrings.get(form);
      if (readers != null) {
        return readers.iterator().next();
      }
    }
    int slash = relative.lastIndexOf('/');
    String base = relative.substring(slash + 1);
    String stem = base.contains(".") ? base.substring(0, base.indexOf('.')) : base;
    String dir = slash < 0 ? "" : relative.substring(0, slash);
    String[] parts = dir.split("/");
    String last = parts[parts.length - 1];
    String lastTwo = parts.length >= 2 ? parts[parts.length - 2] + "/" + last : last;
    for (String name : new String[]{base, stem}) {
      for (String reader : generatorStrings.getOrDefault(name, Set.of())) {
        String text = generatorTexts.getOrDefault(reader, "");
        boolean namesFolder = text.contains(lastTwo)
            || (parts.length >= 2 && quotes(text, parts[parts.length - 2]) && quotes(text, last));
        if (namesFolder) {
          return reader;
        }
      }
    }
    return null;
  }

  private static boolean quotes(String text, String word) {
    return text.contains("\"" + word + "\"") || text.contains("'" + word + "'");
  }

  /**
   * The file a companion belongs to: {@code x.png} for {@code x.png.mcmeta} and for
   * {@code x_e.png}, when that file exists; otherwise null. An {@code x_e.png.mcmeta} resolves
   * to {@code x_e.png}, which resolves in turn.
   */
  private String companionBase(String canonical) {
    String base = null;
    if (canonical.endsWith(".mcmeta")) {
      base = canonical.substring(0, canonical.length() - ".mcmeta".length());
    } else if (canonical.endsWith(emissiveSuffix + ".png")) {
      base = canonical.substring(0, canonical.length() - (emissiveSuffix + ".png").length())
          + ".png";
    }
    if (base == null) {
      return null;
    }
    if (new File(base).isFile()) {
      return base;
    }
    // The game merges every jar's assets by resource path, so a companion in one tree applies to
    // a base in another: the signal tools' _e overlays ship in Core, their icons in Roads.
    String relative = relativeToAssets(base);
    if (relative != null) {
      for (File root : assetRoots) {
        File candidate = new File(root, relative);
        if (candidate.isFile()) {
          return canonical(candidate);
        }
      }
    }
    return null;
  }

  private boolean namedInJava(String relative) {
    Set<String> literals = sources.literals();
    for (String form : names(relative)) {
      if (literals.contains(form)) {
        return true;
      }
    }
    return false;
  }

  private boolean isBoundItemModel(String relative) {
    if (!relative.startsWith("models/item/") || !relative.endsWith(".json")) {
      return false;
    }
    String name = relative.substring("models/item/".length(), relative.length() - ".json".length());
    for (String block : blocksWithBoundItemModels) {
      if (name.startsWith(block + "_")) {
        boolean allWords = true;
        for (String part : name.substring(block.length() + 1).split("_")) {
          if (!sources.isWord(part)) {
            allWords = false;
            break;
          }
        }
        if (allWords) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Every way a Java string or a generator can name an asset at this path under
   * {@code assets/csm}.
   */
  private static Set<String> names(String relative) {
    Set<String> forms = new HashSet<>();
    String stem = relative.contains(".") ? relative.substring(0, relative.lastIndexOf('.'))
        : relative;
    for (String p : new String[]{relative, stem}) {
      forms.add(p);
      forms.add("csm:" + p);
    }
    for (String prefix : new String[]{"textures/", "models/block/", "models/"}) {
      if (relative.startsWith(prefix)) {
        String rest = stem.substring(prefix.length());
        forms.add(rest);
        forms.add("csm:" + rest);
        forms.add(relative.substring(prefix.length()));
        forms.add("csm:" + relative.substring(prefix.length()));
      }
    }
    if (relative.startsWith("models/block/")) {
      forms.add("csm:block/" + stem.substring("models/block/".length()));
    }
    return forms;
  }

  /**
   * Whether a block class, a superclass, or a class one of them names, binds item models in
   * code. One hop only: the exit signs call {@code ExitSignItemModels.register(this)}.
   */
  private boolean bindsItemModels(String blockClass, Map<String, CsmLayout.SourceClass> classes) {
    Map<String, String> texts = sources.texts();
    for (String cls = blockClass; cls != null;
        cls = classes.containsKey(cls) ? classes.get(cls).superClass : null) {
      String text = texts.getOrDefault(cls, "");
      if (BINDS_ITEM_MODELS.matcher(text).find()) {
        return true;
      }
      Matcher token = CLASS_TOKEN.matcher(text);
      Set<String> seen = new HashSet<>();
      while (token.find()) {
        String named = token.group(1);
        if (seen.add(named) && !named.equals(cls) && texts.containsKey(named)
            && named.contains("ItemModel")
            && BINDS_ITEM_MODELS.matcher(texts.get(named)).find()) {
          return true;
        }
      }
    }
    return false;
  }

  private void collectGeneratorStrings(File root, String extension) {
    if (!root.isDirectory()) {
      return;
    }
    Pattern quoted = Pattern.compile("\"([^\"\\n]+)\"|'([^'\\n]+)'");
    try (Stream<Path> paths = Files.walk(root.toPath())) {
      paths.filter(p -> p.toString().endsWith(extension)).forEach(p -> {
        String text;
        try {
          text = LangKeyUsage.stripComments(Files.readString(p));
        } catch (IOException e) {
          return;
        }
        String reader = p.getFileName().toString();
        generatorTexts.put(reader, text);
        Matcher m = quoted.matcher(text);
        while (m.find()) {
          String s = m.group(1) != null ? m.group(1) : m.group(2);
          generatorStrings.computeIfAbsent(s, k -> new java.util.LinkedHashSet<>()).add(reader);
        }
      });
    } catch (IOException ignored) {
      // An unreadable tree names nothing.
    }
  }

  private static String readEmissiveSuffix(CsmLayout layout) {
    for (File root : layout.assetRoots()) {
      File props = new File(root.getParentFile(), "minecraft/optifine/emissive.properties");
      if (props.isFile()) {
        Properties p = new Properties();
        try (java.io.Reader r = Files.newBufferedReader(props.toPath())) {
          p.load(r);
          return p.getProperty("suffix.emissive", "_e");
        } catch (IOException ignored) {
          // Fall through to the default.
        }
      }
    }
    return "_e";
  }

  static String canonical(File file) {
    try {
      return file.getCanonicalPath();
    } catch (IOException e) {
      return file.getAbsolutePath();
    }
  }

  private static String relativeToAssets(String canonical) {
    String p = canonical.replace('\\', '/');
    int at = p.indexOf("/assets/csm/");
    return at < 0 ? null : p.substring(at + "/assets/csm/".length());
  }
}
