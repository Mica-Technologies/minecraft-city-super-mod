package com.micatechnologies.minecraft.csm.furnishings;

import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.CsmLifecycleHooks;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import com.micatechnologies.minecraft.csm.novelties.BlockHd;
import com.micatechnologies.minecraft.csm.novelties.NoveltiesGuiProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Furniture &amp; Novelties module — indoor and outdoor furniture, arcade cabinets and
 * the decorative novelties.
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
@Mod(modid = CsmFurnishings.MOD_ID,
     name = CsmFurnishings.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmFurnishings {

  public static final String MOD_ID = "csm_furnishings";
  public static final String MOD_NAME = "CSM: Furniture & Novelties";

  @Mod.Instance(MOD_ID)
  public static CsmFurnishings instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);

    CsmGuiRegistry.register(new NoveltiesGuiProvider());

    // A lambda, not BlockHd::clearClientCaches: that method is @SideOnly(CLIENT), so it is
    // stripped from the class on a dedicated server and a method reference — which resolves the
    // method as soon as it is created — would fail there. A lambda resolves the call only when
    // it runs, and the client disconnect hooks only ever run on the client.
    CsmLifecycleHooks.onClientDisconnect(() -> BlockHd.clearClientCaches());
  }
}
