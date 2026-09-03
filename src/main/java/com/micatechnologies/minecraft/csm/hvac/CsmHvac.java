package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: HVAC module — thermostats, air handlers, ducting and the rest of the heating and
 * cooling equipment.
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
@Mod(modid = CsmHvac.MOD_ID,
     name = CsmHvac.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmHvac {

  public static final String MOD_ID = "csm_hvac";
  public static final String MOD_NAME = "CSM: HVAC";

  /**
   * This module's network channel. Its packets are registered below, in a fixed order, so
   * their discriminators are the same on every client and server regardless of which other
   * modules are installed.
   *
   * @since 2026.9
   */
  public static final CsmNetwork NETWORK = CsmNetwork.create(MOD_ID);

  @Mod.Instance(MOD_ID)
  public static CsmHvac instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);

    CsmGuiRegistry.register(new HvacGuiProvider());

    // The packet order here fixes this channel's discriminators; only append to it.
    NETWORK.registerMessage(
        HvacThermostatConfigPacketHandler.class,
        HvacThermostatConfigPacket.class,
        Side.SERVER);
  }
}
