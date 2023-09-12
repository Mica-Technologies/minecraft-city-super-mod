/*
 *    MCreator note:
 *
 *    If you lock base mod element files, you can edit this file and the proxy files
 *    and they won't get overwritten. If you change your mod package or modid, you
 *    need to apply these changes to this file MANUALLY.
 *
 *    Settings in @Mod annotation WON'T be changed in case of the base mod element
 *    files lock too, so you need to set them manually here in such case.
 *
 *    Keep the ElementsCitySuperMod object in this class and all calls to this object
 *    INTACT in order to preserve functionality of mod elements generated by MCreator.
 *
 *    If you do not lock base mod element files in Workspace settings, this file
 *    will be REGENERATED on each build.
 *
 */
package com.micatechnologies.minecraft.csm;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import com.micatechnologies.minecraft.csm.codeutils.IHasModel;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * The main class of the City Super Mod.
 * <p>
 * This class performs  initialization and registers the mod with Forge. It also contains the network channel and sided
 * proxy for the mod.
 *
 * @version N/A
 * @since 1.0.0
 */
@Mod( modid = CsmConstants.MOD_NAMESPACE, name = CsmConstants.MOD_NAME, version = CsmConstants.MOD_VERSION )
public class Csm
{
    /**
     * The network channel for the mod. Used for sending packets between client and server.
     *
     * @since 1.0.0
     */
    public static final SimpleNetworkWrapper PACKET_HANDLER = NetworkRegistry.INSTANCE.newSimpleChannel(
            CsmConstants.MOD_NAMESPACE + ":a" );

    /**
     * The sided proxy for initialization and loading of client or server/common specific code.
     *
     * @since 1.0.0
     */
    @SidedProxy( clientSide = "com.micatechnologies.minecraft.csm.CsmClientProxy", serverSide = "com.micatechnologies.minecraft.csm.CsmCommonProxy" )
    public static ICsmProxy proxy;

    /**
     * The mod instance.
     *
     * @since 1.0.0
     */
    @Mod.Instance( CsmConstants.MOD_NAMESPACE )
    public static Csm instance;

    // Enables the required universal bucket
    static {
        FluidRegistry.enableUniversalBucket();
    }

    /**
     * The world generator for the mod.
     *
     * @since 2.0.0
     */
    public CsmWorldGenerator csmWorldGenerator = new CsmWorldGenerator();

    /**
     * The logger for the mod.
     *
     * @since 2.0.0
     */
    private static Logger logger;

    /**
     * Performs pre-initialization of the mod by registering tabs, blocks, items, etc.
     *
     * @param event the pre-initialization event
     *
     * @see FMLPreInitializationEvent
     * @see Mod.EventHandler
     * @since 1.0.0
     */
    @Mod.EventHandler
    public void preInit( FMLPreInitializationEvent event ) {
        // Load the logger
        logger = event.getModLog();

        // Output start of pre-initialization
        logger.info( "Pre-initializing " + CsmConstants.MOD_NAME + " v" + CsmConstants.MOD_VERSION );

        // Register the mod's event bus
        logger.info( "Registering event bus" );
        MinecraftForge.EVENT_BUS.register( this );
        logger.info( "Finished registering event bus" );

        // Register the mod's world generator
        logger.info( "Registering world generator" );
        GameRegistry.registerWorldGenerator( csmWorldGenerator, CsmWorldGenerator.WORLD_GENERATION_WEIGHT );
        logger.info( "Finished registering world generator" );

        // Register the mod's network message(s)
        logger.info( "Registering network message(s)" );
        CsmRegistry.registerNetworkMessage( CitySuperModVariables.WorldSavedDataSyncMessageHandler.class,
                                            CitySuperModVariables.WorldSavedDataSyncMessage.class, Side.SERVER,
                                            Side.CLIENT );
        logger.info( "Finished registering network message(s)" );

        // Build the mod's tabs and elements
        logger.info( "Building mod tabs and elements" );
        try {
            CsmTab.initTabs( event );
        }
        catch ( Exception e ) {
            logger.error( "Failed to build tabs and/or elements!", e );
        }
        logger.info( "Finished building mod tabs and elements" );

        // Call pre-initialization of the proxy (client or server/common)
        String side = event.getSide().isClient() ? "client" : "server";
        logger.info( "Calling pre-initialization of proxy (" + side + ")" );
        proxy.preInit( event );
        logger.info( "Finished pre-initialization of proxy (" + side + ")" );

        // Output completion of pre-initialization
        logger.info( "Finished pre-initializing " + CsmConstants.MOD_NAME + " v" + CsmConstants.MOD_VERSION );
    }

    /**
     * Performs initialization of the mod by registering handlers, network messages, etc.
     *
     * @param event the initialization event
     *
     * @see FMLInitializationEvent
     * @see Mod.EventHandler
     * @since 1.0.0
     */
    @Mod.EventHandler
    public void init( FMLInitializationEvent event ) {
        // Output start of initialization
        logger.info( "Initializing " + CsmConstants.MOD_NAME + " v" + CsmConstants.MOD_VERSION );

        // Register the mod's GUI handler
        logger.info( "Registering GUI handler" );
        NetworkRegistry.INSTANCE.registerGuiHandler( this, new CsmGuiHandler() );
        logger.info( "Finished registering GUI handler" );

        // Call initialization of the proxy (client or server/common)
        String side = event.getSide().isClient() ? "client" : "server";
        logger.info( "Calling initialization of proxy (" + side + ")" );
        proxy.init( event );
        logger.info( "Finished initialization of proxy (" + side + ")" );

        // Output completion of initialization
        logger.info( "Finished initializing " + CsmConstants.MOD_NAME + " v" + CsmConstants.MOD_VERSION );
    }

