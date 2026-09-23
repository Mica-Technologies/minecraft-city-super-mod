package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockBirdbath;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockLargeFlowerPot;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockParkSwingA;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockParkSwingB;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockParkTrashCan;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockRoundFlowerPot;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockTeeterTotter;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockWbs;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockWbt;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Parks tab of the Parks &amp; Greenery module: benches, tables, bins, playground pieces,
 * fountains, irrigation and the other amenities of a park.
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 19)
public class CsmTabParks extends CsmTab {

  @Override
  public String getTabId() {
    return "tabparks";
  }

  @Override
  public Block getTabIcon() {
    return Blocks.RED_FLOWER;
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
    // Playground, drinking fountains and planters, moved here from Furniture & Novelties with
    // their registry names unchanged.
    initTabBlock(BlockParkSwingA.class, fmlPreInitializationEvent); // Park Swing A
    initTabBlock(BlockParkSwingB.class, fmlPreInitializationEvent); // Park Swing B
    initTabBlock(BlockTeeterTotter.class, fmlPreInitializationEvent); // Teeter Totter
    initTabBlock(BlockParkTrashCan.class, fmlPreInitializationEvent); // Park Trash Can
    initTabBlock(BlockWbs.class, fmlPreInitializationEvent); // Water Bubbler (Short)
    initTabBlock(BlockWbt.class, fmlPreInitializationEvent); // Water Bubbler (Tall)
    initTabBlock(BlockBirdbath.class, fmlPreInitializationEvent); // Bird Bath
    initTabBlock(BlockLargeFlowerPot.class, fmlPreInitializationEvent); // Large Flower Pot
    initTabBlock(BlockRoundFlowerPot.class, fmlPreInitializationEvent); // Round Flower Pot
  }
}
