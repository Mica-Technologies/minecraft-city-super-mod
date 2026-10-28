package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.technology.rack.RackModels;
import com.micatechnologies.minecraft.csm.technology.school.TileEntitySchoolClock;
import com.micatechnologies.minecraft.csm.technology.school.TileEntitySchoolClockRenderer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The technology module's proxy on the client: the renderer for the school clocks' hands, and
 * the server racks' units, baked into the chunk mesh by {@link RackModels}. Everything else in
 * the module is drawn from baked models.
 *
 * @since 2026.10
 */
public class CsmTechnologyClientProxy extends CsmTechnologyCommonProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    RackModels.register();
    // With SUM installed, the iPhones open SUM's phone (by reflection; SUM stays optional).
    // Client only: ItemTooltipEvent references client classes, so subscribing to it on a
    // dedicated server crashes the boot.
    MinecraftForge.EVENT_BUS.register(new IphoneOpensSumPhone());
  }

  @Override
  public void init(FMLInitializationEvent event) {
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySchoolClock.class,
        new TileEntitySchoolClockRenderer());
  }
}
