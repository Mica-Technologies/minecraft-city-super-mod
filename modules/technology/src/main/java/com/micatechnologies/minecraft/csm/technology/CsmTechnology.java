package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.CsmLifecycleHooks;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Technology module — computers, servers, routers, televisions, speakers and the transit
 * fare equipment.
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
@Mod(modid = CsmTechnology.MOD_ID,
     name = CsmTechnology.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmTechnology {

  public static final String MOD_ID = "csm_technology";
  public static final String MOD_NAME = "CSM: Technology";

  /**
   * This module's network channel. Its packets are registered below, in a fixed order, so
   * their discriminators are the same on every client and server regardless of which other
   * modules are installed.
   *
   * @since 2026.9
   */
  public static final CsmNetwork NETWORK = CsmNetwork.create(MOD_ID);

  @Mod.Instance(MOD_ID)
  public static CsmTechnology instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);

    CsmGuiRegistry.register(new TechnologyGuiProvider());

    // A lambda, not SpeakerAmbientPacketHandler::stopAllSounds: that method is
    // @SideOnly(CLIENT), so it is stripped from the class on a dedicated server and a method
    // reference — which resolves the method as soon as it is created — would fail there. A
    // lambda resolves the call only when it runs, and the client disconnect hooks only ever run
    // on the client.
    CsmLifecycleHooks.onClientDisconnect(() -> SpeakerAmbientPacketHandler.stopAllSounds());

    // The packet order here fixes this channel's discriminators; only append to it.
    NETWORK.registerMessage(
        ComputerNotepadHandler.class,
        ComputerNotepadPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        SpeakerAmbientPacketHandler.class,
        SpeakerAmbientPacket.class,
        Side.CLIENT);
    NETWORK.registerMessage(
        FareVendingPurchaseHandler.class,
        FareVendingPurchasePacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        FareGateOpModeHandler.class,
        FareGateOpModePacket.class,
        Side.SERVER);

    // Hand this module's sound names to Core's registrar. Forge runs every mod's
    // pre-initialization before it fires the sound registry event, so Core sees the complete
    // union when it creates the sound events.
    TechnologySounds.registerSounds();
  }
}
