package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import com.micatechnologies.minecraft.csm.venues.av.ScoreboardPacket;
import com.micatechnologies.minecraft.csm.venues.av.VenueTextPacket;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Sports & Venues module: sports venues of every kind (arenas, stadiums, ballparks,
 * football and soccer fields, bowling alleys, courts, tracks and pools), from what they share
 * (the seating bowl, bleachers, signs, scoreboards, concourses) to each sport's own playing
 * surface and equipment.
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

  /**
   * This module's network channel. Its packets are registered in {@link #preInit}, in a fixed
   * order, so their discriminators are the same on every client and server whatever other
   * modules are installed.
   */
  public static final CsmNetwork NETWORK = CsmNetwork.create(MOD_ID);

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
    CsmFabricatorCosts.registerRule(VenuesFabricatorRules.TAB_ARENA,
        VenuesFabricatorRules::price);
    CsmFabricatorCosts.registerRule(VenuesFabricatorRules.TAB_CONCOURSE,
        VenuesFabricatorRules::price);
    CsmFabricatorCosts.registerRule(VenuesFabricatorRules.TAB_BALLPARK,
        VenuesFabricatorRules::price);
    CsmFabricatorCosts.registerRule(VenuesFabricatorRules.TAB_FOOTBALL,
        VenuesFabricatorRules::price);
    CsmFabricatorCosts.registerRule(VenuesFabricatorRules.TAB_BOWLING,
        VenuesFabricatorRules::price);
    CsmFabricatorCosts.registerRule(VenuesFabricatorRules.TAB_COURTS,
        VenuesFabricatorRules::price);
    CsmFabricatorCosts.registerRule(VenuesFabricatorRules.TAB_GOLF,
        VenuesFabricatorRules::price);
    CsmFabricatorCosts.registerRule(VenuesFabricatorRules.TAB_SKATE,
        VenuesFabricatorRules::price);
    VenuesSounds.registerSounds();

    CsmGuiRegistry.register(new VenuesGuiProvider());
    // The packet order here fixes this channel's discriminators; only append to it.
    NETWORK.registerMessage(ScoreboardPacket.Handler.class, ScoreboardPacket.class, Side.SERVER);
    NETWORK.registerMessage(VenueTextPacket.Handler.class, VenueTextPacket.class, Side.SERVER);

    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }
}
