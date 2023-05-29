package com.micatechnologies.minecraft.csm;

import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.client.model.obj.OBJLoader;

/**
 * Sided proxy for initialization and loading of the mod on the client side. This class is referenced by the
 * {@code @SidedProxy} annotation in {@link CitySuperMod}. This class is instantiated by Forge.
 *
 * @version 1.0
 * @see ICsmProxy
 * @since 1.0
 */
public class CsmClientProxy implements ICsmProxy
{

    /**
     * Pre-initialize the mod. This method is called by Minecraft Forge during pre-initialization.
     *
     * @param event : additionalData[0] The {@link FMLPreInitializationEvent} that is being processed.
     *
     * @since 1.0
     */
    @Override
    public void preInit( FMLPreInitializationEvent event ) {
        OBJLoader.INSTANCE.addDomain( "csm" );
    }

    /**
     * Initialize the mod. This method is called by Minecraft Forge during initialization.
     *
     * @param event : additionalData[0] The {@link FMLInitializationEvent} that is being processed.
     *
     * @since 1.0
     */
    @Override
    public void init( FMLInitializationEvent event ) {
        // Not implemented (yet)
    }

    /**
     * Post-initialize the mod. This method is called by Minecraft Forge during post-initialization.
     *
     * @param event : additionalData[0] The {@link FMLPostInitializationEvent} that is being processed.
     *
     * @since 1.0
     */
    @Override
    public void postInit( FMLPostInitializationEvent event ) {
        // Not implemented (yet)
    }

    /**
     * Load the mod on the server. This method is called by Minecraft Forge during server load.
     *
     * @param event : additionalData[0] The {@link FMLServerStartingEvent} that is being processed.
     *
     * @since 1.0
     */
    @Override
    public void serverLoad( FMLServerStartingEvent event ) {
        // Not implemented (yet)
    }
}
