package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockPCC;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.hvac.BlockART1;
import com.micatechnologies.minecraft.csm.hvac.BlockSV4;
import com.micatechnologies.minecraft.csm.novelties.*;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for novelty blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabNovelties extends CsmTab
{
    /**
     * Gets the ID (unique identifier) of the tab.
     *
     * @return the ID of the tab
     *
     * @since 1.0
     */
    @Override
    public String getTabId() {
        return "tabnovelties";
    }

    /**
     * Gets the block to use as the icon of the tab
     *
     * @return the block to use as the icon of the tab
     *
     * @since 1.0
     */
    @Override
    public Block getTabIcon() {
        return CsmRegistry.getBlock( "airhockeytable" );
    }

    /**
     * Gets a boolean indicating if the tab is searchable (has its own search bar).
     *
     * @return {@code true} if the tab is searchable, otherwise {@code false}
     *
     * @since 1.0
     */
    @Override
    public boolean getTabSearchable() {
        return false;
    }

    /**
     * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
     *
     * @return {@code true} if the tab is hidden, otherwise {@code false}
     *
     * @since 1.0
     */
    @Override
    public boolean getTabHidden() {
        return false;
    }

    /**
     * Initializes all the items belonging to the tab.
     *
     * @since 1.0
     */
    @Override
    public void initTabElements( FMLPreInitializationEvent fmlPreInitializationEvent ) {
        initTabBlock( BlockACAsteroids.class, fmlPreInitializationEvent ); // AC Asteroids
        initTabBlock( BlockACBattleZone.class, fmlPreInitializationEvent ); // AC Battle Zone
        initTabBlock( BlockACCentipede.class, fmlPreInitializationEvent ); // AC Centipede
        initTabBlock( BlockACGalaga.class, fmlPreInitializationEvent ); // AC Galaga
        initTabBlock( BlockACMisCmd.class, fmlPreInitializationEvent ); // AC Missile Command
        initTabBlock( BlockACPacMan.class, fmlPreInitializationEvent ); // AC PacMan
        initTabBlock( BlockACTempest.class, fmlPreInitializationEvent ); // AC Tempest
        initTabBlock( BlockAirHockeyTable.class, fmlPreInitializationEvent ); // Air Hockey Table
        initTabBlock( BlockHd.class, fmlPreInitializationEvent ); // Hand Dryer
        initTabBlock( BlockOldRecordPlayer.class, fmlPreInitializationEvent ); // Old Record Player
        initTabBlock( BlockPingPongTable.class, fmlPreInitializationEvent ); // Ping Pong Table
        initTabBlock( BlockPSHawkA97.class, fmlPreInitializationEvent ); // Player Statue HawkA97
        initTabBlock( BlockPSPapaGinos.class, fmlPreInitializationEvent ); // Player Statue PapaGinos
        initTabBlock( BlockPSThatCrazyPandog.class, fmlPreInitializationEvent ); // Player Statue ThatCrazyPandog
        initTabBlock( BlockWaterDispenser.class, fmlPreInitializationEvent ); // Water Dispenser
        initTabBlock( BlockWbs.class, fmlPreInitializationEvent ); // Water Bubbler (Short)
        initTabBlock( BlockWbt.class, fmlPreInitializationEvent ); // Water Bubbler (Tall)
    }
}
