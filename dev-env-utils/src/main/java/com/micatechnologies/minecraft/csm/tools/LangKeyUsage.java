package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which lang keys the mod's Java sources actually use, beyond a block's or item's own name.
 *
 * <p>The integrity tool used to count a key as used only if it was a block's or item's
 * {@code .name}, a tab's name, or an {@code I18n.format("...")} literal in a registered block's own
 * class. Everything a screen, a chat message or a tooltip names -- the ad board, door, crane and
 * exit sign keys -- was reported as unused, 176 keys in each of four languages, every one of them
 * in use. This reads the sources the way the game does, in three ways, each strict enough that a
 * key that really is unused is still reported:
 *
 * <ol>
 *   <li><b>A literal.</b> A string literal, anywhere in any tree, that is exactly the key: a
 *       {@code TextComponentTranslation}, a key handed to a helper that translates it, one side
 *       of a ternary. Comments are stripped first, so a key only mentioned in a comment does not
 *       count.</li>
 *   <li><b>A template.</b> A key built by concatenation, such as
 *       {@code "gui.csm.door.movement." + s.movement().key()}. The whole expression becomes a
 *       pattern in which each non-literal part matches exactly one lower-case segment
 *       ({@code [a-z0-9_]+}), so {@code "csm.exitsign." + option + "." + value} accepts
 *       {@code csm.exitsign.mount.wall} but not a key with a third segment. And each such
 *       segment must be a word the mod has (see {@link #vocabulary}): {@code wall} is the
 *       lower-cased {@code Mount.WALL}, while {@code gui.csm.door.movement.teleport} fits the
 *       movement template and is still reported. Templates never vouch for {@code tile.} or {@code item.} keys, which the block
 *       and item rules own: a template such as {@code "tile." + name + ".name"} would otherwise
 *       hide the name of every block that no longer exists.</li>
 *   <li><b>A per-stack name.</b> {@code tile.<block>.<part>.name}, for a registered block whose
 *       item block appends a part to the stack's translation key (the crane masts' liveries, the
 *       framing walls' insulation), found by following the block class and its superclasses to
 *       the {@code new ItemBlockX(this)} it creates and checking that class's
 *       {@code getTranslationKey} appends {@code "."}. The part must be in the vocabulary too.</li>
 * </ol>
 *
 * <p>What it still cannot see: a key assembled somewhere other than the expression that
 * translates it (stored in a field, passed through two methods). The tool reports those as
 * unused, and each should be checked by hand before it is deleted. What it can still let
 * through: a variable part that is a real word of the mod but not a value that expression can
 * produce ({@code tile.crane_mast.purple.name}, if some other enum has a {@code PURPLE}). The
 * vocabulary knows the words, not which enum an expression reads.
 *
 * @since 1.2
 */
final class LangKeyUsage {

  private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"");

  /** A string literal ending in a dot and followed by {@code +}: the head of a template. */
  private static final Pattern TEMPLATE_HEAD = Pattern.compile(
      "\"([a-z0-9_]+(?:\\.[a-z0-9_]+)*\\.)\"\\s*\\+");

  private static final Pattern NEW_ITEM_BLOCK = Pattern.compile(
      "new\\s+(ItemBlock\\w*)\\s*\\(\\s*this\\b");

  private static final Pattern APPENDS_TO_TRANSLATION_KEY = Pattern.compile(
      "String\\s+getTranslationKey\\s*\\(.*?getTranslationKey\\s*\\([^)]*\\)\\s*\\+\\s*\"\\.\"",
      Pattern.DOTALL);

  private static final String SEGMENT = "([a-z0-9_]+)";

  /** An upper-case identifier: an enum constant (or any other constant) whose name may be a key part. */
  private static final Pattern CONSTANT_NAME = Pattern.compile("\\b([A-Z][A-Z0-9_]*)\\b");

  private final Set<String> literals = new HashSet<>();
  private final List<Pattern> templates = new ArrayList<>();
  private final Set<String> blocksWithStackNames = new HashSet<>();

  /**
   * Every word a variable part of a key can plausibly be: each constant name lower-cased (what
   * {@code name().toLowerCase()} and an enum's {@code getName()} return), each string literal, and
   * each string in the mod's data files (the ad index names its categories). A template or
   * per-stack segment must be one of these, so {@code gui.csm.door.movement.teleport} is still
   * reported although it fits the movement template.
   */
  private final Set<String> vocabulary = new HashSet<>();

  /**
   * Reads every source class the layout knows and every block registration.
   *
   * @param layout the repository layout
   */
  LangKeyUsage(CsmLayout layout) {
    Map<String, CsmLayout.SourceClass> classes = layout.classes();
    Map<String, String> texts = new java.util.HashMap<>();
    for (Map.Entry<String, CsmLayout.SourceClass> entry : classes.entrySet()) {
      CsmLayout.SourceClass info = entry.getValue();
      if (info.file == null) {
        continue;
      }
      String text;
      try {
        text = stripComments(Files.readString(info.file.toPath()));
      } catch (IOException e) {
        continue;
      }
      texts.put(entry.getKey(), text);
      Matcher literal = STRING_LITERAL.matcher(text);
      while (literal.find()) {
        literals.add(literal.group(1));
        vocabulary.add(literal.group(1));
      }
      Matcher constant = CONSTANT_NAME.matcher(text);
      while (constant.find()) {
        vocabulary.add(constant.group(1).toLowerCase(java.util.Locale.ROOT));
      }
      Matcher head = TEMPLATE_HEAD.matcher(text);
      while (head.find()) {
        Pattern template = template(text, head.start());
        if (template != null) {
          templates.add(template);
        }
      }
    }
    for (CsmLayout.Registration registration : layout.registrations()) {
      if (!registration.item && appendsStackName(registration.className, classes, texts)) {
        blocksWithStackNames.add(registration.registryName);
      }
    }
    for (java.io.File assets : layout.assetRoots()) {
      addDataStrings(assets);
    }
  }

  /**
   * Adds every string in the data JSON under an asset root: everything except blockstates,
   * models and texture metadata, which name assets rather than values the code reads.
   */
  private void addDataStrings(java.io.File assets) {
    try (java.util.stream.Stream<java.nio.file.Path> paths = Files.walk(assets.toPath())) {
      paths.filter(p -> p.toString().endsWith(".json"))
          .filter(p -> {
            String s = p.toString().replace('\\', '/');
            return !s.contains("/blockstates/") && !s.contains("/models/")
                && !s.contains("/textures/");
          })
          .forEach(p -> {
            try {
              addStrings(com.google.gson.JsonParser.parseString(Files.readString(p)));
            } catch (Exception ignored) {
              // Not JSON the tool can read; nothing to add.
            }
          });
    } catch (IOException ignored) {
      // An unreadable tree adds nothing.
    }
  }

  private void addStrings(com.google.gson.JsonElement element) {
    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
      vocabulary.add(element.getAsString());
    } else if (element.isJsonArray()) {
      element.getAsJsonArray().forEach(this::addStrings);
    } else if (element.isJsonObject()) {
      element.getAsJsonObject().entrySet().forEach(e -> addStrings(e.getValue()));
    }
  }

  private boolean allInVocabulary(Matcher match) {
    for (int g = 1; g <= match.groupCount(); g++) {
      if (!vocabulary.contains(match.group(g))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Whether the sources use the key, by any of the three rules in the class description.
   *
   * @param key a lang key
   *
   * @return true if some source names it
   */
  boolean isUsed(String key) {
    if (literals.contains(key)) {
      return true;
    }
    if (key.startsWith("tile.") && key.endsWith(".name")) {
      String middle = key.substring("tile.".length(), key.length() - ".name".length());
      int dot = middle.lastIndexOf('.');
      if (dot <= 0 || !blocksWithStackNames.contains(middle.substring(0, dot))) {
        return false;
      }
      String part = middle.substring(dot + 1);
      return part.matches(SEGMENT) && vocabulary.contains(part);
    }
    if (key.startsWith("item.")) {
      return false;
    }
    for (Pattern template : templates) {
      Matcher match = template.matcher(key);
      if (match.matches() && allInVocabulary(match)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Turns the concatenation starting at a string literal into a pattern: literal parts as
   * themselves, every other part as one segment. Stops at the first top-level {@code ,}, {@code )},
   * {@code ;}, {@code ?} or {@code :} after a complete term.
   *
   * @param text  the stripped source
   * @param start the index of the head literal's opening quote
   *
   * @return the pattern, or null if the expression has no non-literal part
   */
  private static Pattern template(String text, int start) {
    StringBuilder regex = new StringBuilder();
    boolean dynamic = false;
    int i = start;
    while (true) {
      i = skipSpace(text, i);
      if (i >= text.length()) {
        break;
      }
      if (text.charAt(i) == '"') {
        int end = endOfString(text, i);
        regex.append(Pattern.quote(text.substring(i + 1, end)));
        i = end + 1;
      } else {
        int end = endOfTerm(text, i);
        if (end == i) {
          break;
        }
        regex.append(SEGMENT);
        dynamic = true;
        i = end;
      }
      i = skipSpace(text, i);
      if (i < text.length() && text.charAt(i) == '+') {
        i++;
      } else {
        break;
      }
    }
    return dynamic ? Pattern.compile(regex.toString()) : null;
  }

  private static int skipSpace(String text, int i) {
    while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
      i++;
    }
    return i;
  }

  private static int endOfString(String text, int open) {
    int i = open + 1;
    while (i < text.length() && text.charAt(i) != '"') {
      i += text.charAt(i) == '\\' ? 2 : 1;
    }
    return Math.min(i, text.length() - 1);
  }

  /** The end of one non-literal term: up to a top-level operator or delimiter. */
  private static int endOfTerm(String text, int i) {
    int depth = 0;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c == '"') {
        i = endOfString(text, i) + 1;
        continue;
      }
      if (c == '(' || c == '[') {
        depth++;
      } else if (c == ')' || c == ']') {
        if (depth == 0) {
          return i;
        }
        depth--;
      } else if (depth == 0 && (c == '+' || c == ',' || c == ';' || c == '?' || c == ':')) {
        return i;
      }
      i++;
    }
    return i;
  }

  /**
   * Whether a block class, or a superclass, creates an item block whose
   * {@code getTranslationKey} appends a part to the key.
   */
  private static boolean appendsStackName(String blockClass,
      Map<String, CsmLayout.SourceClass> classes, Map<String, String> texts) {
    for (String cls = blockClass; cls != null;
        cls = classes.containsKey(cls) ? classes.get(cls).superClass : null) {
      Matcher itemBlock = NEW_ITEM_BLOCK.matcher(texts.getOrDefault(cls, ""));
      while (itemBlock.find()) {
        if (APPENDS_TO_TRANSLATION_KEY.matcher(texts.getOrDefault(itemBlock.group(1), ""))
            .find()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Java source with its comments removed and its string and character literals kept, so a
   * {@code //} inside a string (a URL) is not taken for a comment.
   */
  static String stripComments(String source) {
    StringBuilder out = new StringBuilder(source.length());
    int i = 0;
    int n = source.length();
    while (i < n) {
      char c = source.charAt(i);
      char next = i + 1 < n ? source.charAt(i + 1) : '\0';
      if (c == '/' && next == '/') {
        while (i < n && source.charAt(i) != '\n') {
          i++;
        }
      } else if (c == '/' && next == '*') {
        int end = source.indexOf("*/", i + 2);
        i = end < 0 ? n : end + 2;
        out.append(' ');
      } else if (c == '"' || c == '\'') {
        int start = i;
        i++;
        while (i < n && source.charAt(i) != c && source.charAt(i) != '\n') {
          i += source.charAt(i) == '\\' ? 2 : 1;
        }
        i = Math.min(i + 1, n);
        out.append(source, start, i);
      } else {
        out.append(c);
        i++;
      }
    }
    return out.toString();
  }
}
