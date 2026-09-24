package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.BlockAppleCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockBananaCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockBarbedWire;
import com.micatechnologies.minecraft.csm.furniture.BlockBeerRack;
import com.micatechnologies.minecraft.csm.furniture.BlockBeertap;
import com.micatechnologies.minecraft.csm.furniture.BlockBeetCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockBirdhouse;
import com.micatechnologies.minecraft.csm.furniture.BlockBoardedWoodPlanks;
import com.micatechnologies.minecraft.csm.furniture.BlockCarrotBarrel;
import com.micatechnologies.minecraft.csm.furniture.BlockCarrotCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockChains;
import com.micatechnologies.minecraft.csm.furniture.BlockCoatrack;
import com.micatechnologies.minecraft.csm.furniture.BlockCornCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockCsmJukebox;
import com.micatechnologies.minecraft.csm.furniture.BlockCsmRadiator;
import com.micatechnologies.minecraft.csm.furniture.BlockDoghouse;
import com.micatechnologies.minecraft.csm.furniture.BlockGoldenApples;
import com.micatechnologies.minecraft.csm.furniture.BlockGrandPiano;
import com.micatechnologies.minecraft.csm.furniture.BlockGrandfatherClock;
import com.micatechnologies.minecraft.csm.furniture.BlockGreenAppleCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockHottub;
import com.micatechnologies.minecraft.csm.furniture.BlockLargeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockLettuceCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockOfficeChair;
import com.micatechnologies.minecraft.csm.furniture.BlockOnionCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockOrangeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockPearCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockPotatoeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockRestroomSignFemale;
import com.micatechnologies.minecraft.csm.furniture.BlockRestroomSignMale;
import com.micatechnologies.minecraft.csm.furniture.BlockSwingchair;
import com.micatechnologies.minecraft.csm.furniture.BlockTallWallMirror;
import com.micatechnologies.minecraft.csm.furniture.BlockTomatoeCrate;
import com.micatechnologies.minecraft.csm.furniture.BlockWaterPump;
import com.micatechnologies.minecraft.csm.furniture.BlockWineRack;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for furniture and household blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 12)
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
    initTabBlock(BlockCsmJukebox.class, fmlPreInitializationEvent); // Jukebox
    initTabBlock(BlockCsmRadiator.class, fmlPreInitializationEvent); // Radiator
    initTabBlock(BlockGrandPiano.class, fmlPreInitializationEvent); // Grand Piano
    initTabBlock(BlockGrandfatherClock.class, fmlPreInitializationEvent); // Grandfather Clock
    initTabBlock(BlockHottub.class, fmlPreInitializationEvent); // Hot Tub
    initTabBlock(BlockOfficeChair.class, fmlPreInitializationEvent); // Office Chair
    initTabBlock(BlockSwingchair.class, fmlPreInitializationEvent); // Swing Chair
    initTabBlock(BlockTallWallMirror.class, fmlPreInitializationEvent); // Tall Wall Mirror

    // Kitchen & Dining
    initTabBlock(BlockBeerRack.class, fmlPreInitializationEvent); // Beer Rack
    initTabBlock(BlockBeertap.class, fmlPreInitializationEvent); // Beer Tap
    initTabBlock(new BlockRotatableNSEWUDFactory("woodenbarrel", Material.WOOD, SoundType.WOOD, "axe", 0, 2F, 5F, 0F, 0, new AxisAlignedBB(0.200000, 0.000000, 0.000000, 0.800000, 0.800000, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false)); // Wooden Barrel
    initTabBlock(BlockWineRack.class, fmlPreInitializationEvent); // Wine Rack

    // Outdoor & Garden
    initTabBlock(BlockBarbedWire.class, fmlPreInitializationEvent); // Barbed Wire
    initTabBlock(BlockBirdhouse.class, fmlPreInitializationEvent); // Birdhouse
    initTabBlock(BlockDoghouse.class, fmlPreInitializationEvent); // Doghouse
    initTabBlock(BlockWaterPump.class, fmlPreInitializationEvent); // Water Pump

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
    initTabBlock(BlockBoardedWoodPlanks.class, fmlPreInitializationEvent); // Boarded Wood Planks
    initTabBlock(BlockChains.class, fmlPreInitializationEvent); // Chains
    initTabBlock(BlockRestroomSignFemale.class, fmlPreInitializationEvent); // Restroom Sign (Female)
    initTabBlock(BlockRestroomSignMale.class, fmlPreInitializationEvent); // Restroom Sign (Male)
  }
}
