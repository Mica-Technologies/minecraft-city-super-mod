package com.micatechnologies.minecraft.csm.healthcare;

import com.micatechnologies.minecraft.csm.healthcare.clinical.TileEntityEdTrackingBoard;
import com.micatechnologies.minecraft.csm.healthcare.clinical.TileEntityEdTrackingBoardRenderer;
import com.micatechnologies.minecraft.csm.healthcare.queue.TileEntityNowServingDisplay;
import com.micatechnologies.minecraft.csm.healthcare.queue.TileEntityNowServingDisplayRenderer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * The healthcare module's proxy on the client. Every block is drawn from baked models except the
 * ED tracking board's screen, whose text {@link TileEntityEdTrackingBoardRenderer} draws, and the
 * now-serving display's, drawn by {@link TileEntityNowServingDisplayRenderer}. The hospital
 * wayfinding panel's legend is baked, by Core's wayfinding engine.
 *
 * @since 2026.10
 */
public class CsmHealthcareClientProxy extends CsmHealthcareCommonProxy {

  @Override
  public void init(FMLInitializationEvent event) {
    super.init(event);
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityEdTrackingBoard.class,
        new TileEntityEdTrackingBoardRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityNowServingDisplay.class,
        new TileEntityNowServingDisplayRenderer());
    MinecraftForge.EVENT_BUS.register(new HealthcareScreenSprites.Events());
  }
}
