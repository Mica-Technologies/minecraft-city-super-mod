package com.micatechnologies.minecraft.csm.healthcare;

import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.CsmLifecycleHooks;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.healthcare.nursecall.NurseCallLinking;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Healthcare module: the fit-out of a hospital, from the patient rooms and wards to the
 * nursing stations, the emergency department, imaging, the lab and the building systems a
 * hospital needs.
 *
 * <p>A module's mod container exists so that Forge serves the module jar's {@code assets/csm}
 * resources and shows it in the mod list. Content registration is entirely Core's: the creative
 * tab classes in this jar are discovered by {@code CsmTab.initTabs} through the ASM data table,
 * their blocks register themselves with {@code CsmRegistry} as they are constructed, and Core's
 * registry-event listeners hand them to Forge under the {@code csm} namespace. Nothing here may
 * call a Forge registry directly.</p>
 *
 * <p>Healthcare depends on Core alone. The dependency pins Core to this exact version: every
 * module jar is built from the same tree and released together, and a mismatch is a broken
 * install that should fail at startup.</p>
 *
 * @since 2026.10
 */
@Mod(modid = CsmHealthcare.MOD_ID,
     name = CsmHealthcare.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmHealthcare {

  public static final String MOD_ID = "csm_healthcare";
  public static final String MOD_NAME = "CSM: Healthcare";

  @SidedProxy(
      clientSide = "com.micatechnologies.minecraft.csm.healthcare.CsmHealthcareClientProxy",
      serverSide = "com.micatechnologies.minecraft.csm.healthcare.CsmHealthcareCommonProxy")
  public static ICsmProxy proxy;

  @Mod.Instance(MOD_ID)
  public static CsmHealthcare instance;

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
    CsmFabricatorCosts.registerRule(HealthcareFabricatorRules.TAB_PATIENT_CARE,
        HealthcareFabricatorRules::price);
    CsmFabricatorCosts.registerRule(HealthcareFabricatorRules.TAB_CLINICAL,
        HealthcareFabricatorRules::price);
    // A player's picked nurse call panel or dome light is forgotten when they leave.
    CsmLifecycleHooks.onPlayerLoggedOut(uuid -> NurseCallLinking.forget(uuid));
    HealthcareSounds.registerSounds();
    // A nurse call device, code blue button or wayfinding cell set over itself by /setblock.
    HealthcareStateChanges.register();

    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }

  @Mod.EventHandler
  public void serverStarting(FMLServerStartingEvent event) {
    event.registerServerCommand(new CommandHealthcare());
  }
}
