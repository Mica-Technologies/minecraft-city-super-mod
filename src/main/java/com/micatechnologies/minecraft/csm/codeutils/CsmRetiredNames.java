package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmConstants;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Block and item names the mod once registered and has removed on purpose. A world that still
 * holds one loses it quietly, as air, instead of stopping the load with Forge's missing-entries
 * prompt.
 *
 * <p>Only the names listed here are dropped. A missing {@code csm:} name that is not listed is
 * left to Forge's prompt, because it usually means a module jar is missing from the install, and
 * a silent load would delete that module's blocks from the world for good.</p>
 *
 * <p>Every name here is also in {@code assets/to-be-added-to-mod/BLOCKS_TO_REVISIT.md}, which
 * says what each block was. A block that comes back under its old name must be taken off this
 * list.</p>
 *
 * @since 2026.9
 */
public final class CsmRetiredNames {

  /** Removed 2026-09-24: their models were not our own work. */
  private static final Set<String> RETIRED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      "anchor", "cookooclock", "cowhide", "creeperplush", "csmharp", "cuttingboard",
      "dollhouse1", "dollhouse2", "elf", "etchasketch", "foodprocessor", "gardengnome", "goldbars",
      "goldenfurawardstrophy", "honeypot", "hourglass", "hummingbirdfeeder", "minicmastree",
      "phonograph", "playingcards", "plunger", "presents", "r2d2", "reindeer", "rubixcube",
      "shootingdummy", "silverware", "singlepumpkin", "smallanchor", "snowglobe", "tardis",
      "telescope", "tikitorch", "toyboxboy", "toyboxgirl", "treasurechest", "utensilhooks",
      "vintagesewingmachine", "waterbucket", "windchime")));

  private CsmRetiredNames() {
  }

  /**
   * Whether a registry path (the part after {@code csm:}) is a retired name.
   *
   * @param path the registry path
   *
   * @return {@code true} if the name was retired on purpose
   */
  public static boolean isRetired(String path) {
    return RETIRED.contains(path);
  }

  /**
   * The retired names, for tests.
   *
   * @return every retired registry path
   */
  public static Set<String> all() {
    return RETIRED;
  }

  /**
   * Drops every retired {@code csm:} name a world remembers from a registry's missing mappings.
   * The block or item registry both come through here; an item block shares its block's name.
   *
   * @param event the registry's missing-mappings event
   * @param <T>   the registry's entry type
   */
  public static <T extends IForgeRegistryEntry<T>> void ignoreRetired(
      RegistryEvent.MissingMappings<T> event) {
    for (RegistryEvent.MissingMappings.Mapping<T> mapping : event.getAllMappings()) {
      if (CsmConstants.MOD_NAMESPACE.equals(mapping.key.getNamespace())
          && isRetired(mapping.key.getPath())) {
        mapping.ignore();
      }
    }
  }
}
