package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Sports & Venues module: the fit-out of an arena or stadium, from the seating bowl
 * (stadium seats, aisle lights and rails, guards, section signs) to the event floor, the
 * scoreboard and the concourse.
 *
 * <p>A module's mod container exists so that Forge serves the module jar's {@code assets/csm}
 * resources and shows it in the mod list. Content registration is entirely Core's: the creative
 * tab classes in this jar are discovered by {@code CsmTab.initTabs} through the ASM data table,
 * their blocks register themselves with {@code CsmRegistry} as they are constructed, and Core's
 * registry-event listeners hand them to Forge under the {@code csm} namespace. Nothing here may
 * call a Forge registry directly.</p>
 *
 * <p>Sports & Venues depends on Core alone. The dependency pins Core to this exact version: every
 * module jar is built from the same tree and released together, and a mismatch is a broken
 * install that should fail at startup.</p>
 *
 * @since 2026.10
 */
@Mod(modid = CsmVenues.MOD_ID,
     name = CsmVenues.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmVenues {

  public static final String MOD_ID = "csm_venues";
  public static final String MOD_NAME = "CSM: Sports & Venues";

  @SidedProxy(
      clientSide = "com.micatechnologies.minecraft.csm.venues.CsmVenuesClientProxy",
      serverSide = "com.micatechnologies.minecraft.csm.venues.CsmVenuesCommonProxy")
  public static ICsmProxy proxy;

  @Mod.Instance(MOD_ID)
  public static CsmVenues instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);

    // Safe here: Fabricator costs are first read at post-initialization and thereafter only
    // when a Fabricator GUI is opened, both after every mod's pre-initialization.
    CsmFabricatorCosts.registerRule(VenuesFabricatorRules.TAB_SEATING,
        VenuesFabricatorRules::price);

    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }
}
