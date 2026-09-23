package com.micatechnologies.minecraft.csm.parks;

import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Parks &amp; Greenery module: street trees built from log and leaves blocks, the planting
 * tool that grows them, plantings, planters and park amenities.
 *
 * <p>A module's mod container exists so that Forge serves the module jar's {@code assets/csm}
 * resources and shows it in the mod list. Content registration is entirely Core's: the creative
 * tab classes in this jar are discovered by {@code CsmTab.initTabs} through the ASM data table,
 * their blocks register themselves with {@code CsmRegistry} as they are constructed, and Core's
 * registry-event listeners hand them to Forge under the {@code csm} namespace. Nothing here may
 * call a Forge registry directly.</p>
 *
 * <p>The dependency pins Core to this exact version. Every module jar is built from the same
 * tree and released together; a mismatch is a broken install and should fail at startup rather
 * than somewhere subtle later.</p>
 *
 * @since 2026.9
 */
@Mod(modid = CsmParks.MOD_ID,
     name = CsmParks.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmParks {

  public static final String MOD_ID = "csm_parks";
  public static final String MOD_NAME = "CSM: Parks & Greenery";

  @SidedProxy(
      clientSide = "com.micatechnologies.minecraft.csm.parks.CsmParksClientProxy",
      serverSide = "com.micatechnologies.minecraft.csm.parks.CsmParksCommonProxy")
  public static ICsmProxy proxy;

  @Mod.Instance(MOD_ID)
  public static CsmParks instance;

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
    CsmFabricatorCosts.registerRule(ParksFabricatorRules.TAB_ID, ParksFabricatorRules::price);
    CsmFabricatorCosts.registerRule(ParksFabricatorRules.PARKS_TAB_ID,
        ParksFabricatorRules::priceParks);

    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }
}
