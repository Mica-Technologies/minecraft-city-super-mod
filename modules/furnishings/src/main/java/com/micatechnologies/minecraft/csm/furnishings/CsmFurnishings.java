package com.micatechnologies.minecraft.csm.furnishings;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.Tags;
import com.micatechnologies.minecraft.csm.codeutils.CsmLifecycleHooks;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import com.micatechnologies.minecraft.csm.furniture.market.MarketFabricatorRules;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.CommandPawn;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnActionPacket;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnEvents;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnGuiProvider;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnQuotePacket;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValuation;
import com.micatechnologies.minecraft.csm.furniture.market.shop.ShopActionPacket;
import com.micatechnologies.minecraft.csm.furniture.market.shop.ShopEconomy;
import com.micatechnologies.minecraft.csm.furniture.market.shop.ShopGuiProvider;
import com.micatechnologies.minecraft.csm.furniture.market.shop.ShopStatusPacket;
import com.micatechnologies.minecraft.csm.furniture.office.CubicleNamePlatePacket;
import com.micatechnologies.minecraft.csm.furniture.office.CubicleNamePlatePacketHandler;
import com.micatechnologies.minecraft.csm.furniture.office.OfficeFabricatorRules;
import com.micatechnologies.minecraft.csm.furniture.outdoor.Bounce;
import com.micatechnologies.minecraft.csm.furniture.residential.BedSleepClientHandler;
import com.micatechnologies.minecraft.csm.furniture.residential.ResidentialFabricatorRules;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.novelties.ArcadeHighScoreHandler;
import com.micatechnologies.minecraft.csm.novelties.ArcadeHighScorePacket;
import com.micatechnologies.minecraft.csm.novelties.BlockHd;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import com.micatechnologies.minecraft.csm.novelties.NoveltiesGuiProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.relauncher.Side;
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

  @SidedProxy(
      clientSide = "com.micatechnologies.minecraft.csm.furnishings.CsmFurnishingsClientProxy",
      serverSide = "com.micatechnologies.minecraft.csm.furnishings.CsmFurnishingsCommonProxy")
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
  public static CsmFurnishings instance;

  private static Logger logger;

  public static Logger getLogger() {
    return logger;
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    logger.info("Pre-initializing " + MOD_NAME + " v" + Tags.VERSION);

    // The pawn shop's values file, config/csm/pawn_values.json.
    PawnValuation.setConfigDirectory(event.getModConfigurationDirectory());

    CsmGuiRegistry.register(new NoveltiesGuiProvider());
    CsmGuiRegistry.register(new ShopGuiProvider());
    CsmGuiRegistry.register(new PawnGuiProvider());

    // The Residential tab is this module's own, so Core prices it through the rule it is given.
    CsmFabricatorCosts.registerRule(ResidentialFabricatorRules.TAB_ID,
        ResidentialFabricatorRules::price);
    // So is the Commercial & Office tab.
    CsmFabricatorCosts.registerRule(OfficeFabricatorRules.TAB_ID,
        OfficeFabricatorRules::price);
    // And the Market & Store tab.
    CsmFabricatorCosts.registerRule(MarketFabricatorRules.TAB_ID,
        MarketFabricatorRules::price);

    // Trampolines, the bounce castle and the diving board: a jump off one goes higher. Both
    // sides, since a player's movement is the client's and a mob's the server's.
    MinecraftForge.EVENT_BUS.register(new Bounce());
    // Pays what pawn shops owe a player who was away when something was pawned in their name.
    MinecraftForge.EVENT_BUS.register(new PawnEvents());

    // A sleeper in a Residential bed is laid out along it on the client (the handler touches a
    // client-only field, so a dedicated server never loads it).
    if (event.getSide().isClient()) {
      MinecraftForge.EVENT_BUS.register(new BedSleepClientHandler());
    }

    // A lambda, not BlockHd::clearClientCaches: that method is @SideOnly(CLIENT), so it is
    // stripped from the class on a dedicated server and a method reference — which resolves the
    // method as soon as it is created — would fail there. A lambda resolves the call only when
    // it runs, and the client disconnect hooks only ever run on the client.
    CsmLifecycleHooks.onClientDisconnect(() -> BlockHd.clearClientCaches());

    // The packet order here fixes this channel's discriminators; only append to it.
    NETWORK.registerMessage(
        ArcadeHighScoreHandler.class,
        ArcadeHighScorePacket.class,
        Side.SERVER);
    // A cubicle panel's name plate, from its editor.
    NETWORK.registerMessage(
        CubicleNamePlatePacketHandler.class,
        CubicleNamePlatePacket.class,
        Side.SERVER);
    // A shop: a button on its counter or setup screen, and the status the server sends back.
    NETWORK.registerMessage(
        ShopActionPacket.Handler.class,
        ShopActionPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        ShopStatusPacket.Handler.class,
        ShopStatusPacket.class,
        Side.CLIENT);
    // A pawn counter: a button on its screens, and the quotes the server sends back.
    NETWORK.registerMessage(
        PawnActionPacket.Handler.class,
        PawnActionPacket.class,
        Side.SERVER);
    NETWORK.registerMessage(
        PawnQuotePacket.Handler.class,
        PawnQuotePacket.class,
        Side.CLIENT);

    // Hand this module's sound names to Core's registrar. Forge runs every mod's
    // pre-initialization before it fires the sound registry event, so Core sees the complete
    // union when it creates the sound events.
    FurnishingsSounds.registerSounds();
    proxy.preInit(event);
  }

  /**
   * Asks SUM's economy for the shops' handle, if SUM is there and allows {@code csm_furnishings}.
   * Server started, not starting: SUM attaches its economy in its own starting handler and loads
   * after CSM.
   *
   * @since 2026.10
   */
  @Mod.EventHandler
  public void serverStarted(FMLServerStartedEvent event) {
    ShopEconomy.SUM.acquire();
    // Every mod's recipes are registered by now, so the pawn shop can value everything. Not when
    // it is disabled: the solve walks every recipe in the game, and a command or quote that does
    // need a value solves on first use (PawnValuation.result()).
    if (CsmConfig.isPawnEnabled()) {
      PawnValuation.rebuild();
    }
  }

  /**
   * Registers {@code /csmpawn}.
   *
   * @since 2026.10
   */
  @Mod.EventHandler
  public void serverStarting(FMLServerStartingEvent event) {
    event.registerServerCommand(new CommandPawn());
  }

  /**
   * Drops the shops' economy handle.
   *
   * @since 2026.10
   */
  @Mod.EventHandler
  public void serverStopping(FMLServerStoppingEvent event) {
    ShopEconomy.SUM.release();
    PawnValuation.clear();
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    // Client: bind this module's tile-entity renderers (the clocks, the ceiling fan). Server:
    // nothing.
    proxy.init(event);
  }
}
