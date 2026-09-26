package com.micatechnologies.minecraft.csm.powergrid;

import com.micatechnologies.minecraft.csm.Tags;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Utilities module -- utility poles, insulators, cross arms and the Forge Energy blocks
 * (the Power Grid tab), and the services a city runs to its buildings (the Utilities tab).
 *
 * <p>It was CSM: Power Grid. Only the display name changed: the mod id stays
 * {@code csm_powergrid} and the module tree {@code modules/powergrid}, and every registry name,
 * asset path and GUI id is the one it had, so a world saved with Power Grid loads unchanged.</p>
 *
 * <p>A module's mod container exists so that Forge serves the module jar's {@code assets/csm}
 * resources and shows it in the mod list. Content registration is entirely Core's: the creative
 * tab classes in this jar are discovered by {@code CsmTab.initTabs} through the ASM data table,
 * their blocks register themselves with {@code CsmRegistry} as they are constructed, and Core's
 * registry-event listeners hand them to Forge under the {@code csm} namespace. Nothing here may
 * call a Forge registry directly.</p>
 *
 * <p>This module also requires Roads &amp; Traffic: the larger utility pieces are Roads' utility
 * box multi-block (the root draws the whole unit, invisible parts fill the rest, placing is all
 * or nothing), and what stands at street level settles onto the road surface the way Roads'
 * street fixtures do. The dependency only ever points this way; Roads names nothing here.</p>
 *
 * <p>The dependencies pin Core and Roads to this exact version. Every module jar is built from
 * the same tree and released together; a mismatch is a broken install and should fail at startup
 * rather than somewhere subtle later.</p>
 */
@Mod(modid = CsmPowerGrid.MOD_ID,
     name = CsmPowerGrid.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "];"
         + "required-after:csm_roads@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmPowerGrid {

  public static final String MOD_ID = "csm_powergrid";
  public static final String MOD_NAME = "CSM: Utilities";

  @Mod.Instance(MOD_ID)
  public static CsmPowerGrid instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);
  }
}
