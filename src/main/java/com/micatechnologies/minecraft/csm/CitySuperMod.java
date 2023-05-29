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
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.world.biome.Biome;
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
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
public class CitySuperMod
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
    public static CitySuperMod instance;

    // Enables the required universal bucket
    static {
        FluidRegistry.enableUniversalBucket();
    }

    public ElementsCitySuperMod elements = new ElementsCitySuperMod();

    /**
     * The world generator for the mod.
     *
     * @since 2.0.0
     */
    public CsmWorldGenerator csmWorldGenerator = new CsmWorldGenerator();

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
        // Register the mod's event bus
        MinecraftForge.EVENT_BUS.register( this );

        // Register the mod's world generator
        GameRegistry.registerWorldGenerator( csmWorldGenerator, CsmWorldGenerator.WORLD_GENERATION_WEIGHT );

        // Initialize the mod's tabs
        try {
            CsmTab.initTabs( event );
        }
        catch ( Exception e ) {
            System.err.println( "Failed to initialize tabs!" );
            e.printStackTrace();
        }
        elements.preInit( event );
        MinecraftForge.EVENT_BUS.register( elements );
        elements.getElements().forEach( element -> element.preInit( event ) );

        // Call pre-initialization of the proxy (client or server/common)
        proxy.preInit( event );
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
        elements.getElements().forEach( element -> element.init( event ) );
        NetworkRegistry.INSTANCE.registerGuiHandler( this, new CsmGuiHandler() );

        // Call initialization of the proxy (client or server/common)
        proxy.init( event );
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
        // Call post-initialization of the proxy (client or server/common)
        proxy.postInit( event );
    }

    /**
     * Performs server-starting initialization of the mod by registering commands, etc.
     *
     * @param event the server-starting initialization event
     *
     * @see FMLPostInitializationEvent
     * @see Mod.EventHandler
     * @since 1.0.0
     */
    @Mod.EventHandler
    public void serverLoad( FMLServerStartingEvent event ) {
        // Call server load of the proxy (client or server/common)
        proxy.serverLoad( event );
    }

    @SubscribeEvent
    public void registerBlocks( RegistryEvent.Register< Block > event ) {
        event.getRegistry().registerAll( elements.getBlocks().stream().map( Supplier::get ).toArray( Block[]::new ) );
    }

    @SubscribeEvent
    public void registerItems( RegistryEvent.Register< Item > event ) {
        event.getRegistry().registerAll( elements.getItems().stream().map( Supplier::get ).toArray( Item[]::new ) );
    }

    @SubscribeEvent
    public void registerBiomes( RegistryEvent.Register< Biome > event ) {
        event.getRegistry().registerAll( elements.getBiomes().stream().map( Supplier::get ).toArray( Biome[]::new ) );
    }

    @SubscribeEvent
    public void registerEntities( RegistryEvent.Register< EntityEntry > event ) {
        event.getRegistry()
             .registerAll( elements.getEntities().stream().map( Supplier::get ).toArray( EntityEntry[]::new ) );
    }

    @SubscribeEvent
    public void registerPotions( RegistryEvent.Register< Potion > event ) {
        event.getRegistry().registerAll( elements.getPotions().stream().map( Supplier::get ).toArray( Potion[]::new ) );
    }

    @SubscribeEvent
    public void registerSounds( RegistryEvent.Register< net.minecraft.util.SoundEvent > event ) {
        CsmSounds.registerSounds( event );
    }

    @SubscribeEvent
    @SideOnly( Side.CLIENT )
    public void registerModels( ModelRegistryEvent event ) {
        elements.getElements().forEach( element -> element.registerModels( event ) );
    }

    @SubscribeEvent
    public void onPlayerLoggedIn( net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event ) {
        if ( !event.player.world.isRemote ) {
            WorldSavedData mapdata = CitySuperModVariables.MapVariables.get( event.player.world );
            WorldSavedData worlddata = CitySuperModVariables.WorldVariables.get( event.player.world );
            if ( mapdata != null ) {
                CitySuperMod.PACKET_HANDLER.sendTo( new CitySuperModVariables.WorldSavedDataSyncMessage( 0, mapdata ),
                                                    ( EntityPlayerMP ) event.player );
            }
            if ( worlddata != null ) {
                CitySuperMod.PACKET_HANDLER.sendTo( new CitySuperModVariables.WorldSavedDataSyncMessage( 1, worlddata ),
                                                    ( EntityPlayerMP ) event.player );
            }
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension( net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event ) {
        if ( !event.player.world.isRemote ) {
            WorldSavedData worlddata = CitySuperModVariables.WorldVariables.get( event.player.world );
            if ( worlddata != null ) {
                CitySuperMod.PACKET_HANDLER.sendTo( new CitySuperModVariables.WorldSavedDataSyncMessage( 1, worlddata ),
                                                    ( EntityPlayerMP ) event.player );
            }
        }
    }
}
