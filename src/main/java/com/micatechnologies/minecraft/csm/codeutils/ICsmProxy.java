package com.micatechnologies.minecraft.csm.codeutils;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Interface defining the client/server proxy contract for the mod. Implementations handle
 * side-specific initialization logic during the Forge mod lifecycle (pre-init, init, post-init,
 * and server starting events).
 *
 * @author Mica Technologies
 */
public interface ICsmProxy {

  /**
   * Pre-initialize the mod. This method is called by Minecraft Forge during pre-initialization.
   *
   * @param event : additionalData[0] The {@link FMLPreInitializationEvent} that is being
   *              processed.
   *
   * @since 1.0
   */
  void preInit(FMLPreInitializationEvent event);

  /**
   * Initialize the mod. This method is called by Minecraft Forge during initialization.
   *
   * @param event : additionalData[0] The {@link FMLInitializationEvent} that is being processed.
   *
   * @since 1.0
   */
  void init(FMLInitializationEvent event);

  /**
   * Post-initialize the mod. This method is called by Minecraft Forge during post-initialization.
   *
   * @param event : additionalData[0] The {@link FMLPostInitializationEvent} that is being
   *              processed.
   *
   * @since 1.0
   */
  void postInit(FMLPostInitializationEvent event);

  /**
   * Load the mod on the server. This method is called by Minecraft Forge during server load.
   *
   * @param event : additionalData[0] The {@link FMLServerStartingEvent} that is being processed.
   *
   * @since 1.0
   */
  void serverLoad(FMLServerStartingEvent event);

  /**
   * Set the custom model resource location for the specified {@link Item} with the specified
   * metadata and id.
   *
   * @param item The {@link Item} to set the custom model resource location for.
   * @param meta The metadata of the {@link Item} to set the custom model resource location for.
   * @param id   The id to set the custom model resource location for.
   *
   * @since 1.0
   */
  void setCustomModelResourceLocation(Item item, int meta, String id);

  /**
   * Runs the {@code /csm memstats} report. On a server only the block states are measured, on the
   * calling thread; the client proxy adds the baked models and runs on the client thread, where
   * the model manager lives.
   *
   * @param outRoot the folder the report is written under
   * @param dump    true to write the full report (and hash every quad for the duplicate figures)
   * @param reply   receives the chat lines when the report is done
   */
  default void runMemStats(File outRoot, boolean dump, Consumer<List<String>> reply) {
    reply.accept(CsmMemStats.run(outRoot, dump, dump, null));
  }

  /**
   * Runs {@code /csm memstats variants}: how many of CSM's blockstate variants and baked models
   * repeat another's content. Client only; a server replies that there are no models to count.
   *
   * @param reply receives the chat lines when the count is done
   */
  default void runMemStatsVariants(Consumer<List<String>> reply) {
    reply.accept(java.util.Collections.singletonList(
        "memstats variants: models exist only on a client"));
  }
}
