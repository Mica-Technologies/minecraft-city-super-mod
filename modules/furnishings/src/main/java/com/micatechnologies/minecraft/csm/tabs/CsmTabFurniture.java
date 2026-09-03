package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.BlockAnchor;
import com.micatechnologies.minecraft.csm.furniture.BlockAppleCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockBananaCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockBarbedWire;
import com.micatechnologies.minecraft.csm.furniture.BlockBeerRack;
import com.micatechnologies.minecraft.csm.furniture.BlockBeertap;
import com.micatechnologies.minecraft.csm.furniture.BlockBeetCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockBirdbath;
import com.micatechnologies.minecraft.csm.furniture.BlockBirdhouse;
import com.micatechnologies.minecraft.csm.furniture.BlockBoardedWoodPlanks;
import com.micatechnologies.minecraft.csm.furniture.BlockCarrotBarrel;
import com.micatechnologies.minecraft.csm.furniture.BlockCarrotCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockChains;
import com.micatechnologies.minecraft.csm.furniture.BlockCoatrack;
import com.micatechnologies.minecraft.csm.furniture.BlockCookooClock;
import com.micatechnologies.minecraft.csm.furniture.BlockCornCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockCowhide;
import com.micatechnologies.minecraft.csm.furniture.BlockCsmHarp;
import com.micatechnologies.minecraft.csm.furniture.BlockCsmJukebox;
import com.micatechnologies.minecraft.csm.furniture.BlockCsmRadiator;
import com.micatechnologies.minecraft.csm.furniture.BlockCuttingBoard;
import com.micatechnologies.minecraft.csm.furniture.BlockDoghouse;
import com.micatechnologies.minecraft.csm.furniture.BlockFireHydrant;
import com.micatechnologies.minecraft.csm.furniture.BlockFoodProcessor;
import com.micatechnologies.minecraft.csm.furniture.BlockGoldenApples;
import com.micatechnologies.minecraft.csm.furniture.BlockGrandPiano;
import com.micatechnologies.minecraft.csm.furniture.BlockGrandfatherClock;
import com.micatechnologies.minecraft.csm.furniture.BlockGreenAppleCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockHoneypot;
import com.micatechnologies.minecraft.csm.furniture.BlockHottub;
import com.micatechnologies.minecraft.csm.furniture.BlockHourglass;
import com.micatechnologies.minecraft.csm.furniture.BlockHummingbirdFeeder;
import com.micatechnologies.minecraft.csm.furniture.BlockLargeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockLargeFlowerPot;
import com.micatechnologies.minecraft.csm.furniture.BlockLettuceCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockOfficeChair;
import com.micatechnologies.minecraft.csm.furniture.BlockOnionCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockOrangeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockParkSwingA;
import com.micatechnologies.minecraft.csm.furniture.BlockParkSwingB;
import com.micatechnologies.minecraft.csm.furniture.BlockParkTrashCan;
import com.micatechnologies.minecraft.csm.furniture.BlockPearCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockPhonograph;
import com.micatechnologies.minecraft.csm.furniture.BlockPlunger;
import com.micatechnologies.minecraft.csm.furniture.BlockPotatoeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockRestroomSignFemale;
import com.micatechnologies.minecraft.csm.furniture.BlockRestroomSignMale;
import com.micatechnologies.minecraft.csm.furniture.BlockRoundFlowerPot;
import com.micatechnologies.minecraft.csm.furniture.BlockSilverware;
import com.micatechnologies.minecraft.csm.furniture.BlockSmallAnchor;
import com.micatechnologies.minecraft.csm.furniture.BlockSwingchair;
import com.micatechnologies.minecraft.csm.furniture.BlockTallWallMirror;
import com.micatechnologies.minecraft.csm.furniture.BlockTeeterTotter;
import com.micatechnologies.minecraft.csm.furniture.BlockTelescope;
import com.micatechnologies.minecraft.csm.furniture.BlockTikiTorch;
import com.micatechnologies.minecraft.csm.furniture.BlockTomatoeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockUtensilHooks;
import com.micatechnologies.minecraft.csm.furniture.BlockVintageSewingMachine;
import com.micatechnologies.minecraft.csm.furniture.BlockWaterBucket;
import com.micatechnologies.minecraft.csm.furniture.BlockWaterPump;
import com.micatechnologies.minecraft.csm.furniture.BlockWindChime;
import com.micatechnologies.minecraft.csm.furniture.BlockWineRack;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for furniture and household blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 11)
public class CsmTabFurniture extends CsmTab {

