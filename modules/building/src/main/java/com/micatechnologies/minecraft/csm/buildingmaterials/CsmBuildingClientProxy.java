package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.constructionsite.ScaffoldClimbHandler;
import com.micatechnologies.minecraft.csm.constructionsite.TileEntityCraneHead;
import com.micatechnologies.minecraft.csm.constructionsite.TileEntityCraneHeadRenderer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The building module's proxy on the client: the site climb handler, and the crane head's and
 * the moving garage door's and the swinging door's renderers.
 *
 * @version 1.0
 * @since 2026.9
 */
public class CsmBuildingClientProxy extends CsmBuildingCommonProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    // Jump-to-climb for scaffolding and crane masts. The client decides its own player's
    // movement, so this lives here and nowhere else.
    ScaffoldClimbHandler.register();
  }

  @Override
  public void init(FMLInitializationEvent event) {
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCraneHead.class,
        new TileEntityCraneHeadRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityGarageDoor.class,
        new TileEntityGarageDoorRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDoorSwing.class,
        new TileEntityDoorSwingRenderer());
  }
}
