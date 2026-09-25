package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.BlockBarbedWire;
import com.micatechnologies.minecraft.csm.furniture.BlockBeerRack;
import com.micatechnologies.minecraft.csm.furniture.BlockBeertap;
import com.micatechnologies.minecraft.csm.furniture.BlockBirdhouse;
import com.micatechnologies.minecraft.csm.furniture.BlockBoardedWoodPlanks;
import com.micatechnologies.minecraft.csm.furniture.BlockChains;
import com.micatechnologies.minecraft.csm.furniture.BlockCoatrack;
import com.micatechnologies.minecraft.csm.furniture.BlockCsmJukebox;
import com.micatechnologies.minecraft.csm.furniture.BlockCsmRadiator;
import com.micatechnologies.minecraft.csm.furniture.BlockDoghouse;
import com.micatechnologies.minecraft.csm.furniture.BlockGrandPiano;
import com.micatechnologies.minecraft.csm.furniture.BlockGrandfatherClock;
import com.micatechnologies.minecraft.csm.furniture.BlockHottub;
import com.micatechnologies.minecraft.csm.furniture.BlockOfficeChair;
import com.micatechnologies.minecraft.csm.furniture.BlockRestroomSignFemale;
import com.micatechnologies.minecraft.csm.furniture.BlockRestroomSignMale;
import com.micatechnologies.minecraft.csm.furniture.BlockSwingchair;
import com.micatechnologies.minecraft.csm.furniture.BlockTallWallMirror;
import com.micatechnologies.minecraft.csm.furniture.BlockWaterPump;
import com.micatechnologies.minecraft.csm.furniture.BlockWineRack;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for furniture and household blocks. (The produce crates moved to the Market &amp;
 * Store tab.)
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

    // Miscellaneous
    initTabBlock(BlockBoardedWoodPlanks.class, fmlPreInitializationEvent); // Boarded Wood Planks
    initTabBlock(BlockChains.class, fmlPreInitializationEvent); // Chains
    initTabBlock(BlockRestroomSignFemale.class, fmlPreInitializationEvent); // Restroom Sign (Female)
    initTabBlock(BlockRestroomSignMale.class, fmlPreInitializationEvent); // Restroom Sign (Male)
  }
}
