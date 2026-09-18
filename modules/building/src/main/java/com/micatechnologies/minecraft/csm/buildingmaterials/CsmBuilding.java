package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import com.micatechnologies.minecraft.csm.constructionsite.BuildingGuiProvider;
import com.micatechnologies.minecraft.csm.constructionsite.CraneHeadConfigPacket;
import com.micatechnologies.minecraft.csm.constructionsite.CraneHeadConfigPacketHandler;
import com.micatechnologies.minecraft.csm.constructionsite.ScaffoldRailCollision;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

/**
 * The CSM: Building Materials module — the block, stair, slab and fence sets the rest of a city
 * is built out of.
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
@Mod(modid = CsmBuilding.MOD_ID,
     name = CsmBuilding.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmBuilding {

  public static final String MOD_ID = "csm_building";
  public static final String MOD_NAME = "CSM: Building Materials";

  @SidedProxy(
      clientSide = "com.micatechnologies.minecraft.csm.buildingmaterials.CsmBuildingClientProxy",
      serverSide = "com.micatechnologies.minecraft.csm.buildingmaterials.CsmBuildingCommonProxy")
  public static ICsmProxy proxy;

  /**
   * This module's network channel. Its packets are registered in {@link #preInit}, in a fixed
   * order and only ever appended to, so their discriminators match on every client and server.
   *
   * @since 2026.9
   */
  public static final CsmNetwork NETWORK = CsmNetwork.create(MOD_ID);

  @Mod.Instance(MOD_ID)
  public static CsmBuilding instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);
    // Guardrail collision that a jump would otherwise clear. Both sides, so they agree.
    ScaffoldRailCollision.register();
    CsmGuiRegistry.register(new BuildingGuiProvider());
    // The packet order here fixes this channel's discriminators; only append to it.
    NETWORK.registerMessage(CraneHeadConfigPacketHandler.class, CraneHeadConfigPacket.class,
        Side.SERVER);
    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }
}
