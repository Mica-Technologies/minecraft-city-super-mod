package com.micatechnologies.minecraft.csm;

import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTESRProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTESR;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Sided proxy for initialization and loading of the mod on the client side. This class is
 * referenced by the {@code @SidedProxy} annotation in {@link Csm}. This class is instantiated by
 * Forge.
 *
 * @version 1.1
 * @see ICsmProxy
 * @since 1.0
 */
public class CsmClientProxy implements ICsmProxy {

  /**
   * Set of registered TESRs.
   *
   * @since 1.1
   */
  private final Set<String> registeredTESRs = new HashSet<>();

  /**
   * Set of warned TESRs.
   *
   * @since 1.1
   */
  Set<String> warnedTESRs = new HashSet<>();

  /**
   * Pre-initialize the mod. This method is called by Minecraft Forge during pre-initialization.
   *
   * @param event : additionalData[0] The {@link FMLPreInitializationEvent} that is being
   *              processed.
   *
   * @since 1.0
   */
  @Override
  public void preInit(FMLPreInitializationEvent event) {
    OBJLoader.INSTANCE.addDomain("csm");
  }

  /**
   * Initialize the mod. This method is called by Minecraft Forge during initialization.
   *
   * @param event : additionalData[0] The {@link FMLInitializationEvent} that is being processed.
   *
   * @since 1.0
   */
  @Override
  public void init(FMLInitializationEvent event) {
    CsmRegistry.getBlocks().stream().filter(block -> block instanceof ICsmTESRProvider)
        .forEach(this::registerBlockTESR);
  }

  /**
   * Register the TESR for the specified block.
   * <p>
   * This method is called by the {@link #init(FMLInitializationEvent)} method for each block that
   * implements the {@link ICsmTESRProvider} interface.
   * </p>
   * <p>
   * If the block is not an instance of {@link ICsmTESRProvider}, a fatal error is logged, and the
   * game will crash. If the TESR is already registered, a warning is logged (once) and the TESR is
   * not registered again.
   * </p>
   *
   * @param block The block to register the TESR for.
   * @param <T>   The type of the tile entity.
   *
   * @since 1.1
   */
  private <T extends TileEntity> void registerBlockTESR(Block block) {
    try {
      // Check if the block is an ICsmTESRProvider
      if (!(block instanceof ICsmTESRProvider)) {
        Csm.getLogger().fatal("Illegal attempt to register non-existent TESR mapping for block: {}",
            block.getRegistryName());
        return;
      }

      // Cast the block to an ICsmTESRProvider (ignore the warning, we check instanceof above)
      ICsmTESRProvider<T> tesrProvider = (ICsmTESRProvider<T>) block;

      // Get TESR tile entity name
      String tesrName = CsmConstants.MOD_NAMESPACE + ":" + tesrProvider.getTileEntityName();

      // Check if tesrName is already registered
      if (!registeredTESRs.contains(tesrName)) {
        // Create a new TESR instance
        TileEntitySpecialRenderer<T> tesr = tesrProvider.getNewTESR();

        // Bind the TESR to the tile entity class
        ClientRegistry.bindTileEntitySpecialRenderer(tesrProvider.getTileEntityClass(), tesr);

        // Add the name to the set
        registeredTESRs.add(tesrName);
      } else if (!warnedTESRs.contains(tesrName)) {
        // Log a warning if the TESR is already registered
        Csm.getLogger()
            .warn("Attempted to register TESR for tile entity with name: {} more than once.",
                tesrName);

        // Add the name to the set
        warnedTESRs.add(tesrName);
      }
    } catch (Exception e) {
      Csm.getLogger().fatal("Failed to register TESR for block: " + block.getRegistryName(), e);
    }
  }

  /**
   * Post-initialize the mod. This method is called by Minecraft Forge during post-initialization.
   *
   * @param event : additionalData[0] The {@link FMLPostInitializationEvent} that is being
   *              processed.
   *
   * @since 1.0
   */
  @Override
  public void postInit(FMLPostInitializationEvent event) {
    // Not implemented (yet)
  }

  /**
   * Load the mod on the server. This method is called by Minecraft Forge during server load.
   *
   * @param event : additionalData[0] The {@link FMLServerStartingEvent} that is being processed.
   *
   * @since 1.0
   */
  @Override
  public void serverLoad(FMLServerStartingEvent event) {
    // Not implemented (yet)
  }

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
  @Override
  public void setCustomModelResourceLocation(Item item, int meta, String id) {
    if (item != null && item.getRegistryName() != null) {
      ModelLoader.setCustomModelResourceLocation(item, meta,
          new ModelResourceLocation(item.getRegistryName(), id));
    }
  }
}
