package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.BlockBillardTable;
import com.micatechnologies.minecraft.csm.furniture.BlockCardDeck;
import com.micatechnologies.minecraft.csm.furniture.BlockDartBoard;
import com.micatechnologies.minecraft.csm.furniture.BlockDollhouse1;
import com.micatechnologies.minecraft.csm.furniture.BlockDollhouse2;
import com.micatechnologies.minecraft.csm.furniture.BlockEtchASketch;
import com.micatechnologies.minecraft.csm.furniture.BlockPlayingCards;
import com.micatechnologies.minecraft.csm.furniture.BlockToyboxBoy;
import com.micatechnologies.minecraft.csm.furniture.BlockToyboxGirl;
import com.micatechnologies.minecraft.csm.novelties.BlockACAsteroids;
import com.micatechnologies.minecraft.csm.novelties.BlockACBattleZone;
import com.micatechnologies.minecraft.csm.novelties.BlockACCentipede;
import com.micatechnologies.minecraft.csm.novelties.BlockACGalaga;
import com.micatechnologies.minecraft.csm.novelties.BlockACMisCmd;
import com.micatechnologies.minecraft.csm.novelties.BlockACPacMan;
import com.micatechnologies.minecraft.csm.novelties.BlockACTempest;
import com.micatechnologies.minecraft.csm.novelties.BlockAirHockeyTable;
import com.micatechnologies.minecraft.csm.novelties.BlockPingPongTable;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for games, toys, and entertainment blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 12)
public class CsmTabGaming extends CsmTab {

  @Override
  public String getTabId() {
    return "tabgaming";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("airhockeytable");
  }

  @Override
  public boolean getTabSearchable() {
    return true;
  }

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // Arcade Cabinets
    initTabBlock(BlockACAsteroids.class, fmlPreInitializationEvent); // AC Asteroids
    initTabBlock(BlockACBattleZone.class, fmlPreInitializationEvent); // AC Battle Zone
    initTabBlock(BlockACCentipede.class, fmlPreInitializationEvent); // AC Centipede
    initTabBlock(BlockACGalaga.class, fmlPreInitializationEvent); // AC Galaga
    initTabBlock(BlockACMisCmd.class, fmlPreInitializationEvent); // AC Missile Command
    initTabBlock(BlockACPacMan.class, fmlPreInitializationEvent); // AC PacMan
    initTabBlock(BlockACTempest.class, fmlPreInitializationEvent); // AC Tempest

    // Game Tables
    initTabBlock(BlockAirHockeyTable.class, fmlPreInitializationEvent); // Air Hockey Table
    initTabBlock(BlockBillardTable.class, fmlPreInitializationEvent); // Billiard Table
    initTabBlock(BlockPingPongTable.class, fmlPreInitializationEvent); // Ping Pong Table

    // Games & Toys
    initTabBlock(BlockCardDeck.class, fmlPreInitializationEvent); // Card Deck
    initTabBlock(BlockDartBoard.class, fmlPreInitializationEvent); // Dart Board
    initTabBlock(BlockDollhouse1.class, fmlPreInitializationEvent); // Dollhouse 1
    initTabBlock(BlockDollhouse2.class, fmlPreInitializationEvent); // Dollhouse 2
    initTabBlock(BlockEtchASketch.class, fmlPreInitializationEvent); // Etch A Sketch
    initTabBlock(BlockPlayingCards.class, fmlPreInitializationEvent); // Playing Cards
    initTabBlock(BlockToyboxBoy.class, fmlPreInitializationEvent); // Toybox (Boy)
    initTabBlock(BlockToyboxGirl.class, fmlPreInitializationEvent); // Toybox (Girl)
  }
}