    /**
     * Performs post-initialization of the mod by registering recipes, etc.
     *
     * @param event the post-initialization event
     *
     * @see FMLPostInitializationEvent
     * @see Mod.EventHandler
     * @since 1.0.0
     */
    @Mod.EventHandler
    public void postInit( FMLPostInitializationEvent event ) {
        // Output start of post-initialization
        logger.info( "Post-initializing " + CsmConstants.MOD_NAME + " v" + CsmConstants.MOD_VERSION );

        // Call post-initialization of the proxy (client or server/common)
        String side = event.getSide().isClient() ? "client" : "server";
        logger.info( "Calling post-initialization of proxy (" + side + ")" );
        proxy.postInit( event );
        logger.info( "Finished post-initialization of proxy (" + side + ")" );

        // Output completion of post-initialization
        logger.info( "Finished post-initializing " + CsmConstants.MOD_NAME + " v" + CsmConstants.MOD_VERSION );
    }

    /**
     * Performs server-load initialization of the mod by registering commands, etc.
     *
     * @param event the server-load initialization event
     *
     * @see FMLPostInitializationEvent
     * @see Mod.EventHandler
     * @since 1.0.0
     */
    @Mod.EventHandler
    public void serverLoad( FMLServerStartingEvent event ) {
        // Output start of server-load initialization
        logger.info( "Handling server load for " + CsmConstants.MOD_NAME + " v" + CsmConstants.MOD_VERSION );

        // Call server load of the proxy (client or server/common)
        String side = event.getSide().isClient() ? "client" : "server";
        logger.info( "Calling server-load initialization of proxy (" + side + ")" );
        proxy.serverLoad( event );
        logger.info( "Finished server-load initialization of proxy (" + side + ")" );

        // Output completion of server-load initialization
        logger.info( "Finished handling server load for " + CsmConstants.MOD_NAME + " v" + CsmConstants.MOD_VERSION );
    }

    /**
     * Registers the mod's blocks, items, etc. with the game during the block registry event.
     *
     * @param event the block registry event
     *
     * @see RegistryEvent.Register
     * @see Mod.EventBusSubscriber
     * @since 1.0.0
     */
    @SubscribeEvent
    public void registerBlocks( RegistryEvent.Register< Block > event ) {
        event.getRegistry().registerAll( CsmRegistry.getBlocks().toArray( new Block[ 0 ] ) );
    }

    /**
     * Registers the mod's items with the game during the item registry event.
     *
     * @param event the item registry event
     *
     * @see RegistryEvent.Register
     * @see Mod.EventBusSubscriber
     * @since 1.0.0
     */
    @SubscribeEvent
    public void registerItems( RegistryEvent.Register< Item > event ) {
        event.getRegistry().registerAll( CsmRegistry.getItems().toArray( new Item[ 0 ] ) );
    }

    /**
     * Registers the mod's sounds with the game during the sound registry event.
     *
     * @param event the sound registry event
     *
     * @see RegistryEvent.Register
     * @see Mod.EventBusSubscriber
     * @since 1.0.0
     */
    @SubscribeEvent
    public void registerSounds( RegistryEvent.Register< SoundEvent > event ) {
        CsmSounds.registerSounds( event );
    }

    /**
     * Registers the mod's models with the game during the model registry event.
     *
     * @param event the model registry event
     *
     * @see ModelRegistryEvent
     * @see Mod.EventBusSubscriber
     * @since 1.0.0
     */
    @SubscribeEvent
    @SideOnly( Side.CLIENT )
    public void registerModels( ModelRegistryEvent event ) {
        CsmRegistry.getBlocks().forEach( block -> {
            if ( block instanceof IHasModel ) {
                ( ( IHasModel ) block ).registerModels();
            }
        } );
        CsmRegistry.getItems().forEach( item -> {
            if ( item instanceof IHasModel ) {
                ( ( IHasModel ) item ).registerModels();
            }
        } );
    }

    /**
     * Handles the player log in event by sending the player the mod's world and map data.
     *
     * @param event the player log in event
     *
     * @see PlayerEvent.PlayerLoggedInEvent
     * @see Mod.EventBusSubscriber
     * @since 1.0.0
     */
    @SubscribeEvent
    public void onPlayerLoggedIn( PlayerEvent.PlayerLoggedInEvent event ) {
        if ( !event.player.world.isRemote ) {
            WorldSavedData mapdata = CitySuperModVariables.MapVariables.get( event.player.world );
            WorldSavedData worlddata = CitySuperModVariables.WorldVariables.get( event.player.world );
            if ( mapdata != null ) {
                Csm.PACKET_HANDLER.sendTo( new CitySuperModVariables.WorldSavedDataSyncMessage( 0, mapdata ),
                                           ( EntityPlayerMP ) event.player );
            }
            if ( worlddata != null ) {
                Csm.PACKET_HANDLER.sendTo( new CitySuperModVariables.WorldSavedDataSyncMessage( 1, worlddata ),
                                           ( EntityPlayerMP ) event.player );
            }
        }
    }

    /**
     * Handles the player changed dimension event by sending the player the mod's world data.
     *
     * @param event the player changed dimension event
     *
     * @see PlayerEvent.PlayerChangedDimensionEvent
     * @see Mod.EventBusSubscriber
     * @since 1.0.0
     */
    @SubscribeEvent
    public void onPlayerChangedDimension( net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event ) {
        if ( !event.player.world.isRemote ) {
            WorldSavedData worlddata = CitySuperModVariables.WorldVariables.get( event.player.world );
            if ( worlddata != null ) {
                Csm.PACKET_HANDLER.sendTo( new CitySuperModVariables.WorldSavedDataSyncMessage( 1, worlddata ),
                                           ( EntityPlayerMP ) event.player );
            }
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}