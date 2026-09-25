package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Fails the build when a block in Core or any module builds its states with vanilla's
 * {@code BlockStateContainer} or Forge's {@code ExtendedBlockState} instead of
 * {@link CsmBlockStateContainer} / {@link CsmExtendedBlockState}. Vanilla's containers give every
 * state a table of its neighbours, which across CSM's blocks came to about 945 MiB of heap; one
 * new block with a few properties quietly brings a share of that back. See the memory section of
 * {@code PERFORMANCE_AND_SECURITY.md}.
 *
 * <p>A source scan, since the rule is about what a {@code createBlockState} writes and the
 * in-game {@code /csm statecheck} only runs when someone runs it.</p>
 */
class CsmBlockStateContainerUseTest {

  private static final String[] VANILLA = {"new BlockStateContainer(", "new ExtendedBlockState("};

  @Test
  void noBlockBuildsVanillaContainers() throws IOException {
    List<Path> roots = new ArrayList<>();
    roots.add(Paths.get("src", "main", "java"));
    Path modules = Paths.get("modules");
    if (Files.isDirectory(modules)) {
      try (Stream<Path> dirs = Files.list(modules)) {
        dirs.map(d -> d.resolve(Paths.get("src", "main", "java")))
            .filter(Files::isDirectory)
            .forEach(roots::add);
      }
    }
    assertTrue(Files.isDirectory(roots.get(0)), "run from the project root");

    List<String> offenders = new ArrayList<>();
    for (Path root : roots) {
      try (Stream<Path> files = Files.walk(root)) {
        files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
          List<String> lines;
          try {
            lines = Files.readAllLines(p, StandardCharsets.UTF_8);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("*") || line.startsWith("//")) {
              continue;   // Javadoc and comments may name the vanilla constructors
            }
            for (String v : VANILLA) {
              if (line.contains(v)) {
                offenders.add(p + ":" + (i + 1) + "  " + line);
              }
            }
          }
        });
      }
    }
    assertTrue(offenders.isEmpty(), "Use CsmBlockStateContainer / CsmExtendedBlockState, not "
        + "vanilla's containers (a neighbour table per state):\n" + String.join("\n", offenders));
  }
}
