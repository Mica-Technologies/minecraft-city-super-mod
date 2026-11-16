package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The creative tab order ({@link CsmTab#LOAD_ORDER}): tabs sort by {@link CsmTab.Load#order()},
 * then by {@link CsmTab.Load#sub()}, and no two tabs in any tree share the pair, since a tie
 * would build them, and show them, in whatever order the class scan found them.
 */
class CsmTabLoadOrderTest {

  private static final Pattern LOAD =
      Pattern.compile("@CsmTab\\.Load\\(\\s*order\\s*=\\s*(-?\\d+)(?:\\s*,\\s*sub\\s*=\\s*(-?\\d+))?\\s*\\)");

  @CsmTab.Load(order = 9, sub = 1)
  private static final class Late {
  }

  @CsmTab.Load(order = 9)
  private static final class Middle {
  }

  @CsmTab.Load(order = 9, sub = -1)
  private static final class Early {
  }

  @CsmTab.Load(order = 8, sub = 5)
  private static final class Before {
  }

  @CsmTab.Load(order = 10, sub = -5)
  private static final class After {
  }

  @Test
  void sortsByOrderThenSub() {
    List<Class<?>> classes = new ArrayList<>(
        Arrays.asList(After.class, Late.class, Middle.class, Before.class, Early.class));
    classes.sort(CsmTab.LOAD_ORDER);
    assertEquals(Arrays.asList(Before.class, Early.class, Middle.class, Late.class, After.class),
        classes);
  }

  @Test
  void everyTabHasItsOwnOrderAndSub() throws IOException {
    Map<String, String> seen = new HashMap<>();
    List<String> clashes = new ArrayList<>();
    for (Path root : roots(Paths.get("src", "main", "java"))) {
      for (Path file : javaFiles(root)) {
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        Matcher m = LOAD.matcher(text);
        if (!m.find()) {
          continue;
        }
        String key = m.group(1) + "/" + (m.group(2) == null ? "0" : m.group(2));
        String name = file.getFileName().toString();
        String previous = seen.put(key, name);
        if (previous != null) {
          clashes.add(key + ": " + previous + " and " + name);
        }
      }
    }
    assertTrue(seen.size() > 40, "found only " + seen);
    assertTrue(clashes.isEmpty(), "tabs sharing an order and sub: " + clashes);

    // The Roads split keeps the three tabs together where Traffic Accessories sat
    assertEquals("CsmTabTrafficPoles.java", seen.get("9/-1"));
    assertEquals("CsmTabTrafficAccessories.java", seen.get("9/0"));
    assertEquals("CsmTabWorkZone.java", seen.get("9/1"));
  }

  private static List<Path> roots(Path inTree) throws IOException {
    List<Path> roots = new ArrayList<>();
    roots.add(inTree);
    Path modules = Paths.get("modules");
    if (Files.isDirectory(modules)) {
      try (Stream<Path> dirs = Files.list(modules)) {
        dirs.sorted().map(d -> d.resolve(inTree)).filter(Files::isDirectory).forEach(roots::add);
      }
    }
    return roots;
  }

  private static List<Path> javaFiles(Path root) throws IOException {
    try (Stream<Path> files = Files.walk(root)) {
      return files.filter(p -> p.getFileName().toString().startsWith("CsmTab"))
          .filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
    }
  }
}