  @Override
  public String getTabId() {
    return "tabfurniture";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("grandpiano");
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
    // Furniture & Household
    initTabBlock(BlockCoatrack.class, fmlPreInitializationEvent); // Coat Rack
    initTabBlock(BlockCookooClock.class, fmlPreInitializationEvent); // Cuckoo Clock
    initTabBlock(BlockCowhide.class, fmlPreInitializationEvent); // Cowhide
    initTabBlock(BlockCsmHarp.class, fmlPreInitializationEvent); // Harp
    initTabBlock(BlockCsmJukebox.class, fmlPreInitializationEvent); // Jukebox
    initTabBlock(BlockCsmRadiator.class, fmlPreInitializationEvent); // Radiator
    initTabBlock(BlockGrandPiano.class, fmlPreInitializationEvent); // Grand Piano
    initTabBlock(BlockGrandfatherClock.class, fmlPreInitializationEvent); // Grandfather Clock
    initTabBlock(BlockHottub.class, fmlPreInitializationEvent); // Hot Tub
    initTabBlock(BlockOfficeChair.class, fmlPreInitializationEvent); // Office Chair
    initTabBlock(BlockPhonograph.class, fmlPreInitializationEvent); // Phonograph
    initTabBlock(BlockSwingchair.class, fmlPreInitializationEvent); // Swing Chair
    initTabBlock(BlockTallWallMirror.class, fmlPreInitializationEvent); // Tall Wall Mirror
    initTabBlock(BlockTelescope.class, fmlPreInitializationEvent); // Telescope
    initTabBlock(BlockVintageSewingMachine.class, fmlPreInitializationEvent); // Vintage Sewing Machine

    // Kitchen & Dining
    initTabBlock(BlockBeerRack.class, fmlPreInitializationEvent); // Beer Rack
    initTabBlock(BlockBeertap.class, fmlPreInitializationEvent); // Beer Tap
    initTabBlock(BlockCuttingBoard.class, fmlPreInitializationEvent); // Cutting Board
    initTabBlock(BlockFoodProcessor.class, fmlPreInitializationEvent); // Food Processor
    initTabBlock(BlockSilverware.class, fmlPreInitializationEvent); // Silverware
    initTabBlock(BlockUtensilHooks.class, fmlPreInitializationEvent); // Utensil Hooks
    initTabBlock(BlockWineRack.class, fmlPreInitializationEvent); // Wine Rack

    // Outdoor & Garden
    initTabBlock(BlockBarbedWire.class, fmlPreInitializationEvent); // Barbed Wire
    initTabBlock(BlockBirdbath.class, fmlPreInitializationEvent); // Bird Bath
    initTabBlock(BlockBirdhouse.class, fmlPreInitializationEvent); // Birdhouse
    initTabBlock(BlockDoghouse.class, fmlPreInitializationEvent); // Doghouse
    initTabBlock(BlockFireHydrant.class, fmlPreInitializationEvent); // Fire Hydrant
    initTabBlock(BlockHummingbirdFeeder.class, fmlPreInitializationEvent); // Hummingbird Feeder
    initTabBlock(BlockLargeFlowerPot.class, fmlPreInitializationEvent); // Large Flower Pot
    initTabBlock(BlockParkSwingA.class, fmlPreInitializationEvent); // Park Swing A
    initTabBlock(BlockParkSwingB.class, fmlPreInitializationEvent); // Park Swing B
    initTabBlock(BlockParkTrashCan.class, fmlPreInitializationEvent); // Park Trash Can
    initTabBlock(BlockRoundFlowerPot.class, fmlPreInitializationEvent); // Round Flower Pot
    initTabBlock(BlockTeeterTotter.class, fmlPreInitializationEvent); // Teeter Totter
    initTabBlock(BlockTikiTorch.class, fmlPreInitializationEvent); // Tiki Torch
    initTabBlock(BlockWaterBucket.class, fmlPreInitializationEvent); // Water Bucket
    initTabBlock(BlockWaterPump.class, fmlPreInitializationEvent); // Water Pump
    initTabBlock(BlockWindChime.class, fmlPreInitializationEvent); // Wind Chime

    // Storage & Produce
    initTabBlock(BlockAppleCrate.class, fmlPreInitializationEvent); // Apple Crate
    initTabBlock(BlockBananaCrate.class, fmlPreInitializationEvent); // Banana Crate
    initTabBlock(BlockBeetCrate.class, fmlPreInitializationEvent); // Beet Crate
    initTabBlock(BlockCarrotBarrel.class, fmlPreInitializationEvent); // Carrot Barrel
    initTabBlock(BlockCarrotCrate.class, fmlPreInitializationEvent); // Carrot Crate
    initTabBlock(BlockCornCrate.class, fmlPreInitializationEvent); // Corn Crate
    initTabBlock(BlockGoldenApples.class, fmlPreInitializationEvent); // Golden Apples
    initTabBlock(BlockGreenAppleCrate.class, fmlPreInitializationEvent); // Green Apple Crate
    initTabBlock(BlockLargeCrate.class, fmlPreInitializationEvent); // Large Crate
    initTabBlock(BlockLettuceCrate.class, fmlPreInitializationEvent); // Lettuce Crate
    initTabBlock(BlockOnionCrate.class, fmlPreInitializationEvent); // Onion Crate
    initTabBlock(BlockOrangeCrate.class, fmlPreInitializationEvent); // Orange Crate
    initTabBlock(BlockPearCrate.class, fmlPreInitializationEvent); // Pear Crate
    initTabBlock(BlockPotatoeCrate.class, fmlPreInitializationEvent); // Potato Crate
    initTabBlock(BlockTomatoeCrate.class, fmlPreInitializationEvent); // Tomato Crate

    // Miscellaneous
    initTabBlock(BlockAnchor.class, fmlPreInitializationEvent); // Anchor
    initTabBlock(BlockBoardedWoodPlanks.class, fmlPreInitializationEvent); // Boarded Wood Planks
    initTabBlock(BlockChains.class, fmlPreInitializationEvent); // Chains
    initTabBlock(BlockHoneypot.class, fmlPreInitializationEvent); // Honey Pot
    initTabBlock(BlockHourglass.class, fmlPreInitializationEvent); // Hourglass
    initTabBlock(BlockPlunger.class, fmlPreInitializationEvent); // Plunger
    initTabBlock(BlockRestroomSignFemale.class, fmlPreInitializationEvent); // Restroom Sign (Female)
    initTabBlock(BlockRestroomSignMale.class, fmlPreInitializationEvent); // Restroom Sign (Male)
    initTabBlock(BlockSmallAnchor.class, fmlPreInitializationEvent); // Small Anchor
  }
}
