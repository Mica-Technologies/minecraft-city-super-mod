package com.micatechnologies.minecraft.csm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

/**
 * Registry utility class for the City Super Mod.
 *
 * @version 1.1
 * @since 2.0
 * @since 2023.2.1
 */
public class CsmRegistry {

  /**
   * The map of blocks registered with the mod.
   *
   * @since 1.0
   */
  private static final Map<String, Block> BLOCKS = new HashMap<>();

  /**
   * The list of items registered with the mod.
   *
   * @since 1.0
   */
  private static final List<Item> ITEMS = new ArrayList<>();


  /**
   * Returns the map of blocks registered with the mod.
   *
   * @return The map of blocks registered with the mod.
   *
   * @since 1.0
   */
  public static Map<String, Block> getBlocksMap() {
    return BLOCKS;
  }

  /**
   * Returns the list of blocks registered with the mod.
   *
   * @return The list of blocks registered with the mod.
   *
   * @since 1.0
   */
  public static Collection<Block> getBlocks() {
    return BLOCKS.values();
  }

  /**
   * Returns the block registered with the mod that is of the specified class.
   *
   * @param blockId The ID of the block to return.
   *
   * @return The block registered with the mod that is of the specified class.
   *
   * @since 1.0
   */
  public static Block getBlock(String blockId) {
    Block block = BLOCKS.get(CsmConstants.MOD_NAMESPACE + ":" + blockId);
    if (block == null) {
      block = BLOCKS.get(blockId);
    }
    return block;
  }

  /**
   * Returns the list of items registered with the mod.
   *
   * @return The list of items registered with the mod.
   *
   * @since 1.0
   */
  public static Collection<Item> getItems() {
    return ITEMS;
  }

  /**
   * Registers a block with the mod.
   *
   * @param block The block to register.
   *
   * @since 1.0
   */
  public static void registerBlock(Block block) {
    // Use the block's registry name as the key
    String key = block.getRegistryName() != null
        ? block.getRegistryName().toString()
        : block.getTranslationKey();

    // Check if the block is already registered.
    if (BLOCKS.containsKey(key)) {
      throw new IllegalArgumentException(
          "Block with registry name " + key + " already registered.");
    }

    BLOCKS.put(key, block);
  }

  /**
   * Registers an item with the mod.
   *
   * @param item The item to register.
   *
   * @since 1.0
   */
  public static void registerItem(Item item) {
    // Check if the item is already registered.
    if (ITEMS.contains(item)) {
      throw new IllegalArgumentException("Item " + item.getRegistryName() + " already registered.");
    }

    ITEMS.add(item);
  }
}
