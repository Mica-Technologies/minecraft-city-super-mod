package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockBirdbath;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockIrrigationController;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockLargeFlowerPot;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockParkBench;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockParkSwingA;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockParkSwingB;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockParkTrashCan;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockRoundFlowerPot;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockSprinkler;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockTeeterTotter;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockWbs;
import com.micatechnologies.minecraft.csm.parks.amenities.BlockWbt;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkFacing;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkJoining;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkProp;
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

    // Benches, bins, playground, pergola, fountains and irrigation, written by
    // gen_park_amenities.py (--fragments).
    initTabBlock(new BlockParkBench("park_bench_wood", new int[]{0, 0, 1, 16, 15, 12}));
    initTabBlock(new BlockParkBench("park_bench_backless", new int[]{0, 0, 2, 16, 8, 14}));
    initTabBlock(new BlockParkBench("park_bench_steel", new int[]{0, 0, 1, 16, 15, 12}));
    initTabBlock(new BlockParkBench("picnic_table_wood", new int[]{0, 0, 0, 16, 12, 16}));
    initTabBlock(new BlockParkBench("picnic_table_steel", new int[]{0, 0, 0, 16, 12, 16}));
    initTabBlock(new BlockParkFacing("recycling_bin", new int[]{3, 0, 3, 13, 15, 13}, true));
    initTabBlock(new BlockParkFacing("trash_recycling_station", new int[]{1, 0, 3, 15, 15, 13}, true));
    initTabBlock(new BlockParkFacing("dog_waste_station", new int[]{4, 0, 4, 12, 16, 13}, true));
    initTabBlock(new BlockParkFacing("playground_slide", new int[]{2, 0, 0, 14, 16, 16}, true));
    initTabBlock(new BlockParkFacing("spring_rider", new int[]{4, 0, 2, 12, 14, 14}, true));
    initTabBlock(new BlockParkProp("pergola_post", BlockParkProp.Kind.POST, 16, 5));
    initTabBlock(new BlockParkJoining("pergola_top", BlockParkJoining.Kind.PERGOLA, 6, 16));
    initTabBlock(new BlockParkJoining("fountain_basin", BlockParkJoining.Kind.BED, 11, 16));
    initTabBlock(new BlockParkProp("fountain_tiered", BlockParkProp.Kind.PLANTER, 16, 2));
    initTabBlock(BlockSprinkler.class, fmlPreInitializationEvent);
    initTabBlock(BlockIrrigationController.class, fmlPreInitializationEvent);
    initTabBlock(new BlockParkFacing("backflow_preventer", new int[]{2, 0, 3, 14, 14, 13}, true));
    initTabBlock(new BlockParkProp("ground_rubber_safety", BlockParkProp.Kind.COVER, 1, 0));
  }
}
