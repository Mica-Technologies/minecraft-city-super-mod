package com.micatechnologies.minecraft.csm.tts;

import com.micatechnologies.minecraft.csm.Tags;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Text to Speech module — the speech engine and the blocks that speak with it.
 *
 * <p>The class is named {@code CsmTextToSpeech} rather than {@code CsmTts} because
 * {@code codeutils.CsmTts} is the speech facade the rest of the mod calls; this class is only the
 * mod container.</p>
 *
 * <p>A module's mod container exists so that Forge serves the module jar's {@code assets/csm}
 * resources and shows it in the mod list. Content registration is entirely Core's: the creative
 * tab classes are discovered by {@code CsmTab.initTabs} through the ASM data table, blocks
 * register themselves with {@code CsmRegistry} as they are constructed, and Core's registry-event
 * listeners hand them to Forge under the {@code csm} namespace. Nothing here may call a Forge
 * registry directly.</p>
 *
 * <p>This module also requires Technology: its blocks appear in the Technology creative tab, so
 * without that module there would be no tab to put them in.</p>
 *
 * <p>The dependencies pin Core and Technology to this exact version. Every module jar is built
 * from the same tree and released together; a mismatch is a broken install and should fail at
 * startup rather than somewhere subtle later.</p>
 */
@Mod(modid = CsmTextToSpeech.MOD_ID,
     name = CsmTextToSpeech.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "];"
         + "required-after:csm_technology@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmTextToSpeech {

  public static final String MOD_ID = "csm_tts";
  public static final String MOD_NAME = "CSM: Text to Speech";

  @Mod.Instance(MOD_ID)
  public static CsmTextToSpeech instance;

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
