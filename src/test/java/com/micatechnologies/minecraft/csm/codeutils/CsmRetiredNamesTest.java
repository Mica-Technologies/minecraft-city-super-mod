package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Holds {@link CsmRetiredNames} to the tree: a retired name must be written up in
 * {@code BLOCKS_TO_REVISIT.md}, so nobody loses track of what it was, and must have no blockstate
 * left in any tree, since a retired name that still ships would be silently deleted from worlds
 * that hold it.
 */
class CsmRetiredNamesTest {

  private static File repoRoot() {
    File dir = new File("").getAbsoluteFile();
    while (dir != null && !new File(dir, "settings.gradle").isFile()) {
      dir = dir.getParentFile();
    }
    assertNotNull(dir, "repository root not found from " + new File("").getAbsolutePath());
    return dir;
  }

  private static List<File> blockstateDirs(File root) {
    List<File> dirs = new ArrayList<>();
    dirs.add(new File(root, "src/main/resources/assets/csm/blockstates"));
    File[] modules = new File(root, "modules").listFiles(File::isDirectory);
    if (modules != null) {
      for (File module : modules) {
        dirs.add(new File(module, "src/main/resources/assets/csm/blockstates"));
      }
    }
    return dirs;
  }

  @Test
  void everyRetiredNameIsWrittenUpToRevisit() throws IOException {
    File list = new File(repoRoot(), "assets/to-be-added-to-mod/BLOCKS_TO_REVISIT.md");
    assertTrue(list.isFile(), "missing " + list);
    String text = new String(Files.readAllBytes(list.toPath()), StandardCharsets.UTF_8);
    for (String name : CsmRetiredNames.all()) {
      assertTrue(text.contains("`" + name + "`"),
          name + " is retired but not listed in BLOCKS_TO_REVISIT.md");
    }
  }

  @Test
  void noRetiredNameStillShips() {
    List<File> dirs = blockstateDirs(repoRoot());
    for (String name : CsmRetiredNames.all()) {
      for (File dir : dirs) {
        assertFalse(new File(dir, name + ".json").exists(),
            name + " is retired but still has a blockstate in " + dir);
      }
    }
  }
}
