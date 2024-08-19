package com.micatechnologies.minecraft.csm;

import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHeadRenderer;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
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
 * @version 1.0
 * @see ICsmProxy
 * @since 1.0
 */
public class CsmClientProxy implements ICsmProxy {

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
    // Not implemented (yet)

    // Bind the TESR to the TileEntity
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTrafficSignalHead.class, new TileEntityTrafficSignalHeadRenderer());
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
