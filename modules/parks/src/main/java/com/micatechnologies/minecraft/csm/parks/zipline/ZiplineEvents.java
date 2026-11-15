package com.micatechnologies.minecraft.csm.parks.zipline;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Keeps a zipline rider on until it is safe to let go: sneaking off mid-line is refused, with a
 * word, until the brake zone over the landing ({@link EntityZiplineTrolley#mayDismount}).
 *
 * @version 1.0
 * @since 2026.11
 */
public class ZiplineEvents {

  @SubscribeEvent
  public void onMount(EntityMountEvent event) {
    if (!event.isDismounting() || !(event.getEntityBeingMounted() instanceof EntityZiplineTrolley)) {
      return;
    }
    EntityZiplineTrolley trolley = (EntityZiplineTrolley) event.getEntityBeingMounted();
    Entity rider = event.getEntityMounting();
    if (!trolley.mayDismount(rider)) {
      event.setCanceled(true);
      if (!event.getWorldObj().isRemote && rider instanceof EntityPlayer) {
        ((EntityPlayer) rider).sendStatusMessage(
            new TextComponentTranslation("csm.zipline.hold_on"), true);
      }
    }
  }
}
