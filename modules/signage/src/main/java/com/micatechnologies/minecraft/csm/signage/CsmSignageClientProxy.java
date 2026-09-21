package com.micatechnologies.minecraft.csm.signage;

import com.micatechnologies.minecraft.csm.codeutils.CsmLifecycleHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The signage module's proxy on the client: the board renderer, the outline a board's screen
 * draws in the world, and re-preparing the ad textures after a resource reload.
 */
public class CsmSignageClientProxy extends CsmSignageCommonProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    AdBoardPreview.register();
    // A lambda, not a method reference: see CsmTechnology on why that matters on a server.
    CsmLifecycleHooks.onClientDisconnect(() -> ServerAdImages.clear());
  }

  @Override
  public void init(FMLInitializationEvent event) {
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAdBoard.class,
        new TileEntityAdBoardRenderer());
    IResourceManager resources = Minecraft.getMinecraft().getResourceManager();
    if (resources instanceof IReloadableResourceManager) {
      // Registered after the texture manager, so this runs once the textures are uploaded again.
      ((IReloadableResourceManager) resources).registerReloadListener(
          manager -> AdTextures.reset());
    }
  }
}
