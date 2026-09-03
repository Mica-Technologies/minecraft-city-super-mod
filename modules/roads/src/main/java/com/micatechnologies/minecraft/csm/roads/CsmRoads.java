package com.micatechnologies.minecraft.csm.roads;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.codeutils.CsmLifecycleHooks;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import com.micatechnologies.minecraft.csm.codeutils.packets.DynamicGuideSignUpdateHandler;
import com.micatechnologies.minecraft.csm.codeutils.packets.DynamicGuideSignUpdatePacket;
import com.micatechnologies.minecraft.csm.codeutils.packets.DynamicStreetSignUpdateHandler;
import com.micatechnologies.minecraft.csm.codeutils.packets.DynamicStreetSignUpdatePacket;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityPortableMessageSignUpdateHandler;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityPortableMessageSignUpdatePacket;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityVariableSpeedLimitUpdateHandler;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityVariableSpeedLimitUpdatePacket;
import com.micatechnologies.minecraft.csm.trafficaccessories.LaneControlSignalConfigPacket;
import com.micatechnologies.minecraft.csm.trafficaccessories.LaneControlSignalConfigPacketHandler;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.trafficaccessories.TrafficAccessoriesGuiProvider;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.SpanWireMountConfigPacket;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.SpanWireMountConfigPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.APSSoundPacket;
import com.micatechnologies.minecraft.csm.trafficsignals.APSSoundPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.AdvancedSignalControllerConfigPacket;
import com.micatechnologies.minecraft.csm.trafficsignals.AdvancedSignalControllerConfigPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.BlankoutBoxConfigPacket;
import com.micatechnologies.minecraft.csm.trafficsignals.BlankoutBoxConfigPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockOverheightDetectionSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.CrosswalkConfigPacket;
import com.micatechnologies.minecraft.csm.trafficsignals.CrosswalkConfigPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.RoadsSounds;
import com.micatechnologies.minecraft.csm.trafficsignals.SensorConfigPacket;
import com.micatechnologies.minecraft.csm.trafficsignals.SensorConfigPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalControllerConfigPacket;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalControllerConfigPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalControllerSetValuePacket;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalControllerSetValuePacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalHeadAppearancePacket;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalHeadAppearancePacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalHeadConfigPacket;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalHeadConfigPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalHeadSectionConfigPacket;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalHeadSectionConfigPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.TrafficSignalsFabricatorRules;
import com.micatechnologies.minecraft.csm.trafficsignals.TrafficSignalsGuiProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
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

  @SidedProxy(clientSide = "com.micatechnologies.minecraft.csm.roads.CsmRoadsClientProxy",
              serverSide = "com.micatechnologies.minecraft.csm.roads.CsmRoadsCommonProxy")
  public static ICsmProxy proxy;

  /**
   * This module's network channel. Its packets are registered below, in a fixed order, so
   * their discriminators are the same on every client and server regardless of which other
   * modules are installed.
   *
   * @since 2026.9
   */
  public static final CsmNetwork NETWORK = CsmNetwork.create(MOD_ID);

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

    // The packet order here fixes this channel's discriminators; only append to it.
    NETWORK.registerMessage(
        APSSoundPacketHandler.class,
        APSSoundPacket.class,
        Side.CLIENT);
    NETWORK.registerMessage(
        SignalHeadConfigPacketHandler.class,
        SignalHeadConfigPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        SignalHeadSectionConfigPacketHandler.class,
        SignalHeadSectionConfigPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        SignalHeadAppearancePacketHandler.class,
        SignalHeadAppearancePacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        SensorConfigPacketHandler.class,
        SensorConfigPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        CrosswalkConfigPacketHandler.class,
        CrosswalkConfigPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        BlankoutBoxConfigPacketHandler.class,
        BlankoutBoxConfigPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        SpanWireMountConfigPacketHandler.class,
        SpanWireMountConfigPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        LaneControlSignalConfigPacketHandler.class,
        LaneControlSignalConfigPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        SignalControllerConfigPacketHandler.class,
        SignalControllerConfigPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        SignalControllerSetValuePacketHandler.class,
        SignalControllerSetValuePacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        AdvancedSignalControllerConfigPacketHandler.class,
        AdvancedSignalControllerConfigPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        TileEntityPortableMessageSignUpdateHandler.class,
        TileEntityPortableMessageSignUpdatePacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        TileEntityVariableSpeedLimitUpdateHandler.class,
        TileEntityVariableSpeedLimitUpdatePacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        DynamicGuideSignUpdateHandler.class,
        DynamicGuideSignUpdatePacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        DynamicStreetSignUpdateHandler.class,
        DynamicStreetSignUpdatePacket.class,
        Side.SERVER);

    // Hand this module's sound names to Core's registrar. Forge runs every mod's
    // pre-initialization before it fires the sound registry event, so Core sees the complete
    // union when it creates the sound events.
    RoadsSounds.registerSounds();
    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    // Client: bind this module's tile-entity renderers. Server: nothing.
    proxy.init(event);
  }
}
