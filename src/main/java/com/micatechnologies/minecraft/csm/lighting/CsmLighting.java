package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.Tags;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Lighting module — street and area luminaires, the decorative pendant and sconce
 * family, and the rest of the lighting equipment.
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
@Mod(modid = CsmLighting.MOD_ID,
     name = CsmLighting.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmLighting {

  public static final String MOD_ID = "csm_lighting";
  public static final String MOD_NAME = "CSM: Lighting";

  @Mod.Instance(MOD_ID)
  public static CsmLighting instance;

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
