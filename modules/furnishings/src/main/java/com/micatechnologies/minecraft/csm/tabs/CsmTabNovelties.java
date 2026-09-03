package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.BlockBarberpole;
import com.micatechnologies.minecraft.csm.furniture.BlockCmasTree;
import com.micatechnologies.minecraft.csm.furniture.BlockCmasWreath;
import com.micatechnologies.minecraft.csm.furniture.BlockCookies;
import com.micatechnologies.minecraft.csm.furniture.BlockElf;
import com.micatechnologies.minecraft.csm.furniture.BlockGardenFlamingo;
import com.micatechnologies.minecraft.csm.furniture.BlockMiniCmasTree;
import com.micatechnologies.minecraft.csm.furniture.BlockPresents;
import com.micatechnologies.minecraft.csm.furniture.BlockReindeer;
import com.micatechnologies.minecraft.csm.furniture.BlockSnowglobe;
import com.micatechnologies.minecraft.csm.furniture.BlockSnowman;
import com.micatechnologies.minecraft.csm.furniture.BlockTardis;
import com.micatechnologies.minecraft.csm.furniture.BlockTreasureChest;
import com.micatechnologies.minecraft.csm.novelties.BlockCoffeeCup;
import com.micatechnologies.minecraft.csm.novelties.BlockCreeperPlush;
import com.micatechnologies.minecraft.csm.novelties.BlockGardenGnome;
import com.micatechnologies.minecraft.csm.novelties.BlockGoldBars;
import com.micatechnologies.minecraft.csm.novelties.BlockGoldenFurAwardsTrophy;
import com.micatechnologies.minecraft.csm.novelties.BlockHd;
import com.micatechnologies.minecraft.csm.novelties.BlockNutcracker;
import com.micatechnologies.minecraft.csm.novelties.BlockOldRecordPlayer;
import com.micatechnologies.minecraft.csm.novelties.BlockPSHawkA97;
import com.micatechnologies.minecraft.csm.novelties.BlockPSPapaGinos;
import com.micatechnologies.minecraft.csm.novelties.BlockPSThatCrazyPandog;
import com.micatechnologies.minecraft.csm.novelties.BlockPicnicBasket;
import com.micatechnologies.minecraft.csm.novelties.BlockPumpkins;
import com.micatechnologies.minecraft.csm.novelties.BlockR2d2;
import com.micatechnologies.minecraft.csm.novelties.BlockRubixCube;
import com.micatechnologies.minecraft.csm.novelties.BlockScarecrow;
import com.micatechnologies.minecraft.csm.novelties.BlockShootingDummy;
import com.micatechnologies.minecraft.csm.novelties.BlockSinglePumpkin;
import com.micatechnologies.minecraft.csm.novelties.BlockWaterDispenser;
import com.micatechnologies.minecraft.csm.novelties.BlockWbs;
import com.micatechnologies.minecraft.csm.novelties.BlockWbt;
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
    return CsmRegistry.getBlock("creeperplush");
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
    initTabBlock(BlockElf.class, fmlPreInitializationEvent); // Elf
    initTabBlock(BlockMiniCmasTree.class, fmlPreInitializationEvent); // Mini Christmas Tree
    initTabBlock(BlockPresents.class, fmlPreInitializationEvent); // Presents
    initTabBlock(BlockPumpkins.class, fmlPreInitializationEvent); // Pumpkins
    initTabBlock(BlockReindeer.class, fmlPreInitializationEvent); // Reindeer
    initTabBlock(BlockScarecrow.class, fmlPreInitializationEvent); // Scarecrow
    initTabBlock(BlockSinglePumpkin.class, fmlPreInitializationEvent); // Single Pumpkin
    initTabBlock(BlockSnowglobe.class, fmlPreInitializationEvent); // Snow Globe
    initTabBlock(BlockSnowman.class, fmlPreInitializationEvent); // Snowman

    // Collectibles / Figurines
    initTabBlock(BlockCreeperPlush.class, fmlPreInitializationEvent); // Creeper Plush
    initTabBlock(BlockGardenFlamingo.class, fmlPreInitializationEvent); // Garden Flamingo
    initTabBlock(BlockGardenGnome.class, fmlPreInitializationEvent); // Garden Gnome
    initTabBlock(BlockGoldBars.class, fmlPreInitializationEvent); // Gold Bars
    initTabBlock(BlockGoldenFurAwardsTrophy.class, fmlPreInitializationEvent); // Golden Fur Awards Trophy
    initTabBlock(BlockNutcracker.class, fmlPreInitializationEvent); // Nutcracker
    initTabBlock(BlockR2d2.class, fmlPreInitializationEvent); // R2-D2
    initTabBlock(BlockRubixCube.class, fmlPreInitializationEvent); // Rubix Cube
    initTabBlock(BlockShootingDummy.class, fmlPreInitializationEvent); // Shooting Dummy
    initTabBlock(BlockTardis.class, fmlPreInitializationEvent); // TARDIS
    initTabBlock(BlockTreasureChest.class, fmlPreInitializationEvent); // Treasure Chest

    // Decorative / Misc
    initTabBlock(BlockCoffeeCup.class, fmlPreInitializationEvent); // Coffee Cup
    initTabBlock(BlockHd.class, fmlPreInitializationEvent); // Hand Dryer
    initTabBlock(BlockOldRecordPlayer.class, fmlPreInitializationEvent); // Old Record Player
    initTabBlock(BlockPicnicBasket.class, fmlPreInitializationEvent); // Picnic Basket
    initTabBlock(BlockPSHawkA97.class, fmlPreInitializationEvent); // Player Statue HawkA97
    initTabBlock(BlockPSPapaGinos.class, fmlPreInitializationEvent); // Player Statue PapaGinos
    initTabBlock(BlockPSThatCrazyPandog.class,
        fmlPreInitializationEvent); // Player Statue ThatCrazyPandog
    initTabBlock(BlockWaterDispenser.class, fmlPreInitializationEvent); // Water Dispenser
    initTabBlock(BlockWbs.class, fmlPreInitializationEvent); // Water Bubbler (Short)
    initTabBlock(BlockWbt.class, fmlPreInitializationEvent); // Water Bubbler (Tall)
    initTabBlock(BlockXylophone.class, fmlPreInitializationEvent); // Xylophone
  }
}
