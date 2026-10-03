package com.micatechnologies.minecraft.csm.vehicles;

import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.CsmPreemptSources;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Vehicles module: the City Super Mod's side of Immersive Vehicles.
 *
 * <p>It does two things. Its jar carries an Immersive Vehicles content pack (pack id
 * {@value #PACK_ID}, under {@code assets/csmvehicles/}): lightbars, siren speakers and a preemption
 * emitter that fit the vehicles other packs already provide. Immersive Vehicles finds that pack
 * itself, by scanning every jar in the mods folder for a {@code packdefinition.json}, so nothing
 * here registers it. And it tells Roads' preempt detectors where the emergency vehicles are, by
 * registering {@link IvPreemptSource} with Core's {@link CsmPreemptSources}: an intersection
 * preempts for any Immersive Vehicles vehicle running its emergency lights towards it.</p>
 *
 * <p>Immersive Vehicles is a hard dependency: without it this module has nothing to do. The
 * dependency pins Core and Roads to this exact version, like every module.</p>
 *
 * @since 2026.10
 */
@Mod(modid = CsmVehicles.MOD_ID,
     name = CsmVehicles.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "];"
         + "required-after:csm_roads@[" + Tags.VERSION + "];"
         + "required-after:mts",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmVehicles {

  public static final String MOD_ID = "csm_vehicles";
  public static final String MOD_NAME = "CSM: Vehicles";

  /** The Immersive Vehicles pack id of the content pack this jar carries. */
  public static final String PACK_ID = "csmvehicles";

  @Mod.Instance(MOD_ID)
  public static CsmVehicles instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);
    CsmPreemptSources.register(new IvPreemptSource());
  }
}
