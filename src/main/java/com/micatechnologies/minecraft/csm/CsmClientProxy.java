package com.micatechnologies.minecraft.csm;

import com.micatechnologies.minecraft.csm.codeutils.CsmVersionChecker;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.codeutils.IHasModel;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignalNewRenderer;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignalRenderer;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHeadRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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

    // Register on the event bus early so we receive ModelRegistryEvent (fires during preInit)
    MinecraftForge.EVENT_BUS.register(this);
  }

  /**
   * Registers the mod's models with the game during the model registry event.
   *
   * @param event the model registry event
   *
   * @see ModelRegistryEvent
   * @since 1.0.0
   */
  @SubscribeEvent
  public void registerModels(ModelRegistryEvent event) {
    CsmRegistry.getBlocks().forEach(block -> {
      if (block instanceof IHasModel) {
        ((IHasModel) block).registerModels();
      }
    });
    CsmRegistry.getItems().forEach(item -> {
      if (item instanceof IHasModel) {
        ((IHasModel) item).registerModels();
      }
    });
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
    // Bind the TESR to the TileEntity
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTrafficSignalHead.class, new TileEntityTrafficSignalHeadRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCrosswalkSignal.class, new TileEntityCrosswalkSignalRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCrosswalkSignalNew.class, new TileEntityCrosswalkSignalNewRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmStrobe.class,
        new com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmStrobeRenderer());
    // Also bind to TileEntityFireAlarmSoundIndex so Gentex Commander 3 (which uses that TE
    // for sound selection) can also render strobe effects — the renderer checks IStrobeBlock
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmSoundIndex.class,
        new com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmStrobeRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.TileEntityEmergencyLight.class,
        new com.micatechnologies.minecraft.csm.lifesafety.TileEntityEmergencyLightRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityTrafficLightMountKit.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityTrafficLightMountKitRenderer());

    // Note: event bus registration moved to preInit() so ModelRegistryEvent is received.
    // The version check handler (onEntityJoinWorld) also benefits from that registration.
  }

  @SubscribeEvent
  public void onEntityJoinWorld(EntityJoinWorldEvent event) {
    if (event.getWorld().isRemote
        && event.getEntity() instanceof EntityPlayer
        && event.getEntity() == Minecraft.getMinecraft().player
        && CsmConfig.isUpdateCheckEnabled()) {
      CsmVersionChecker.checkForUpdatesAsync();
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
