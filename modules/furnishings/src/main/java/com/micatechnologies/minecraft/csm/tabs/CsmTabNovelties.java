package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.BlockBarberpole;
import com.micatechnologies.minecraft.csm.furniture.BlockCmasTree;
import com.micatechnologies.minecraft.csm.furniture.BlockCmasWreath;
import com.micatechnologies.minecraft.csm.furniture.BlockCookies;
import com.micatechnologies.minecraft.csm.furniture.BlockGardenFlamingo;
import com.micatechnologies.minecraft.csm.furniture.BlockSnowman;
import com.micatechnologies.minecraft.csm.novelties.BlockCoffeeCup;
import com.micatechnologies.minecraft.csm.novelties.BlockHd;
import com.micatechnologies.minecraft.csm.novelties.BlockNutcracker;
import com.micatechnologies.minecraft.csm.novelties.BlockOldRecordPlayer;
import com.micatechnologies.minecraft.csm.novelties.BlockSonyDreamMachine;
import com.micatechnologies.minecraft.csm.novelties.BlockPSHawkA97;
import com.micatechnologies.minecraft.csm.novelties.BlockPSPapaGinos;
import com.micatechnologies.minecraft.csm.novelties.BlockPSThatCrazyPandog;
import com.micatechnologies.minecraft.csm.novelties.BlockPicnicBasket;
import com.micatechnologies.minecraft.csm.novelties.BlockPumpkins;
import com.micatechnologies.minecraft.csm.novelties.BlockScarecrow;
import com.micatechnologies.minecraft.csm.novelties.BlockWaterDispenser;
import com.micatechnologies.minecraft.csm.novelties.BlockXylophone;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for novelty blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 5)
public class CsmTabNovelties extends CsmTab {

  @Override
  public String getTabId() {
    return "tabnovelties";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("nutcracker");
  }

  @Override
  public boolean getTabSearchable() {
    return false;
  }

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // Seasonal / Holiday
    initTabBlock(BlockBarberpole.class, fmlPreInitializationEvent); // Barber Pole
    initTabBlock(BlockCmasTree.class, fmlPreInitializationEvent); // Christmas Tree
    initTabBlock(BlockCmasWreath.class, fmlPreInitializationEvent); // Christmas Wreath
    initTabBlock(BlockCookies.class, fmlPreInitializationEvent); // Cookies
    initTabBlock(BlockPumpkins.class, fmlPreInitializationEvent); // Pumpkins
    initTabBlock(BlockScarecrow.class, fmlPreInitializationEvent); // Scarecrow
    initTabBlock(BlockSnowman.class, fmlPreInitializationEvent); // Snowman

    // Collectibles / Figurines
    initTabBlock(BlockGardenFlamingo.class, fmlPreInitializationEvent); // Garden Flamingo
    initTabBlock(BlockNutcracker.class, fmlPreInitializationEvent); // Nutcracker

    // Decorative / Misc
    initTabBlock(BlockCoffeeCup.class, fmlPreInitializationEvent); // Coffee Cup
    initTabBlock(BlockHd.class, fmlPreInitializationEvent); // Hand Dryer
    initTabBlock(BlockOldRecordPlayer.class, fmlPreInitializationEvent); // Old Record Player
    initTabBlock(BlockSonyDreamMachine.class, fmlPreInitializationEvent); // Sony Dream Machine
    initTabBlock(BlockPicnicBasket.class, fmlPreInitializationEvent); // Picnic Basket
    initTabBlock(BlockPSHawkA97.class, fmlPreInitializationEvent); // Player Statue HawkA97
    initTabBlock(BlockPSPapaGinos.class, fmlPreInitializationEvent); // Player Statue PapaGinos
    initTabBlock(BlockPSThatCrazyPandog.class,
        fmlPreInitializationEvent); // Player Statue ThatCrazyPandog
    initTabBlock(BlockWaterDispenser.class, fmlPreInitializationEvent); // Water Dispenser
    initTabBlock(BlockXylophone.class, fmlPreInitializationEvent); // Xylophone
  }
}
