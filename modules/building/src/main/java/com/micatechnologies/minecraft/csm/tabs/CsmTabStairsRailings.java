package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.buildingmaterials.stairs.BlockStairFlight;
import com.micatechnologies.minecraft.csm.buildingmaterials.stairs.BlockStairRailing;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for stairs and what runs up them: stairs, and the railings, guards and wall handrails
 * that follow a flight and its landings. The tab's lines are written by
 * {@code dev-env-utils/scripts/gen_stairs_railings.py --fragments}.
 *
 * @since 2026.10
 */
@CsmTab.Load(order = 33)
public class CsmTabStairsRailings extends CsmTab {

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public String getTabId() {
    return "tabstairsrailings";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("railing_picket_black");
  }

  @Override
  public boolean getTabSearchable() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(new BlockStairFlight("stairs_steel_pan_grey", false));
    initTabBlock(new BlockStairFlight("stairs_concrete_nosing", false));
    initTabBlock(new BlockStairFlight("stairs_open_tread_oak", true));
    initTabBlock(new BlockStairFlight("stairs_residential_oak", true));
    initTabBlock(new BlockStairFlight("stairs_residential_walnut", true));
    initTabBlock(new BlockStairFlight("stairs_carpet_grey", true));
    initTabBlock(new BlockStairRailing("railing_picket_black", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_picket_white", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_picket_grey", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_picket_bronze", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_picket_galvanized", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_picket_round_black", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_picket_round_grey", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_picket_round_beige", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_pipe_black", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_pipe_galvanized", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_pipe_yellow", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_cable_stainless", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_cable_black_oak", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_cable_black", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_glass_stainless", BlockStairRailing.Kind.GLASS, false));
    initTabBlock(new BlockStairRailing("railing_glass_black", BlockStairRailing.Kind.GLASS, false));
    initTabBlock(new BlockStairRailing("handrail_wall_stainless_round", BlockStairRailing.Kind.WALL, false));
    initTabBlock(new BlockStairRailing("handrail_wall_black_square", BlockStairRailing.Kind.WALL, false));
    initTabBlock(new BlockStairRailing("handrail_wall_oak_round", BlockStairRailing.Kind.WALL, true));
    initTabBlock(new BlockStairRailing("handrail_wall_white_round", BlockStairRailing.Kind.WALL, false));
    initTabBlock(new BlockStairRailing("railing_baluster_oak", BlockStairRailing.Kind.GUARD, true));
    initTabBlock(new BlockStairRailing("railing_baluster_walnut", BlockStairRailing.Kind.GUARD, true));
    initTabBlock(new BlockStairRailing("railing_baluster_black", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_baluster_white", BlockStairRailing.Kind.GUARD, false));
    initTabBlock(new BlockStairRailing("railing_turned_oak", BlockStairRailing.Kind.GUARD, true));
    initTabBlock(new BlockStairRailing("railing_turned_walnut", BlockStairRailing.Kind.GUARD, true));
    initTabBlock(new BlockStairRailing("railing_turned_cherry", BlockStairRailing.Kind.GUARD, true));
  }
}
