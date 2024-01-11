package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class CsmWikiGenerator {

  public static void generateWikiFiles() {
    File outputDirectory = new File(".", CsmConfig.getWikiFilesFolder());
    if (outputDirectory.exists()) {
      deleteDirectory(outputDirectory);
    }
    outputDirectory.mkdir();

    // Group blocks by tab and generate markdown
    Map<CreativeTabs, List<Block>> blocksByTab = groupBlocksByTab(CsmRegistry.getBlocks());
    Map<CreativeTabs, List<Item>> itemsByTab = groupItemsByTab(CsmRegistry.getItems());

    // Combine all tabs from blocks and items
    Set<CreativeTabs> allTabs = new HashSet<>(blocksByTab.keySet());
    allTabs.addAll(itemsByTab.keySet());

    for (CreativeTabs tab : allTabs) {
      String tabName = (tab != null) ? tab.getTabLabel() : "tabnone";
      File markdownFile = new File(outputDirectory, sanitizeForFileSystem(tabName) + ".md");

      // Add heading for blocks
      if (blocksByTab.containsKey(tab)) {
        writeFile(markdownFile, "## Blocks\n\n", true);
      }

      // Generate markdown for blocks
      List<Block> blocksForTab = blocksByTab.getOrDefault(tab, Collections.emptyList());
      for (Block block : blocksForTab) {
        generateMarkdownForBlock(block, markdownFile);
      }

      // Add heading for items
      if (itemsByTab.containsKey(tab)) {
        writeFile(markdownFile, "\n## Items\n\n", true);
      }

      // Generate markdown for items
      List<Item> itemsForTab = itemsByTab.getOrDefault(tab, Collections.emptyList());
      for (Item item : itemsForTab) {
        if (!(item instanceof net.minecraft.item.ItemBlock)) {
          generateMarkdownForItem(item, markdownFile);
        }
      }
    }

  }


  private static void deleteDirectory(File directory) {
    File[] allContents = directory.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
      }
    }
    directory.delete();
  }


  private static Map<CreativeTabs, List<Block>> groupBlocksByTab(Collection<Block> blocks) {
    Map<CreativeTabs, List<Block>> blocksByTab = new HashMap<>();
    for (Block block : blocks) {
      blocksByTab.computeIfAbsent(block.getCreativeTab(), k -> new ArrayList<>())
          .add(block);
    }
    return blocksByTab;
  }

  private static void generateMarkdownForBlock(Block block, File markdownFile) {
    try {
      StringBuilder markdownContent = new StringBuilder();

      // Get default block state
      IBlockState defaultBlockState = block.getDefaultState();

      // Block Name as Heading
      markdownContent.append("### ").append(block.getLocalizedName()).append("\n\n");

      // Block ID
      markdownContent.append("- **ID**: ").append(block.getRegistryName()).append("\n\n\n");

      // Approx Size
      double approxSize;
      try {
        approxSize = block.getBoundingBox(defaultBlockState, null, null).getAverageEdgeLength();
      } catch (Exception e) {
        approxSize = 0;
      }
      markdownContent.append("- **Approx Size (1.0 = Full)**: ").append(approxSize)
          .append("\n\n\n");

      // Hardness and Resistance
      markdownContent.append("- **Hardness**: ")
          .append(block.getBlockHardness(defaultBlockState, null, null))
          .append("\n");
      markdownContent.append("- **Resistance**: ").append(block.getExplosionResistance(null))
          .append("\n");

      // Light Level
      if (block.getLightValue(defaultBlockState) > 0) {
        markdownContent.append("- **Light Level**: ").append(block.getLightValue(defaultBlockState))
            .append("\n");
      }

      // Harvest Level and Tool
      String toolClass = block.getHarvestTool(defaultBlockState);
      if (toolClass != null) {
        markdownContent.append("- **Tool**: ").append(toolClass).append("\n");
        markdownContent.append("- **Harvest Level**: ")
            .append(block.getHarvestLevel(defaultBlockState))
            .append("\n");
      }

      // Append to the markdown file
      writeFile(markdownFile, markdownContent.toString(), true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void generateMarkdownForItem(Item item, File markdownFile) {
    try {
      StringBuilder markdownContent = new StringBuilder();

      // Item Name as Heading
      markdownContent.append("### ").append(item.getItemStackDisplayName(new ItemStack(item)))
          .append("\n\n");

      // Item ID
      markdownContent.append("- **ID**: ").append(item.getRegistryName()).append("\n\n\n");

      // Stack Size
      markdownContent.append("- **Max Stack Size**: ").append(item.getItemStackLimit())
          .append("\n");

      // Append to the markdown file
      writeFile(markdownFile, markdownContent.toString(), true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private static Map<CreativeTabs, List<Item>> groupItemsByTab(Collection<Item> items) {
    Map<CreativeTabs, List<Item>> itemsByTab = new HashMap<>();
    for (Item item : items) {
      itemsByTab.computeIfAbsent(item.getCreativeTab(), k -> new ArrayList<>()).add(item);
    }
    return itemsByTab;
  }


  private static void writeFile(File file, String content, boolean append) {
    try (FileWriter writer = new FileWriter(file, append)) {
      writer.write(content);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private static String sanitizeForFileSystem(String input) {
    // Replace spaces with dashes
    String sanitized = input.replace(" ", "-");
    // Replace any set of non-alphanumeric characters (except dashes) with an underscore
    sanitized = sanitized.replaceAll("[^a-zA-Z0-9-]", "_");
    return sanitized;
  }

}


