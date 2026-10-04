package com.micatechnologies.minecraft.csm.transit;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.transit.fare.FareGateOpModeHandler;
import com.micatechnologies.minecraft.csm.transit.fare.FareGateOpModePacket;
import com.micatechnologies.minecraft.csm.transit.fare.FareVendingPurchaseHandler;
import com.micatechnologies.minecraft.csm.transit.fare.FareVendingPurchasePacket;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingPanelPacket;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingPanelPacketHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Transit module: the fare gates, the fare vending machine and the tickets and cards
 * they take, the bus stops, the bus shelters and the station and platform fit-out.
 *
 * <p>A module's mod container exists so that Forge serves the module jar's {@code assets/csm}
 * resources and shows it in the mod list. Content registration is entirely Core's: the creative
 * tab classes in this jar are discovered by {@code CsmTab.initTabs} through the ASM data table,
 * their blocks register themselves with {@code CsmRegistry} as they are constructed, and Core's
 * registry-event listeners hand them to Forge under the {@code csm} namespace. Nothing here may
 * call a Forge registry directly.</p>
 *
 * <p>This module also requires Roads &amp; Traffic: its bus stop flags, arrival display and poster
 * cases are road signs, subclasses of Roads' {@code AbstractBlockSign}, so they stand on Roads'
 * sign posts and take the sign system's setback, back-to-back pairing and extension post rather
 * than a second pole family of their own. The dependency only ever points this way; Roads names
 * nothing of this module's.</p>
 *
 * <p>The dependencies pin Core and Roads to this exact version. Every module jar is built from
 * the same tree and released together; a mismatch is a broken install and should fail at startup
 * rather than somewhere subtle later.</p>
 *
 * @since 2026.9
 */
@Mod(modid = CsmTransit.MOD_ID,
     name = CsmTransit.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "];"
         + "required-after:csm_roads@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmTransit {

  public static final String MOD_ID = "csm_transit";
  public static final String MOD_NAME = "CSM: Transit";

  /**
   * This module's network channel. Its packets are registered in {@link #preInit}, in a fixed
   * order, so their discriminators are the same on every client and server regardless of which
   * other modules are installed.
   */
  public static final CsmNetwork NETWORK = CsmNetwork.create(MOD_ID);

  @SidedProxy(
      clientSide = "com.micatechnologies.minecraft.csm.transit.CsmTransitClientProxy",
      serverSide = "com.micatechnologies.minecraft.csm.transit.CsmTransitCommonProxy")
  public static ICsmProxy proxy;

  @Mod.Instance(MOD_ID)
  public static CsmTransit instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);

    CsmGuiRegistry.register(new TransitGuiProvider());
    TransitSounds.registerSounds();

    // Safe here: Fabricator costs are first read at post-initialization and thereafter only
    // when a Fabricator GUI is opened, both after every mod's pre-initialization.
    CsmFabricatorCosts.registerRule(TransitFabricatorRules.TAB_ID, TransitFabricatorRules::price);

    // The packet order here fixes this channel's discriminators; only append to it.
    NETWORK.registerMessage(
        FareVendingPurchaseHandler.class,
        FareVendingPurchasePacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        FareGateOpModeHandler.class,
        FareGateOpModePacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        WayfindingPanelPacketHandler.class,
        WayfindingPanelPacket.class,
        Side.SERVER);

    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }
}
