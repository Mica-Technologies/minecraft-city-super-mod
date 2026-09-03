package com.micatechnologies.minecraft.csm.roads;

import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.CsmLifecycleHooks;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.trafficaccessories.TrafficAccessoriesGuiProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.APSSoundPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockOverheightDetectionSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.TrafficSignalsFabricatorRules;
import com.micatechnologies.minecraft.csm.trafficsignals.TrafficSignalsGuiProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Roads &amp; Traffic module — traffic signals, traffic accessories and road signs.
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
 */
@Mod(modid = CsmRoads.MOD_ID,
     name = CsmRoads.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmRoads {

  public static final String MOD_ID = "csm_roads";
  public static final String MOD_NAME = "CSM: Roads & Traffic";

  @Mod.Instance(MOD_ID)
  public static CsmRoads instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);

    CsmGuiRegistry.register(new TrafficSignalsGuiProvider());
    CsmGuiRegistry.register(new TrafficAccessoriesGuiProvider());

    // Safe here: Fabricator costs are first read at post-initialization and thereafter only
    // when a Fabricator GUI is opened, both after every mod's pre-initialization.
    CsmFabricatorCosts.registerRule(TrafficSignalsFabricatorRules.TAB_ID,
        TrafficSignalsFabricatorRules::price);

    // A lambda, not APSSoundPacketHandler::stopAllSounds: that method is @SideOnly(CLIENT), so
    // it is stripped from the class on a dedicated server and a method reference — which
    // resolves the method as soon as it is created — would fail there. A lambda resolves the
    // call only when it runs, and the client disconnect hooks only ever run on the client.
    CsmLifecycleHooks.onClientDisconnect(() -> APSSoundPacketHandler.stopAllSounds());
    CsmLifecycleHooks.onPlayerLoggedOut(BlockOverheightDetectionSensor::clearPendingPairing);
  }
}
