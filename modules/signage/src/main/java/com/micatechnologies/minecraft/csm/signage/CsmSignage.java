package com.micatechnologies.minecraft.csm.signage;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.CsmLifecycleHooks;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import java.io.File;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Signage &amp; Advertising module — street ad kiosks, wall poster boards and billboards.
 *
 * <p>Road signs are not signage in this sense and stay in CSM: Roads &amp; Traffic; this module
 * is for the advertising and commercial signs a city puts up for itself.</p>
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
@Mod(modid = CsmSignage.MOD_ID,
     name = CsmSignage.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmSignage {

  public static final String MOD_ID = "csm_signage";
  public static final String MOD_NAME = "CSM: Signage & Advertising";

  /**
   * This module's network channel. Its packets are registered in {@link #preInit}, in a fixed
   * order, so their discriminators are the same on every client and server regardless of which
   * other modules are installed.
   */
  public static final CsmNetwork NETWORK = CsmNetwork.create(MOD_ID);

  @SidedProxy(
      clientSide = "com.micatechnologies.minecraft.csm.signage.CsmSignageClientProxy",
      serverSide = "com.micatechnologies.minecraft.csm.signage.CsmSignageCommonProxy")
  public static ICsmProxy proxy;

  @Mod.Instance(MOD_ID)
  public static CsmSignage instance;

  private static Logger logger;

  private static File configDirectory;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);
    configDirectory = event.getModConfigurationDirectory();
    SignageConfig.load(configDirectory);
    CsmGuiRegistry.register(new SignageGuiProvider());
    // The packet order here fixes this channel's discriminators; only append to it.
    NETWORK.registerMessage(AdBoardConfigHandler.class, AdBoardConfigPacket.class, Side.SERVER);
    NETWORK.registerMessage(ServerAdPackets.CatalogueHandler.class,
        ServerAdPackets.Catalogue.class, Side.CLIENT);
    NETWORK.registerMessage(ServerAdPackets.RequestHandler.class, ServerAdPackets.Request.class,
        Side.SERVER);
    NETWORK.registerMessage(ServerAdPackets.ChunkHandler.class, ServerAdPackets.Chunk.class,
        Side.CLIENT);
    MinecraftForge.EVENT_BUS.register(new ServerAdSync.Events());
    CsmLifecycleHooks.onPlayerLoggedOut(ServerAdSync::forget);
    proxy.preInit(event);
  }

  /** Reads the server's own ads, from {@code config/csm/ads/}, as it starts. */
  @Mod.EventHandler
  public void serverStarting(FMLServerStartingEvent event) {
    ServerAds.load(new File(new File(configDirectory, "csm"), "ads"),
        SignageConfig.isServerAdsAllowed());
  }

  @Mod.EventHandler
  public void serverStopped(FMLServerStoppedEvent event) {
    ServerAds.clear();
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }
}
