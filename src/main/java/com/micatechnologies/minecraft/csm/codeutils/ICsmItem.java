package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Mod item interface which provides common method stubs and properties for all items in this mod.
 *
 * @version 1.0
 * @since 2023.3
 */
public interface ICsmItem {

  /**
   * Retrieves the registry name of the item.
   *
   * @return The registry name of the item.
   *
   * @since 1.0
   */
  String getItemRegistryName();
}
