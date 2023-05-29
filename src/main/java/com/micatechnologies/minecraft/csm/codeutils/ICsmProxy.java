package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public interface ICsmProxy
{
    /**
     * Pre-initialize the mod. This method is called by Minecraft Forge during pre-initialization.
     *
     * @param event : additionalData[0] The {@link FMLPreInitializationEvent} that is being processed.
     *
     * @since 1.0
     */
    void preInit( FMLPreInitializationEvent event );

    /**
     * Initialize the mod. This method is called by Minecraft Forge during initialization.
     *
     * @param event : additionalData[0] The {@link FMLInitializationEvent} that is being processed.
     *
     * @since 1.0
     */
    void init( FMLInitializationEvent event );

    /**
     * Post-initialize the mod. This method is called by Minecraft Forge during post-initialization.
     *
     * @param event : additionalData[0] The {@link FMLPostInitializationEvent} that is being processed.
     *
     * @since 1.0
     */
    void postInit( FMLPostInitializationEvent event );

    /**
     * Load the mod on the server. This method is called by Minecraft Forge during server load.
     *
     * @param event : additionalData[0] The {@link FMLServerStartingEvent} that is being processed.
     *
     * @since 1.0
     */
    void serverLoad( FMLServerStartingEvent event );
}
